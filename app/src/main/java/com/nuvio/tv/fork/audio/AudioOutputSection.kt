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
    val passthroughAllowed by viewModel.passthroughAllowed.collectAsStateWithLifecycle()
    PassthroughFormat.entries.forEach { format ->
        val allowed = passthroughAllowed[format] ?: true
        SettingsToggleRow(
            title = stringResource(format.titleRes()),
            subtitle = stringResource(R.string.audio_passthrough_format_sub),
            checked = allowed,
            enabled = enabled,
            onToggle = { viewModel.setPassthroughAllowed(format, !allowed) }
        )
    }
}

private fun PassthroughFormat.titleRes(): Int = when (this) {
    PassthroughFormat.AC3 -> R.string.audio_passthrough_ac3
    PassthroughFormat.EAC3 -> R.string.audio_passthrough_eac3
    PassthroughFormat.TRUEHD -> R.string.audio_passthrough_truehd
    PassthroughFormat.DTS -> R.string.audio_passthrough_dts
    PassthroughFormat.DTS_HD -> R.string.audio_passthrough_dtshd
}
