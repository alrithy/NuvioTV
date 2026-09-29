package com.nuvio.tv.fork.playback

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.PlayerSettings
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry
import com.nuvio.tv.fork.resource.AdaptiveResources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The strategy decision for one playback and the settings the ExoPlayer session is built with. */
data class PlaybackStrategyPlan(val decision: StrategyDecision, val settings: PlayerSettings)

/**
 * Stores the selected strategy per profile (`fork_playback_strategy`, key `selected_strategy`;
 * absent or unknown = Official) and turns it into a session-only copy of the official settings.
 * Stored official settings are never written.
 */
@Singleton
class PlaybackStrategySession @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager,
    registry: FeatureRegistry,
) {
    private val mode: FeatureMode = registry.mode(FeatureId.PLAYBACK_STRATEGY_ENGINE)
    private val selectedKey = stringPreferencesKey("selected_strategy")

    val enabled: Boolean = mode != FeatureMode.OFF

    private fun store(profileId: Int = profileManager.activeProfileId.value) = factory.get(profileId, FEATURE)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selected: Flow<PlaybackStrategy> = profileManager.activeProfileId.flatMapLatest { profileId ->
        store(profileId).data.map { PlaybackStrategy.fromKey(it[selectedKey]) }
    }

    suspend fun select(strategy: PlaybackStrategy) {
        store().edit { it[selectedKey] = strategy.key }
    }

    /** The decision for [facts] under the stored selection; Official while the engine is OFF. */
    suspend fun decide(facts: PlaybackFacts): StrategyDecision {
        val selection = if (enabled) runCatching { selected.first() }.getOrDefault(PlaybackStrategy.OFFICIAL)
        else PlaybackStrategy.OFFICIAL
        return PlaybackStrategies.resolve(selection, facts, mode)
    }

    suspend fun plan(
        stored: PlayerSettings,
        facts: PlaybackFacts,
        storedEffectiveBufferMb: Int,
    ): PlaybackStrategyPlan {
        val decision = decide(facts)
        val knobs = PlaybackStrategies.knobs(
            effective = decision.effective,
            storedConnections = stored.parallelConnectionCount,
            storedEffectiveBufferMb = storedEffectiveBufferMb,
            policy = AdaptiveResources.policy,
        )
        return PlaybackStrategyPlan(decision, stored.withKnobs(knobs))
    }

    private companion object {
        const val FEATURE = "fork_playback_strategy"
    }
}

/** Applies [knobs] to a copy; [StrategyKnobs.NONE] returns the same instance (official parity). */
internal fun PlayerSettings.withKnobs(knobs: StrategyKnobs): PlayerSettings {
    if (knobs.isOfficial) return this
    return copy(
        parallelNetworkEnabled = knobs.parallelNetwork ?: parallelNetworkEnabled,
        useParallelConnections = knobs.useParallelConnections ?: useParallelConnections,
        parallelConnectionCount = knobs.parallelConnectionCount ?: parallelConnectionCount,
        bufferEngineEnabled = knobs.bufferEngine ?: bufferEngineEnabled,
        bufferBudgetManaged = knobs.bufferBudgetManaged ?: bufferBudgetManaged,
        vodCacheEnabled = knobs.vodCache ?: vodCacheEnabled,
        bufferSettings = knobs.targetBufferSizeMb
            ?.let { bufferSettings.copy(targetBufferSizeMb = it) }
            ?: bufferSettings,
    )
}
