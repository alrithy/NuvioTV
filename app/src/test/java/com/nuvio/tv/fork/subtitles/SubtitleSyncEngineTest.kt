package com.nuvio.tv.fork.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SubtitleSyncEngineTest {
    @Test
    fun alignedCuesRemainAligned() {
        val target = irregularCues(32, 0L, "العربية")
        val result = SubtitleSyncEngine.align(target.map { it.copy(text = "English") }, target)

        assertNotNull(result)
        assertEquals(0L, result!!.estimatedOffsetMs)
        assertEquals(1.0, result.estimatedScale, 0.0001)
        assertEquals(target.map { it.startTimeMs }, result.cues.map { it.startTimeMs })
    }

    @Test
    fun estimatesAndAppliesFixedOffset() {
        val target = irregularCues(32, 0L, "Target")
        val reference = transform(target, offsetMs = 12_000L, scale = 1.0)

        val result = SubtitleSyncEngine.align(reference, target)

        assertNotNull(result)
        assertEquals(12_000L, result!!.estimatedOffsetMs)
        assertEquals(reference.map { it.startTimeMs }, result.cues.map { it.startTimeMs })
    }

    @Test
    fun correctsGradualClockDrift() {
        val target = irregularCues(40, 0L, "Target")
        val reference = transform(target, offsetMs = 5_000L, scale = 1.025)

        val result = SubtitleSyncEngine.align(reference, target)

        assertNotNull(result)
        assertEquals(1.025, result!!.estimatedScale, 0.001)
        assertTrue(abs(result.cues.last().startTimeMs - reference.last().startTimeMs) < 1_000L)
    }

    @Test
    fun followsBoundedPiecewiseDrift() {
        val target = irregularCues(48, 0L, "Target")
        val pivot = target[target.size / 2].startTimeMs
        val reference = target.map { cue ->
            val localDrift = if (cue.startTimeMs <= pivot) 0L else
                ((cue.startTimeMs - pivot) * 0.035).toLong()
            cue.copy(
                startTimeMs = cue.startTimeMs + 4_000L + localDrift,
                endTimeMs = cue.endTimeMs + 4_000L + localDrift,
                text = "Reference"
            )
        }

        val result = SubtitleSyncEngine.align(reference, target)

        assertNotNull(result)
        val affineAtThreeQuarters = (target[36].startTimeMs * result!!.estimatedScale +
            result.estimatedOffsetMs).toLong()
        assertTrue(result.cues[36].startTimeMs != affineAtThreeQuarters)
        assertTrue(abs(result.cues[36].startTimeMs - reference[36].startTimeMs) < 4_000L)
        assertTrue(result.cues.zipWithNext().all { (left, right) ->
            right.startTimeMs > left.startTimeMs
        })
    }

    @Test
    fun failsClosedOnLowConfidenceOrMissingReference() {
        val target = irregularCues(32, 0L, "Target")
        val unrelated = (0 until 32).map { index ->
            val start = index * 60_000L
            SubtitleAlignmentCue(start, start + if (index % 2 == 0) 20_000L else 100L, "x")
        }

        assertNull(SubtitleSyncEngine.align(emptyList(), target))
        assertNull(SubtitleSyncEngine.align(unrelated, target))
    }

    @Test
    fun alignsArabicTargetToEnglishReferenceWithoutReadingWords() {
        val arabic = irregularCues(36, 0L, "مرحبا بالعالم")
        val englishReference = transform(arabic, offsetMs = 7_500L, scale = 1.01)
            .mapIndexed { index, cue -> cue.copy(text = "English sentence $index") }

        val result = SubtitleSyncEngine.align(englishReference, arabic)

        assertNotNull(result)
        assertTrue(result!!.confidence >= 0.35)
        assertTrue(abs(result.cues[20].startTimeMs - englishReference[20].startTimeMs) < 2_000L)
        assertTrue(result.cues[20].text.startsWith("مرحبا"))
    }

    @Test
    fun rejectsInsufficientTimelineEvidence() {
        assertNull(SubtitleSyncEngine.align(
            irregularCues(7, 0L, "Reference"),
            irregularCues(7, 0L, "Target")
        ))
    }

    private fun transform(
        cues: List<SubtitleAlignmentCue>,
        offsetMs: Long,
        scale: Double
    ) = cues.map { cue ->
        cue.copy(
            startTimeMs = (cue.startTimeMs * scale).toLong() + offsetMs,
            endTimeMs = (cue.endTimeMs * scale).toLong() + offsetMs,
            text = "Reference"
        )
    }

    private fun irregularCues(
        count: Int,
        offsetMs: Long,
        prefix: String
    ): List<SubtitleAlignmentCue> {
        var start = offsetMs
        return (0 until count).map { index ->
            if (index > 0) start += 6_000L + ((index * 3_173L) % 9_000L)
            val duration = 1_400L + ((index * 911L) % 4_000L)
            SubtitleAlignmentCue(start, start + duration, "$prefix $index")
        }
    }
}
