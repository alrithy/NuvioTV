package com.nuvio.tv.fork.streams

import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Connection fit (G8c, feature 165). FILE_PORT of Reshaped `core/connection` @ 0ccf049
 * (`StreamConnectionFit`, `averageBitrateMbps`, `ConnectionSpeedEstimator.estimateMbps` /
 * `appendSample`, `PlaybackThroughputSampler`, `isInternetPlaybackSource`), with the Android parts
 * split out so everything here is pure and unit-tested.
 */
enum class NetworkKind { WIFI, CELLULAR, OTHER }

data class ConnectionSpeedSample(val network: NetworkKind, val mbps: Double, val recordedAtMs: Long)

object ConnectionFitRules {
    /** Bitrate peaks run well above a file's average; the player buffer only absorbs part of that. */
    const val BITRATE_HEADROOM = 1.5

    private const val MIN_RUNTIME_MINUTES = 10
    private const val MAX_RUNTIME_MINUTES = 600
    private const val MIN_SIZE_BYTES = 50L * 1024 * 1024
    private const val MIN_PLAUSIBLE_MBPS = 0.2
    private const val MAX_PLAUSIBLE_MBPS = 200.0

    const val MAX_SAMPLES_PER_NETWORK = 3
    private const val MIN_SAMPLES = 2
    private const val MAX_SAMPLE_AGE_MS = 14L * 24 * 60 * 60 * 1000
    private const val MIN_VALID_MBPS = 0.2
    private const val MAX_VALID_MBPS = 1_000.0

    /**
     * Average bitrate from size and runtime, or null when either is missing or the result is
     * implausible (for example a season pack's size reported for a single episode).
     */
    fun averageBitrateMbps(sizeBytes: Long?, runtimeMinutes: Int?): Double? {
        if (sizeBytes == null || runtimeMinutes == null) return null
        if (runtimeMinutes !in MIN_RUNTIME_MINUTES..MAX_RUNTIME_MINUTES || sizeBytes < MIN_SIZE_BYTES) return null
        val mbps = sizeBytes * 8.0 / (runtimeMinutes * 60.0) / 1_000_000.0
        return mbps.takeIf { it in MIN_PLAUSIBLE_MBPS..MAX_PLAUSIBLE_MBPS }
    }

    /**
     * 1 when the stream's average bitrate is above what the connection sustains with headroom,
     * else 0. Unknown bitrate or connection stays 0: missing metadata is not evidence of a heavy
     * stream.
     */
    fun connectionTier(sizeBytes: Long?, runtimeMinutes: Int?, connectionMbps: Double?): Int {
        val connection = connectionMbps ?: return 0
        val bitrate = averageBitrateMbps(sizeBytes, runtimeMinutes) ?: return 0
        return if (bitrate > connection / BITRATE_HEADROOM) 1 else 0
    }

    fun isValidThroughput(mbps: Double): Boolean = mbps.isFinite() && mbps in MIN_VALID_MBPS..MAX_VALID_MBPS

    /**
     * The best of the last few samples on [network]. Each sample is capped by the server that
     * delivered it, so the fastest is the tightest lower bound on the connection; one slow host
     * must not make every other source look unplayable. Two samples are required so a single
     * session never drives ranking on its own; samples older than two weeks are ignored.
     */
    fun estimateMbps(samples: List<ConnectionSpeedSample>, network: NetworkKind, nowMs: Long): Double? {
        var count = 0
        var best = 0.0
        for (index in samples.indices.reversed()) {
            val sample = samples[index]
            if (sample.network != network) continue
            if (nowMs - sample.recordedAtMs !in 0..MAX_SAMPLE_AGE_MS || !isValidThroughput(sample.mbps)) continue
            if (sample.mbps > best) best = sample.mbps
            if (++count == MAX_SAMPLES_PER_NETWORK) break
        }
        return best.takeIf { count >= MIN_SAMPLES }
    }

    fun appendSample(samples: List<ConnectionSpeedSample>, sample: ConnectionSpeedSample): List<ConnectionSpeedSample> {
        val (sameNetwork, otherNetworks) = samples.partition { it.network == sample.network }
        return otherNetworks + sameNetwork.takeLast(MAX_SAMPLES_PER_NETWORK - 1) + sample
    }

    /** `KIND:mbps:time;…`; no URLs, hosts or titles are ever stored. */
    fun encode(samples: List<ConnectionSpeedSample>): String =
        samples.joinToString(";") { "${it.network.name}:${it.mbps}:${it.recordedAtMs}" }

    fun decode(value: String?): List<ConnectionSpeedSample> =
        value.orEmpty().split(';').mapNotNull { entry ->
            val parts = entry.split(':')
            if (parts.size != 3) return@mapNotNull null
            val network = NetworkKind.entries.firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
            val mbps = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            val at = parts[2].toLongOrNull() ?: return@mapNotNull null
            ConnectionSpeedSample(network, mbps, at)
        }

    /**
     * True for http(s) sources reached over the internet. Local files, the on-device torrent
     * proxy and LAN servers measure something other than the internet connection.
     */
    fun isInternetPlaybackSource(url: String): Boolean {
        val value = url.trim()
        val scheme = value.substringBefore("://", missingDelimiterValue = "").lowercase()
        if (scheme != "http" && scheme != "https") return false
        val authority = value.substringAfter("://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('@')
        val host = if (authority.startsWith("[")) {
            authority.removePrefix("[").substringBefore(']')
        } else {
            authority.substringBefore(':')
        }.lowercase()
        if (host.isEmpty() || host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) return false
        if (':' in host) {
            return host != "::1" && !host.startsWith("fe80:") && !host.startsWith("fc") && !host.startsWith("fd")
        }
        val octets = host.split('.').map { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it == null || it !in 0..255 }) return true
        val first = octets[0]!!
        val second = octets[1]!!
        return !(
            first == 0 || first == 10 || first == 127 ||
                (first == 169 && second == 254) ||
                (first == 172 && second in 16..31) ||
                (first == 192 && second == 168)
            )
    }
}

/**
 * Measures sustained download throughput over one playback and reports it once. Only ticks
 * where the player is fetching and data arrives count (a full buffer or connection setup would
 * make a fast line look slow); the first second (TCP/TLS slow start) is skipped; the sample is
 * dropped if the default network changed meanwhile. Runs on the player's existing progress tick:
 * no requests, no polling of its own.
 */
class PlaybackThroughputSampler(
    sourceUrl: String,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val networkKind: () -> NetworkKind?,
    private val networkGeneration: () -> Int,
    private val onSample: (NetworkKind, Double) -> Unit,
) {
    private val isEligible = ConnectionFitRules.isInternetPlaybackSource(sourceUrl)
    private var lastTick: TimeMark? = null
    private var warmupMs = 0L
    private var activeBytes = 0L
    private var activeMs = 0L
    private var measuredNetwork: NetworkKind? = null
    private var measuredGeneration = 0
    private var isFinished = false

    /** [bytes] received since the previous tick. */
    fun onBytesTick(bytes: Long, isFetching: Boolean) = tick(isFetching) { bytes }

    /** For players that report a transfer rate rather than a byte count (mpv `cache-speed`). */
    fun onRateTick(bytesPerSecond: Long, isFetching: Boolean) = tick(isFetching) { elapsedMs -> bytesPerSecond * elapsedMs / 1000 }

    fun finish() {
        if (isFinished) return
        isFinished = true
        val network = measuredNetwork ?: return
        if (activeMs < MIN_WINDOW_MS) return
        if (activeBytes < MIN_WINDOW_BYTES && activeMs < SLOW_WINDOW_MS) return
        if (networkGeneration() != measuredGeneration) return
        onSample(network, activeBytes * 8.0 / activeMs / 1000.0)
    }

    private inline fun tick(isFetching: Boolean, bytesFor: (elapsedMs: Long) -> Long) {
        if (!isEligible || isFinished) return
        val previous = lastTick
        lastTick = timeSource.markNow()
        val elapsedMs = previous?.elapsedNow()?.inWholeMilliseconds ?: return
        // A long gap means the app was suspended; the interval says nothing about the network.
        if (!isFetching || elapsedMs <= 0 || elapsedMs > MAX_TICK_GAP_MS) return
        val bytes = bytesFor(elapsedMs)
        if (bytes <= 0L) return
        if (warmupMs < WARMUP_MS) {
            if (warmupMs == 0L) {
                measuredNetwork = networkKind() ?: run { isFinished = true; return }
                measuredGeneration = networkGeneration()
            }
            warmupMs += elapsedMs
            return
        }
        activeBytes += bytes
        activeMs += elapsedMs
        if (activeMs >= MAX_WINDOW_MS) finish()
    }

    private companion object {
        const val WARMUP_MS = 1_000L
        const val MIN_WINDOW_MS = 3_000L
        const val MIN_WINDOW_BYTES = 8L * 1024 * 1024
        const val SLOW_WINDOW_MS = 10_000L
        const val MAX_WINDOW_MS = 10_000L
        const val MAX_TICK_GAP_MS = 2_000L
    }
}
