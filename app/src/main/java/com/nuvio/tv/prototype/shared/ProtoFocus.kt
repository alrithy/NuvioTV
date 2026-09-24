package com.nuvio.tv.prototype.shared

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

// ---------------------------------------------------------------------------------------------
// Keys
// ---------------------------------------------------------------------------------------------

private val EnterKeys = setOf(Key.Enter, Key.NumPadEnter, Key.DirectionCenter)
private val BackKeys = setOf(Key.Back, Key.Escape)

val KeyEvent.isEnter: Boolean get() = key in EnterKeys
val KeyEvent.isBack: Boolean get() = key in BackKeys
val KeyEvent.isDown: Boolean get() = type == KeyEventType.KeyDown
val KeyEvent.isUp: Boolean get() = type == KeyEventType.KeyUp

// ---------------------------------------------------------------------------------------------
// Focusable + click in one modifier, tuned for D-pad.
// ---------------------------------------------------------------------------------------------

/**
 * Makes an element focusable and clickable with the D-pad centre / Enter key.
 *
 * Deliberately avoids `clickable` so behaviour is identical on TV, desktop previews and
 * touch-less devices, and so no platform ripple leaks into the concepts.
 */
fun Modifier.protoFocusable(
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onKey: ((KeyEvent) -> Boolean)? = null,
): Modifier {
    var m: Modifier = this
    if (focusRequester != null) m = m.focusRequester(focusRequester)
    if (onFocusChange != null) m = m.onFocusChanged { onFocusChange(it.isFocused) }
    if (onClick != null || onKey != null) {
        m = m.onKeyEvent { e ->
            if (onKey != null && onKey(e)) return@onKeyEvent true
            if (onClick != null && e.isEnter) {
                if (e.isUp) onClick()
                true
            } else {
                false
            }
        }
    }
    return m.focusable()
}

/** Requests focus once the node is attached. Retries for a few frames because TV layouts settle late. */
@Composable
fun RequestFocusOnce(requester: FocusRequester, key: Any? = Unit, enabled: Boolean = true) {
    LaunchedEffect(key, enabled) {
        if (!enabled) return@LaunchedEffect
        repeat(8) {
            withFrameNanos { }
            if (runCatching { requester.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }
}

/** Holds one FocusRequester per key so screens can restore focus to the last item. */
class FocusRegistry {
    private val map = HashMap<Any, FocusRequester>()
    var lastFocused: Any? by mutableStateOf(null)

    fun requester(key: Any): FocusRequester = map.getOrPut(key) { FocusRequester() }

    fun restore(fallback: Any? = null): Boolean {
        val target = lastFocused ?: fallback ?: return false
        return runCatching { requester(target).requestFocus() }.isSuccess
    }
}

@Composable
fun RestoreFocus(registry: FocusRegistry, fallback: Any?, key: Any? = Unit) {
    LaunchedEffect(key) {
        repeat(10) {
            withFrameNanos { }
            if (registry.restore(fallback)) return@LaunchedEffect
        }
    }
}

/**
 * Tab rows that select on focus (seasons, library tabs, stream modes). While focus is outside the
 * row only the selected tab can take focus, so arriving from above or below always lands on it and
 * never switches the selection by accident. Once inside, every tab is reachable with left/right.
 */
class TabRowFocus {
    var inside by mutableStateOf(false)
}

@Composable
fun rememberTabRowFocus(): TabRowFocus = remember { TabRowFocus() }

/** Put on the container that holds every tab of the row. */
fun Modifier.tabRow(state: TabRowFocus): Modifier = onFocusChanged { state.inside = it.hasFocus }

/** Put on each tab, before its focus target. */
fun Modifier.tabItem(state: TabRowFocus, selected: Boolean): Modifier = focusProperties { canFocus = state.inside || selected }

// ---------------------------------------------------------------------------------------------
// Back handling that works identically on Android TV and on the desktop render harness.
// ---------------------------------------------------------------------------------------------

class ProtoBackDispatcher {
    private val callbacks = mutableStateListOf<BackCallback>()

    class BackCallback(var enabled: Boolean, var onBack: () -> Unit)

    fun add(cb: BackCallback) { callbacks.add(cb) }
    fun remove(cb: BackCallback) { callbacks.remove(cb) }

    /** Returns true if some registered handler consumed the back press. */
    fun dispatch(): Boolean {
        val cb = callbacks.lastOrNull { it.enabled } ?: return false
        cb.onBack()
        return true
    }

    val hasEnabled: Boolean get() = callbacks.any { it.enabled }
}

val LocalProtoBack = staticCompositionLocalOf { ProtoBackDispatcher() }

/** Platform-neutral BackHandler. The innermost enabled handler wins. */
@Composable
fun ProtoBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val dispatcher = LocalProtoBack.current
    val latest by rememberUpdatedState(onBack)
    val callback = remember { ProtoBackDispatcher.BackCallback(enabled) { latest() } }
    callback.enabled = enabled
    DisposableEffect(dispatcher) {
        dispatcher.add(callback)
        onDispose { dispatcher.remove(callback) }
    }
}

/**
 * Root-level key interception that routes Back into [ProtoBackDispatcher].
 * Back is always consumed here; [onUnhandled] runs when no handler is registered (e.g. exit).
 */
fun Modifier.routeBackKey(dispatcher: ProtoBackDispatcher, onUnhandled: () -> Unit): Modifier =
    onPreviewKeyEvent { e ->
        if (!e.isBack) return@onPreviewKeyEvent false
        if (e.isUp && !dispatcher.dispatch()) onUnhandled()
        true
    }

// ---------------------------------------------------------------------------------------------
// Scroll pivots: how a focused item is brought into view is a big part of each concept's feel.
// ---------------------------------------------------------------------------------------------

object ProtoEasing {
    /** Material "emphasized decelerate" — calm arrival, no overshoot. */
    val Decelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    /** Cinematic ease: slow in, long settle. */
    val Cinematic: Easing = CubicBezierEasing(0.22f, 0.0f, 0.0f, 1.0f)
    /** Precise mechanical detent for console-like UIs. */
    val Detent: Easing = CubicBezierEasing(0.3f, 0.0f, 0.1f, 1.0f)
    val Standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val Exit: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
}

/**
 * Platform-neutral description of how a focused item is scrolled into view. The Android layer
 * turns this into a BringIntoViewSpec for every scrollable in the concept.
 *
 *  - [ScrollPivot.Anchor]: keep the focused item's leading edge at a fixed fraction of the
 *    container, so content slides under a stationary focus position (cinema reel / Apple TV).
 *  - [ScrollPivot.Center]: keep the focused item centred.
 *  - [ScrollPivot.Padded]: only scroll when the item leaves a padded safe zone. Precise and quiet.
 */
sealed interface ScrollPivot {
    val durationMs: Int
    val easing: Easing

    data class Anchor(
        val fraction: Float,
        val offsetDp: Float = 0f,
        override val durationMs: Int = 320,
        override val easing: Easing = ProtoEasing.Cinematic,
    ) : ScrollPivot

    data class Center(
        override val durationMs: Int = 300,
        override val easing: Easing = ProtoEasing.Decelerate,
    ) : ScrollPivot

    data class Padded(
        val paddingDp: Float,
        override val durationMs: Int = 220,
        override val easing: Easing = ProtoEasing.Detent,
    ) : ScrollPivot
}
