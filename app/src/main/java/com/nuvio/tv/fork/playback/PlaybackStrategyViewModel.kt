package com.nuvio.tv.fork.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaybackStrategyViewModel @Inject constructor(
    private val session: PlaybackStrategySession,
) : ViewModel() {

    val enabled: Boolean = session.enabled

    val selected: StateFlow<PlaybackStrategy> =
        session.selected.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackStrategy.OFFICIAL)

    fun select(strategy: PlaybackStrategy) {
        viewModelScope.launch { session.select(strategy) }
    }
}
