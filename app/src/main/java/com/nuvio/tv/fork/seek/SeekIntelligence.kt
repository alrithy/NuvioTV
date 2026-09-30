package com.nuvio.tv.fork.seek

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry

/** G7 feature group switch (D052); OFF keeps the official scrubber exactly. */
object SeekIntelligence {
    /** Process-wide: registry defaults are fixed per process (as `AdaptiveResources.install`). */
    @JvmField
    val enabled: Boolean = FeatureRegistry().mode(FeatureId.SEEK_INTELLIGENCE) != FeatureMode.OFF
}
