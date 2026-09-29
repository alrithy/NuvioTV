package com.nuvio.tv.fork.playback

import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.playback.ConnectionPrewarmPolicy.HEAD_WINDOW_BYTES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionPrewarmPolicyTest {

    private val url = "https://cdn.example/Movie.2160p.REMUX.mkv"
    private val tail = 4_194_304L

    private fun decision(effective: PlaybackStrategy) =
        StrategyDecision(PlaybackStrategy.AUTO, effective, StrategyReason.AUTO_LARGE_FILE)

    @Test
    fun warmsOnlyForTheRemuxThroughputStrategy() {
        assertTrue(ConnectionPrewarmPolicy.shouldPrewarm(FeatureMode.AUTO, decision(PlaybackStrategy.REMUX_THROUGHPUT), url))
        PlaybackStrategy.entries.filter { it != PlaybackStrategy.REMUX_THROUGHPUT }.forEach {
            assertFalse("$it", ConnectionPrewarmPolicy.shouldPrewarm(FeatureMode.AUTO, decision(it), url))
        }
    }

    @Test
    fun featureOffOrNonHttpUrlNeverWarms() {
        val remux = decision(PlaybackStrategy.REMUX_THROUGHPUT)
        assertFalse(ConnectionPrewarmPolicy.shouldPrewarm(FeatureMode.OFF, remux, url))
        listOf(null, "", "magnet:?xt=urn:btih:abc", "torrent://abc", "file:///sdcard/a.mkv").forEach {
            assertFalse("$it", ConnectionPrewarmPolicy.shouldPrewarm(FeatureMode.AUTO, remux, it))
        }
        assertTrue(ConnectionPrewarmPolicy.shouldPrewarm(FeatureMode.AUTO, remux, " HTTP://cdn.example/a.mkv"))
    }

    @Test
    fun theTailWindowIsSkippedOnLowRam() {
        assertFalse(ConnectionPrewarmPolicy.warmTail(lowRam = true))
        assertTrue(ConnectionPrewarmPolicy.warmTail(lowRam = false))
    }

    @Test
    fun aRepeatedPressInsideTheWindowIsDeduplicated() {
        assertTrue(ConnectionPrewarmPolicy.isDuplicate(url, 30_000, url, 0))
        assertFalse(ConnectionPrewarmPolicy.isDuplicate(url, 60_000, url, 0))
        assertFalse(ConnectionPrewarmPolicy.isDuplicate(url, 30_000, "https://other/b.mkv", 0))
        assertFalse(ConnectionPrewarmPolicy.isDuplicate(url, 30_000, null, 0))
        // Clock went backwards (reboot-free elapsedRealtime cannot, but never trust a negative delta).
        assertFalse(ConnectionPrewarmPolicy.isDuplicate(url, 0, url, 30_000))
    }

    @Test
    fun parsesContentRange() {
        assertEquals(9_402_232_472L, ConnectionPrewarmPolicy.contentRangeTotal("bytes 0-262143/9402232472"))
        assertEquals(-1L, ConnectionPrewarmPolicy.contentRangeTotal("bytes 0-262143/*"))
        assertEquals(-1L, ConnectionPrewarmPolicy.contentRangeTotal(null))
        assertEquals(-1L, ConnectionPrewarmPolicy.contentRangeTotal("garbage"))
        assertEquals(71_196_784_383L, ConnectionPrewarmPolicy.contentRangeStart("bytes 71196784383-71200978686/71200978687"))
        assertEquals(-1L, ConnectionPrewarmPolicy.contentRangeStart("bytes */71200978687"))
        assertEquals(-1L, ConnectionPrewarmPolicy.contentRangeStart(null))
    }

    @Test
    fun storesOnlyExactWindows() {
        assertTrue(ConnectionPrewarmPolicy.isStorableHead(HEAD_WINDOW_BYTES.toInt(), 10_000_000))
        assertFalse(ConnectionPrewarmPolicy.isStorableHead(0, 10_000_000))
        assertFalse(ConnectionPrewarmPolicy.isStorableHead(HEAD_WINDOW_BYTES.toInt() + 1, 10_000_000))
        assertFalse(ConnectionPrewarmPolicy.isStorableHead(1_000, -1))

        val total = 71_200_978_687L
        assertTrue(ConnectionPrewarmPolicy.isStorableTail(total - tail, tail, total, tail))
        assertFalse(ConnectionPrewarmPolicy.isStorableTail(total - tail, tail - 1, total, tail))
        assertFalse(ConnectionPrewarmPolicy.isStorableTail(total - tail - 1, tail, total, tail))
        assertFalse(ConnectionPrewarmPolicy.isStorableTail(-1, tail, total, tail))
    }

    @Test
    fun fallbackTailIsSkippedWhenTheHeadCoversTheFile() {
        assertEquals(10_000_000L - tail, ConnectionPrewarmPolicy.fallbackTailStart(10_000_000L, tail))
        assertNull(ConnectionPrewarmPolicy.fallbackTailStart(tail + HEAD_WINDOW_BYTES, tail))
        assertNull(ConnectionPrewarmPolicy.fallbackTailStart(1_000L, tail))
    }
}
