package com.nuvio.tv.fork.seek

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SeekrCalibrationTest {

    /** Candidates every 10 s from -30 s to +30 s; [trueOffset] scores [peak], the rest [floor]. */
    private fun anchor(frameMs: Long, trueOffset: Long, peak: Double = 0.9, floor: Double = 0.5) =
        SeekrCalibration.Anchor(
            frameMs,
            (-3..3).map { step ->
                val offset = step * 10_000L
                SeekrCalibration.Candidate(offset, if (offset == trueOffset) peak else floor)
            },
        )

    @Test
    fun constantOffsetIsRecoveredAndAccepted() {
        val result = SeekrCalibration.estimate((1..5).map { anchor(it * 600_000L, trueOffset = 10_000L) })
        assertTrue(result.calibrated)
        assertTrue(result.accepted)
        assertEquals(10_000L, result.offsetMs)
        assertEquals(5, result.anchorsUsed)
    }

    @Test
    fun alignedReleaseCalibratesToZero() {
        val result = SeekrCalibration.estimate((1..4).map { anchor(it * 600_000L, trueOffset = 0L) })
        assertTrue(result.accepted)
        assertEquals(0L, result.offsetMs)
    }

    @Test
    fun oneSceneCutOutlierDoesNotMoveTheOffset() {
        val anchors = (1..4).map { anchor(it * 600_000L, trueOffset = -20_000L) } +
            anchor(3_000_000L, trueOffset = 30_000L)
        val result = SeekrCalibration.estimate(anchors)
        assertTrue(result.accepted)
        assertEquals(-20_000L, result.offsetMs)
        assertEquals(4, result.anchorsUsed)
    }

    @Test
    fun weakMatchesAreRejected() {
        val result = SeekrCalibration.estimate((1..5).map { anchor(it * 600_000L, 10_000L, peak = 0.52, floor = 0.4) })
        assertFalse(result.calibrated)
        assertFalse(result.accepted)
        assertEquals(0L, result.offsetMs)
    }

    @Test
    fun ambiguousMatchesAreRejected() {
        val ambiguous = SeekrCalibration.Anchor(
            600_000L,
            listOf(SeekrCalibration.Candidate(0L, 0.80), SeekrCalibration.Candidate(10_000L, 0.79)),
        )
        val result = SeekrCalibration.estimate(List(5) { ambiguous })
        assertFalse(result.calibrated)
    }

    @Test
    fun tooFewAgreeingAnchorsAreNotAppliedAutomatically() {
        val anchors = listOf(anchor(600_000L, 10_000L), anchor(1_200_000L, 10_000L)) +
            List(3) { SeekrCalibration.Anchor(1_800_000L + it, emptyList()) }
        val result = SeekrCalibration.estimate(anchors)
        assertTrue(result.calibrated)
        assertFalse("2 anchors is below MIN_ANCHORS", result.accepted)
    }

    @Test
    fun confidenceScalesWithAgreement() {
        val all = SeekrCalibration.estimate((1..5).map { anchor(it * 600_000L, 10_000L) })
        val some = SeekrCalibration.estimate(
            (1..3).map { anchor(it * 600_000L, 10_000L) } + List(2) { anchor(9_000_000L + it, 0L, peak = 0.3, floor = 0.2) },
        )
        assertTrue(all.confidence > some.confidence)
        assertTrue(all.confidence <= 1f)
    }

    @Test
    fun candidateOffsetsStayInBoundsAroundZeroAndTheHint() {
        val offsets = SeekrCalibration.candidateOffsets(cueIntervalMs = 10_000L, suggestedOffsetMs = 235_000L)
        assertTrue(offsets.all { abs(it) <= SeekrCalibration.MAX_OFFSET_MS })
        assertTrue(0L in offsets && 235_000L in offsets && 30_000L in offsets)
        assertEquals(offsets.distinct(), offsets)
        assertEquals(0L, offsets.first())
    }

    @Test
    fun similarityIsNormalizedCrossCorrelation() {
        val gradient = DoubleArray(SeekrCalibration.GRID_WIDTH * SeekrCalibration.GRID_HEIGHT) { it.toDouble() }
        val brighter = DoubleArray(gradient.size) { gradient[it] * 2 + 10 }
        val inverted = DoubleArray(gradient.size) { -gradient[it] }
        assertEquals(1.0, SeekrCalibration.similarity(gradient, brighter), 1e-9)
        assertEquals(0.0, SeekrCalibration.similarity(gradient, inverted), 1e-9)
        assertEquals(0.0, SeekrCalibration.similarity(gradient, DoubleArray(gradient.size) { 7.0 }), 1e-9)
        assertEquals(0.0, SeekrCalibration.similarity(gradient, null), 1e-9)
    }

    @Test
    fun lumaGridSamplesAFixedGridFromAnySize() {
        val white = 0xFFFFFFFF.toInt()
        val grid = SeekrCalibration.lumaGrid(1920, 800) { _, _ -> white }!!
        assertEquals(SeekrCalibration.GRID_WIDTH * SeekrCalibration.GRID_HEIGHT, grid.size)
        assertEquals(255.0, grid[0], 1e-6)
        assertEquals(null, SeekrCalibration.lumaGrid(0, 10) { _, _ -> white })
    }
}
