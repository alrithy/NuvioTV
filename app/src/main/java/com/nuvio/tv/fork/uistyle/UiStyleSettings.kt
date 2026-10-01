package com.nuvio.tv.fork.uistyle

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
 * Per-profile UI style choices in `fork_ui_style` (G12a, D058). Official layout preferences are never
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
