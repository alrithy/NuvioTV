package com.nuvio.tv.fork.recovery

import com.nuvio.tv.fork.recovery.StartupWatchdogPolicy.Reason
import com.nuvio.tv.fork.recovery.StartupWatchdogPolicy.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRecoveryTest {

    private val urls = listOf("https://a/1.mkv", "https://b/2.mkv", null, "https://c/3.mkv")

    @Test
    fun onlyPermanentHttpStatusesAreDead() {
        assertTrue(DeadSourcePolicy.isDeadHttpStatus(404))
        assertTrue(DeadSourcePolicy.isDeadHttpStatus(410))
        // Transient: debrid rate limits, server errors, range errors, timeouts (no status).
        listOf(429, 503, 500, 416, 403, null).forEach { assertFalse("$it", DeadSourcePolicy.isDeadHttpStatus(it)) }
    }

    @Test
    fun advancesToTheNextSourceInTheUsersOrder() {
        assertEquals(1, DeadSourcePolicy.nextLiveIndex(urls, 0, setOf(urls[0]!!), 0))
    }

    @Test
    fun skipsDeadUrlsAndAllowsSourcesWithoutAnHttpUrl() {
        val dead = setOf("https://a/1.mkv", "https://b/2.mkv")
        assertEquals(2, DeadSourcePolicy.nextLiveIndex(urls, 0, dead, 0))
    }

    @Test
    fun unknownCurrentSourceStartsFromTheTop() {
        assertEquals(0, DeadSourcePolicy.nextLiveIndex(urls, -1, emptySet(), 0))
    }

    @Test
    fun stopsAtTheEndOfTheListOrTheCap() {
        assertNull(DeadSourcePolicy.nextLiveIndex(urls, 3, emptySet(), 0))
        assertNull(DeadSourcePolicy.nextLiveIndex(urls, 0, emptySet(), DeadSourcePolicy.MAX_FAILOVERS))
        assertNull(DeadSourcePolicy.nextLiveIndex(emptyList(), -1, emptySet(), 0))
        assertEquals(1, DeadSourcePolicy.nextLiveIndex(urls, 0, emptySet(), DeadSourcePolicy.MAX_FAILOVERS - 1))
    }

    @Test
    fun watchdogExtendsOnlyWhileDataGrowsAndTheCeilingAllows() {
        assertEquals(Verdict.EXTEND, StartupWatchdogPolicy.verdict(20_000, 3_000, 0))
        assertEquals(Verdict.EXTEND, StartupWatchdogPolicy.verdict(40_000, 6_000, 3_000))
        // At 60 s another 20 s interval no longer fits under the 60 s ceiling.
        assertEquals(Verdict.FIRE, StartupWatchdogPolicy.verdict(60_000, 9_000, 6_000))
        // No growth fires at the first check.
        assertEquals(Verdict.FIRE, StartupWatchdogPolicy.verdict(20_000, 0, 0))
        assertEquals(Verdict.FIRE, StartupWatchdogPolicy.verdict(40_000, 3_000, 3_000))
    }

    @Test
    fun watchdogReasonSaysSomethingTrue() {
        assertEquals(Reason.TRACKS_NOT_READ, StartupWatchdogPolicy.reason(false, true, true))
        assertEquals(Reason.VIDEO_TRACK_UNSUPPORTED, StartupWatchdogPolicy.reason(true, true, false))
        assertEquals(Reason.NO_VIDEO_TRACK, StartupWatchdogPolicy.reason(true, false, false))
        assertEquals(Reason.DECODER_UNRESPONSIVE, StartupWatchdogPolicy.reason(true, true, true))
    }
}
