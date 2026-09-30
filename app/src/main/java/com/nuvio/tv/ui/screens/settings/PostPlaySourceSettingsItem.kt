package com.nuvio.tv.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.R
import com.nuvio.tv.fork.postplay.ForkPostPlaySettingsViewModel
import com.nuvio.tv.fork.postplay.ForkPostPlaySource

/**
 * Post-play source (G9c, D054) under official's post-play rows. "Same as More like this" keeps
 * official's chain and 4 cards; hidden while DISCOVERY_SKIP_RECOMMENDATIONS is OFF.
 */
@Composable
internal fun postPlaySourceSettingsItem(viewModel: ForkPostPlaySettingsViewModel = hiltViewModel()) {
    if (!viewModel.featureEnabled) return
    val source by viewModel.source.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }

    SettingsActionRow(
        title = stringResource(R.string.post_play_source),
        subtitle = stringResource(R.string.post_play_source_sub),
        value = stringResource(source.titleRes()),
        onClick = { picking = true },
    )

    if (picking) {
        SettingsSingleChoiceDialog(
            title = stringResource(R.string.post_play_source),
            options = ForkPostPlaySource.entries.map {
                SettingsPickerOption(it, stringResource(it.titleRes()), stringResource(it.descriptionRes()))
            },
            selectedValue = source,
            onOptionSelected = {
                viewModel.setSource(it)
                picking = false
            },
            onDismiss = { picking = false },
            width = 520.dp,
            maxHeight = 420.dp,
        )
    }
}

private fun ForkPostPlaySource.titleRes(): Int = when (this) {
    ForkPostPlaySource.OFFICIAL -> R.string.post_play_source_official
    ForkPostPlaySource.AUTO -> R.string.post_play_source_auto
    ForkPostPlaySource.KURATO_AI -> R.string.post_play_source_kurato
    ForkPostPlaySource.BINGECAT_AI -> R.string.post_play_source_bingecat
    ForkPostPlaySource.MDBLIST -> R.string.post_play_source_mdblist
}

private fun ForkPostPlaySource.descriptionRes(): Int = when (this) {
    ForkPostPlaySource.OFFICIAL -> R.string.post_play_source_official_desc
    ForkPostPlaySource.AUTO -> R.string.post_play_source_auto_desc
    ForkPostPlaySource.KURATO_AI -> R.string.post_play_source_kurato_desc
    ForkPostPlaySource.BINGECAT_AI -> R.string.post_play_source_bingecat_desc
    ForkPostPlaySource.MDBLIST -> R.string.post_play_source_mdblist_desc
}
