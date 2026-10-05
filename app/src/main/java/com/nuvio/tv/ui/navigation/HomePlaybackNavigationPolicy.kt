package com.nuvio.tv.ui.navigation

/**
 * Resolve a Home Play request through the existing metadata/playback handoff.
 *
 * Home previews identify a title, not necessarily a playable video. The detail owner resolves the
 * add-on metadata, series resume/next episode and playback availability before entering Stream,
 * whose existing settings continue to decide stream selection and autoplay.
 */
internal fun homePlaybackRoute(
    itemId: String,
    itemType: String,
    addonBaseUrl: String?,
    heroBackdropUrl: String? = null
): String = Screen.Detail.createRoute(
    itemId = itemId,
    itemType = itemType,
    addonBaseUrl = addonBaseUrl,
    heroBackdropUrl = heroBackdropUrl,
    playOnLoad = true,
    returnToHomeOnBack = true
)
