package com.nuvio.tv.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.R
import com.nuvio.tv.fork.uistyle.NavigationStyle
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

    fun setNavigationStyle(style: NavigationStyle) {
        viewModelScope.launch { settings.setNavigationStyle(style) }
    }

    fun setClockEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setClockEnabled(enabled) }
    }
}

/**
 * "Navigation style" in the sidebar settings (G12a, D058): official's sidebar (default), a top bar or a
 * pill, per profile; under a top menu, the clock beside it. Hidden while UI_STYLES is OFF.
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
}
