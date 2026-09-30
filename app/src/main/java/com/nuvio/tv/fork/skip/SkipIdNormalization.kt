package com.nuvio.tv.fork.skip

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry

/**
 * G9a (feature 134): official resolves TMDB / MAL / Kitsu ids to IMDb for movies only; with the
 * group on, a series played under a non-IMDb id uses the IMDb id of its cached meta, so every skip
 * provider (official ones included) can answer. OFF = official: such series get no skip lookup.
 */
object SkipIdNormalization {
    /** Process-wide: registry defaults are fixed per process (as `StreamIntelligence`). */
    @JvmField
    val enabled: Boolean = FeatureRegistry().mode(FeatureId.DISCOVERY_SKIP_RECOMMENDATIONS) != FeatureMode.OFF
}
