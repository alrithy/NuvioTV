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
 * Fork audio / DV output preferences of the G5 group, per profile (`fork_audio_output`). Every
 * value defaults to official behavior (D048); nothing here is written into the official
 * PlayerSettings store.
 */
@Singleton
class AudioOutputPreferences @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager,
    registry: FeatureRegistry,
) {
    val enabled: Boolean = registry.mode(FeatureId.AUDIO_DV_AFR) != FeatureMode.OFF

    private val preferLosslessKey = booleanPreferencesKey("prefer_lossless_audio")
    private val passthroughKeys = PassthroughFormat.entries.associateWith { booleanPreferencesKey(it.key) }
    private val hdr10SeiOnDvStripKey = booleanPreferencesKey("hdr10_sei_on_dv_strip")

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

    /** G5c (feature 38): "receiver decodes this format" per format; all on = official. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val passthroughAllowed: Flow<Map<PassthroughFormat, Boolean>> = profileManager.activeProfileId.flatMapLatest { profileId ->
        store(profileId).data.map { prefs ->
            PassthroughFormat.entries.associateWith { !enabled || (prefs[passthroughKeys.getValue(it)] ?: true) }
        }
    }

    suspend fun setPassthroughAllowed(format: PassthroughFormat, value: Boolean) {
        store().edit { it[passthroughKeys.getValue(format)] = value }
    }

    /** Snapshot for one playback; ALLOW_ALL when the group is OFF or on any read failure. */
    suspend fun passthroughPolicyNow(softwareDecodersAvailable: Boolean): AudioPassthroughPolicy {
        if (!enabled) return AudioPassthroughPolicy.ALLOW_ALL
        val allowed = runCatching { passthroughAllowed.first() }.getOrNull() ?: return AudioPassthroughPolicy.ALLOW_ALL
        return AudioPassthroughPolicy(
            allowAc3 = allowed.getValue(PassthroughFormat.AC3),
            allowEac3 = allowed.getValue(PassthroughFormat.EAC3),
            allowTrueHd = allowed.getValue(PassthroughFormat.TRUEHD),
            allowDts = allowed.getValue(PassthroughFormat.DTS),
            allowDtsHd = allowed.getValue(PassthroughFormat.DTS_HD),
            softwareDecodersAvailable = softwareDecodersAvailable,
        )
    }

    /** G5d (feature 54): add HDR10 metadata when the DV layer is stripped. Off = official. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val hdr10SeiOnDvStrip: Flow<Boolean> = profileManager.activeProfileId.flatMapLatest { profileId ->
        store(profileId).data.map { enabled && (it[hdr10SeiOnDvStripKey] ?: false) }
    }

    suspend fun setHdr10SeiOnDvStrip(value: Boolean) {
        store().edit { it[hdr10SeiOnDvStripKey] = value }
    }

    suspend fun hdr10SeiOnDvStripNow(): Boolean =
        enabled && runCatching { hdr10SeiOnDvStrip.first() }.getOrDefault(false)

    private companion object {
        const val FEATURE = "fork_audio_output"
    }
}
