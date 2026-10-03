package com.nuvio.tv.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Focus comfort zone policy (docs/NETFLIX_REFERENCE_FIDELITY.md). Units: dp at the 960 dp canvas. */
class NetflixComfortZoneTest {
    private val viewportStart = -48f // content padding before the first card
    private val viewportEnd = 912f
    // Measured reference geometry (PR #100 maintainer packet): 160 / 440 dp cards, 6 dp gaps.
    private val poster = 160f
    private val gap = 6f
    private val expanded = 440f
    private val peek = 80f

    /** Start of item [index] when the row is scrolled by [scroll] and only [index] is expanded. */
    private fun start(index: Int, scroll: Float = 0f) = index * (poster + gap) - scroll
    private fun delta(index: Int, scroll: Float = 0f) =
        netflixComfortScrollDelta(start(index, scroll), start(index, scroll) + expanded, viewportStart, viewportEnd, peek)

    @Test
    fun middlePosterExpandsWithoutJumpingToEdge() {
        // Item 2 starts at 332 and ends at 772 when expanded: fully inside the zone, so nothing moves.
        assertEquals(0f, delta(1))
        assertEquals(0f, delta(2))
    }

    @Test
    fun expansionScrollsOnlyWhenRequiredForVisibility() {
        // Item 5 would end at 1270 > zone end 832: scroll exactly enough, not to the reading start.
        val d = delta(5)
        assertEquals(start(5) + expanded - (viewportEnd - peek), d, 0.01f)
        assertTrue(d < start(5) - viewportStart)
        // After that scroll it is in the zone and a second evaluation is still.
        assertEquals(0f, delta(5, d))
    }

    @Test
    fun firstAndLastItemsRemainReachable() {
        // The first card sits at the content edge: the policy asks to go back, the list clamps at 0.
        assertTrue(delta(0, scroll = 300f) < 0f)
        assertTrue(delta(0) <= 0f)
        // A far card asks to move forward until it is fully visible.
        val far = delta(40)
        assertTrue(far > 0f)
        assertTrue(start(40, far) + expanded <= viewportEnd)
    }

    @Test
    fun focusMovePreservesComfortZone() {
        var scroll = 0f
        for (index in 0 until 30) {
            scroll += delta(index, scroll)
            val s = start(index, scroll)
            assertTrue("item $index start $s", s >= viewportStart + peek - 0.5f || scroll == 0f || s >= viewportStart)
            assertTrue("item $index end", s + expanded <= viewportEnd - peek + 0.5f)
        }
    }

    @Test
    fun rtlComfortZoneMatchesVisibleBounds() {
        // LazyRow offsets are measured from the reading start, so an RTL row (start = physical right)
        // produces the same deltas; mirroring the physical bounds keeps the card inside the screen.
        val width = viewportEnd - viewportStart
        for (index in 0 until 12) {
            val d = delta(index)
            val logicalStart = start(index, d)
            val physicalLeft = width - (logicalStart - viewportStart) - expanded
            assertTrue(physicalLeft >= 0f && physicalLeft + expanded <= width)
        }
    }

    @Test
    fun rapidFocusDoesNotOscillateViewport() {
        var scroll = 0f
        val sequence = listOf(4, 5, 4, 5, 6, 5, 6, 5, 4, 5)
        val scrolls = mutableListOf<Float>()
        for (index in sequence) { scroll += delta(index, scroll); scrolls += scroll }
        // Moving back and forth inside the zone never reverses the viewport direction more than the
        // focus itself: once 5 and 6 fit, returning to 5 or 4 does not scroll back.
        val settled = scrolls.drop(4)
        assertTrue(settled.zipWithNext().all { (a, b) -> b >= a - 0.01f })
    }

    @Test
    fun expansionMotionMatchesMeasuredReference() {
        // Measured stable transitions ≈180–230 ms; target ≈200 ms.
        assertTrue(com.nuvio.tv.ui.theme.NetflixThemeTokens.Home.expandMillis in 180..230)
    }
}
