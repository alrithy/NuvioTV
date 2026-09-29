package com.nuvio.tv.fork.audio

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry
import kotlin.math.abs
import kotlin.math.tanh

/**
 * Soft clipping for boosted volume (G5a, feature 48). ALGORITHM_PORT of Reshaped
 * `volumeboost/VolumeBoostSoftClip.kt` @ 0ccf049 (`softClipBoosted`).
 *
 * Official `GainAudioProcessor` hard-clamps amplified samples at full scale, which is what makes a
 * boosted explosion crackle. Samples under the knee pass untouched (normal speech costs one
 * compare); peaks above it bend smoothly toward full scale and never exceed it. Only reached when
 * the user has set an amplification above 0 dB.
 */
object SoftClip {
    const val KNEE = 0.8f
    private const val HEADROOM = 1f - KNEE

    /** Process-wide: registry defaults are fixed per process (as `AdaptiveResources.install`). */
    @JvmField
    val enabled: Boolean = FeatureRegistry().mode(FeatureId.AUDIO_DV_AFR) != FeatureMode.OFF

    /** [sample] at full scale 1; the result is in (-1, 1) for any finite input. */
    fun apply(sample: Float): Float {
        val magnitude = abs(sample)
        if (magnitude <= KNEE) return sample
        val bent = KNEE + HEADROOM * tanh((magnitude - KNEE) / HEADROOM)
        return if (sample < 0f) -bent else bent
    }
}
