package com.nuvio.tv.fork.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioOutputViewModel @Inject constructor(
    private val preferences: AudioOutputPreferences,
) : ViewModel() {

    val enabled: Boolean = preferences.enabled

    val preferLossless: StateFlow<Boolean> =
        preferences.preferLossless.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setPreferLossless(value: Boolean) {
        viewModelScope.launch { preferences.setPreferLossless(value) }
    }

    val passthroughAllowed: StateFlow<Map<PassthroughFormat, Boolean>> =
        preferences.passthroughAllowed.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PassthroughFormat.entries.associateWith { true }
        )

    fun setPassthroughAllowed(format: PassthroughFormat, value: Boolean) {
        viewModelScope.launch { preferences.setPassthroughAllowed(format, value) }
    }
}
