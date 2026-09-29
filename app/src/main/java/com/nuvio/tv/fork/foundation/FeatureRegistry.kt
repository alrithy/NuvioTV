package com.nuvio.tv.fork.foundation

import java.util.Collections
import java.util.EnumMap

/**
 * Resolves the [FeatureMode] of each Superfork feature group.
 *
 * Every group defaults to [FeatureMode.OFF] so that the registry alone never changes
 * official Nuvio behavior, except groups listed in [DECIDED_DEFAULTS] by a recorded decision.
 * [overrides] is the seam for explicit user choices in later gates; it is copied on construction.
 */
class FeatureRegistry(overrides: Map<FeatureId, FeatureMode> = emptyMap()) {

    private val modes: Map<FeatureId, FeatureMode> = immutableModes(DEFAULTS + overrides)

    fun mode(id: FeatureId): FeatureMode = modes.getValue(id)

    companion object {
        /**
         * D041: passive, read-only diagnostics (no network, no persistence) run by default.
         * D042: the resource manager only tightens limits on low-RAM/constrained devices.
         * D043: the strategy selector is visible; the stored selection defaults to Official.
         */
        internal val DECIDED_DEFAULTS: Map<FeatureId, FeatureMode> = mapOf(
            FeatureId.UNIFIED_DIAGNOSTICS to FeatureMode.AUTO,
            FeatureId.ADAPTIVE_RESOURCE_MANAGER to FeatureMode.AUTO,
            FeatureId.PLAYBACK_STRATEGY_ENGINE to FeatureMode.AUTO,
        )

        val DEFAULTS: Map<FeatureId, FeatureMode> = immutableModes(
            FeatureId.entries.associateWith { DECIDED_DEFAULTS[it] ?: FeatureMode.OFF }
        )

        private fun immutableModes(source: Map<FeatureId, FeatureMode>): Map<FeatureId, FeatureMode> =
            Collections.unmodifiableMap(EnumMap(source))
    }
}
