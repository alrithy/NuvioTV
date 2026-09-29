package com.nuvio.tv.fork.diagnostics

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.FrameRateMatchingMode
import com.nuvio.tv.data.local.PlayerSettingsDataStore
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies an [AssessmentPlan] through the official PlayerSettingsDataStore setters, after saving
 * the exact previous values in a profile-scoped snapshot so [revert] restores them. A second apply
 * replaces the snapshot, so revert always undoes the most recent apply (ysosrs
 * DeviceAssessmentApplier, 45e0984, reduced to the settings G1 assesses).
 */
@Singleton
class DeviceAssessmentApplier @Inject constructor(
    private val playerSettings: PlayerSettingsDataStore,
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager,
) {
    private val useParallelKey = booleanPreferencesKey("snapshot_use_parallel_connections")
    private val connectionsKey = intPreferencesKey("snapshot_parallel_connection_count")
    private val targetBufferKey = intPreferencesKey("snapshot_target_buffer_size_mb")
    private val frameRateModeKey = stringPreferencesKey("snapshot_frame_rate_matching_mode")

    private fun store(profileId: Int = profileManager.activeProfileId.value) = factory.get(profileId, FEATURE)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val canRevert: Flow<Boolean> = profileManager.activeProfileId.flatMapLatest { profileId ->
        store(profileId).data.map { it[frameRateModeKey] != null }
    }

    /** Returns the number of settings written; 0 leaves settings and any snapshot untouched. */
    suspend fun apply(plan: AssessmentPlan): Int {
        if (plan.isEmpty) return 0
        val before = playerSettings.playerSettings.first()
        store().edit {
            it[useParallelKey] = before.useParallelConnections
            it[connectionsKey] = before.parallelConnectionCount
            it[targetBufferKey] = before.bufferSettings.targetBufferSizeMb
            it[frameRateModeKey] = before.frameRateMatchingMode.name
        }

        var written = 0
        plan.useParallelConnections?.let { playerSettings.setUseParallelConnections(it); written++ }
        plan.parallelConnectionCount?.let { playerSettings.setParallelConnectionCount(it); written++ }
        plan.targetBufferSizeMb?.let { playerSettings.setBufferTargetSizeMb(it); written++ }
        plan.frameRateMatchingOn?.let {
            playerSettings.setFrameRateMatchingMode(if (it) FrameRateMatchingMode.START else FrameRateMatchingMode.OFF)
            written++
        }
        return written
    }

    /** Restores the values captured by the last [apply]; false when there is nothing to revert. */
    suspend fun revert(): Boolean {
        val snapshot = store().data.first()
        val modeName = snapshot[frameRateModeKey] ?: return false
        // A corrupt snapshot is discarded rather than half-applied or left stuck in the UI.
        val mode = FrameRateMatchingMode.entries.firstOrNull { it.name == modeName }
        if (mode != null) {
            snapshot[useParallelKey]?.let { playerSettings.setUseParallelConnections(it) }
            snapshot[connectionsKey]?.let { playerSettings.setParallelConnectionCount(it) }
            snapshot[targetBufferKey]?.let { playerSettings.setBufferTargetSizeMb(it) }
            playerSettings.setFrameRateMatchingMode(mode)
        }
        store().edit {
            it.remove(useParallelKey)
            it.remove(connectionsKey)
            it.remove(targetBufferKey)
            it.remove(frameRateModeKey)
        }
        return mode != null
    }

    private companion object {
        const val FEATURE = "fork_device_assessment"
    }
}
