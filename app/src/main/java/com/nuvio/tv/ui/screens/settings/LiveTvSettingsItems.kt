package com.nuvio.tv.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.R
import com.nuvio.tv.fork.livetv.LiveTvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** The Live TV menu switch; reads and writes the per-profile setting only (no channels load). */
@HiltViewModel
class LiveTvMenuSettingsViewModel @Inject constructor(private val repository: LiveTvRepository) : ViewModel() {
    val featureEnabled: Boolean get() = repository.featureEnabled

    val menuEnabled: StateFlow<Boolean> =
        repository.menuEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setMenuEnabled(enabled: Boolean) = repository.setMenuEnabled(enabled)
}

/**
 * "Live TV" in the sidebar settings (G10b, D055): shows Live TV (M3U, Xtream and Stalker channel
 * lists) in the menu for this profile. Off by default (Reshaped's default); hidden while LIVE_TV is OFF.
 */
@Composable
internal fun LiveTvSettingsItems(viewModel: LiveTvMenuSettingsViewModel = hiltViewModel()) {
    if (!viewModel.featureEnabled) return
    val enabled by viewModel.menuEnabled.collectAsStateWithLifecycle()
    SettingsToggleRow(
        title = stringResource(R.string.settings_live_tv_title),
        subtitle = stringResource(R.string.settings_live_tv_description),
        checked = enabled,
        onToggle = { viewModel.setMenuEnabled(!enabled) },
    )
}
