package com.nuvio.tv.fork.postplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForkPostPlaySettingsViewModel @Inject constructor(
    private val settings: ForkPostPlaySettings,
) : ViewModel() {

    val featureEnabled: Boolean = settings.featureEnabled

    val source: StateFlow<ForkPostPlaySource> =
        settings.source.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForkPostPlaySource.OFFICIAL)

    fun setSource(value: ForkPostPlaySource) {
        viewModelScope.launch { settings.setSource(value) }
    }
}
