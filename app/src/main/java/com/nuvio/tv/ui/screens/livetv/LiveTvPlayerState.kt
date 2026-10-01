package com.nuvio.tv.ui.screens.livetv

import android.content.Context
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.domain.model.ProxyHeaders
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamBehaviorHints
import com.nuvio.tv.fork.livetv.LiveTvChannel
import com.nuvio.tv.fork.livetv.LiveTvOrganisation
import com.nuvio.tv.fork.livetv.LiveTvPlaybackRegistry
import com.nuvio.tv.fork.livetv.LiveTvRepository
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerMediaSourceFactory
import com.nuvio.tv.ui.screens.player.PlayerRuntimeController
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.onEvent
import com.nuvio.tv.ui.screens.player.switchToSourceStream
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The player screen reaches the Live TV repository through Hilt without a new view model. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface LiveTvPlayerEntryPoint {
    fun liveTvRepository(): LiveTvRepository
}

/**
 * Live TV inside Nuvio's player (G10e, features 228–232). FILE_PORT of Reshaped
 * `LiveTvPlayerOverlay.LiveTvPlayerState` @ 0ccf049, keyed by channel key: CH+/CH− (and ▲▼ while
 * the controls are hidden) switch channel inside the list it was picked from, ◀ opens the channel
 * list and ◀ again its categories, ▶ the player's controls, OK the Now/Next card, and a banner shows
 * what is on after each switch. Everything is inert unless the player shows a Live TV channel.
 */
@Stable
internal class LiveTvPlayerState(
    private val controller: PlayerRuntimeController,
    private val containerFocusRequester: FocusRequester,
    private val scope: CoroutineScope,
    val repository: LiveTvRepository,
) {
    var panelOpen by mutableStateOf(false)
        private set
    /** The categories column beside the channel list (◀ from the list). */
    var foldersOpen by mutableStateOf(false)
        private set
    /** The category the panel lists, or null for the list being zapped. */
    var panelFolderKey by mutableStateOf<String?>(null)
        private set
    /** The channels the panel lists. */
    var panelChannels by mutableStateOf<List<LiveTvChannel>>(emptyList())
        private set
    private var folderJob: Job? = null
    /** The list entry of the channel playing now (a Stalker link differs from its list link). */
    var currentKey by mutableStateOf<Long?>(null)
        private set
    /** Bumped on each switch so the banner shows again. */
    var bannerKey by mutableIntStateOf(0)
        private set
    private var switchJob: Job? = null

    /** The Now/Next card OK shows over the picture; OK again opens the controls. Never pauses. */
    var infoOpen by mutableStateOf(false)
        private set
    private var infoJob: Job? = null
    /** The release of an OK press Live TV acted on, which must not reach the controls it opened. */
    private var swallowOkRelease = false

    /** The category the panel's list is, so the categories open on it; null for a search. */
    var zappedFolderKey by mutableStateOf<String?>(null)
        private set

    /** The player, for the details the Now/Next card shows. */
    internal val player: PlayerRuntimeController get() = controller

    /** Whether the player is showing a Live TV channel (read on each key; a memory lookup). */
    fun isActive(): Boolean = LiveTvPlaybackRegistry.isLiveTv(controller.currentStreamUrl)

    internal fun syncCurrent() {
        if (currentKey == null && isActive()) {
            currentKey = LiveTvPlaybackRegistry.channelKeyFor(controller.currentStreamUrl)
            bannerKey++
        }
    }

    /** The list zapping moves through: the one the channel was picked from, else every shown channel. */
    internal fun zapList(): List<LiveTvChannel> = repository.zapTarget(currentKey).first

    private fun showInfo() {
        infoOpen = true
        infoJob?.cancel()
        infoJob = scope.launch {
            delay(INFO_MS)
            infoOpen = false
        }
    }

    internal fun hideInfo() {
        infoJob?.cancel()
        infoJob = null
        infoOpen = false
    }

    /** Called first by the player's key handler; true when the key was Live TV's. */
    fun onPreviewKey(event: KeyEvent, uiState: PlayerUiState): Boolean {
        if (!isActive()) return false
        val nuvioOverlayOpen = uiState.showEpisodesPanel || uiState.showSourcesPanel ||
            uiState.showAudioOverlay || uiState.showSubtitleOverlay || uiState.showSubtitleStylePanel ||
            uiState.showSpeedDialog || uiState.showSubtitleDelayOverlay || uiState.showSubtitleTimingDialog ||
            uiState.showMoreDialog || uiState.showStreamInfoOverlay
        val down = event.action == KeyEvent.ACTION_DOWN
        if (panelOpen && foldersOpen) {
            return when (event.keyCode) {
                // Back to the channels, which show the category last focused.
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (!down) foldersOpen = false
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN -> true
                else -> false // the column handles the rest
            }
        }
        if (panelOpen) {
            return when (event.keyCode) {
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    if (!down) closePanel()
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (down && event.repeatCount == 0) foldersOpen = true
                    true
                }
                // Nothing lies to the right of the list; the arrow must not seek the channel behind it.
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN -> true
                else -> false // the list handles the rest
            }
        }
        when (event.keyCode) {
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                if (nuvioOverlayOpen && uiState.error == null) return false
                if (down) zap(if (event.keyCode == KeyEvent.KEYCODE_CHANNEL_UP) -1 else 1)
                return true
            }
        }
        if (event.keyCode in OK_KEYS && !down && swallowOkRelease) {
            swallowOkRelease = false
            return true
        }
        // ▲▼◀ and OK are Live TV's only on the bare picture: never over controls, panels or errors.
        if (uiState.showControls || nuvioOverlayOpen || uiState.error != null || uiState.showPauseOverlay) {
            if (infoOpen) hideInfo()
            return false
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                // Acts on the press; the release is swallowed too, so the player never sees OK (which pauses).
                if (down && event.repeatCount == 0) {
                    swallowOkRelease = true
                    if (infoOpen) {
                        hideInfo()
                        controller.onEvent(PlayerEvent.OnToggleControls)
                    } else {
                        showInfo()
                    }
                }
                true
            }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                if (!infoOpen) return false
                if (!down) hideInfo()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (down) zap(if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) -1 else 1)
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (down && event.repeatCount == 0) openPanel()
                true // also swallows the release, which would commit a seek
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // A live channel has nothing to seek to: ▶ opens the controls (audio, subtitles).
                if (down && event.repeatCount == 0) {
                    hideInfo()
                    controller.onEvent(PlayerEvent.OnToggleControls)
                }
                true
            }
            else -> false
        }
    }

    private fun zap(step: Int) {
        hideInfo()
        val next = LiveTvOrganisation.neighbour(zapList(), currentKey, step) ?: return
        switchTo(next)
    }

    private fun openPanel() {
        hideInfo()
        folderJob?.cancel()
        panelFolderKey = null
        val (channels, folderKey) = repository.zapTarget(currentKey)
        panelChannels = channels
        zappedFolderKey = folderKey
        foldersOpen = false
        panelOpen = true
    }

    /** Shows a category's channels in the panel; zapping follows once one of them is picked. */
    internal fun showFolder(key: String) {
        if (key == panelFolderKey) return
        panelFolderKey = key
        folderJob?.cancel()
        folderJob = scope.launch {
            val state = repository.state.value
            panelChannels = withContext(Dispatchers.Default) { LiveTvOrganisation.filter(state.channels, state.library, key) }
        }
    }

    /** A channel picked from the panel: zapping then stays in the list it was picked from. */
    internal fun pickFromPanel(channel: LiveTvChannel) {
        panelFolderKey?.let { repository.setZapList(panelChannels, it) }
        switchTo(channel)
    }

    internal fun switchTo(channel: LiveTvChannel) {
        if (channel.key == currentKey) {
            closePanel()
            return
        }
        currentKey = channel.key
        bannerKey++
        closePanel()
        // The loading and pause screens show the player's logo: the new channel's, from the first press.
        controller._uiState.update { it.copy(title = channel.name, logo = repository.state.value.logoFor(channel)) }
        // Quick presses land on the last channel only.
        switchJob?.cancel()
        switchJob = scope.launch {
            delay(ZAP_SETTLE_MS)
            try {
                val playback = repository.playableChannel(channel)
                LiveTvPlaybackRegistry.register(playback.streamUrl, channel.key)
                LiveTvPlaybackRegistry.register(
                    PlayerMediaSourceFactory.normalizePlaybackRequest(playback.streamUrl, playback.headers).url,
                    channel.key,
                )
                repository.recordRecentChannel(channel)
                controller.switchToSourceStream(channel.toStream(playback))
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
            }
        }
    }

    internal fun closePanel() {
        if (!panelOpen) return
        panelOpen = false
        foldersOpen = false
        runCatching { containerFocusRequester.requestFocus() }
    }

    private fun LiveTvChannel.toStream(playback: LiveTvChannel) = Stream(
        name = name,
        title = name,
        description = group.takeIf(String::isNotBlank),
        url = playback.streamUrl,
        ytId = null,
        infoHash = null,
        fileIdx = null,
        externalUrl = null,
        behaviorHints = StreamBehaviorHints(
            notWebReady = null,
            bingeGroup = null,
            countryWhitelist = null,
            proxyHeaders = ProxyHeaders(request = playback.headers, response = null),
        ),
        addonName = LiveTvViewModel.LIVE_TV_ADDON_NAME,
        addonLogo = null,
    )

    private companion object {
        const val ZAP_SETTLE_MS = 350L
        const val INFO_MS = 6_000L
        val OK_KEYS = intArrayOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)
    }
}

/** The Live TV state of a player screen; its keys and overlay do nothing unless a channel is playing. */
@Composable
internal fun rememberLiveTvPlayer(controller: PlayerRuntimeController, containerFocusRequester: FocusRequester): LiveTvPlayerState {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    return remember(controller) {
        LiveTvPlayerState(controller, containerFocusRequester, scope, liveTvRepository(context))
    }
}

private fun liveTvRepository(context: Context): LiveTvRepository =
    EntryPointAccessors.fromApplication(context.applicationContext, LiveTvPlayerEntryPoint::class.java).liveTvRepository()
