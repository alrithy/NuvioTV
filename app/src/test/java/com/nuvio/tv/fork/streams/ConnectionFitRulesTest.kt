package com.nuvio.tv.fork.streams

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

class ConnectionFitRulesTest {

    private val gb = 1_000_000_000L

    @Test
    fun averageBitrateNeedsPlausibleSizeAndRuntime() {
        assertEquals(22.2, ConnectionFitRules.averageBitrateMbps(20 * gb, 120)!!, 0.1)
        assertNull("unknown size", ConnectionFitRules.averageBitrateMbps(null, 120))
        assertNull("unknown runtime", ConnectionFitRules.averageBitrateMbps(20 * gb, null))
        assertNull("runtime out of range", ConnectionFitRules.averageBitrateMbps(20 * gb, 5))
        assertNull("tiny file", ConnectionFitRules.averageBitrateMbps(10L * 1024 * 1024, 120))
        assertNull("a season pack's size for one episode", ConnectionFitRules.averageBitrateMbps(300 * gb, 45))
    }

    @Test
    fun onlyStreamsAboveConnectionOverHeadroomAreHeavy() {
        // 50 Mbps sustains up to 33.3 Mbps average.
        assertEquals(1, ConnectionFitRules.connectionTier(40 * gb, 120, 50.0)) // 44 Mbps
        assertEquals(0, ConnectionFitRules.connectionTier(20 * gb, 120, 50.0)) // 22 Mbps
        assertEquals("unknown bitrate stays in place", 0, ConnectionFitRules.connectionTier(null, 120, 50.0))
        assertEquals("still learning", 0, ConnectionFitRules.connectionTier(40 * gb, 120, null))
        assertEquals("no runtime", 0, ConnectionFitRules.connectionTier(40 * gb, null, 50.0))
    }

    @Test
    fun heavyStreamsDropWithinTheirCacheTierOnly() {
        fun input(cacheTier: Int, connection: Int, resolution: Int) =
            StreamRankInput(cacheTier, resolution, 0, 0, 0, 0, 0, 0, null, 0, connection)
        val order = StreamRankRules.rank(
            listOf(
                "heavy-4k" to input(cacheTier = 0, connection = 1, resolution = 2160),
                "uncached-1080" to input(cacheTier = 3, connection = 0, resolution = 1080),
                "fits-1080" to input(cacheTier = 0, connection = 0, resolution = 1080),
            ),
        ) { it.second }.map { it.first }
        assertEquals(listOf("fits-1080", "heavy-4k", "uncached-1080"), order)
    }

    @Test
    fun estimateIsTheBestOfTheLastSamplesOnThatNetwork() {
        val now = 100L * 24 * 60 * 60 * 1000
        val samples = listOf(
            ConnectionSpeedSample(NetworkKind.WIFI, 80.0, now - 1000),
            ConnectionSpeedSample(NetworkKind.CELLULAR, 500.0, now - 900),
            ConnectionSpeedSample(NetworkKind.WIFI, 40.0, now - 800),
        )
        assertEquals(80.0, ConnectionFitRules.estimateMbps(samples, NetworkKind.WIFI, now)!!, 0.0)
        assertNull("one sample never drives ranking", ConnectionFitRules.estimateMbps(samples, NetworkKind.CELLULAR, now))
        val stale = samples.map { it.copy(recordedAtMs = now - 15L * 24 * 60 * 60 * 1000) }
        assertNull("samples older than two weeks are ignored", ConnectionFitRules.estimateMbps(stale, NetworkKind.WIFI, now))
    }

    @Test
    fun onlyTheLastThreeSamplesPerNetworkAreKept() {
        var samples = emptyList<ConnectionSpeedSample>()
        (1..5).forEach { samples = ConnectionFitRules.appendSample(samples, ConnectionSpeedSample(NetworkKind.WIFI, it.toDouble(), it.toLong())) }
        samples = ConnectionFitRules.appendSample(samples, ConnectionSpeedSample(NetworkKind.CELLULAR, 9.0, 9))
        assertEquals(listOf(3.0, 4.0, 5.0), samples.filter { it.network == NetworkKind.WIFI }.map { it.mbps })
        assertEquals(1, samples.count { it.network == NetworkKind.CELLULAR })
    }

    @Test
    fun samplesRoundTripAndBadEntriesAreSkipped() {
        val samples = listOf(ConnectionSpeedSample(NetworkKind.WIFI, 42.5, 123L), ConnectionSpeedSample(NetworkKind.OTHER, 7.0, 456L))
        assertEquals(samples, ConnectionFitRules.decode(ConnectionFitRules.encode(samples)))
        assertEquals(samples.take(1), ConnectionFitRules.decode(ConnectionFitRules.encode(samples.take(1)) + ";junk;NOPE:1:2"))
        assertTrue(ConnectionFitRules.decode(null).isEmpty())
    }

    @Test
    fun onlyInternetSourcesAreMeasured() {
        assertTrue(ConnectionFitRules.isInternetPlaybackSource("https://cdn.example.com/a.mkv"))
        assertTrue(ConnectionFitRules.isInternetPlaybackSource("http://8.8.8.8:8080/x"))
        assertFalse(ConnectionFitRules.isInternetPlaybackSource("http://127.0.0.1:8090/torrent"))
        assertFalse(ConnectionFitRules.isInternetPlaybackSource("http://192.168.1.10/movie.mkv"))
        assertFalse(ConnectionFitRules.isInternetPlaybackSource("http://nas.local/movie.mkv"))
        assertFalse(ConnectionFitRules.isInternetPlaybackSource("file:///sdcard/movie.mkv"))
        assertFalse(ConnectionFitRules.isInternetPlaybackSource("http://[::1]/x"))
    }

    private class Harness(url: String = "https://cdn.example.com/a.mkv") {
        val time = TestTimeSource()
        var generation = 0
        var network: NetworkKind? = NetworkKind.WIFI
        val samples = mutableListOf<Pair<NetworkKind, Double>>()
        val sampler = PlaybackThroughputSampler(url, time, { network }, { generation }) { kind, mbps -> samples += kind to mbps }

        /** 500 ms ticks at [mbps]. */
        fun run(ticks: Int, mbps: Double, fetching: Boolean = true) {
            sampler.onBytesTick(0, fetching) // first tick only starts the clock
            repeat(ticks) {
                time += 500.milliseconds
                sampler.onBytesTick((mbps * 1_000_000 / 8 / 2).toLong(), fetching)
            }
        }
    }

    @Test
    fun samplerReportsOnceAfterWarmupAndAFullWindow() {
        val harness = Harness()
        harness.run(ticks = 30, mbps = 40.0)
        harness.sampler.finish()
        assertEquals(1, harness.samples.size)
        assertEquals(NetworkKind.WIFI, harness.samples.single().first)
        assertEquals(40.0, harness.samples.single().second, 0.5)
    }

    @Test
    fun samplerDropsIdleTicksNetworkChangesAndLocalSources() {
        val idle = Harness().apply { run(ticks = 30, mbps = 40.0, fetching = false); sampler.finish() }
        assertTrue(idle.samples.isEmpty())

        val switched = Harness().apply {
            run(ticks = 4, mbps = 40.0)
            generation++
            run(ticks = 10, mbps = 40.0)
            sampler.finish()
        }
        assertTrue(switched.samples.isEmpty())

        val local = Harness("http://127.0.0.1:8090/stream").apply { run(ticks = 30, mbps = 40.0); sampler.finish() }
        assertTrue(local.samples.isEmpty())

        val short = Harness().apply { run(ticks = 4, mbps = 40.0); sampler.finish() }
        assertTrue("under 3 s of transfer after warm-up", short.samples.isEmpty())
    }
}
