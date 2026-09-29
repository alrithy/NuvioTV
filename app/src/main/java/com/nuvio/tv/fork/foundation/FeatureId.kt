package com.nuvio.tv.fork.foundation

/**
 * Module-level Superfork feature groups, one per architectural owner.
 * Individual product features are tracked in integration/feature_traceability.csv.
 */
enum class FeatureId(val experimental: Boolean = false) {
    UNIFIED_DIAGNOSTICS,
    ADAPTIVE_RESOURCE_MANAGER,
    PLAYBACK_STRATEGY_ENGINE,
    REMUX_PERFORMANCE,
    AUDIO_DV_AFR,
    SUBTITLE_INTELLIGENCE,
    SEEK_INTELLIGENCE,
    STREAM_INTELLIGENCE,
    DISCOVERY_SKIP_RECOMMENDATIONS,
    LIVE_TV,
    WATCH_PARTY,
    UI_STYLES,
    AI_MEDIA(experimental = true),
    MAT_AUDIO(experimental = true),
}
