package com.nuvio.tv.fork.subtitles

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry

/** G6 feature group switch (D051); OFF keeps official subtitle behavior exactly. */
object SubtitleIntelligence {
    /** Process-wide: registry defaults are fixed per process (as `AdaptiveResources.install`). */
    @JvmField
    val enabled: Boolean = FeatureRegistry().mode(FeatureId.SUBTITLE_INTELLIGENCE) != FeatureMode.OFF
}
