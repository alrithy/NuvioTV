package com.nuvio.tv.fork.postplay

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-profile post-play source (G9c) in `fork_post_play`; [ForkPostPlaySource.OFFICIAL] by default and
 * whenever DISCOVERY_SKIP_RECOMMENDATIONS is OFF, so official's chain and 4 cards stay the default.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class ForkPostPlaySettings @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager,
    registry: FeatureRegistry,
) {
    val featureEnabled: Boolean = registry.mode(FeatureId.DISCOVERY_SKIP_RECOMMENDATIONS) != FeatureMode.OFF

    private val sourceKey = stringPreferencesKey("source")

    private fun store(profileId: Int = profileManager.activeProfileId.value) = factory.get(profileId, FEATURE)

    val source: Flow<ForkPostPlaySource> = if (!featureEnabled) {
        flowOf(ForkPostPlaySource.OFFICIAL)
    } else {
        profileManager.activeProfileId.flatMapLatest { profileId ->
            store(profileId).data.map { ForkPostPlaySource.fromKey(it[sourceKey]) }
        }
    }

    /** Official on any read failure. */
    suspend fun sourceNow(): ForkPostPlaySource = runCatching { source.first() }.getOrDefault(ForkPostPlaySource.OFFICIAL)

    suspend fun setSource(value: ForkPostPlaySource) {
        store().edit { it[sourceKey] = value.key }
    }

    private companion object {
        const val FEATURE = "fork_post_play"
    }
}
