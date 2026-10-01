package com.nuvio.tv.fork.uistyle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * G12d optional screensaver (D058, D062; feature 292). ALGORITHM_PORT of Cxsmo-ai/NuvioTV-Custom
 * @ 3e0d0fa (`core/player/ScreensaverController.kt`, the 1 Hz ticker and key handling in
 * `MainActivity`), made pure so every transition is JVM-tested with a fake clock. Adapted: off by
 * default and per profile (Cxsmo had it on), hidden while UI_STYLES is OFF; the key swallowing lives
 * here instead of in the activity.
 */

/**
 * The idle dimmer's state. Only [maybeEngage] can show it; any key press hides it and is swallowed
 * whole (down, repeats and the matching up) so waking never also navigates.
 */
class ScreensaverMachine(private val now: () -> Long) {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    private var lastInteractionMs = now()
    private var playbackActive = false
    private var windowFocused = true
    private var swallowUntilUp = false

    /** While shown, hero trailers must not start (they would keep the screen on under the overlay). */
    val trailersSuppressed: Boolean get() = _visible.value

    /** Any input: restarts the idle clock. */
    fun interaction() {
        lastInteractionMs = now()
    }

    /** Hides the overlay and restarts the idle clock. True when it was shown. */
    fun wake(): Boolean {
        lastInteractionMs = now()
        if (!_visible.value) return false
        _visible.value = false
        return true
    }

    /**
     * A key reached the activity. Returns true when it must be consumed: the press that wakes the
     * screen and everything up to its release.
     */
    fun onKey(down: Boolean, up: Boolean): Boolean {
        if (_visible.value) {
            if (down) {
                wake()
                swallowUntilUp = true
            }
            return true
        }
        if (swallowUntilUp) {
            if (up) swallowUntilUp = false
            return true
        }
        interaction()
        return false
    }

    /**
     * Dialogs are separate windows whose keys the activity never sees, so the screensaver waits while
     * one has focus; regaining focus restarts the idle clock.
     */
    fun setWindowFocused(focused: Boolean) {
        windowFocused = focused
        if (focused) lastInteractionMs = now()
    }

    /**
     * Playing or buffering blocks it. Resuming wakes it (a MediaSession resume never crosses the
     * activity's keys); pausing restarts the idle clock, so a long film does not dim the moment it
     * is paused.
     */
    fun setPlaybackActive(active: Boolean) {
        if (playbackActive == active) return
        playbackActive = active
        if (active) wake() else lastInteractionMs = now()
    }

    /** The 1 Hz check. True when it engaged on this call. */
    fun maybeEngage(timeoutMs: Long): Boolean {
        if (_visible.value || !windowFocused || playbackActive) return false
        if (now() - lastInteractionMs < timeoutMs) return false
        _visible.value = true
        return true
    }
}

object ScreensaverRules {
    /** Minutes of no input before it dims. */
    val TIMEOUT_OPTIONS = listOf(1, 2, 5, 10, 15, 30)
    const val DEFAULT_TIMEOUT_MINUTES = 5

    /** How dark it gets (Cxsmo's gentle / balanced / strong). */
    val DIM_OPTIONS = listOf(50, 70, 85)
    const val DEFAULT_DIM_PERCENT = 70

    const val TICK_MS = 1_000L

    fun timeoutMs(minutes: Int): Long = coerceTimeout(minutes) * 60_000L

    fun coerceTimeout(minutes: Int?): Int = minutes?.takeIf { it in TIMEOUT_OPTIONS } ?: DEFAULT_TIMEOUT_MINUTES

    fun coerceDim(percent: Int?): Int = percent?.takeIf { it in DIM_OPTIONS } ?: DEFAULT_DIM_PERCENT

    fun nextTimeout(minutes: Int): Int = next(TIMEOUT_OPTIONS, coerceTimeout(minutes))

    fun nextDim(percent: Int): Int = next(DIM_OPTIONS, coerceDim(percent))

    private fun next(options: List<Int>, current: Int): Int = options[(options.indexOf(current) + 1) % options.size]
}
