package com.nuvio.tv.fork.recovery

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry

/**
 * Mid-stream Matroska resync (G4c, feature 21). ALGORITHM_PORT of ysosrs `MatroskaExtractor`
 * @ 45e0984 (`resyncToNextCluster`, MAX_RESYNC_ATTEMPTS, RESYNC_BLOCK_BYTES).
 *
 * A zero-filled hole (incomplete Usenet article, CDN splice) corrupts an element header mid-file
 * and the official extractor fails playback. After the seek map is sent, the extractor skips
 * forward to the next Cluster (which opens on a keyframe) instead, a bounded number of times per
 * extractor, within a bounded scan span. Official truncated-tail handling keeps priority.
 */
object MkvResync {
    const val MAX_ATTEMPTS = 8
    const val MAX_SCAN_BYTES = 64L * 1024 * 1024
    const val BLOCK_BYTES = 64 * 1024

    /** Cluster ID, canonical 4-byte EBML encoding. */
    private val CLUSTER_ID = byteArrayOf(0x1F, 0x43, 0xB6.toByte(), 0x75)
    const val CLUSTER_ID_BYTES = 4

    /**
     * Process-wide: `FeatureRegistry` modes are fixed per process and no user override store
     * exists, so the defaults are read directly (as `AdaptiveResources.install` does).
     */
    @JvmField
    val enabled: Boolean = FeatureRegistry().mode(FeatureId.REMUX_PERFORMANCE) != FeatureMode.OFF

    /** Index of the first Cluster ID in `block[0, length)`, or -1. */
    @JvmStatic
    fun indexOfClusterId(block: ByteArray, length: Int): Int {
        var i = 0
        while (i + CLUSTER_ID_BYTES <= length) {
            if (block[i] == CLUSTER_ID[0] && block[i + 1] == CLUSTER_ID[1] &&
                block[i + 2] == CLUSTER_ID[2] && block[i + 3] == CLUSTER_ID[3]
            ) {
                return i
            }
            i++
        }
        return -1
    }

    /**
     * Bytes to skip after a full block with no Cluster ID: keep the last 3 bytes so an ID that
     * straddles the block boundary is found on the next pass.
     */
    @JvmStatic
    fun advanceAfterMiss(blockLength: Int): Int = blockLength - (CLUSTER_ID_BYTES - 1)
}
