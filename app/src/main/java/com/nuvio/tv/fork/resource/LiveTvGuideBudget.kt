package com.nuvio.tv.fork.resource

/** G10c limits supplied by AdaptiveResources, with generous room for 100+ MB guides. */
data class LiveTvGuideBudget(
    val compressedBytes: Long,
    val expandedBytes: Long,
    val downloadTimeoutMs: Long = 5 * 60_000L,
) {
    init {
        require(compressedBytes > 0 && expandedBytes > 0 && downloadTimeoutMs > 0)
    }
}
