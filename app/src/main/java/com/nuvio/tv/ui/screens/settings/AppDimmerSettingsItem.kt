package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.R
import com.nuvio.tv.fork.dimmer.AppDimmerRules
import com.nuvio.tv.fork.dimmer.AppDimmerViewModel

/** Superfork G9f (206): App Dimmer level under Appearance; hidden while DISCOVERY_SKIP_RECOMMENDATIONS is OFF. */
@Composable
internal fun AppDimmerSettingsCard(viewModel: AppDimmerViewModel = hiltViewModel()) {
    if (!AppDimmerRules.enabled) return
    val percent by viewModel.percent.collectAsStateWithLifecycle()
    SettingsGroupCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.app_dimmer_title),
        subtitle = stringResource(R.string.app_dimmer_subtitle),
    ) {
        SliderSettingsItem(
            title = stringResource(R.string.app_dimmer_title),
            subtitle = stringResource(R.string.app_dimmer_subtitle),
            value = percent,
            valueText = appDimmerLabel(percent),
            minValue = 0,
            maxValue = AppDimmerRules.MAX_PERCENT,
            step = AppDimmerRules.STEP,
            onValueChange = viewModel::setPercent,
        )
    }
}

/** Superfork G9f (207): the player's dimmer picker (a dialog, so it dims itself as it changes). */
@Composable
internal fun AppDimmerPickerDialog(onDismiss: () -> Unit, viewModel: AppDimmerViewModel = hiltViewModel()) {
    val percent by viewModel.percent.collectAsStateWithLifecycle()
    SettingsSingleChoiceDialog(
        title = stringResource(R.string.app_dimmer_title),
        subtitle = stringResource(R.string.app_dimmer_player_subtitle),
        options = AppDimmerRules.PRESETS.map { SettingsPickerOption(it, appDimmerLabel(it)) },
        selectedValue = AppDimmerRules.PRESETS.minBy { kotlin.math.abs(it - percent) },
        onOptionSelected = viewModel::setPercent,
        onDismiss = onDismiss,
    )
}

@Composable
private fun appDimmerLabel(percent: Int): String =
    if (percent == 0) stringResource(R.string.app_dimmer_off) else "$percent%"
