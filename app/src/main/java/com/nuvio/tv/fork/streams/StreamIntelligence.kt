package com.nuvio.tv.fork.streams

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry

/** G8 feature group switch (D053); OFF keeps official stream requests, lists and autoplay exactly. */
object StreamIntelligence {
    /** Process-wide: registry defaults are fixed per process (as `AdaptiveResources.install`). */
    @JvmField
    val enabled: Boolean = FeatureRegistry().mode(FeatureId.STREAM_INTELLIGENCE) != FeatureMode.OFF
}
