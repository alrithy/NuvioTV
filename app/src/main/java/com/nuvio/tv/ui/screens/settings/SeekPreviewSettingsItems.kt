package com.nuvio.tv.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.R
import com.nuvio.tv.fork.seek.SeekIntelligence
import com.nuvio.tv.fork.seek.SeekrKeyValidator
import com.nuvio.tv.fork.seek.local.LocalSeekPreviewSettings
import com.nuvio.tv.fork.seek.seekrKeyStore
import kotlinx.coroutines.launch

/**
 * Seek preview rows (G7a, D052): "Generate previews on device" (default from the resource budget)
 * and the user's own Seekr key, which is checked with Seekr before it is saved (encrypted per
 * profile) and never shown back in full. Hidden while SEEK_INTELLIGENCE is OFF.
 */
@Composable
internal fun seekPreviewSettingsItems(enabled: Boolean) {
    if (!SeekIntelligence.enabled) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localEnabled by LocalSeekPreviewSettings.enabled(context).collectAsStateWithLifecycle()
    val keyStore = remember { seekrKeyStore(context) }
    val apiKey by keyStore.apiKey.collectAsStateWithLifecycle(initialValue = "")
    var showKeyDialog by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    SettingsSectionLabel(text = stringResource(R.string.settings_seek_preview_label))
    SettingsToggleRow(
        title = stringResource(R.string.settings_seek_preview_local),
        subtitle = stringResource(R.string.settings_seek_preview_local_description),
        checked = localEnabled,
        onToggle = { LocalSeekPreviewSettings.setEnabled(context, !localEnabled) },
        enabled = enabled,
    )
    SettingsActionRow(
        title = stringResource(R.string.settings_seekr_api_key),
        subtitle = status ?: stringResource(R.string.settings_seekr_api_key_description),
        value = if (apiKey.isBlank()) {
            stringResource(R.string.settings_seekr_api_key_none)
        } else {
            stringResource(R.string.settings_seekr_api_key_custom)
        },
        enabled = enabled,
        onClick = { showKeyDialog = true },
    )

    if (showKeyDialog) {
        val checking = stringResource(R.string.settings_seekr_api_key_checking)
        val saved = stringResource(R.string.settings_seekr_api_key_saved)
        val invalid = stringResource(R.string.settings_seekr_api_key_invalid)
        val cleared = stringResource(R.string.settings_seekr_api_key_cleared)
        ForkTextEntryDialog(
            title = stringResource(R.string.settings_seekr_api_key),
            description = stringResource(R.string.settings_seekr_api_key_entry),
            placeholder = stringResource(R.string.settings_seekr_api_key_placeholder),
            confirmText = stringResource(R.string.action_save),
            keyboardType = KeyboardType.Password,
            onDismiss = { showKeyDialog = false },
            onConfirm = { entered ->
                showKeyDialog = false
                if (entered.equals(CLEAR_WORD, ignoreCase = true)) {
                    scope.launch { keyStore.setApiKey("") }
                    status = cleared
                    return@ForkTextEntryDialog
                }
                status = checking
                scope.launch {
                    status = if (SeekrKeyValidator.validate(entered)) {
                        keyStore.setApiKey(entered)
                        saved
                    } else {
                        invalid
                    }
                }
            },
        )
    }
}

/** Typed instead of a key to remove the stored one. */
private const val CLEAR_WORD = "clear"
