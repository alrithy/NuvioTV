package com.nuvio.tv.fork.livetv

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTvPlaybackTest {
    @After
    fun clear() = LiveTvPlaybackRegistry.clearForTest()

    @Test
    fun onlyRegisteredUrlsAreLiveTv() {
        assertFalse(LiveTvPlaybackRegistry.isLiveTv("http://vod.example/film.mkv"))
        LiveTvPlaybackRegistry.register("http://panel/live/u/p/1.ts", 7L)
        LiveTvPlaybackRegistry.register("", 8L)
        assertTrue(LiveTvPlaybackRegistry.isLiveTv("http://panel/live/u/p/1.ts"))
        assertFalse(LiveTvPlaybackRegistry.isLiveTv("http://vod.example/film.mkv"))
        assertFalse(LiveTvPlaybackRegistry.isLiveTv(null))
        assertEquals(7L, LiveTvPlaybackRegistry.channelKeyFor("http://panel/live/u/p/1.ts"))
        assertNull(LiveTvPlaybackRegistry.channelKeyFor("http://vod.example/film.mkv"))
    }

    @Test
    fun theRegistryKeepsOnlyTheLatestChannels() {
        (0 until 20).forEach { LiveTvPlaybackRegistry.register("http://c/$it", it.toLong()) }
        assertFalse(LiveTvPlaybackRegistry.isLiveTv("http://c/3"))
        assertTrue(LiveTvPlaybackRegistry.isLiveTv("http://c/4"))
        LiveTvPlaybackRegistry.register("http://c/19", 99L)
        assertEquals(99L, LiveTvPlaybackRegistry.channelKeyFor("http://c/19"))
    }

    @Test
    fun httpRefusalsGetThreeQuickRetriesOtherErrorsKeepTheDefault() {
        assertEquals(700L, LiveTvPlaybackRules.retryDelayMs(403, 1) { error("not used") })
        assertEquals(2_100L, LiveTvPlaybackRules.retryDelayMs(404, 3) { error("not used") })
        assertNull(LiveTvPlaybackRules.retryDelayMs(403, 4) { error("not used") })
        assertEquals(1_000L, LiveTvPlaybackRules.retryDelayMs(null, 1) { 1_000L })
    }

    @Test
    fun liveEdgeRejoinsAreLimitedPerMinute() {
        val rejoins = LiveTvPlaybackRules.LiveEdgeRejoins()
        assertTrue(rejoins.tryAcquire(0))
        assertTrue(rejoins.tryAcquire(1_000))
        assertTrue(rejoins.tryAcquire(2_000))
        assertFalse(rejoins.tryAcquire(3_000))
        assertTrue(rejoins.tryAcquire(61_000))
    }

    @Test
    fun frameRateIsTheMedianGap() {
        val fifty = LongArray(LiveTvPlaybackRules.FRAME_SAMPLES) { it * 20_000L }
        assertEquals(50f, LiveTvPlaybackRules.frameRateFromTimes(fifty), 0.01f)
        // A dropped frame and a timestamp jump do not move it.
        val jumpy = fifty.copyOf().also { it[10] = it[9]; it[30] = it[30] + 5_000_000L }
        assertEquals(50f, LiveTvPlaybackRules.frameRateFromTimes(jumpy), 0.01f)
        val ntsc = LongArray(LiveTvPlaybackRules.FRAME_SAMPLES) { it * 16_683L }
        assertEquals(59.94f, LiveTvPlaybackRules.frameRateFromTimes(ntsc), 0.01f)
        assertEquals(0f, LiveTvPlaybackRules.frameRateFromTimes(LongArray(LiveTvPlaybackRules.FRAME_SAMPLES) { it * 1_000L }))
        assertEquals(0f, LiveTvPlaybackRules.frameRateFromTimes(LongArray(LiveTvPlaybackRules.FRAME_SAMPLES)))
    }
}
