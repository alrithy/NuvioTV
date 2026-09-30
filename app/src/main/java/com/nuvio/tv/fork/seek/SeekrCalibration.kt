package com.nuvio.tv.fork.seek

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Automatic Seekr calibration against real frames of the playing file (G7b, features 111, 113,
 * 114). ALGORITHM_PORT of Cxsmo `SeekrFrameCalibrator` @ 3e0d0fa (luma-grid similarity, weak and
 * ambiguous match rejection, median + MAD over the per-anchor results, confidence), turned around
 * per D052: the reference frames are the on-device keyframe thumbnails of the playing release,
 * so the live player is never sought. Each anchor is one local frame; its candidates are the
 * Seekr thumbnails at a set of offsets around it. Pure: no Android types.
 */
object SeekrCalibration {
    const val MIN_SIMILARITY = 0.56
    const val MIN_MARGIN = 0.035
    const val MAX_OFFSET_MS = SEEK_PREVIEW_OFFSET_MAX_MS.toLong()

    /** Fewer agreeing anchors cannot tell a constant offset from one lucky match. */
    const val MIN_ANCHORS = 3
    const val MIN_CONFIDENCE = 0.5f

    /** Offsets closer than this are within one Seekr cue of each other; no gain in applying. */
    const val MIN_USEFUL_OFFSET_MS = 1_000L

    const val GRID_WIDTH = 32
    const val GRID_HEIGHT = 18

    data class Candidate(val offsetMs: Long, val similarity: Double)

    /** One local frame and the Seekr thumbnails compared with it (one candidate per distinct cue). */
    data class Anchor(val localFrameMs: Long, val candidates: List<Candidate>)

    data class Result(
        val offsetMs: Long = 0L,
        val confidence: Float = 0f,
        val anchorsUsed: Int = 0,
        val calibrated: Boolean = false,
    ) {
        /** Strong enough to apply automatically (113); anything else keeps the current offset (114). */
        val accepted: Boolean
            get() = calibrated && anchorsUsed >= MIN_ANCHORS && confidence >= MIN_CONFIDENCE
    }

    /**
     * Best candidate per anchor, rejecting weak (< [MIN_SIMILARITY]) and ambiguous (runner-up within
     * [MIN_MARGIN]) ones, then a median over the survivors with a MAD band, so one anchor on a scene
     * cut cannot move the whole title.
     */
    fun estimate(anchors: List<Anchor>): Result {
        if (anchors.isEmpty()) return Result()
        val matches = anchors.mapNotNull { anchor ->
            val ranked = anchor.candidates.sortedByDescending { it.similarity }
            val best = ranked.firstOrNull() ?: return@mapNotNull null
            val second = ranked.getOrNull(1)
            if (best.similarity < MIN_SIMILARITY) return@mapNotNull null
            if (second != null && best.similarity - second.similarity < MIN_MARGIN) return@mapNotNull null
            best
        }
        if (matches.size < 2) return Result()

        val sorted = matches.map { it.offsetMs }.sorted()
        val median = median(sorted)
        val mad = medianDouble(sorted.map { abs(it - median) }.sorted())
        val tolerance = max(1_250.0, mad * 3.0)
        val inliers = matches.filter { abs(it.offsetMs - median) <= tolerance }
        if (inliers.size < 2) return Result()

        val offset = median(inliers.map { it.offsetMs }.sorted())
            .coerceIn(-MAX_OFFSET_MS.toDouble(), MAX_OFFSET_MS.toDouble())
            .toLong()
        val confidence = (inliers.map { it.similarity }.average() * inliers.size / anchors.size)
            .toFloat().coerceIn(0f, 1f)
        return Result(offsetMs = offset, confidence = confidence, anchorsUsed = inliers.size, calibrated = true)
    }

    /**
     * Offsets tried per anchor: a grid of [steps] cues either side of 0 and of the duration-gap
     * hint, within ±[MAX_OFFSET_MS]. Duplicates (same cue reached twice) are removed by the caller.
     */
    fun candidateOffsets(cueIntervalMs: Long, suggestedOffsetMs: Long, steps: Int = 3): List<Long> {
        val interval = cueIntervalMs.coerceAtLeast(1_000L)
        val centres = listOf(0L, suggestedOffsetMs).distinct()
        return centres.flatMap { centre -> (-steps..steps).map { centre + it * interval } }
            .filter { abs(it) <= MAX_OFFSET_MS }
            .distinct()
            .sortedBy { abs(it) }
    }

    /**
     * [GRID_WIDTH] × [GRID_HEIGHT] luminance samples of an ARGB image, read at grid points so any
     * size or letterbox scales the same way.
     */
    fun lumaGrid(width: Int, height: Int, pixelAt: (x: Int, y: Int) -> Int): DoubleArray? {
        if (width <= 0 || height <= 0) return null
        val result = DoubleArray(GRID_WIDTH * GRID_HEIGHT)
        var index = 0
        for (y in 0 until GRID_HEIGHT) {
            val sourceY = min(height - 1, y * height / GRID_HEIGHT)
            for (x in 0 until GRID_WIDTH) {
                val sourceX = min(width - 1, x * width / GRID_WIDTH)
                val pixel = pixelAt(sourceX, sourceY)
                result[index++] = 0.2126 * ((pixel shr 16) and 0xff) +
                    0.7152 * ((pixel shr 8) and 0xff) +
                    0.0722 * (pixel and 0xff)
            }
        }
        return result
    }

    /** Normalized cross-correlation of two luma grids mapped to [0, 1]; flat images score 0. */
    fun similarity(first: DoubleArray?, second: DoubleArray?): Double {
        if (first == null || second == null || first.size != second.size || first.isEmpty()) return 0.0
        val firstMean = first.average()
        val secondMean = second.average()
        var numerator = 0.0
        var firstEnergy = 0.0
        var secondEnergy = 0.0
        for (i in first.indices) {
            val a = first[i] - firstMean
            val b = second[i] - secondMean
            numerator += a * b
            firstEnergy += a * a
            secondEnergy += b * b
        }
        if (firstEnergy <= 1e-9 || secondEnergy <= 1e-9) return 0.0
        return (numerator / sqrt(firstEnergy * secondEnergy) + 1.0) / 2.0
    }

    private fun median(values: List<Long>): Double {
        if (values.isEmpty()) return 0.0
        val middle = values.size / 2
        return if (values.size % 2 == 1) values[middle].toDouble()
        else (values[middle - 1] + values[middle]).toDouble() / 2.0
    }

    private fun medianDouble(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val middle = values.size / 2
        return if (values.size % 2 == 1) values[middle] else (values[middle - 1] + values[middle]) / 2.0
    }
}
