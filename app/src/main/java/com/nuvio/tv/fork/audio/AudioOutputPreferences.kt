package com.nuvio.tv.fork.audio

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fork audio output preferences, per profile (`fork_audio_output`). Every value defaults to
 * official behavior (D048); nothing here is written into the official PlayerSettings store.
 */
@Singleton
class AudioOutputPreferences @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager,
    registry: FeatureRegistry,
) {
    val enabled: Boolean = registry.mode(FeatureId.AUDIO_DV_AFR) != FeatureMode.OFF

    private val preferLosslessKey = booleanPreferencesKey("prefer_lossless_audio")

    private fun store(profileId: Int = profileManager.activeProfileId.value) = factory.get(profileId, FEATURE)

    /** G5b (feature 37): default to the best lossless audio track. Off = official selection. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val preferLossless: Flow<Boolean> = profileManager.activeProfileId.flatMapLatest { profileId ->
        store(profileId).data.map { enabled && (it[preferLosslessKey] ?: false) }
    }

    suspend fun setPreferLossless(value: Boolean) {
        store().edit { it[preferLosslessKey] = value }
    }

    /** Snapshot for one playback; false on any read failure (official behavior). */
    suspend fun preferLosslessNow(): Boolean =
        enabled && runCatching { preferLossless.first() }.getOrDefault(false)

    private companion object {
        const val FEATURE = "fork_audio_output"
    }
}
