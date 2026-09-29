package com.nuvio.tv.fork.diagnostics

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Passive, in-memory add-on health keyed by canonical add-on base URL. It only records outcomes
 * of requests that already happen: no polling, no persistence, nothing logged.
 */
@Singleton
class AddonHealthTracker internal constructor(private val enabled: Boolean) {

    @Inject
    constructor(registry: FeatureRegistry) :
        this(registry.mode(FeatureId.UNIFIED_DIAGNOSTICS) != FeatureMode.OFF)

    private val _health = MutableStateFlow<Map<String, AddonHealth>>(emptyMap())
    val health: StateFlow<Map<String, AddonHealth>> = _health.asStateFlow()

    fun record(addonKey: String, state: AddonHealthState, latencyMs: Long) {
        if (!enabled || state == AddonHealthState.UNKNOWN) return
        _health.update { current ->
            val failures = if (state.isFailure) (current[addonKey]?.consecutiveFailures ?: 0) + 1 else 0
            current + (addonKey to AddonHealth(state, failures, latencyMs))
        }
    }

    companion object {
        /** Default for code paths built without DI (tests); records nothing. */
        val DISABLED = AddonHealthTracker(enabled = false)
    }
}
