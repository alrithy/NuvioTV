package com.nuvio.tv.fork.streams

import com.nuvio.tv.fork.diagnostics.AddonHealthState

/**
 * What the G8b ranker compares for one stream (D053). Every field is "lower is better" except
 * [resolution] and [sizeBytes]. Built by [StreamRanker] from official `DirectDebridStreamFilter`
 * facts, so this file stays pure and the order is unit-tested on its own.
 */
data class StreamRankInput(
    /** 0 ready to play (cached debrid or a direct link), 1 P2P, 2 cache still unknown, 3 not cached. */
    val cacheTier: Int,
    /** Vertical lines (2160, 1080, …); 0 when unknown. */
    val resolution: Int,
    val quality: Int,
    val releaseGroupTier: Int,
    val visual: Int,
    val audio: Int,
    val channels: Int,
    val encode: Int,
    /** Within one title the runtime is the same for every stream, so size orders by average bitrate. */
    val sizeBytes: Long?,
    val reliability: Int,
)

/**
 * The ranking chain (ALGORITHM_PORT of Cxsmo `StreamQualityRank` @ 3e0d0fa, reordered and extended
 * per D053): cache tier → resolution → quality → release-group tier → HDR/DV → audio (lossless
 * first) → channels → codec → size (bitrate) → source reliability. Stable: ties keep the incoming
 * add-on order, and nothing is ever dropped.
 */
object StreamRankRules {

    val COMPARATOR: Comparator<StreamRankInput> =
        compareBy<StreamRankInput> { it.cacheTier }
            .thenByDescending { it.resolution }
            .thenBy { it.quality }
            .thenBy { it.releaseGroupTier }
            .thenBy { it.visual }
            .thenBy { it.audio }
            .thenBy { it.channels }
            .thenBy { it.encode }
            .thenByDescending { it.sizeBytes ?: -1L }
            .thenBy { it.reliability }

    /** Stable best-first order; [inputFor] runs once per item. */
    fun <T> rank(items: List<T>, inputFor: (T) -> StreamRankInput): List<T> {
        if (items.size <= 1) return items
        val inputs = items.map(inputFor)
        return items.indices.sortedWith { a, b -> COMPARATOR.compare(inputs[a], inputs[b]) }.map { items[it] }
    }

    /**
     * HDR / DV (feature 162). With a Dolby Vision display: DV with an HDR base layer, DV, HDR10+,
     * HDR, SDR. Without one, DV-only releases (no HDR base layer to fall back to) go last and a DV
     * release with an HDR base layer counts as HDR.
     */
    fun visual(dv: Boolean, hdr: Boolean, hdr10Plus: Boolean, displaySupportsDv: Boolean): Int = when {
        displaySupportsDv && dv && hdr -> 0
        displaySupportsDv && dv -> 1
        hdr10Plus -> 2
        hdr -> 3
        dv -> 5
        else -> 4
    }

    /** Feature 166: the last tiebreak; a source that failed recently ranks below one that did not. */
    fun reliability(state: AddonHealthState?): Int = when (state) {
        AddonHealthState.HEALTHY -> 0
        null, AddonHealthState.UNKNOWN -> 1
        AddonHealthState.SLOW, AddonHealthState.NO_STREAMS -> 2
        AddonHealthState.TIMEOUT, AddonHealthState.AUTH_ERROR,
        AddonHealthState.MANIFEST_ERROR, AddonHealthState.REQUEST_ERROR -> 3
    }
}
