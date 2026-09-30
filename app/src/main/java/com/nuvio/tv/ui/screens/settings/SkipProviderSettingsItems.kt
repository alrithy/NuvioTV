package com.nuvio.tv.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.R
import com.nuvio.tv.fork.skip.ForkSkipProvider
import com.nuvio.tv.fork.skip.SkipProviderSettingsViewModel

/**
 * Extra skip providers (G9a, D054) under the official skip rows: one switch per provider, all off by
 * default, and an API key for the providers that take one (encrypted per profile, never shown back).
 * Hidden while DISCOVERY_SKIP_RECOMMENDATIONS is OFF.
 */
@Composable
internal fun skipProviderSettingsItems(enabled: Boolean, viewModel: SkipProviderSettingsViewModel = hiltViewModel()) {
    if (!viewModel.featureEnabled) return
    val config by viewModel.config.collectAsStateWithLifecycle()
    var keyDialogFor by remember { mutableStateOf<ForkSkipProvider?>(null) }

    SettingsSectionLabel(
        text = stringResource(R.string.skip_providers_label),
        description = stringResource(R.string.skip_providers_description),
    )
    ForkSkipProvider.entries.forEach { provider ->
        val on = provider in config.enabled
        SettingsToggleRow(
            title = stringResource(provider.titleRes()),
            subtitle = stringResource(provider.subtitleRes()),
            checked = on,
            onToggle = { viewModel.setEnabled(provider, !on) },
            enabled = enabled,
        )
        if (provider.takesKey()) {
            val hasKey = !config.keys[provider].isNullOrBlank()
            SettingsActionRow(
                title = stringResource(R.string.skip_provider_api_key, stringResource(provider.titleRes())),
                subtitle = stringResource(
                    if (provider.requiresKey) R.string.skip_provider_api_key_required else R.string.skip_provider_api_key_optional,
                ),
                value = stringResource(if (hasKey) R.string.skip_provider_api_key_set else R.string.skip_provider_api_key_none),
                enabled = enabled && on,
                onClick = { keyDialogFor = provider },
            )
        }
    }

    keyDialogFor?.let { provider ->
        ForkTextEntryDialog(
            title = stringResource(R.string.skip_provider_api_key, stringResource(provider.titleRes())),
            description = stringResource(R.string.skip_provider_api_key_entry),
            placeholder = stringResource(R.string.skip_provider_api_key_placeholder),
            confirmText = stringResource(R.string.action_save),
            keyboardType = KeyboardType.Password,
            onDismiss = { keyDialogFor = null },
            onConfirm = { entered ->
                keyDialogFor = null
                viewModel.setApiKey(provider, if (entered.equals(CLEAR_WORD, ignoreCase = true)) "" else entered)
            },
        )
    }
}

private fun ForkSkipProvider.takesKey(): Boolean = this != ForkSkipProvider.SKIP_ME

private fun ForkSkipProvider.titleRes(): Int = when (this) {
    ForkSkipProvider.SKIP_ME -> R.string.skip_provider_skipme
    ForkSkipProvider.THE_INTRO_DB -> R.string.skip_provider_theintrodb
    ForkSkipProvider.PUBLIC_META_DB -> R.string.skip_provider_publicmetadb
}

private fun ForkSkipProvider.subtitleRes(): Int = when (this) {
    ForkSkipProvider.SKIP_ME -> R.string.skip_provider_skipme_sub
    ForkSkipProvider.THE_INTRO_DB -> R.string.skip_provider_theintrodb_sub
    ForkSkipProvider.PUBLIC_META_DB -> R.string.skip_provider_publicmetadb_sub
}

/** Typed instead of a key to remove the stored one (as for the Seekr key). */
private const val CLEAR_WORD = "clear"
