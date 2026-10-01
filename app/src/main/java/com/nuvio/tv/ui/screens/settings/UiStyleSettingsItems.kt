package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.R
import com.nuvio.tv.fork.uistyle.NavigationStyle
import com.nuvio.tv.fork.uistyle.ScreensaverRules
import com.nuvio.tv.fork.uistyle.UiStyleSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class UiStyleSettingsViewModel @Inject constructor(private val settings: UiStyleSettings) : ViewModel() {
    val featureEnabled: Boolean get() = settings.featureEnabled

    val navigationStyle: StateFlow<NavigationStyle> =
        settings.navigationStyle.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NavigationStyle.SIDEBAR)

    val clockEnabled: StateFlow<Boolean> =
        settings.clockEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val lightweightEffects: StateFlow<Boolean> =
        settings.lightweightEffects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setNavigationStyle(style: NavigationStyle) {
        viewModelScope.launch { settings.setNavigationStyle(style) }
    }

    fun setClockEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setClockEnabled(enabled) }
    }

    fun setLightweightEffects(enabled: Boolean) {
        viewModelScope.launch { settings.setLightweightEffects(enabled) }
    }

    val screensaverEnabled: StateFlow<Boolean> =
        settings.screensaverEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val screensaverTimeoutMinutes: StateFlow<Int> =
        settings.screensaverTimeoutMinutes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScreensaverRules.DEFAULT_TIMEOUT_MINUTES)

    val screensaverDimPercent: StateFlow<Int> =
        settings.screensaverDimPercent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScreensaverRules.DEFAULT_DIM_PERCENT)

    fun setScreensaverEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setScreensaverEnabled(enabled) }
    }

    fun setScreensaverTimeoutMinutes(minutes: Int) {
        viewModelScope.launch { settings.setScreensaverTimeoutMinutes(minutes) }
    }

    fun setScreensaverDimPercent(percent: Int) {
        viewModelScope.launch { settings.setScreensaverDimPercent(percent) }
    }
}

/**
 * "Navigation style" in the sidebar settings (G12a–G12c, D058, D060, D061): official's sidebar
 * (default), a top bar, a pill, Glass or Cinematic Glass, per profile; under a top menu, the clock beside it; under Glass, the
 * "Lightweight effects" switch. Hidden while UI_STYLES is OFF.
 */
@Composable
internal fun UiStyleSettingsItems(viewModel: UiStyleSettingsViewModel = hiltViewModel()) {
    if (!viewModel.featureEnabled) return
    val style by viewModel.navigationStyle.collectAsStateWithLifecycle()
    SettingsActionRow(
        title = stringResource(R.string.settings_navigation_style_title),
        subtitle = stringResource(R.string.settings_navigation_style_description),
        value = stringResource(
            when (style) {
                NavigationStyle.SIDEBAR -> R.string.navigation_style_sidebar
                NavigationStyle.TOP_BAR -> R.string.navigation_style_top_bar
                NavigationStyle.PILL -> R.string.navigation_style_pill
                NavigationStyle.GLASS -> R.string.navigation_style_glass
                NavigationStyle.CINEMATIC_GLASS -> R.string.navigation_style_cinematic_glass
            },
        ),
        onClick = { viewModel.setNavigationStyle(style.next()) },
    )
    if (style == NavigationStyle.SIDEBAR) return
    val clock by viewModel.clockEnabled.collectAsStateWithLifecycle()
    SettingsToggleRow(
        title = stringResource(R.string.settings_top_menu_clock_title),
        subtitle = stringResource(R.string.settings_top_menu_clock_description),
        checked = clock,
        onToggle = { viewModel.setClockEnabled(!clock) },
    )
    if (!style.isGlass) return
    val lightweight by viewModel.lightweightEffects.collectAsStateWithLifecycle()
    SettingsToggleRow(
        title = stringResource(R.string.settings_lightweight_effects_title),
        subtitle = stringResource(R.string.settings_lightweight_effects_description),
        checked = lightweight,
        onToggle = { viewModel.setLightweightEffects(!lightweight) },
    )
}

/**
 * Superfork G12d (292): the idle screensaver under Appearance, per profile and off by default; how
 * long before it dims and how dark. Hidden while UI_STYLES is OFF.
 */
@Composable
internal fun ScreensaverSettingsCard(viewModel: UiStyleSettingsViewModel = hiltViewModel()) {
    if (!viewModel.featureEnabled) return
    val enabled by viewModel.screensaverEnabled.collectAsStateWithLifecycle()
    SettingsGroupCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.screensaver_title),
        subtitle = stringResource(R.string.screensaver_subtitle),
    ) {
        SettingsToggleRow(
            title = stringResource(R.string.screensaver_enabled),
            subtitle = stringResource(R.string.screensaver_enabled_subtitle),
            checked = enabled,
            onToggle = { viewModel.setScreensaverEnabled(!enabled) },
        )
        if (enabled) {
            val minutes by viewModel.screensaverTimeoutMinutes.collectAsStateWithLifecycle()
            val dim by viewModel.screensaverDimPercent.collectAsStateWithLifecycle()
            SettingsActionRow(
                title = stringResource(R.string.screensaver_timeout),
                subtitle = stringResource(R.string.screensaver_timeout_subtitle),
                value = stringResource(R.string.screensaver_timeout_value, minutes),
                onClick = { viewModel.setScreensaverTimeoutMinutes(ScreensaverRules.nextTimeout(minutes)) },
            )
            SettingsActionRow(
                title = stringResource(R.string.screensaver_dim),
                subtitle = stringResource(R.string.screensaver_dim_subtitle),
                value = "$dim%",
                onClick = { viewModel.setScreensaverDimPercent(ScreensaverRules.nextDim(dim)) },
            )
        }
    }
}
