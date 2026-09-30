package com.nuvio.tv.fork.streams

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.R
import com.nuvio.tv.ui.screens.settings.SettingsToggleRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamRankingViewModel @Inject constructor(
    private val preferences: StreamRankingPreferences,
) : ViewModel() {

    val enabled: Boolean = preferences.enabled

    val bestQualityListOrder: StateFlow<Boolean> =
        preferences.bestQualityListOrder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setBestQualityListOrder(value: Boolean) {
        viewModelScope.launch { preferences.setBestQualityListOrder(value) }
    }
}

/** G8b row inside the official stream-selection settings; nothing while STREAM_INTELLIGENCE is OFF. */
@Composable
internal fun StreamRankingSection(viewModel: StreamRankingViewModel = hiltViewModel()) {
    if (!viewModel.enabled) return
    val checked by viewModel.bestQualityListOrder.collectAsStateWithLifecycle()
    SettingsToggleRow(
        title = stringResource(R.string.stream_list_order_best_quality),
        subtitle = stringResource(R.string.stream_list_order_best_quality_sub),
        checked = checked,
        onToggle = { viewModel.setBestQualityListOrder(!checked) }
    )
}
