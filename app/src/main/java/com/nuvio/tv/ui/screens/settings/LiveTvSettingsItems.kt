package com.nuvio.tv.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.R
import com.nuvio.tv.fork.livetv.LiveTvPreviewSettings
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

    /** G10f: channel previews in the list and their sound (default from AdaptiveResources). */
    val previewSettings: StateFlow<LiveTvPreviewSettings> =
        repository.previewSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveTvPreviewSettings.OFF)

    fun setPreviewsEnabled(enabled: Boolean) = repository.setPreviewsEnabled(enabled)

    fun setPreviewSound(enabled: Boolean) = repository.setPreviewSound(enabled)
}

/**
 * "Live TV" in the sidebar settings (G10b, D055): shows Live TV (M3U, Xtream and Stalker channel
 * lists) in the menu for this profile. Off by default (Reshaped's default); hidden while LIVE_TV is OFF.
 * Under it while on (G10f): channel previews, off by default on low-RAM devices, and their sound.
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
    if (!enabled) return
    val preview by viewModel.previewSettings.collectAsStateWithLifecycle()
    SettingsToggleRow(
        title = stringResource(R.string.settings_live_tv_previews_title),
        subtitle = stringResource(R.string.settings_live_tv_previews_description),
        checked = preview.previews,
        onToggle = { viewModel.setPreviewsEnabled(!preview.previews) },
    )
    if (preview.previews) {
        SettingsToggleRow(
            title = stringResource(R.string.settings_live_tv_preview_sound_title),
            subtitle = stringResource(R.string.settings_live_tv_preview_sound_description),
            checked = preview.sound,
            onToggle = { viewModel.setPreviewSound(!preview.sound) },
        )
    }
}
