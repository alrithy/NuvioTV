package com.nuvio.tv.fork.dimmer

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry

/**
 * G9f App Dimmer (206, 207): Cxsmo `AppDimmerOverlay` @ 3e0d0fa, a black layer over the whole app,
 * player included, that never takes input. Pure.
 */
object AppDimmerRules {
    val enabled: Boolean = FeatureRegistry().mode(FeatureId.DISCOVERY_SKIP_RECOMMENDATIONS) != FeatureMode.OFF

    /** Cxsmo's settings stop at 90 % (its overlay allows 95); off by default. */
    const val MAX_PERCENT = 90
    const val STEP = 10
    val PRESETS: List<Int> = (0..MAX_PERCENT step STEP).toList()

    fun clamp(percent: Int): Int = percent.coerceIn(0, MAX_PERCENT)

    /** Overlay alpha for a stored percent; 0 draws nothing. */
    fun alpha(percent: Int): Float = clamp(percent) / 100f
}
