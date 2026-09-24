package com.nuvio.tv.prototype.concept08

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.formatRuntime
import com.nuvio.tv.prototype.shared.isBack
import com.nuvio.tv.prototype.shared.isDown
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberProtoClock
import com.nuvio.tv.prototype.shared.resumeLine
import com.nuvio.tv.prototype.shared.tr
import kotlinx.coroutines.delay

private class Shelf(val name: Bi, val items: List<ProtoTitle>)

private val shelves = listOf(
    Shelf(Bi("Resume", "استئناف"), MockCatalog.continueWatching),
    Shelf(Bi("For this evening", "لهذا المساء"), MockCatalog.tonight),
    Shelf(Bi("Short watches", "مشاهدات قصيرة"), MockCatalog.shortWatches),
    Shelf(Bi("Arabic", "عربي"), MockCatalog.arabicCinema),
    Shelf(Bi("New", "جديد"), MockCatalog.newEpisodes),
)

@Composable
internal fun C08Home(session: ProtoSession) {
    val type = c08Type()
    val lang = LocalProtoLang.current
    val clock by rememberProtoClock()
    val frozen = LocalProtoEnv.current.frozen
    var shelf by remember { mutableIntStateOf(session.value("c08.shelf", if (session.demoFocus) 1 else 0)) }
    var focusedIdx by remember { mutableStateOf<Int?>(null) }
    val items = shelves[shelf].items
    var lit by remember { mutableStateOf(items[(session.value("c08.idx", if (session.demoFocus) 2 else 0)).coerceIn(0, items.lastIndex)]) }
    val reqs = remember(shelf) { List(items.size) { FocusRequester() } }

    // Ambient idle: after a while the interface steps back and only the clock and light remain.
    var lastKey by remember { mutableLongStateOf(0L) }
    var ambient by remember { mutableStateOf(false) }
    LaunchedEffect(lastKey) {
        ambient = false
        if (!frozen) { delay(25_000); ambient = true }
    }
    val ui by animateFloatAsState(if (ambient) 0f else 1f, tween(if (ambient) 2400 else 400), label = "ambient")

    LaunchedEffect(shelf) {
        session.set("c08.shelf", shelf)
        val target = (session.value("c08.idx", if (session.demoFocus) 2 else 0)).coerceIn(0, items.lastIndex)
        // Dock slots are reused across shelves, so no focus event fires when the shelf changes.
        lit = items[target]
        if (focusedIdx != null) focusedIdx = target
        repeat(8) {
            withFrameNanos { }
            if (runCatching { reqs[target].requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    Box(
        Modifier.fillMaxSize().onPreviewKeyEvent { e ->
            if (!e.isDown) return@onPreviewKeyEvent false
            lastKey = System.nanoTime()
            if (ambient && !e.isBack) { ambient = false; true } else false
        },
    ) {
        C08Screen(lit.palette, strength = if (ambient) 1f else 0.8f) {
            // Clock first. Everything else is secondary to the room.
            Column(Modifier.align(Alignment.TopStart).padding(start = C08.Margin, top = 28.dp)) {
                Txt(clock.clock24(), type.clock)
                Txt(clock.weekdayName(lang) + " · " + clock.day + " " + clock.monthName(lang), type.title.copy(color = C08.Ink2))
            }
            Row(Modifier.align(Alignment.TopEnd).padding(end = C08.Margin, top = 44.dp).graphicsLayer { alpha = ui }, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                C08Pill("", Glyph.SEARCH) { session.nav.push(ProtoRoute.Search) }
                C08Pill("", Glyph.LIBRARY) { session.nav.push(ProtoRoute.Library) }
                C08Pill("", Glyph.PROFILE) { session.nav.push(ProtoRoute.Profile) }
            }

            // The evening suggestion: one thing, gently offered.
            Column(Modifier.align(Alignment.BottomStart).padding(start = C08.Margin, bottom = 180.dp).width(560.dp).graphicsLayer { alpha = ui }) {
                AnimatedContent(lit, transitionSpec = { fadeIn(tween(600, 200)) togetherWith fadeOut(tween(250)) }, label = "lit") { t ->
                    Column {
                        Txt(clock.greeting(lang) + tr(", Faisal", "، فيصل"), type.small)
                        Spacer(Modifier.height(6.dp))
                        Txt(t.title.get(), type.hero, maxLines = 1)
                        Txt(resumeLine(t) ?: (formatRuntime(t.runtimeMin, lang) + " · " + t.primaryGenre.get()), type.body)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    C08Pill(if (lit.progress != null) tr("Resume", "استئناف") else tr("Play", "تشغيل"), Glyph.PLAY, glow = lit.palette.ui) { session.nav.push(ProtoRoute.Streams(lit.id)) }
                    C08Pill(tr("About", "عن العمل"), glow = lit.palette.ui) { session.nav.push(ProtoRoute.Details(lit.id)) }
                }
            }

            // The dock.
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 22.dp).graphicsLayer { alpha = ui }, horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    shelves.forEachIndexed { i, s ->
                        Txt(s.name.get(), type.small.copy(color = if (i == shelf) C08.Ink else C08.Ink3))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.onPreviewKeyEvent { e ->
                        if (!e.isDown) return@onPreviewKeyEvent false
                        when {
                            e.key == Key.DirectionDown && shelf < shelves.lastIndex -> { shelf++; true }
                            e.key == Key.DirectionUp && shelf > 0 -> { shelf--; true }
                            else -> false
                        }
                    }.height(124.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    items.forEachIndexed { i, t ->
                        C08DockTile(t, dockSize(i, focusedIdx, 76.dp), reqs[i], focused = focusedIdx == i, onFocus = { f ->
                            if (f) {
                                focusedIdx = i
                                lit = t
                                session.set("c08.idx", i)
                            } else if (focusedIdx == i) focusedIdx = null
                        }) { session.nav.push(ProtoRoute.Details(t.id)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun C08DockTile(t: ProtoTitle, size: Dp, requester: FocusRequester?, focused: Boolean, onFocus: (Boolean) -> Unit, onClick: () -> Unit) {
    val type = c08Type()
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(340, easing = ProtoEasing.Decelerate), label = "tile")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (focused) {
            Txt(t.title.get(), type.label, Modifier.graphicsLayer { alpha = f }, maxLines = 1)
            Spacer(Modifier.height(6.dp))
        }
        Box(
            Modifier
                .size(size)
                .halo(t.palette.ui, f)
                .clip(RoundedCornerShape(size * 0.28f))
                .protoFocusable(requester, onFocusChange = onFocus, onClick = onClick),
        ) {
            ProtoArtwork(t, ArtKind.POSTER, Modifier.fillMaxSize().graphicsLayer { alpha = 0.72f + 0.28f * f })
            val p = t.progress
            if (p != null) Box(Modifier.align(Alignment.BottomCenter).padding(8.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x55FFFFFF))) {
                Box(Modifier.fillMaxWidth(p).height(3.dp).background(Color.White))
            }
        }
    }
}
