package com.nuvio.tv.ui.screens.home

import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.nuvio.tv.fork.resource.MemoryTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetflixHomePreviewPolicyTest {
    @Test fun `resource tiers keep previews bounded and stop video and motion on weaker TVs`() {
        val capable = netflixHomePreviewPolicy(MemoryTier.STANDARD)
        val constrained = netflixHomePreviewPolicy(MemoryTier.CONSTRAINED)
        val lowRam = netflixHomePreviewPolicy(MemoryTier.LOW_RAM)
        assertTrue(capable.allowVideo)
        assertFalse(constrained.allowVideo)
        assertFalse(lowRam.allowVideo)
        assertFalse(lowRam.animate)
        assertTrue(capable.expandedScale > constrained.expandedScale)
        assertEquals(1f, lowRam.expandedScale, 0f)
        val tiers = listOf(MemoryTier.STANDARD, MemoryTier.CONSTRAINED, MemoryTier.LOW_RAM)
        assertEquals(listOf(12, 8, 6), tiers.map(::netflixRecommendationLimit))
        assertEquals(listOf(3840, 1920, 1280), tiers.map { netflixHomePreviewPolicy(it).heroMaxWidthPx })
    }

    @Test fun `floating previews clamp to safe margins at either edge and resolution`() {
        for (scale in listOf(2, 4)) {
            val window = IntSize(960 * scale, 540 * scale)
            val content = IntSize(200 * scale, 230 * scale)
            val margin = 24 * scale
            val anchors = listOf(IntRect(0, 0, 160 * scale, 90 * scale),
                IntRect(window.width - 160 * scale, window.height - 90 * scale, window.width, window.height))
            for (anchor in anchors) {
                val position = netflixPreviewPosition(anchor, window, content, margin)
                assertTrue(position.x >= margin && position.y >= margin)
                assertTrue(position.x + content.width <= window.width - margin)
                assertTrue(position.y + content.height <= window.height - margin)
            }
        }
    }

    @Test fun `preview action traversal keeps internal focus and exits only at physical outer edges in RTL and LTR`() {
        for (rtl in listOf(false, true)) {
            assertEquals(FocusDirection.Left, netflixPreviewExitDirection(Key.DirectionLeft, if (rtl) 2 else 0, rtl))
            assertEquals(FocusDirection.Right, netflixPreviewExitDirection(Key.DirectionRight, if (rtl) 0 else 2, rtl))
            assertNull(netflixPreviewExitDirection(Key.DirectionLeft, 1, rtl))
            assertNull(netflixPreviewExitDirection(Key.DirectionRight, 1, rtl))
            assertEquals(FocusDirection.Up, netflixPreviewExitDirection(Key.DirectionUp, 1, rtl))
            assertEquals(FocusDirection.Down, netflixPreviewExitDirection(Key.DirectionDown, 1, rtl))
            assertNull(netflixPreviewExitDirection(Key.DirectionCenter, 0, rtl))
        }
    }
}
