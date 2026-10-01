package com.nuvio.tv.ui.screens.aimedia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.nuvio.tv.R
import com.nuvio.tv.fork.foundation.ExperimentalOptIn
import com.nuvio.tv.fork.foundation.ExperimentalOptInStore
import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.ui.components.AppDimmedDialog
import com.nuvio.tv.ui.screens.settings.SettingsActionRow
import com.nuvio.tv.ui.screens.settings.SettingsGroupCard
import com.nuvio.tv.ui.screens.settings.SettingsToggleRow
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * Settings → Advanced → Experimental (G13b / G13c, D063): the only place an experimental group is turned on.
 * Each switch is off by default and applies on the next app start, so the running app never changes
 * under the user; while AI media providers are active, their Provider Center opens from here.
 */
@Composable
fun ExperimentalSettingsCard() {
    val context = LocalContext.current
    var stored by remember { mutableStateOf(ExperimentalOptInStore.stored(context)) }
    var showProviders by remember { mutableStateOf(false) }
    val aiStored = FeatureId.AI_MEDIA.name in stored
    val aiActive = FeatureId.AI_MEDIA in ExperimentalOptIn.enabled
    val matStored = FeatureId.MAT_AUDIO.name in stored
    val matActive = FeatureId.MAT_AUDIO in ExperimentalOptIn.enabled

    SettingsGroupCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.experimental_title),
        subtitle = stringResource(R.string.experimental_subtitle),
    ) {
        SettingsToggleRow(
            title = stringResource(R.string.experimental_ai_media_title),
            subtitle = if (aiStored != aiActive) {
                stringResource(R.string.experimental_restart_pending)
            } else {
                stringResource(R.string.experimental_ai_media_description)
            },
            checked = aiStored,
            onToggle = {
                ExperimentalOptInStore.setOptedIn(context, FeatureId.AI_MEDIA, !aiStored)
                stored = ExperimentalOptInStore.stored(context)
            },
        )
        // G13c (45): app-side TrueHD -> MAT for eARC receivers; the audio path is official's while off.
        SettingsToggleRow(
            title = stringResource(R.string.experimental_mat_title),
            subtitle = if (matStored != matActive) {
                stringResource(R.string.experimental_restart_pending)
            } else {
                stringResource(R.string.experimental_mat_description)
            },
            checked = matStored,
            onToggle = {
                ExperimentalOptInStore.setOptedIn(context, FeatureId.MAT_AUDIO, !matStored)
                stored = ExperimentalOptInStore.stored(context)
            },
        )
        if (aiActive) {
            SettingsActionRow(
                title = stringResource(R.string.experimental_open_providers),
                subtitle = stringResource(R.string.provider_center_subtitle),
                onClick = { showProviders = true },
            )
        }
    }

    if (showProviders && aiActive) {
        AppDimmedDialog(
            onDismissRequest = { showProviders = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(NuvioTheme.colors.Background)) {
                ProviderCenterScreen(onBackPress = { showProviders = false })
            }
        }
    }
}
