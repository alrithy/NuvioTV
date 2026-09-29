package com.nuvio.tv.fork.resource

import com.nuvio.tv.fork.foundation.FeatureMode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class AdaptiveResourcesTest {

    private val mib = 1024L * 1024L
    private val low = AdaptiveResourcePolicy(MemoryTier.LOW_RAM)
    private val constrained = AdaptiveResourcePolicy(MemoryTier.CONSTRAINED)
    private val standard = AdaptiveResourcePolicy(MemoryTier.STANDARD)

    @After
    fun tearDown() = AdaptiveResources.resetForTest()

    @Test
    fun tierCutsSitWhereDocumented() {
        assertEquals(MemoryTier.LOW_RAM, MemoryTier.classify(1600, isLowRamDevice = false))
        assertEquals(MemoryTier.CONSTRAINED, MemoryTier.classify(1601, isLowRamDevice = false))
        assertEquals(MemoryTier.CONSTRAINED, MemoryTier.classify(2560, isLowRamDevice = false))
        assertEquals(MemoryTier.STANDARD, MemoryTier.classify(2561, isLowRamDevice = false))
    }

    @Test
    fun twoGigBoxReportingAbout1800MbIsConstrainedNotLowRam() {
        val policy = AdaptiveResourcePolicy(MemoryTier.classify(1800, isLowRamDevice = false))
        assertTrue(policy.isConstrained)
        assertFalse(policy.isLowRam)
    }

    @Test
    fun unknownOrZeroRamCountsAsLowRam() {
        assertEquals(MemoryTier.LOW_RAM, MemoryTier.classify(0, isLowRamDevice = false))
        assertEquals(MemoryTier.LOW_RAM, MemoryTier.classify(-1, isLowRamDevice = false))
    }

    @Test
    fun isLowRamDeviceWinsOverAmpleRam() {
        assertEquals(MemoryTier.LOW_RAM, MemoryTier.classify(8192, isLowRamDevice = true))
    }

    @Test
    fun standardTierKeepsEveryOfficialValue() {
        assertNull(standard.addonFetchConcurrency)
        assertNull(standard.addonFetchLimiter())
        assertEquals(3, standard.catalogLoadConcurrency(3))
        assertEquals(0.25, standard.imageMemoryCachePercent(0.25), 0.0)
        assertFalse(standard.allowRgb565(userPreference = false))
        assertTrue(standard.allowRgb565(userPreference = true))
        assertTrue(standard.animatedPosters)
        assertTrue(standard.posterRevalidation)
        assertEquals(4, standard.imageDecodeParallelism(4))
        assertEquals(0 until 7, standard.postPlayPrefetchIndices(7, 3))
        assertFalse(standard.isConstrained)
    }

    @Test
    fun constrainedTierOnlyBoundsFanOut() {
        assertEquals(6, constrained.addonFetchConcurrency)
        assertEquals(3, constrained.catalogLoadConcurrency(3))
        assertEquals(0.15, constrained.imageMemoryCachePercent(0.15), 0.0)
        assertFalse(constrained.allowRgb565(userPreference = false))
        assertTrue(constrained.animatedPosters)
        assertTrue(constrained.posterRevalidation)
        assertEquals(4, constrained.imageDecodeParallelism(4))
        assertEquals(0 until 7, constrained.postPlayPrefetchIndices(7, 3))
    }

    @Test
    fun lowRamTierAppliesComfortCuts() {
        assertEquals(3, low.addonFetchConcurrency)
        assertEquals(2, low.catalogLoadConcurrency(3))
        assertEquals(0.08, low.imageMemoryCachePercent(0.15), 0.0)
        assertTrue(low.allowRgb565(userPreference = false))
        assertFalse(low.animatedPosters)
        assertFalse(low.posterRevalidation)
        assertEquals(2, low.imageDecodeParallelism(4))
    }

    @Test
    fun lowRamCutsNeverRaiseAnOfficialValue() {
        assertEquals(1, low.catalogLoadConcurrency(1))
        assertEquals(0.05, low.imageMemoryCachePercent(0.05), 0.0)
        assertEquals(1, low.imageDecodeParallelism(1))
    }

    @Test
    fun constrainedDevicesGetAllocationSafetyLimits() {
        // A 2 GB box with largeHeap: 512 MB heap, heap reserve leaves 302 MB; the ceiling wins.
        assertEquals(250, constrained.heapBufferBudgetMb(302))
        assertEquals(250, low.heapBufferBudgetMb(302))
        // The heap reserve still wins when it is tighter.
        assertEquals(190, constrained.heapBufferBudgetMb(190))
        // Performance mode's 16 connections come back to the non-performance maximum.
        assertEquals(4, constrained.parallelConnections(16))
        assertEquals(4, low.parallelConnections(16))
        assertEquals(2, constrained.parallelConnections(2))
    }

    @Test
    fun standardDevicesKeepOfficialPlaybackBudgets() {
        assertEquals(1740, standard.heapBufferBudgetMb(1740))
        assertEquals(16, standard.parallelConnections(16))
    }

    @Test
    fun boundedCacheKeepsTheOfficialMapOnStandardDevices() {
        val official = java.util.concurrent.ConcurrentHashMap<String, Int>()
        assertSame(official, standard.boundedCache(official, 2))
    }

    @Test
    fun boundedCacheEvictsOnConstrainedAndLowRamDevices() {
        for (policy in listOf(constrained, low)) {
            val cache = policy.boundedCache(java.util.concurrent.ConcurrentHashMap<String, Int>(), 2)
            cache["a"] = 1
            cache["b"] = 2
            cache["c"] = 3
            assertEquals(2, cache.size)
            assertFalse(cache.containsKey("a"))
        }
    }

    @Test
    fun lowRamPostPlayResolvesOnlyTheCardOnScreen() {
        assertEquals(2..2, low.postPlayPrefetchIndices(5, 2))
        assertTrue(low.postPlayPrefetchIndices(5, 9).isEmpty())
        assertTrue(low.postPlayPrefetchIndices(5, -1).isEmpty())
        assertTrue(low.postPlayPrefetchIndices(0, 0).isEmpty())
        assertTrue(standard.postPlayPrefetchIndices(0, 0).isEmpty())
    }

    @Test
    fun beforeInstallThePolicyIsOfficial() {
        assertSame(AdaptiveResourcePolicy.OFFICIAL, AdaptiveResources.policy)
        assertNull(AdaptiveResources.detectedTier)
    }

    @Test
    fun installClassifiesPhysicalRamBytes() {
        AdaptiveResources.install(1600 * mib, isLowRamDevice = false, mode = FeatureMode.AUTO)
        assertEquals(MemoryTier.LOW_RAM, AdaptiveResources.policy.tier)
        AdaptiveResources.install(1801 * mib, isLowRamDevice = false, mode = FeatureMode.ON)
        assertEquals(MemoryTier.CONSTRAINED, AdaptiveResources.policy.tier)
        AdaptiveResources.install(4096 * mib, isLowRamDevice = false, mode = FeatureMode.AUTO)
        assertEquals(MemoryTier.STANDARD, AdaptiveResources.policy.tier)
    }

    @Test
    fun offModeKeepsOfficialButStillReportsTheDetectedTier() {
        AdaptiveResources.install(1024 * mib, isLowRamDevice = true, mode = FeatureMode.OFF)
        assertSame(AdaptiveResourcePolicy.OFFICIAL, AdaptiveResources.policy)
        assertEquals(MemoryTier.LOW_RAM, AdaptiveResources.detectedTier)
    }

    @Test
    fun optionalPermitBoundsConcurrencyOnlyWhenLimited() = runBlocking {
        assertEquals(3, peakConcurrency(low.addonFetchLimiter(), jobs = 10))
        assertEquals(10, peakConcurrency(null, jobs = 10))
    }

    private suspend fun peakConcurrency(limiter: Semaphore?, jobs: Int): Int {
        val active = AtomicInteger()
        val peak = AtomicInteger()
        kotlinx.coroutines.coroutineScope {
            (1..jobs).map {
                async {
                    limiter.withOptionalPermit {
                        peak.accumulateAndGet(active.incrementAndGet(), ::maxOf)
                        delay(20)
                        active.decrementAndGet()
                    }
                }
            }.awaitAll()
        }
        return peak.get()
    }
}
