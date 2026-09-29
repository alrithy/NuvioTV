package com.nuvio.tv.fork.diagnostics

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.player.DisplayCapabilities
import com.nuvio.tv.data.local.FrameRateMatchingMode
import com.nuvio.tv.data.local.PlayerSettings
import com.nuvio.tv.ui.screens.settings.SettingsActionRow
import com.nuvio.tv.ui.theme.NuvioTheme

/** Device assessment block shown under the official stream test (G1c, features 14–16). */
@Composable
internal fun DeviceAssessmentSection(
    settings: PlayerSettings,
    singleConnectionMbps: Double?,
    parallelMbps: Double?,
    viewModel: DeviceAssessmentViewModel = hiltViewModel(),
) {
    if (!viewModel.enabled) return
    val context = LocalContext.current
    val displaySupportsSwitching = remember(context) {
        context.findActivity()
            ?.let { DisplayCapabilities.detect(it) }
            ?.takeIf { it.apiSupported }
            ?.supportsFrameRateSwitching
    }
    val result = remember(settings, displaySupportsSwitching, singleConnectionMbps, parallelMbps) {
        viewModel.assess(settings, displaySupportsSwitching, singleConnectionMbps, parallelMbps)
    }
    val canRevert by viewModel.canRevert.collectAsStateWithLifecycle()
    val lastAction by viewModel.lastAction.collectAsStateWithLifecycle()
    val changeCount = result.plan.changeCount()

    Column(verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)) {
        Text(
            text = stringResource(R.string.assessment_title),
            style = MaterialTheme.typography.titleSmall,
            color = NuvioTheme.colors.TextPrimary
        )
        result.items.forEach { item -> AssessmentRow(item, result.plan, settings) }
        if (result.items.none { it.key == AssessmentKey.PARALLEL_CONNECTIONS }) {
            Text(
                text = stringResource(R.string.assessment_run_stream_test),
                style = MaterialTheme.typography.bodySmall,
                color = NuvioTheme.colors.TextSecondary
            )
        }
        SettingsActionRow(
            title = stringResource(R.string.assessment_apply),
            subtitle = if (changeCount > 0) {
                stringResource(R.string.assessment_apply_count, changeCount)
            } else {
                stringResource(R.string.assessment_nothing_to_apply)
            },
            enabled = changeCount > 0,
            onClick = { viewModel.apply(result.plan) }
        )
        if (canRevert) {
            SettingsActionRow(
                title = stringResource(R.string.assessment_revert),
                subtitle = stringResource(R.string.assessment_revert_subtitle),
                onClick = viewModel::revert
            )
        }
        lastAction?.let { action ->
            Text(
                text = if (action < 0) {
                    stringResource(R.string.assessment_reverted)
                } else {
                    stringResource(R.string.assessment_applied, action)
                },
                style = MaterialTheme.typography.bodySmall,
                color = NuvioTheme.colors.Success
            )
        }
    }
}

@Composable
private fun AssessmentRow(item: AssessmentItem, plan: AssessmentPlan, settings: PlayerSettings) {
    val on = stringResource(R.string.assessment_value_on)
    val off = stringResource(R.string.assessment_value_off)
    val title = when (item.key) {
        AssessmentKey.PARALLEL_CONNECTIONS -> stringResource(R.string.assessment_row_parallel)
        AssessmentKey.TARGET_BUFFER -> stringResource(R.string.assessment_row_target_buffer)
        AssessmentKey.FRAME_RATE_MATCHING -> stringResource(R.string.assessment_row_frame_rate)
    }
    val tier = when (item.tier) {
        AssessmentTier.MEASURED -> stringResource(R.string.assessment_tier_measured)
        AssessmentTier.CALCULATED -> stringResource(R.string.assessment_tier_calculated)
        AssessmentTier.VERIFY -> stringResource(R.string.assessment_tier_verify)
    }
    val value = when {
        item.tier == AssessmentTier.VERIFY -> stringResource(R.string.assessment_verify_display)
        !item.changeNeeded -> stringResource(R.string.assessment_value_ok)
        else -> when (item.key) {
            AssessmentKey.PARALLEL_CONNECTIONS -> {
                val current = connectionsLabel(settings.useParallelConnections, settings.parallelConnectionCount, off)
                val target = connectionsLabel(
                    plan.useParallelConnections ?: settings.useParallelConnections,
                    plan.parallelConnectionCount ?: settings.parallelConnectionCount,
                    off
                )
                "$current → $target"
            }
            AssessmentKey.TARGET_BUFFER -> stringResource(
                R.string.assessment_value_change_mb,
                settings.bufferSettings.targetBufferSizeMb,
                plan.targetBufferSizeMb ?: settings.bufferSettings.targetBufferSizeMb
            )
            AssessmentKey.FRAME_RATE_MATCHING -> {
                val current = if (settings.frameRateMatchingMode != FrameRateMatchingMode.OFF) on else off
                val target = if (plan.frameRateMatchingOn == true) on else off
                "$current → $target"
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = "$title · $tier",
            style = MaterialTheme.typography.bodySmall,
            color = NuvioTheme.colors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (item.changeNeeded) NuvioTheme.colors.Warning else NuvioTheme.colors.TextPrimary
        )
    }
}

@Composable
private fun connectionsLabel(enabled: Boolean, count: Int, off: String): String =
    if (enabled) stringResource(R.string.assessment_value_connections, count) else off

private fun AssessmentPlan.changeCount(): Int = listOfNotNull(
    useParallelConnections,
    parallelConnectionCount,
    targetBufferSizeMb,
    frameRateMatchingOn,
).size

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
