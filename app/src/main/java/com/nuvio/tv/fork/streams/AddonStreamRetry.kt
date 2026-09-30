package com.nuvio.tv.fork.streams

/**
 * Bounded add-on retry (G8a, feature 289, deferred from G1). One retry of a stream request that
 * timed out or got a server error, only while the request is still young enough that the retry
 * fits inside every scrape mode's wait, never for client errors (4xx: bad config, missing title,
 * rate limit) and never a second time. Pure policy; the repository runs it.
 */
object AddonStreamRetry {
    const val RETRY_DELAY_MS = 750L

    /** A first attempt that took longer than this would push a retry past bounded waits. */
    const val RETRY_WINDOW_MS = 8_000L

    fun shouldRetry(code: Int?, message: String?, elapsedMs: Long, attempt: Int): Boolean {
        if (attempt >= 1 || elapsedMs > RETRY_WINDOW_MS) return false
        return when {
            code != null -> code in 500..599 || code == 408
            else -> message?.contains("timeout", ignoreCase = true) == true ||
                message?.contains("timed out", ignoreCase = true) == true
        }
    }
}
