package com.nuvio.tv.fork.playback

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.screens.settings.SettingsActionRow
import com.nuvio.tv.ui.screens.settings.SettingsGroupCard
import com.nuvio.tv.ui.theme.NuvioTheme

/** Settings card for [PlaybackStrategySection]; renders nothing while the feature is OFF. */
@Composable
internal fun PlaybackStrategyCard(viewModel: PlaybackStrategyViewModel = hiltViewModel()) {
    if (!viewModel.enabled) return
    SettingsGroupCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(NuvioTheme.spacing.xs)) {
            PlaybackStrategySection(viewModel)
        }
    }
}

/** Playback strategy selection (G3, features 3 and 27–31). Official is the default. */
@Composable
internal fun PlaybackStrategySection(viewModel: PlaybackStrategyViewModel) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)) {
        Text(
            text = stringResource(R.string.playback_strategy_title),
            style = MaterialTheme.typography.titleSmall,
            color = NuvioTheme.colors.TextPrimary
        )
        Text(
            text = stringResource(R.string.playback_strategy_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = NuvioTheme.colors.TextSecondary
        )
        PlaybackStrategy.entries.forEach { strategy ->
            SettingsActionRow(
                title = stringResource(strategy.titleRes()),
                subtitle = stringResource(strategy.descriptionRes()),
                value = if (strategy == selected) stringResource(R.string.playback_strategy_selected) else null,
                onClick = { viewModel.select(strategy) }
            )
        }
    }
}

@StringRes
private fun PlaybackStrategy.titleRes(): Int = when (this) {
    PlaybackStrategy.OFFICIAL -> R.string.playback_strategy_official
    PlaybackStrategy.REMUX_THROUGHPUT -> R.string.playback_strategy_remux
    PlaybackStrategy.SEEK_OPTIMIZED -> R.string.playback_strategy_seek
    PlaybackStrategy.LOW_MEMORY -> R.string.playback_strategy_low_memory
    PlaybackStrategy.AUTO -> R.string.playback_strategy_auto
}

@StringRes
private fun PlaybackStrategy.descriptionRes(): Int = when (this) {
    PlaybackStrategy.OFFICIAL -> R.string.playback_strategy_official_sub
    PlaybackStrategy.REMUX_THROUGHPUT -> R.string.playback_strategy_remux_sub
    PlaybackStrategy.SEEK_OPTIMIZED -> R.string.playback_strategy_seek_sub
    PlaybackStrategy.LOW_MEMORY -> R.string.playback_strategy_low_memory_sub
    PlaybackStrategy.AUTO -> R.string.playback_strategy_auto_sub
}
