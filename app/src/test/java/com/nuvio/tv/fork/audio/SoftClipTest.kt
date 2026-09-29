package com.nuvio.tv.fork.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftClipTest {

    @Test
    fun samplesUnderTheKneePassUntouched() {
        listOf(0f, 0.25f, -0.5f, SoftClip.KNEE, -SoftClip.KNEE).forEach {
            assertEquals("$it", it, SoftClip.apply(it), 0f)
        }
    }

    @Test
    fun boostedPeaksBendBelowFullScaleInsteadOfClipping() {
        listOf(0.9f, 1f, 1.2f, 1.5f).forEach { input ->
            val out = SoftClip.apply(input)
            assertTrue("$input -> $out", out > SoftClip.KNEE && out < 1f)
            assertEquals(-out, SoftClip.apply(-input), 0f)
        }
        // Extreme peaks (10 dB boost of a full-scale sample, and beyond) saturate at full scale in
        // float, never above it.
        listOf(3.16f, 100f).forEach { input ->
            val out = SoftClip.apply(input)
            assertTrue("$input -> $out", out > SoftClip.KNEE && out <= 1f)
        }
    }

    @Test
    fun theCurveIsMonotonicAndContinuousAtTheKnee() {
        var previous = SoftClip.apply(0f)
        var x = 0.01f
        while (x < 4f) {
            val y = SoftClip.apply(x)
            assertTrue("not monotonic at $x", y >= previous)
            previous = y
            x += 0.01f
        }
        assertEquals(SoftClip.KNEE, SoftClip.apply(SoftClip.KNEE + 1e-6f), 1e-5f)
    }

    @Test
    fun enabledFollowsTheAudioGroupDefault() {
        // D048: AUDIO_DV_AFR is AUTO, so soft clipping is on (only reached when amplification > 0 dB).
        assertTrue(SoftClip.enabled)
    }
}
