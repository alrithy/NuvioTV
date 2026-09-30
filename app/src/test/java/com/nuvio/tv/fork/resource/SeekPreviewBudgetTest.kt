package com.nuvio.tv.fork.resource

import com.nuvio.tv.fork.foundation.FeatureMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekPreviewBudgetTest {

    private fun budget(tier: MemoryTier, ramMb: Long?) = AdaptiveResourcePolicy(tier, ramMb).seekPreviewBudget

    @Test
    fun standardDevicesOfThreeGbGetTheSourceValues() {
        val b = budget(MemoryTier.STANDARD, 4096)
        assertEquals(48, b.maxDecoded)
        assertEquals(Long.MAX_VALUE, b.maxPixels)
        assertEquals(200_000_000L, b.diskCacheBytes)
        assertEquals(96_000_000L, b.spoolBytes)
        assertTrue(b.localPreviewsByDefault)
    }

    @Test
    fun underThreeGbNeverDecodesAbove1080p() {
        assertEquals(SeekPreviewBudget.FULL_HD_PIXELS, budget(MemoryTier.STANDARD, 2900).maxPixels)
        assertEquals(SeekPreviewBudget.FULL_HD_PIXELS, budget(MemoryTier.CONSTRAINED, 2048).maxPixels)
        // RAM unknown (manager OFF or not installed): stay on the safe side.
        assertEquals(SeekPreviewBudget.FULL_HD_PIXELS, AdaptiveResourcePolicy.OFFICIAL.seekPreviewBudget.maxPixels)
    }

    @Test
    fun lowRamDevicesAreOptInWithTheSmallestBudget() {
        val b = budget(MemoryTier.LOW_RAM, 1024)
        assertFalse(b.localPreviewsByDefault)
        assertTrue(b.maxDecoded < budget(MemoryTier.CONSTRAINED, 2048).maxDecoded)
        assertTrue(b.diskCacheBytes < budget(MemoryTier.CONSTRAINED, 2048).diskCacheBytes)
    }

    @Test
    fun budgetsShrinkWithTheTier() {
        val tiers = listOf(budget(MemoryTier.LOW_RAM, 1024), budget(MemoryTier.CONSTRAINED, 2048), budget(MemoryTier.STANDARD, 8192))
        assertEquals(tiers.sortedBy { it.maxDecoded }, tiers)
        assertEquals(tiers.sortedBy { it.spoolBytes }, tiers)
    }

    @Test
    fun installRecordsRamForTheBudget() {
        try {
            AdaptiveResources.install(8L * 1024 * 1024 * 1024, isLowRamDevice = false, mode = FeatureMode.AUTO)
            assertEquals(Long.MAX_VALUE, AdaptiveResources.policy.seekPreviewBudget.maxPixels)
        } finally {
            AdaptiveResources.resetForTest()
        }
    }
}
