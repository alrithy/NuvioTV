package com.nuvio.tv.fork.playback

import com.nuvio.tv.fork.foundation.FeatureMode

/**
 * Press-time connection warm-up decisions (G4b, feature 18). ALGORITHM_PORT of ysosrs
 * `PlayerPlaybackNetworking.prewarmPlaybackConnection` @ 45e0984 (S1g/S1m/nt8/B-2).
 *
 * At press the stream URL is known ~0.7 s before the player's first open, and upstream measured
 * ~1 s of TCP+TLS on that open. The warm-up fetches the head window the open reads first and the
 * tail window a Matroska extractor reads next (Cues) into the official `PrefetchWindowStore`,
 * whose consumer already exists in the official `ParallelRangeDataSource`. That data source only
 * runs on the parallel REMUX path, so the warm-up runs only when the effective strategy is
 * REMUX / Throughput; every other strategy keeps official behavior (no extra requests).
 */
object ConnectionPrewarmPolicy {
    /** Exactly the official bootstrap read, so the probe finds the bytes it asks for. */
    const val HEAD_WINDOW_BYTES = 262_144L

    /** A repeated press on the same URL inside this window sends nothing. */
    const val DEDUP_WINDOW_MS = 60_000L

    fun shouldPrewarm(remuxPerformance: FeatureMode, decision: StrategyDecision, url: String?): Boolean =
        remuxPerformance != FeatureMode.OFF &&
            decision.effective == PlaybackStrategy.REMUX_THROUGHPUT &&
            isHttp(url)

    /** The tail window is skipped on the low-RAM tier: stored windows stay on the heap (5 min TTL). */
    fun warmTail(lowRam: Boolean): Boolean = !lowRam

    fun isDuplicate(url: String, nowMs: Long, lastUrl: String?, lastAtMs: Long): Boolean =
        url == lastUrl && (nowMs - lastAtMs) in 0 until DEDUP_WINDOW_MS

    /** Total length from "bytes 0-262143/9402232472"; -1 when absent or opaque. */
    fun contentRangeTotal(value: String?): Long {
        val totalPart = value?.substringAfterLast('/', missingDelimiterValue = "")?.trim() ?: return -1L
        if (totalPart.isEmpty() || totalPart == "*") return -1L
        return totalPart.toLongOrNull() ?: -1L
    }

    /** Window start from "bytes 71196784383-71200978686/71200978687"; -1 when absent or opaque. */
    fun contentRangeStart(value: String?): Long {
        val rangePart = value?.trim()?.substringAfter("bytes", "")?.substringBefore('/')?.trim() ?: return -1L
        if (rangePart.isEmpty() || rangePart == "*") return -1L
        return rangePart.substringBefore('-').trim().toLongOrNull() ?: -1L
    }

    /** A head body is stored only when it is non-empty, fits the window and the total is known. */
    fun isStorableHead(bodyBytes: Int, total: Long): Boolean =
        bodyBytes > 0 && bodyBytes <= HEAD_WINDOW_BYTES && total > 0L

    /**
     * A tail body is stored only when it is exactly [tailWindow] bytes ending at [total], so the
     * position arithmetic of the official consumer holds.
     */
    fun isStorableTail(start: Long, bodyBytes: Long, total: Long, tailWindow: Long): Boolean =
        start >= 0L && total > 0L && bodyBytes == tailWindow && start + bodyBytes == total

    /** Fallback tail start once the head gave the total; null when the head already covers it. */
    fun fallbackTailStart(total: Long, tailWindow: Long): Long? =
        (total - tailWindow).takeIf { it > HEAD_WINDOW_BYTES }

    private fun isHttp(url: String?): Boolean {
        val target = url?.trim().orEmpty()
        return target.startsWith("http://", ignoreCase = true) || target.startsWith("https://", ignoreCase = true)
    }
}
