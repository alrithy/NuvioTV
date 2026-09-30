package com.nuvio.tv.data.local

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.domain.model.EpisodeShuffleSettings
import com.nuvio.tv.fork.discovery.ShuffleRules
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

data class EpisodeShuffleProfile(
    val profileId: Int,
    val available: Boolean = false,
    val shows: Map<String, EpisodeShuffleSettings> = emptyMap()
) {
    fun settings(contentId: String, contentType: String): EpisodeShuffleSettings {
        val saved = shows[contentId] ?: EpisodeShuffleSettings()
        return saved.copy(enabled = saved.enabled && available &&
            (contentType.equals("series", true) || contentType.equals("tv", true)))
            .let { if (ShuffleRules.enabled) it else it.copy(season = null, mystery = false, fallbackToWatched = false) } // Superfork G9d
    }
}

@Singleton
class EpisodeShuffleStore @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager,
    private val layoutPreferences: LayoutPreferenceDataStore
) {
    val profiles = profileManager.activeProfileId.flatMapLatest(::observeProfile)

    fun observeProfile(profileId: Int) = combine(
        factory.get(profileId, "episode_shuffle").data,
        layoutPreferences.randomEpisodeEnabledForProfile(profileId)
    ) { preferences, available ->
        EpisodeShuffleProfile(profileId, available, readShuffleSettings(preferences))
    }

    suspend fun save(contentId: String, settings: EpisodeShuffleSettings, profileId: Int) {
        if (contentId.isBlank()) return
        factory.get(profileId, "episode_shuffle").edit {
            it[booleanPreferencesKey("enabled:$contentId")] = settings.enabled
            it[booleanPreferencesKey("watched:$contentId")] = settings.includeWatched
            // Superfork G9d: season scope and Mystery mode.
            val seasonKey = intPreferencesKey("season:$contentId")
            if (settings.season == null) it.remove(seasonKey) else it[seasonKey] = settings.season
            it[booleanPreferencesKey("mystery:$contentId")] = settings.mystery
            it[booleanPreferencesKey("fallback:$contentId")] = settings.fallbackToWatched
        }
    }
}

internal fun readShuffleSettings(preferences: Preferences): Map<String, EpisodeShuffleSettings> =
    preferences.asMap().keys.mapNotNull { key ->
        key.name.takeIf { it.startsWith("enabled:") || it.startsWith("watched:") }
            ?.substringAfter(':')?.takeIf(String::isNotBlank)
    }.distinct().associateWith { id ->
        EpisodeShuffleSettings(
            enabled = preferences[booleanPreferencesKey("enabled:$id")] ?: false,
            includeWatched = preferences[booleanPreferencesKey("watched:$id")] ?: false,
            season = preferences[intPreferencesKey("season:$id")],
            mystery = preferences[booleanPreferencesKey("mystery:$id")] ?: false,
            fallbackToWatched = preferences[booleanPreferencesKey("fallback:$id")] ?: false
        )
    }
