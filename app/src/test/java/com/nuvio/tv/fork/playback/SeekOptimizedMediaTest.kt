package com.nuvio.tv.fork.playback

import com.nuvio.tv.fork.resource.AdaptiveResourcePolicy
import com.nuvio.tv.fork.resource.MemoryTier
import org.junit.Assert.assertEquals
import org.junit.Test

class SeekOptimizedMediaTest {

    private val gib = 1024L * 1024 * 1024
    private val mib = 1024L * 1024

    private fun mode(
        seekOptimized: Boolean = true,
        parallel: Boolean = false,
        adaptive: Boolean = false,
        loopback: Boolean = false,
        mp4: Boolean = false,
        readAheadMb: Int = 512,
    ) = SeekOptimizedMedia.mode(seekOptimized, parallel, adaptive, loopback, mp4, readAheadMb)

    @Test
    fun onlySeekOptimizedProgressiveRemoteStreamsGetAMechanism() {
        assertEquals(SeekMediaMode.READ_AHEAD, mode())
        assertEquals(SeekMediaMode.MP4_SESSION, mode(mp4 = true))
        assertEquals(SeekMediaMode.OFFICIAL, mode(seekOptimized = false))
        assertEquals(SeekMediaMode.OFFICIAL, mode(adaptive = true))
        assertEquals(SeekMediaMode.OFFICIAL, mode(loopback = true))
    }

    @Test
    fun neverStacksOnTheParallelRemuxPath() {
        // D006: the disk read-ahead and parallel REMUX are never combined.
        assertEquals(SeekMediaMode.OFFICIAL, mode(parallel = true))
        assertEquals(SeekMediaMode.OFFICIAL, mode(parallel = true, mp4 = true))
    }

    @Test
    fun mp4UsesTheChunkSessionEvenWithoutARing() {
        assertEquals(SeekMediaMode.MP4_SESSION, mode(mp4 = true, readAheadMb = 0))
        assertEquals(SeekMediaMode.OFFICIAL, mode(readAheadMb = 0))
    }

    @Test
    fun ringSizeFollowsTheMemoryTier() {
        assertEquals(0, SeekOptimizedMedia.readAheadMb(AdaptiveResourcePolicy(MemoryTier.LOW_RAM)))
        assertEquals(256, SeekOptimizedMedia.readAheadMb(AdaptiveResourcePolicy(MemoryTier.CONSTRAINED)))
        assertEquals(512, SeekOptimizedMedia.readAheadMb(AdaptiveResourcePolicy(MemoryTier.STANDARD)))
    }

    @Test
    fun ringCapacityLeavesStorageHeadroom() {
        val chosen = 512 * mib
        assertEquals(chosen, SeekOptimizedMedia.ringCapacityBytes(chosen, 20 * gib))
        // Half the free space, and never into the last 1 GiB.
        assertEquals(1536 * mib, SeekOptimizedMedia.ringCapacityBytes(4 * gib, 3 * gib))
        assertEquals(376 * mib, SeekOptimizedMedia.ringCapacityBytes(chosen, 1400 * mib))
        assertEquals(0L, SeekOptimizedMedia.ringCapacityBytes(chosen, gib + 16 * mib))
        assertEquals(0L, SeekOptimizedMedia.ringCapacityBytes(0L, 20 * gib))
        // Unknown free space keeps the chosen size (Reshaped behavior).
        assertEquals(chosen, SeekOptimizedMedia.ringCapacityBytes(chosen, 0L))
    }
}
