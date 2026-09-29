package com.nuvio.tv.fork.playback

import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.playback.PlaybackStrategy.AUTO
import com.nuvio.tv.fork.playback.PlaybackStrategy.LOW_MEMORY
import com.nuvio.tv.fork.playback.PlaybackStrategy.OFFICIAL
import com.nuvio.tv.fork.playback.PlaybackStrategy.REMUX_THROUGHPUT
import com.nuvio.tv.fork.playback.PlaybackStrategy.SEEK_OPTIMIZED
import com.nuvio.tv.fork.resource.AdaptiveResourcePolicy
import com.nuvio.tv.fork.resource.MemoryTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStrategyTest {

    private val gib = 1024L * 1024L * 1024L
    private val progressive = PlaybackFacts(
        exoPlayerEngine = true, progressiveHttp = true, fileSizeBytes = 4 * gib,
        filename = "Movie.2024.1080p.WEB-DL.mkv", lowRamDevice = false,
    )
    private val standard = AdaptiveResourcePolicy(MemoryTier.STANDARD)

    private fun resolve(selected: PlaybackStrategy, facts: PlaybackFacts = progressive, mode: FeatureMode = FeatureMode.AUTO) =
        PlaybackStrategies.resolve(selected, facts, mode)

    @Test
    fun explicitSelectionsApplyToProgressiveExoPlayback() {
        for (strategy in listOf(REMUX_THROUGHPUT, SEEK_OPTIMIZED, LOW_MEMORY)) {
            val decision = resolve(strategy)
            assertEquals(strategy, decision.effective)
            assertEquals(StrategyReason.SELECTED, decision.reason)
        }
    }

    @Test
    fun officialIsAlwaysOfficialAndChangesNothing() {
        for (mode in FeatureMode.entries) {
            assertEquals(OFFICIAL, resolve(OFFICIAL, mode = mode).effective)
        }
        assertTrue(PlaybackStrategies.knobs(OFFICIAL, 2, 50, standard).isOfficial)
        assertEquals("official", resolve(OFFICIAL).hudLabel)
    }

    @Test
    fun featureOffFallsBackToOfficialForEverySelection() {
        for (strategy in PlaybackStrategy.entries) {
            val decision = resolve(strategy, mode = FeatureMode.OFF)
            assertEquals(OFFICIAL, decision.effective)
        }
    }

    @Test
    fun mpvEngineFallsBackToOfficial() {
        val mpv = progressive.copy(exoPlayerEngine = false)
        for (strategy in listOf(REMUX_THROUGHPUT, SEEK_OPTIMIZED, LOW_MEMORY, AUTO)) {
            val decision = resolve(strategy, mpv)
            assertEquals(OFFICIAL, decision.effective)
            assertEquals(StrategyReason.NOT_EXOPLAYER, decision.reason)
        }
    }

    @Test
    fun networkStrategiesNeedAProgressiveHttpFile() {
        val hls = progressive.copy(progressiveHttp = false)
        assertEquals(OFFICIAL, resolve(REMUX_THROUGHPUT, hls).effective)
        assertEquals(StrategyReason.NOT_PROGRESSIVE_HTTP, resolve(SEEK_OPTIMIZED, hls).reason)
        // Buffer limits apply to any ExoPlayer stream.
        assertEquals(LOW_MEMORY, resolve(LOW_MEMORY, hls).effective)
        assertEquals("seek → official (stream type)", resolve(SEEK_OPTIMIZED, hls).hudLabel)
    }

    @Test
    fun autoPicksLowMemoryOnLowRamDevicesFirst() {
        val facts = progressive.copy(lowRamDevice = true, fileSizeBytes = 60 * gib)
        val decision = resolve(AUTO, facts)
        assertEquals(LOW_MEMORY, decision.effective)
        assertEquals(StrategyReason.AUTO_LOW_RAM, decision.reason)
    }

    @Test
    fun autoPicksThroughputForLargeOrRemuxFiles() {
        assertEquals(REMUX_THROUGHPUT, resolve(AUTO, progressive.copy(fileSizeBytes = 20 * gib)).effective)
        assertEquals(OFFICIAL, resolve(AUTO, progressive.copy(fileSizeBytes = 20 * gib - 1)).effective)
        assertEquals(
            REMUX_THROUGHPUT,
            resolve(AUTO, progressive.copy(fileSizeBytes = null, filename = "Film.2160p.BluRay.REMUX.mkv")).effective
        )
        // "remux" must be a token, not part of another word.
        assertFalse(PlaybackStrategies.isLargeFile(progressive.copy(filename = "Remuxer.Documentary.mkv")))
    }

    @Test
    fun autoStaysOfficialForOrdinaryAndAdaptiveStreams() {
        assertEquals(StrategyReason.AUTO_DEFAULT, resolve(AUTO).reason)
        assertEquals(OFFICIAL, resolve(AUTO).effective)
        val hlsRemux = progressive.copy(progressiveHttp = false, fileSizeBytes = 60 * gib)
        assertEquals(OFFICIAL, resolve(AUTO, hlsRemux).effective)
        assertEquals("auto → official (default)", resolve(AUTO).hudLabel)
    }

    @Test
    fun autoIsDeterministic() {
        val facts = progressive.copy(fileSizeBytes = 30 * gib)
        assertEquals(resolve(AUTO, facts), resolve(AUTO, facts))
    }

    @Test
    fun throughputRaisesConnectionsButRespectsTheResourcePolicy() {
        val knobs = PlaybackStrategies.knobs(REMUX_THROUGHPUT, 2, 50, standard)
        assertEquals(true, knobs.parallelNetwork)
        assertEquals(true, knobs.useParallelConnections)
        assertEquals(4, knobs.parallelConnectionCount)
        assertEquals(true, knobs.bufferBudgetManaged)
        // A higher stored count (performance mode) is kept on standard devices…
        assertEquals(8, PlaybackStrategies.knobs(REMUX_THROUGHPUT, 8, 50, standard).parallelConnectionCount)
        // …and capped on constrained ones (G2b).
        val constrained = AdaptiveResourcePolicy(MemoryTier.CONSTRAINED)
        assertEquals(4, PlaybackStrategies.knobs(REMUX_THROUGHPUT, 16, 50, constrained).parallelConnectionCount)
    }

    @Test
    fun seekOptimizedUsesTheDiskCacheWithoutParallelRemux() {
        val knobs = PlaybackStrategies.knobs(SEEK_OPTIMIZED, 4, 50, standard)
        assertEquals(true, knobs.vodCache)
        assertEquals(false, knobs.useParallelConnections)
        assertEquals(true, knobs.bufferEngine)
    }

    @Test
    fun lowMemoryOnlyEverShrinksTheBuffer() {
        assertEquals(50, PlaybackStrategies.knobs(LOW_MEMORY, 4, 400, standard).targetBufferSizeMb)
        assertEquals(25, PlaybackStrategies.knobs(LOW_MEMORY, 4, 25, standard).targetBufferSizeMb)
        assertEquals(false, PlaybackStrategies.knobs(LOW_MEMORY, 4, 400, standard).useParallelConnections)
    }

    @Test
    fun onlyPlainHttpFilesCountAsProgressive() {
        fun check(url: String, mime: String? = null, torrent: Boolean = false, loopback: Boolean = false) =
            PlaybackStrategies.isProgressiveHttp(url, mime, torrent, loopback)
        assertTrue(check("https://cdn.example/file.mkv"))
        assertTrue(check("HTTP://cdn.example/file.mp4", "video/mp4"))
        assertFalse(check("https://cdn.example/master.m3u8", "application/x-mpegURL"))
        assertFalse(check("https://cdn.example/live", "application/vnd.apple.mpegurl"))
        assertFalse(check("https://cdn.example/manifest.mpd", "application/dash+xml"))
        assertFalse(check("magnet:?xt=urn:btih:abc", torrent = true))
        assertFalse(check("http://127.0.0.1:8080/stream", loopback = true))
        assertFalse(check("content://media/external/video/1"))
    }

    @Test
    fun unknownStoredKeysFallBackToOfficial() {
        assertEquals(OFFICIAL, PlaybackStrategy.fromKey(null))
        assertEquals(OFFICIAL, PlaybackStrategy.fromKey("turbo"))
        PlaybackStrategy.entries.forEach { assertEquals(it, PlaybackStrategy.fromKey(it.key)) }
    }
}
