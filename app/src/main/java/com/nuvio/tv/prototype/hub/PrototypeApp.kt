package com.nuvio.tv.prototype.hub

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.nuvio.tv.prototype.shared.LocalProtoBack
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoBackDispatcher
import com.nuvio.tv.prototype.shared.ProtoBackHandler
import com.nuvio.tv.prototype.shared.ProtoEnv
import com.nuvio.tv.prototype.shared.ProtoFonts
import com.nuvio.tv.prototype.shared.ProtoLang
import com.nuvio.tv.prototype.shared.ProtoScreen
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.isDown
import com.nuvio.tv.prototype.shared.isUp
import com.nuvio.tv.prototype.shared.routeBackKey

/** Top-level state for the prototype system: which concept, which entry screen, which language. */
@Stable
class PrototypeState(
    initialConcept: Int?,
    initialScreen: ProtoScreen,
    initialLang: ProtoLang,
    initialRemoteArt: Boolean,
) {
    var concept: Int? by mutableStateOf(initialConcept)
    var screen: ProtoScreen by mutableStateOf(initialScreen)
    var lang: ProtoLang by mutableStateOf(initialLang)
    var remoteArt: Boolean by mutableStateOf(initialRemoteArt)
    var switcherOpen: Boolean by mutableStateOf(false)
    /** Bumped to force a fresh session when re-entering the same concept/screen. */
    var generation by mutableIntStateOf(0)
    var hubFocus: Int by mutableIntStateOf(initialConcept ?: 1)

    fun open(number: Int, entry: ProtoScreen = screen) {
        concept = number
        screen = entry
        hubFocus = number
        generation++
        switcherOpen = false
    }

    fun toHub() {
        concept?.let { hubFocus = it }
        concept = null
        switcherOpen = false
    }

    fun step(delta: Int) {
        val current = concept ?: hubFocus
        val next = ((current - 1 + delta + 10) % 10) + 1
        open(next, screen)
    }
}

/**
 * Root of the prototype system, shared by the Android activity and the desktop render harness.
 *
 * Remote shortcuts (available everywhere):
 *  - MENU / INFO / GUIDE: quick switcher (concept, screen, language)
 *  - 1–9, 0: jump straight to concept 01–10
 *  - CH+ / CH−: next / previous concept
 *  - BACK at a concept's first screen returns to the hub
 */
@Composable
fun PrototypeRoot(
    fonts: ProtoFonts,
    env: ProtoEnv,
    onExit: () -> Unit,
    state: PrototypeState = remember { PrototypeState(null, ProtoScreen.HOME, ProtoLang.EN, env.remoteArtwork) },
) {
    val back = remember { ProtoBackDispatcher() }
    val effectiveEnv = env.copy(remoteArtwork = env.remoteArtwork && state.remoteArt)
    CompositionLocalProvider(
        LocalProtoLang provides state.lang,
        LocalLayoutDirection provides if (state.lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalProtoFonts provides fonts,
        LocalProtoEnv provides effectiveEnv,
        LocalProtoBack provides back,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .routeBackKey(back) { if (state.concept != null) state.toHub() else onExit() }
                .onPreviewKeyEvent { e ->
                    val k = e.key
                    val isSwitcherKey = k == Key.Menu || k == Key.Info || k == Key.Guide || k == Key.ProgramRed
                    if (isSwitcherKey) {
                        if (e.isUp && state.concept != null) state.switcherOpen = !state.switcherOpen
                        return@onPreviewKeyEvent state.concept != null
                    }
                    if (state.switcherOpen) return@onPreviewKeyEvent false
                    val digit = digitOf(k)
                    if (digit != null) {
                        if (e.isUp) state.open(if (digit == 0) 10 else digit)
                        return@onPreviewKeyEvent true
                    }
                    if (k == Key.ChannelUp || k == Key.PageUp) {
                        if (e.isDown) state.step(1)
                        return@onPreviewKeyEvent true
                    }
                    if (k == Key.ChannelDown || k == Key.PageDown) {
                        if (e.isDown) state.step(-1)
                        return@onPreviewKeyEvent true
                    }
                    if (k == Key.ProgramGreen) {
                        if (e.isUp) state.lang = if (state.lang == ProtoLang.EN) ProtoLang.AR else ProtoLang.EN
                        return@onPreviewKeyEvent true
                    }
                    false
                },
        ) {
            AnimatedContent(
                targetState = state.concept,
                transitionSpec = {
                    val entering = targetState != null
                    (fadeIn(tween(380, delayMillis = 60)) + slideInHorizontally(tween(420)) { if (entering) it / 12 else -it / 12 })
                        .togetherWith(fadeOut(tween(220)) + slideOutHorizontally(tween(320)) { if (entering) -it / 16 else it / 16 })
                },
                label = "hub",
            ) { number ->
                if (number == null) {
                    PrototypeHub(state)
                } else {
                    val info = ConceptRegistry.concepts[number - 1]
                    ProtoBackHandler { state.toHub() }
                    key(number, state.generation) {
                        val session = remember { ProtoSession(state.screen) }
                        info.content(session)
                    }
                }
            }
            AnimatedVisibility(state.switcherOpen, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
                QuickSwitcher(state)
            }
        }
    }
}

private fun digitOf(k: Key): Int? = when (k) {
    Key.Zero, Key.NumPad0 -> 0
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}
