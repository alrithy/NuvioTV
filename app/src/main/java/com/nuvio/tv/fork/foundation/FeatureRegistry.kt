package com.nuvio.tv.fork.foundation

import java.util.Collections
import java.util.EnumMap

/**
 * Resolves the [FeatureMode] of each Superfork feature group.
 *
 * Every group defaults to [FeatureMode.OFF] so that the registry alone never changes
 * official Nuvio behavior. [overrides] is the seam for later gates (explicit user
 * choices or deliberately introduced defaults); it is copied on construction.
 */
class FeatureRegistry(overrides: Map<FeatureId, FeatureMode> = emptyMap()) {

    private val modes: Map<FeatureId, FeatureMode> = immutableModes(DEFAULTS + overrides)

    fun mode(id: FeatureId): FeatureMode = modes.getValue(id)

    companion object {
        val DEFAULTS: Map<FeatureId, FeatureMode> =
            immutableModes(FeatureId.entries.associateWith { FeatureMode.OFF })

        private fun immutableModes(source: Map<FeatureId, FeatureMode>): Map<FeatureId, FeatureMode> =
            Collections.unmodifiableMap(EnumMap(source))
    }
}
