package com.nuvio.tv.fork.resource

import com.nuvio.tv.fork.foundation.FeatureMode
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Device memory class, decided by physical RAM rather than heap size: `largeHeap` reports the
 * same ~512 MB heap on a 2 GB box as on an 8 GB one. [LOW_RAM] implies constrained.
 */
enum class MemoryTier {
    /** 1–1.5 GB class, or `isLowRamDevice`, or RAM that could not be read. */
    LOW_RAM,

    /** 2 GB class: allocation-safety limits only. */
    CONSTRAINED,

    /** Everything above: official values, nothing capped. */
    STANDARD;

    companion object {
        /** 1.5 GB devices report ~1.4 GB of totalMem, so this cut leaves 2 GB boxes out. */
        const val LOW_RAM_THRESHOLD_MB = 1600L

        /** 2 GB devices report ~1.8 GB of totalMem, so the cut sits above that. */
        const val CONSTRAINED_THRESHOLD_MB = 2560L

        /**
         * Unknown RAM (0 or negative) counts as [LOW_RAM]: over-budgeting a 1 GB box gets the
         * process LMK-killed, under-budgeting an 8 GB one only costs comfort.
         */
        fun classify(totalRamMb: Long, isLowRamDevice: Boolean): MemoryTier = when {
            isLowRamDevice || totalRamMb <= LOW_RAM_THRESHOLD_MB -> LOW_RAM
            totalRamMb <= CONSTRAINED_THRESHOLD_MB -> CONSTRAINED
            else -> STANDARD
        }
    }
}

/**
 * The one resource policy later subsystems read. Comfort cuts (cache share, animations,
 * speculative prefetch, fan-out width) key on [isLowRam]; allocation-safety limits key on
 * [isConstrained]. On [MemoryTier.STANDARD] every value is the official one, so strong devices
 * are never capped by this policy.
 */
class AdaptiveResourcePolicy(val tier: MemoryTier) {

    val isLowRam: Boolean get() = tier == MemoryTier.LOW_RAM

    val isConstrained: Boolean get() = tier != MemoryTier.STANDARD

    /**
     * Addon stream/subtitle requests in flight at once; `null` keeps official (unbounded).
     * Each request holds a response body, its parsed DTOs and the mapped list simultaneously.
     */
    val addonFetchConcurrency: Int?
        get() = when (tier) {
            MemoryTier.LOW_RAM -> 3
            MemoryTier.CONSTRAINED -> 6
            MemoryTier.STANDARD -> null
        }

    /**
     * Allocation safety: ceiling on the Java-heap playback buffer budget. `largeHeap` reports the
     * same ~512 MB heap on a 2 GB box as on an 8 GB one, so the heap ratio alone would hand a
     * constrained device ~300 MB of buffers and get the process LMK-killed before any OOM.
     */
    fun heapBufferBudgetMb(official: Int): Int =
        if (isConstrained) official.coerceAtMost(CONSTRAINED_BUFFER_BUDGET_CEILING_MB) else official

    /**
     * Allocation safety: parallel range connections for one playback session. Performance mode
     * stores up to 16, and the prefetch floor is two chunks per connection, which alone exceeds
     * the native safe limit of a 2 GB device.
     */
    fun parallelConnections(official: Int): Int =
        if (isConstrained) official.coerceAtMost(CONSTRAINED_MAX_PARALLEL_CONNECTIONS) else official

    /**
     * An in-memory metadata/rating cache: the [official] map on standard devices, a bounded LRU
     * of [constrainedMaxEntries] on constrained ones, where a TTL alone never drops old entries
     * and every row, card and detail page visited stays resident for the process lifetime.
     */
    fun <K, V> boundedCache(official: MutableMap<K, V>, constrainedMaxEntries: Int): MutableMap<K, V> =
        if (isConstrained) lruCacheMap(constrainedMaxEntries) else official

    /** Home catalog rows loading at once; never above the official value. */
    fun catalogLoadConcurrency(official: Int): Int =
        if (isLowRam) official.coerceAtMost(LOW_RAM_CATALOG_CONCURRENCY) else official

    /** Share of the heap given to the poster memory cache; never above the official share. */
    fun imageMemoryCachePercent(official: Double): Double =
        if (isLowRam) official.coerceAtMost(LOW_RAM_IMAGE_CACHE_PERCENT) else official

    /** RGB_565 halves poster bytes; forced where the bytes are not optional. */
    fun allowRgb565(userPreference: Boolean): Boolean = isLowRam || userPreference

    /** Animated GIF/WebP/HEIF posters retain every frame, dwarfing the poster cache. */
    val animatedPosters: Boolean get() = !isLowRam

    /** Background stale-while-revalidate poster refreshes; normal cache expiry still applies. */
    val posterRevalidation: Boolean get() = !isLowRam

    fun imageDecodeParallelism(official: Int): Int =
        if (isLowRam) official.coerceAtMost(LOW_RAM_DECODE_PARALLELISM) else official

    /**
     * Post-play candidates resolved up front. Each costs an addon meta fetch, a TMDB lookup,
     * ratings and a trailer lookup while the video pipeline is still up, so low-RAM devices
     * resolve only the card on screen and page the rest in on demand.
     */
    fun postPlayPrefetchIndices(count: Int, currentIndex: Int): IntRange = when {
        count <= 0 -> IntRange.EMPTY
        !isLowRam -> 0 until count
        currentIndex in 0 until count -> currentIndex..currentIndex
        else -> IntRange.EMPTY
    }

    companion object {
        const val LOW_RAM_CATALOG_CONCURRENCY = 2
        const val CONSTRAINED_BUFFER_BUDGET_CEILING_MB = 250

        /** Official `MemoryBudget.MAX_CONNECTIONS`, the non-performance-mode maximum. */
        const val CONSTRAINED_MAX_PARALLEL_CONNECTIONS = 4
        const val LOW_RAM_IMAGE_CACHE_PERCENT = 0.08
        const val LOW_RAM_DECODE_PARALLELISM = 2

        /** Official behavior everywhere: used when the manager is OFF or not yet installed. */
        val OFFICIAL = AdaptiveResourcePolicy(MemoryTier.STANDARD)
    }
}

/**
 * Process-wide holder, installed once from `NuvioApplication.onCreate` before Hilt injection so
 * no singleton can read a stale tier. Until [install] runs (unit tests, or a caller that runs
 * too early) the policy is [AdaptiveResourcePolicy.OFFICIAL], i.e. today's behavior.
 */
object AdaptiveResources {

    private const val BYTES_PER_MB = 1024L * 1024L

    @Volatile
    var policy: AdaptiveResourcePolicy = AdaptiveResourcePolicy.OFFICIAL
        private set

    /** Tier detected on this device, even when the manager is OFF (for diagnostics). */
    @Volatile
    var detectedTier: MemoryTier? = null
        private set

    fun install(totalRamBytes: Long, isLowRamDevice: Boolean, mode: FeatureMode) {
        val tier = MemoryTier.classify(totalRamBytes / BYTES_PER_MB, isLowRamDevice)
        detectedTier = tier
        policy = policyFor(tier, mode)
    }

    internal fun policyFor(tier: MemoryTier, mode: FeatureMode): AdaptiveResourcePolicy =
        if (mode == FeatureMode.OFF) AdaptiveResourcePolicy.OFFICIAL else AdaptiveResourcePolicy(tier)

    internal fun resetForTest() {
        policy = AdaptiveResourcePolicy.OFFICIAL
        detectedTier = null
    }
}

/** Runs [block] under a permit, or directly when there is no limit (official behavior). */
suspend inline fun <T> Semaphore?.withOptionalPermit(block: () -> T): T =
    if (this == null) block() else withPermit(block)

/** A fetch limiter for [AdaptiveResourcePolicy.addonFetchConcurrency]; `null` means unbounded. */
fun AdaptiveResourcePolicy.addonFetchLimiter(): Semaphore? = addonFetchConcurrency?.let { Semaphore(it) }
