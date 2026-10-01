package com.nuvio.tv.fork.video

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackAfrPolicyTest {

    private fun decide(
        enabled: Boolean = true,
        exo: Boolean = true,
        afrOn: Boolean = true,
        attempted: Boolean = false,
        fps: Float = 23.976f,
        detected: Boolean = false,
        running: Boolean = false,
        playing: Boolean = false,
        live: Boolean = false,
    ) = TrackAfrPolicy.decide(enabled, exo, afrOn, attempted, fps, detected, running, playing, live)

    @Test
    fun runsOnlyAsAFallbackWhenThePreflightFoundNothing() {
        assertEquals(TrackAfrAction.RUN, decide())
        assertEquals(TrackAfrAction.SKIP, decide(detected = true))
        assertEquals(TrackAfrAction.DEFER, decide(running = true))
    }

    @Test
    fun officialBehaviorWhenNotApplicable() {
        assertEquals(TrackAfrAction.SKIP, decide(enabled = false))
        assertEquals(TrackAfrAction.SKIP, decide(exo = false))
        assertEquals(TrackAfrAction.SKIP, decide(afrOn = false))
        assertEquals(TrackAfrAction.SKIP, decide(attempted = true))
        assertEquals(TrackAfrAction.SKIP, decide(fps = 0f))
    }

    @Test
    fun neverSwitchesForImplausibleRatesOrUnderRunningPlayback() {
        assertEquals(TrackAfrAction.IGNORE_IMPLAUSIBLE, decide(fps = 1f))
        assertEquals(TrackAfrAction.TOO_LATE, decide(playing = true))
        // A running preflight is waited for before the implausible / too-late checks.
        assertEquals(TrackAfrAction.DEFER, decide(fps = 1f, running = true))
    }

    @Test
    fun liveTvSwitchesOnceWithoutAHoldWhenTheRateCameLate() {
        assertEquals(TrackAfrAction.RUN_LIVE, decide(playing = true, live = true, fps = 50f))
        assertEquals(TrackAfrAction.RUN, decide(live = true, fps = 50f))
        assertEquals(TrackAfrAction.SKIP, decide(playing = true, live = true, attempted = true))
        assertEquals(TrackAfrAction.IGNORE_IMPLAUSIBLE, decide(playing = true, live = true, fps = 12f))
        assertEquals(TrackAfrAction.TOO_LATE, decide(playing = true, live = false))
    }
}
