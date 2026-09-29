package com.nuvio.tv.fork.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.R
import com.nuvio.tv.ui.screens.settings.SettingsToggleRow

/** Fork audio output rows (G5) inside the official audio settings; nothing while the group is OFF. */
@Composable
internal fun AudioOutputSection(enabled: Boolean, viewModel: AudioOutputViewModel = hiltViewModel()) {
    if (!viewModel.enabled) return
    val preferLossless by viewModel.preferLossless.collectAsStateWithLifecycle()
    SettingsToggleRow(
        title = stringResource(R.string.audio_prefer_lossless),
        subtitle = stringResource(R.string.audio_prefer_lossless_sub),
        checked = preferLossless,
        enabled = enabled,
        onToggle = { viewModel.setPreferLossless(!preferLossless) }
    )
}
