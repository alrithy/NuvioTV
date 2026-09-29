package com.nuvio.tv.fork.subtitles

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * The single, language-independent subtitle timing engine for the superfork.
 *
 * Algorithm-port of VibeSubtitle's cue-rhythm aligner at the pinned source SHA. The engine
 * compares timing shape, not words. Reshaped's local-retime safety principle is retained as
 * bounded piecewise interpolation; its parallel AutoSync system is intentionally not imported.
 *
 * The reference file is expected to belong to the active video (ideally an in-band or
 * hash-matched English track). Since the target and reference are different languages, their
 * words cannot be compared directly. Instead, the aligner compares their monotonic cue rhythm:
 * cue order, duration, silence gaps, punctuation boundaries, and the coarse timeline.
 *
 * This is deliberately conservative. A null result means the caller should render the original
 * target subtitle and keep the manual delay fallback rather than applying a bad correction.
 */
data class SubtitleAlignmentCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

data class SubtitleAlignmentAnchor(
    val targetTimeMs: Long,
    val referenceTimeMs: Long
)

data class SubtitleAlignmentResult(
    val cues: List<SubtitleAlignmentCue>,
    val confidence: Double,
    val estimatedOffsetMs: Long,
    val estimatedScale: Double,
    val anchorCount: Int,
    val anchors: List<SubtitleAlignmentAnchor> = emptyList()
)

object SubtitleSyncEngine {
    private const val MIN_CUES = 8
    private const val MIN_TIMELINE_SPAN_MS = 60_000L
    private const val MIN_ANCHORS = 5
    private const val MAX_ANCHORS = 25
    private const val MAX_MEDIAN_ROUGH_RESIDUAL_MS = 90_000.0
    private const val MIN_GLOBAL_SCALE = 0.85
    private const val MAX_GLOBAL_SCALE = 1.15

    fun align(
        referenceCues: List<SubtitleAlignmentCue>,
        targetCues: List<SubtitleAlignmentCue>
    ): SubtitleAlignmentResult? {
        val reference = referenceCues.normalized()
        val target = targetCues.normalized()
        if (reference.size < MIN_CUES || target.size < MIN_CUES) return null

        val referenceSpan = reference.last().startTimeMs - reference.first().startTimeMs
        val targetSpan = target.last().startTimeMs - target.first().startTimeMs
        if (referenceSpan < MIN_TIMELINE_SPAN_MS || targetSpan < MIN_TIMELINE_SPAN_MS) {
            return null
        }

        val roughScale = referenceSpan.toDouble() / targetSpan.toDouble()
        if (!roughScale.isFinite() || roughScale !in MIN_GLOBAL_SCALE..MAX_GLOBAL_SCALE) {
            return null
        }
        val roughOffset = reference.first().startTimeMs -
            target.first().startTimeMs * roughScale

        val referenceMedianDuration = median(
            reference.map { (it.endTimeMs - it.startTimeMs).coerceAtLeast(1L).toDouble() }
        )
        val targetMedianDuration = median(
            target.map { (it.endTimeMs - it.startTimeMs).coerceAtLeast(1L).toDouble() }
        )
        val referenceMedianGap = median(
            reference.mapIndexed { index, _ -> gapBefore(reference, index).toDouble() }
                .filter { it > 0.0 }
        ).coerceAtLeast(250.0)
        val targetMedianGap = median(
            target.mapIndexed { index, _ -> gapBefore(target, index).toDouble() }
                .filter { it > 0.0 }
        ).coerceAtLeast(250.0)

        val anchorTargetCount = minOf(reference.size, target.size)
        val anchorCount = minOf(
            MAX_ANCHORS,
            maxOf(MIN_ANCHORS, anchorTargetCount / 12)
        )
        val anchors = mutableListOf<SubtitleAlignmentAnchor>()
        var previousTargetIndex = -1
        var previousReferenceIndex = -1

        for (anchorNumber in 1..anchorCount) {
            val fraction = anchorNumber.toDouble() / (anchorCount + 1).toDouble()
            val targetIndex = (target.lastIndex * fraction).roundToInt()
                .coerceIn(0, target.lastIndex)
            val expectedReferenceIndex = (reference.lastIndex * fraction).roundToInt()
                .coerceIn(0, reference.lastIndex)

            if (targetIndex <= previousTargetIndex) continue

            val candidateWindow = maxOf(6, (reference.size * 0.12).roundToInt())
            val lowerBound = maxOf(
                previousReferenceIndex + 1,
                expectedReferenceIndex - candidateWindow
            )
            val upperBound = minOf(
                reference.lastIndex,
                expectedReferenceIndex + candidateWindow
            )
            if (lowerBound > upperBound) continue

            val targetCue = target[targetIndex]
            val expectedReferenceTime =
                targetCue.startTimeMs * roughScale + roughOffset
            val timeScale = maxOf(10_000.0, referenceSpan * 0.04)
            var bestCandidate: Candidate? = null

            for (referenceIndex in lowerBound..upperBound) {
                val referenceCue = reference[referenceIndex]
                val indexDistance = abs(
                    referenceIndex.toDouble() / reference.lastIndex.toDouble() - fraction
                )
                val timeDistance = abs(
                    referenceCue.startTimeMs.toDouble() - expectedReferenceTime
                ) / timeScale
                val durationDistance = logRatio(
                    (targetCue.endTimeMs - targetCue.startTimeMs)
                        .coerceAtLeast(1L)
                        .toDouble() * roughScale,
                    (referenceCue.endTimeMs - referenceCue.startTimeMs)
                        .coerceAtLeast(1L)
                        .toDouble()
                )
                val targetGap = gapBefore(target, targetIndex).toDouble() * roughScale
                val referenceGap = gapBefore(reference, referenceIndex).toDouble()
                val gapDistance = logRatio(
                    targetGap.coerceAtLeast(250.0),
                    referenceGap.coerceAtLeast(250.0)
                )
                val punctuationDistance = if (
                    punctuationClass(targetCue.text) == punctuationClass(referenceCue.text)
                ) {
                    0.0
                } else {
                    1.0
                }
                val score = indexDistance * 5.0 +
                    timeDistance * 1.25 +
                    durationDistance * 1.2 +
                    gapDistance * 0.8 +
                    punctuationDistance * 0.25

                if (bestCandidate == null || score < bestCandidate.score) {
                    bestCandidate = Candidate(referenceIndex, score)
                }
            }

            val candidate = bestCandidate ?: continue
            previousTargetIndex = targetIndex
            previousReferenceIndex = candidate.referenceIndex
            anchors += SubtitleAlignmentAnchor(
                targetTimeMs = targetCue.startTimeMs,
                referenceTimeMs = reference[candidate.referenceIndex].startTimeMs
            )
        }

        if (anchors.size < MIN_ANCHORS) return null

        val roughResiduals = anchors.map { anchor ->
            abs(
                anchor.referenceTimeMs.toDouble() -
                    (anchor.targetTimeMs * roughScale + roughOffset)
            )
        }
        val medianRoughResidual = median(roughResiduals)
        if (medianRoughResidual > MAX_MEDIAN_ROUGH_RESIDUAL_MS) return null

        val durationShape = logRatio(
            targetMedianDuration * roughScale,
            referenceMedianDuration
        )
        val gapShape = logRatio(
            targetMedianGap * roughScale,
            referenceMedianGap
        )
        val residualConfidence = 1.0 - (
            medianRoughResidual / MAX_MEDIAN_ROUGH_RESIDUAL_MS
            ).coerceIn(0.0, 1.0)
        val shapeConfidence = 1.0 - (
            (durationShape * 0.6 + gapShape * 0.4) / 2.5
            ).coerceIn(0.0, 1.0)
        val countConfidence = (
            minOf(reference.size, target.size).toDouble() /
                maxOf(reference.size, target.size).toDouble()
            ).coerceIn(0.0, 1.0)
        val confidence = (
            residualConfidence * 0.55 +
                shapeConfidence * 0.25 +
                countConfidence * 0.20
            ).coerceIn(0.0, 1.0)
        if (confidence < 0.35) return null

        val alignedCues = target.map { cue ->
            val alignedStart = mapTime(
                timeMs = cue.startTimeMs,
                anchors = anchors,
                roughScale = roughScale,
                roughOffset = roughOffset
            )
            val alignedEnd = mapTime(
                timeMs = cue.endTimeMs,
                anchors = anchors,
                roughScale = roughScale,
                roughOffset = roughOffset
            ).coerceAtLeast(alignedStart + 1L)
            SubtitleAlignmentCue(
                startTimeMs = alignedStart.coerceAtLeast(0L),
                endTimeMs = alignedEnd.coerceAtLeast(alignedStart + 1L),
                text = cue.text
            )
        }

        return SubtitleAlignmentResult(
            cues = alignedCues,
            confidence = confidence,
            estimatedOffsetMs = roughOffset.roundToLong(),
            estimatedScale = roughScale,
            anchorCount = anchors.size,
            anchors = anchors.toList()
        )
    }

    /**
     * Aligns a complete target subtitle file when the reference is only a window of the video.
     *
     * An in-band subtitle track is decoded by the player as playback advances, so at the moment
     * an addon subtitle is selected we may only have the first minute or two of reference cues.
     * This method finds the corresponding target window using cue rhythm, estimates the mapping
     * from that evidence, and applies it to the complete target timeline. Later reference cues
     * can be fed back into this method to refine drift without requiring a second media load.
     */
    fun alignUsingReferenceWindow(
        referenceCues: List<SubtitleAlignmentCue>,
        targetCues: List<SubtitleAlignmentCue>,
        targetStartHintMs: Long? = null
    ): SubtitleAlignmentResult? {
        val reference = referenceCues.normalized()
        val target = targetCues.normalized()
        if (reference.size < MIN_CUES || target.size < MIN_CUES) return null

        val referenceSpan = reference.last().startTimeMs - reference.first().startTimeMs
        if (referenceSpan < MIN_TIMELINE_SPAN_MS) return null

        val hint = targetStartHintMs ?: reference.first().startTimeMs
        val nearestTargetIndex = target.indices.minByOrNull { index ->
            abs(target[index].startTimeMs - hint)
        } ?: return null
        val firstIndexStart = maxOf(0, nearestTargetIndex - REFERENCE_WINDOW_START_RADIUS)
        val firstIndexEnd = minOf(target.lastIndex, nearestTargetIndex + REFERENCE_WINDOW_START_RADIUS)
        val maxWindowSpanMs = maxOf(
            REFERENCE_WINDOW_EXTRA_MS,
            (referenceSpan * REFERENCE_WINDOW_MAX_SCALE).roundToLong()
        )

        var best: WindowCandidate? = null
        for (windowStart in firstIndexStart..firstIndexEnd) {
            val minimumWindowEnd = windowStart + MIN_CUES - 1
            if (minimumWindowEnd > target.lastIndex) continue

            // Target and reference can have a large constant offset. Bound the candidate by its
            // own start rather than by the reference's absolute timestamp.
            val maxWindowEndTime = target[windowStart].startTimeMs + maxWindowSpanMs
            val timeLimitedEnd = target.indexOfLast { cue ->
                cue.startTimeMs <= maxWindowEndTime
            }.takeIf { it >= minimumWindowEnd } ?: continue
            val maximumWindowEnd = minOf(
                target.lastIndex,
                timeLimitedEnd,
                windowStart + REFERENCE_WINDOW_MAX_CUES - 1
            )

            for (windowEnd in minimumWindowEnd..maximumWindowEnd) {
                val targetWindow = target.subList(windowStart, windowEnd + 1)
                if (
                    targetWindow.last().startTimeMs - targetWindow.first().startTimeMs <
                        MIN_TIMELINE_SPAN_MS
                ) {
                    continue
                }
                val windowAlignment = align(reference, targetWindow) ?: continue
                val mappedFirstTime = windowAlignment.cues.firstOrNull()?.startTimeMs ?: continue
                val mappedLastTime = windowAlignment.cues.lastOrNull()?.startTimeMs ?: continue
                val startDistance = abs(mappedFirstTime - reference.first().startTimeMs)
                val endDistance = abs(mappedLastTime - reference.last().startTimeMs)
                val scaleDistance = abs(ln(windowAlignment.estimatedScale.coerceAtLeast(0.01)))
                val targetHintDistance = abs(targetWindow.first().startTimeMs - hint)
                val score = windowAlignment.confidence -
                    (startDistance / REFERENCE_WINDOW_DISTANCE_NORMALIZER_MS)
                        .coerceAtMost(0.30) -
                    (endDistance / REFERENCE_WINDOW_DISTANCE_NORMALIZER_MS)
                        .coerceAtMost(0.30) -
                    (scaleDistance * REFERENCE_WINDOW_SCALE_PENALTY)
                        .coerceAtMost(0.25) -
                    (targetHintDistance / REFERENCE_WINDOW_HINT_NORMALIZER_MS)
                        .coerceAtMost(0.15)

                if (best == null || score > best.score) {
                    best = WindowCandidate(
                        score = score,
                        windowAlignment = windowAlignment
                    )
                }
            }
        }

        val selectedCandidate = best ?: return null
        if (selectedCandidate.score < REFERENCE_WINDOW_MIN_SCORE) return null
        val selected = selectedCandidate.windowAlignment

        val mappedTarget = target.map { cue ->
            val mappedStart = mapTime(
                timeMs = cue.startTimeMs,
                anchors = selected.anchors,
                roughScale = selected.estimatedScale,
                roughOffset = selected.estimatedOffsetMs.toDouble()
            )
            val mappedEnd = mapTime(
                timeMs = cue.endTimeMs,
                anchors = selected.anchors,
                roughScale = selected.estimatedScale,
                roughOffset = selected.estimatedOffsetMs.toDouble()
            ).coerceAtLeast(mappedStart + 1L)
            SubtitleAlignmentCue(
                startTimeMs = mappedStart.coerceAtLeast(0L),
                endTimeMs = mappedEnd.coerceAtLeast(mappedStart + 1L),
                text = cue.text
            )
        }

        return selected.copy(
            cues = mappedTarget,
            confidence = (selected.confidence + selectedCandidate.score) / 2.0
        )
    }

    private fun mapTime(
        timeMs: Long,
        anchors: List<SubtitleAlignmentAnchor>,
        roughScale: Double,
        roughOffset: Double
    ): Long {
        if (anchors.isEmpty()) {
            return (timeMs * roughScale + roughOffset).roundToLong()
        }
        if (timeMs <= anchors.first().targetTimeMs) {
            return (timeMs * roughScale + roughOffset).roundToLong()
        }
        if (timeMs >= anchors.last().targetTimeMs) {
            return (timeMs * roughScale + roughOffset).roundToLong()
        }

        for (index in 0 until anchors.lastIndex) {
            val left = anchors[index]
            val right = anchors[index + 1]
            if (timeMs > right.targetTimeMs) continue
            val targetSpan = (right.targetTimeMs - left.targetTimeMs).coerceAtLeast(1L)
            val fraction = (
                (timeMs - left.targetTimeMs).toDouble() / targetSpan.toDouble()
                ).coerceIn(0.0, 1.0)
            val affineMapped = timeMs * roughScale + roughOffset
            val localScale = (right.referenceTimeMs - left.referenceTimeMs).toDouble() /
                targetSpan.toDouble()
            // A bad local match must not create an extreme speed change or timeline jump.
            // Piecewise drift is allowed only close to the globally-supported clock scale and
            // within a finite correction corridor around the affine result.
            val boundedScale = localScale.coerceIn(
                roughScale * MIN_LOCAL_SCALE_RATIO,
                roughScale * MAX_LOCAL_SCALE_RATIO
            )
            val localMapped = left.referenceTimeMs +
                (timeMs - left.targetTimeMs) * boundedScale
            return localMapped.coerceIn(
                affineMapped - MAX_PIECEWISE_CORRECTION_MS,
                affineMapped + MAX_PIECEWISE_CORRECTION_MS
            ).roundToLong()
        }
        return (timeMs * roughScale + roughOffset).roundToLong()
    }

    private fun List<SubtitleAlignmentCue>.normalized(): List<SubtitleAlignmentCue> {
        return asSequence()
            .filter { it.endTimeMs > it.startTimeMs && it.text.isNotBlank() }
            .sortedBy { it.startTimeMs }
            .map { it.copy(text = it.text.trim()) }
            .toList()
    }

    private fun gapBefore(
        cues: List<SubtitleAlignmentCue>,
        index: Int
    ): Long {
        if (index <= 0) return 0L
        return (cues[index].startTimeMs - cues[index - 1].endTimeMs).coerceAtLeast(0L)
    }

    private fun punctuationClass(text: String): Int {
        return when (text.trim().lastOrNull()) {
            '?', '!' -> 1
            '.', '…', ':' -> 2
            else -> 0
        }
    }

    private fun logRatio(left: Double, right: Double): Double {
        return abs(ln(left.coerceAtLeast(1.0) / right.coerceAtLeast(1.0)))
    }

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    private data class Candidate(
        val referenceIndex: Int,
        val score: Double
    )

    private data class WindowCandidate(
        val score: Double,
        val windowAlignment: SubtitleAlignmentResult
    )

    private const val MIN_LOCAL_SCALE_RATIO = 0.90
    private const val MAX_LOCAL_SCALE_RATIO = 1.10
    private const val MAX_PIECEWISE_CORRECTION_MS = 30_000.0

    private const val REFERENCE_WINDOW_START_RADIUS = 6
    private const val REFERENCE_WINDOW_MAX_CUES = 160
    private const val REFERENCE_WINDOW_EXTRA_MS = 120_000L
    private const val REFERENCE_WINDOW_MAX_SCALE = 2.5
    private const val REFERENCE_WINDOW_DISTANCE_NORMALIZER_MS = 600_000.0
    private const val REFERENCE_WINDOW_HINT_NORMALIZER_MS = 300_000.0
    private const val REFERENCE_WINDOW_SCALE_PENALTY = 0.08
    private const val REFERENCE_WINDOW_MIN_SCORE = 0.15
}
