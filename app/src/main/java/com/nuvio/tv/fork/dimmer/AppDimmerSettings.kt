package com.nuvio.tv.fork.dimmer

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Per-profile dimmer level in `fork_app_dimmer` (Cxsmo kept it in its theme store); 0 = off. */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class AppDimmerSettings @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager,
) {
    private val percentKey = intPreferencesKey("percent")

    val percent: Flow<Int> = if (!AppDimmerRules.enabled) {
        flowOf(0)
    } else {
        profileManager.activeProfileId.flatMapLatest { profileId ->
            factory.get(profileId, FEATURE).data.map { AppDimmerRules.clamp(it[percentKey] ?: 0) }
        }
    }

    suspend fun setPercent(value: Int) {
        factory.get(profileManager.activeProfileId.value, FEATURE).edit { it[percentKey] = AppDimmerRules.clamp(value) }
    }

    private companion object {
        const val FEATURE = "fork_app_dimmer"
    }
}

@HiltViewModel
class AppDimmerViewModel @Inject constructor(private val settings: AppDimmerSettings) : ViewModel() {
    val percent: StateFlow<Int> = settings.percent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setPercent(value: Int) {
        viewModelScope.launch { settings.setPercent(value) }
    }
}
