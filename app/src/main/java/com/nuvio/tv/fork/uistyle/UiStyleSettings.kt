package com.nuvio.tv.fork.uistyle

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Per-profile UI style choices in `fork_ui_style` (G12a–G12d, D058, D060, D061). Official layout preferences are never
 * written. While UI_STYLES is OFF every flow reports official's sidebar.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class UiStyleSettings @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager,
) {
    private val navigationKey = stringPreferencesKey("navigation_style")
    private val clockKey = booleanPreferencesKey("top_menu_clock")
    private val lightweightKey = booleanPreferencesKey("lightweight_effects")
    private val screensaverKey = booleanPreferencesKey("screensaver_enabled")
    private val screensaverTimeoutKey = intPreferencesKey("screensaver_timeout_minutes")
    private val screensaverDimKey = intPreferencesKey("screensaver_dim_percent")

    val featureEnabled: Boolean get() = UiStyleRules.enabled

    val navigationStyle: Flow<NavigationStyle> = if (!UiStyleRules.enabled) {
        flowOf(NavigationStyle.SIDEBAR)
    } else {
        profileManager.activeProfileId.flatMapLatest { profileId ->
            factory.get(profileId, FEATURE).data.map { UiStyleRules.navigationStyle(NavigationStyle.fromStored(it[navigationKey])) }
        }
    }

    /** The clock beside the top menu; on by default once a top menu is chosen. */
    val clockEnabled: Flow<Boolean> = if (!UiStyleRules.enabled) {
        flowOf(false)
    } else {
        profileManager.activeProfileId.flatMapLatest { profileId ->
            factory.get(profileId, FEATURE).data.map { it[clockKey] ?: true }
        }
    }

    /** "Lightweight effects": the Glass chrome without live blur on any device (feature 200). */
    val lightweightEffects: Flow<Boolean> = if (!UiStyleRules.enabled) {
        flowOf(true)
    } else {
        profileManager.activeProfileId.flatMapLatest { profileId ->
            factory.get(profileId, FEATURE).data.map { it[lightweightKey] ?: false }
        }
    }

    /** The idle screensaver (G12d, feature 292): off by default, and always off while UI_STYLES is OFF. */
    val screensaverEnabled: Flow<Boolean> = if (!UiStyleRules.enabled) {
        flowOf(false)
    } else {
        profileManager.activeProfileId.flatMapLatest { profileId ->
            factory.get(profileId, FEATURE).data.map { it[screensaverKey] ?: false }
        }
    }

    val screensaverTimeoutMinutes: Flow<Int> = profileManager.activeProfileId.flatMapLatest { profileId ->
        factory.get(profileId, FEATURE).data.map { ScreensaverRules.coerceTimeout(it[screensaverTimeoutKey]) }
    }

    val screensaverDimPercent: Flow<Int> = profileManager.activeProfileId.flatMapLatest { profileId ->
        factory.get(profileId, FEATURE).data.map { ScreensaverRules.coerceDim(it[screensaverDimKey]) }
    }

    suspend fun setScreensaverEnabled(enabled: Boolean) {
        factory.get(profileManager.activeProfileId.value, FEATURE).edit { it[screensaverKey] = enabled }
    }

    suspend fun setScreensaverTimeoutMinutes(minutes: Int) {
        factory.get(profileManager.activeProfileId.value, FEATURE).edit { it[screensaverTimeoutKey] = ScreensaverRules.coerceTimeout(minutes) }
    }

    suspend fun setScreensaverDimPercent(percent: Int) {
        factory.get(profileManager.activeProfileId.value, FEATURE).edit { it[screensaverDimKey] = ScreensaverRules.coerceDim(percent) }
    }

    suspend fun setLightweightEffects(enabled: Boolean) {
        factory.get(profileManager.activeProfileId.value, FEATURE).edit { it[lightweightKey] = enabled }
    }

    suspend fun setNavigationStyle(style: NavigationStyle) {
        factory.get(profileManager.activeProfileId.value, FEATURE).edit { it[navigationKey] = style.name }
    }

    suspend fun setClockEnabled(enabled: Boolean) {
        factory.get(profileManager.activeProfileId.value, FEATURE).edit { it[clockKey] = enabled }
    }

    private companion object {
        const val FEATURE = "fork_ui_style"
    }
}
