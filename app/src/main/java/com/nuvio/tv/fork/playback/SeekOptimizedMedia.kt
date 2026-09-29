package com.nuvio.tv.fork.playback

import com.nuvio.tv.fork.resource.AdaptiveResourcePolicy
import com.nuvio.tv.fork.resource.MemoryTier

/**
 * What the Seek optimized strategy adds under the ExoPlayer data source (G4d, features 24, 25).
 * One mechanism per stream, never stacked with each other or with parallel REMUX (D006):
 * - progressive MP4: ysosrs "MP4 session mode" (45e0984 `PlayerMediaSourceFactory`), a
 *   single-connection 8 MiB chunk session whose chunks survive the data-source recreation of
 *   every seek, for non-faststart / poorly interleaved files that force scatter reads;
 * - other progressive http(s) files: Reshaped disk read-ahead ring (0ccf049 `SeekReadAhead`), one
 *   connection filling a ring file ahead of playback so seeks inside it need no network.
 * A ring would thrash on MP4 scatter reads (every out-of-ring read moves it), hence the split.
 */
enum class SeekMediaMode { OFFICIAL, MP4_SESSION, READ_AHEAD }

object SeekOptimizedMedia {
    const val MP4_SESSION_CHUNK_BYTES = 8L * 1024 * 1024
    const val MP4_SESSION_CONNECTIONS = 1

    /** Reshaped offered 256 MB / 512 MB / 1 GB (default off); the strategy picks by tier. */
    const val READ_AHEAD_MB_STANDARD = 512
    const val READ_AHEAD_MB_CONSTRAINED = 256

    /** Reshaped: a smaller ring is not worth a file. */
    const val MIN_READ_AHEAD_BYTES = 32L * 1024 * 1024

    /** Reshaped: same headroom the official VOD cache leaves on low-storage boxes. */
    const val FREE_SPACE_RESERVE_BYTES = 1024L * 1024 * 1024

    fun mode(
        seekOptimized: Boolean,
        parallelConnections: Boolean,
        adaptive: Boolean,
        loopback: Boolean,
        mp4: Boolean,
        readAheadMb: Int,
    ): SeekMediaMode = when {
        !seekOptimized || parallelConnections || adaptive || loopback -> SeekMediaMode.OFFICIAL
        mp4 -> SeekMediaMode.MP4_SESSION
        readAheadMb > 0 -> SeekMediaMode.READ_AHEAD
        else -> SeekMediaMode.OFFICIAL
    }

    /** Full-speed disk writes are too heavy for the low-RAM tier (Reshaped kept it off by default). */
    fun readAheadMb(policy: AdaptiveResourcePolicy): Int = when (policy.tier) {
        MemoryTier.LOW_RAM -> 0
        MemoryTier.CONSTRAINED -> READ_AHEAD_MB_CONSTRAINED
        MemoryTier.STANDARD -> READ_AHEAD_MB_STANDARD
    }

    /** Ring size for [chosenBytes] given [freeBytes] of storage; 0 means stay direct. */
    fun ringCapacityBytes(chosenBytes: Long, freeBytes: Long): Long {
        if (chosenBytes <= 0L) return 0L
        val capacity = if (freeBytes > 0L) {
            minOf(chosenBytes, freeBytes / 2, freeBytes - FREE_SPACE_RESERVE_BYTES)
        } else {
            chosenBytes
        }
        return if (capacity < MIN_READ_AHEAD_BYTES) 0L else capacity
    }
}
