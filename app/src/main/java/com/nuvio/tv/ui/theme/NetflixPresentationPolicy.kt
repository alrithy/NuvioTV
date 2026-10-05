package com.nuvio.tv.ui.theme

import com.nuvio.tv.fork.resource.MemoryTier

/** Bounded theme presentation derived from the existing AdaptiveResources tier; no RAM probe. */
data class NetflixPresentationPolicy(
    val animate: Boolean,
    val allowVideo: Boolean,
    val expandedScale: Float,
    val maxBackdropWidthPx: Int,
    val maxBackdropHeightPx: Int
) {
    val heroMaxWidthPx: Int get() = maxBackdropWidthPx
}

fun netflixPresentationPolicy(tier: MemoryTier): NetflixPresentationPolicy = when (tier) {
    MemoryTier.STANDARD -> NetflixPresentationPolicy(true, true, NetflixThemeTokens.expandedScale, 3840, 2160)
    MemoryTier.CONSTRAINED -> NetflixPresentationPolicy(true, false, NetflixThemeTokens.focusScale, 1920, 1080)
    MemoryTier.LOW_RAM -> NetflixPresentationPolicy(false, false, 1f, 1280, 720)
}
