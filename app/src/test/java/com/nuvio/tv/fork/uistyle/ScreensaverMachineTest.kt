package com.nuvio.tv.fork.uistyle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** G12d (292): idle, playback, dialog focus and wake transitions, with a fake clock. */
class ScreensaverMachineTest {
    private var nowMs = 0L
    private val machine = ScreensaverMachine { nowMs }
    private val timeout = 60_000L

    @Test
    fun engagesOnlyAfterTheTimeoutWithoutInput() {
        nowMs = 59_999L
        assertFalse(machine.maybeEngage(timeout))
        nowMs = 60_000L
        assertTrue(machine.maybeEngage(timeout))
        assertTrue(machine.visible.value)
        assertTrue(machine.trailersSuppressed)
        assertFalse("engages once", machine.maybeEngage(timeout))
    }

    @Test
    fun inputRestartsTheIdleClock() {
        nowMs = 50_000L
        assertFalse(machine.onKey(down = true, up = false))
        nowMs = 100_000L
        assertFalse(machine.maybeEngage(timeout))
        nowMs = 110_000L
        assertTrue(machine.maybeEngage(timeout))
    }

    @Test
    fun theWakingPressIsSwallowedUpToItsRelease() {
        nowMs = 60_000L
        machine.maybeEngage(timeout)
        assertTrue(machine.onKey(down = true, up = false))
        assertFalse(machine.visible.value)
        assertFalse(machine.trailersSuppressed)
        assertTrue("repeat of the waking press", machine.onKey(down = true, up = false))
        assertTrue("its release", machine.onKey(down = false, up = true))
        assertFalse("the next press navigates", machine.onKey(down = true, up = false))
    }

    @Test
    fun neverDuringPlaybackAndPausingRestartsTheClock() {
        machine.setPlaybackActive(true)
        nowMs = 10 * 60_000L
        assertFalse(machine.maybeEngage(timeout))
        machine.setPlaybackActive(false)
        nowMs += 59_000L
        assertFalse("a long film does not dim the moment it is paused", machine.maybeEngage(timeout))
        nowMs += 1_000L
        assertTrue(machine.maybeEngage(timeout))
        machine.setPlaybackActive(true)
        assertFalse("resuming wakes it", machine.visible.value)
    }

    @Test
    fun waitsWhileADialogHasFocus() {
        machine.setWindowFocused(false)
        nowMs = 5 * 60_000L
        assertFalse(machine.maybeEngage(timeout))
        machine.setWindowFocused(true)
        assertFalse(machine.maybeEngage(timeout))
        nowMs += timeout
        assertTrue(machine.maybeEngage(timeout))
        assertTrue(machine.wake())
        assertFalse(machine.wake())
    }

    @Test
    fun optionsCycleAndUnknownValuesFallBack() {
        assertEquals(5, ScreensaverRules.coerceTimeout(null))
        assertEquals(5, ScreensaverRules.coerceTimeout(7))
        assertEquals(10, ScreensaverRules.nextTimeout(5))
        assertEquals(1, ScreensaverRules.nextTimeout(30))
        assertEquals(70, ScreensaverRules.coerceDim(42))
        assertEquals(85, ScreensaverRules.nextDim(70))
        assertEquals(50, ScreensaverRules.nextDim(85))
        assertEquals(300_000L, ScreensaverRules.timeoutMs(5))
    }
}
