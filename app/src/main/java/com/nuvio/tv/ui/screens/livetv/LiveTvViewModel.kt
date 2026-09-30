package com.nuvio.tv.ui.screens.livetv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.fork.livetv.LiveTvChannel
import com.nuvio.tv.fork.livetv.LiveTvLibrary
import com.nuvio.tv.fork.livetv.LiveTvOrganisation
import com.nuvio.tv.fork.livetv.LiveTvRepository
import com.nuvio.tv.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Keeps [LiveTvRepository] on the active profile (G10b; Reshaped `LiveTvScreenModel` @ 0ccf049).
 * The list lives in the repository; this keeps the filtered view, so coming back from the player
 * shows it at once, where it was.
 */
@HiltViewModel
class LiveTvViewModel @Inject constructor(
    val repository: LiveTvRepository,
    private val profileManager: ProfileManager,
) : ViewModel() {

    init {
        viewModelScope.launch {
            profileManager.activeProfileId.collectLatest { ensureLoaded() }
        }
    }

    /**
     * Called as the screen opens: Live TV lets go of its channels after a while unused, and this
     * model can outlive that (a saved back stack entry), so it loads them again when needed.
     */
    fun ensureLoaded() {
        if (repository.ensureLoaded()) {
            filteredFor = null
            visibleChannels = emptyList()
        }
    }

    var visibleChannels by mutableStateOf<List<LiveTvChannel>>(emptyList())
        private set
    private var filteredFor: LiveTvFilterInput? = null

    fun isFilteredFor(input: LiveTvFilterInput): Boolean = filteredFor?.sameAs(input) == true

    fun setVisible(input: LiveTvFilterInput, channels: List<LiveTvChannel>) {
        filteredFor = input
        visibleChannels = channels
    }

    /** Set when a channel starts playing: on return, focus goes back to the channel last watched. */
    var restoreFocusOnReturn = false

    /**
     * The official player route for [channel]: its playable link (Stalker links are made per play),
     * as Stremio type `channel` with no content id, so the player treats it as live and saves no
     * progress (official `LivePlaybackUiPolicy`). The list entry is remembered as the last channel.
     */
    suspend fun playerRoute(channel: LiveTvChannel): String {
        val playback = repository.playableChannel(channel)
        repository.recordRecentChannel(channel)
        val state = repository.state.value
        val group = LiveTvOrganisation.customName(channel.group, state.library.groupNames) ?: channel.group
        return Screen.Player.createRoute(
            streamUrl = playback.streamUrl,
            title = channel.name,
            streamName = channel.name,
            headers = playback.headers,
            contentType = LIVE_TV_CONTENT_TYPE,
            logo = channel.logoUrl,
            addonName = LIVE_TV_ADDON_NAME,
            streamDescription = group.takeIf(String::isNotBlank),
            profileId = profileManager.activeProfileId.value,
        )
    }

    companion object {
        const val LIVE_TV_CONTENT_TYPE = "channel"
        const val LIVE_TV_ADDON_NAME = "Live TV"
    }
}

/** What the visible list was filtered from; lists are compared by identity, so this is cheap. */
class LiveTvFilterInput(
    val channels: List<LiveTvChannel>,
    val library: LiveTvLibrary,
    val filterKey: String,
    val query: String,
) {
    fun sameAs(other: LiveTvFilterInput): Boolean =
        channels === other.channels && library.favorites === other.library.favorites &&
            library.hiddenGroups === other.library.hiddenGroups && library.hiddenChannels === other.library.hiddenChannels &&
            filterKey == other.filterKey && query == other.query
}
