package com.nuvio.tv.fork.skip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkipProviderSettingsViewModel @Inject constructor(
    private val settings: SkipProviderSettings,
) : ViewModel() {

    val featureEnabled: Boolean = settings.featureEnabled

    val config: StateFlow<SkipProviderConfig> =
        settings.config.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SkipProviderConfig.NONE)

    fun setEnabled(provider: ForkSkipProvider, value: Boolean) {
        viewModelScope.launch { settings.setEnabled(provider, value) }
    }

    fun setApiKey(provider: ForkSkipProvider, value: String) {
        viewModelScope.launch { settings.setApiKey(provider, value) }
    }
}
