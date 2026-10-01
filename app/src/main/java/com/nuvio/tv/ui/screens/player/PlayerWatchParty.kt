package com.nuvio.tv.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.fork.livetv.LiveTvPlaybackRegistry
import com.nuvio.tv.fork.watchparty.WatchPartyMedia
import com.nuvio.tv.fork.watchparty.WatchPartyPlayer
import com.nuvio.tv.fork.watchparty.WatchPartyReceivedStreams
import com.nuvio.tv.fork.watchparty.WatchPartyRole
import com.nuvio.tv.fork.watchparty.WatchPartySession
import com.nuvio.tv.fork.watchparty.WatchPartyShareDecision
import com.nuvio.tv.fork.watchparty.WatchPartySharePolicy
import com.nuvio.tv.fork.watchparty.WatchPartyState
import com.nuvio.tv.fork.watchparty.WatchPartyStatus
import com.nuvio.tv.fork.watchparty.WatchPartyUnshareable
import com.nuvio.tv.ui.screens.watchparty.formatWatchPartyCode
import com.nuvio.tv.ui.screens.watchparty.rememberWatchPartySession
import com.nuvio.tv.ui.screens.watchparty.watchPartyStatusText
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.delay

/*
 * Watch Party in the player (G11b, D056; features 237–243). FILE_PORT of AntoninoScardina/NuvioTV
 * `ui/screens/player/PlayerWatchParty.kt` @ ff597b1. Adapted: what may be shared comes from
 * WatchPartySharePolicy (allow-listed headers; credential, torrent, local and Live TV streams are
 * not shareable, with the reason shown); creating a room is a two-step consent that names what the
 * people who join receive. After review (G11 closeout): remote play / pause does the same
 * bookkeeping as the play / pause button; speed changes are relative to the viewer's own speed; a
 * stream that becomes unshareable stops being shared; a guest is told when the host's stream does not
 * open here.
 */

/** Nuvio's player (ExoPlayer or mpv), as the Watch Party sees it. */
internal class ControllerWatchPartyPlayer(
    private val controller: PlayerRuntimeController,
) : WatchPartyPlayer {
    override val positionMs: Long
        get() = controller.currentPlaybackPositionMs() ?: 0L

    override val isPlaying: Boolean
        get() = controller.hasActivePlayIntent()

    override val isBuffering: Boolean
        get() = controller.uiState.value.let { it.isBuffering || it.showLoadingOverlay }

    override val selectedSpeed: Float
        get() = controller.uiState.value.playbackSpeed

    override fun play() = setPaused(false)

    override fun pause() = setPaused(true)

    override fun seekTo(positionMs: Long) {
        controller.seekPlaybackTo(positionMs.coerceAtLeast(0L))
    }

    override fun setPlaybackSpeed(speed: Float) {
        controller.setPlaybackSpeedInternal(speed)
    }

    /** The play / pause button's bookkeeping (PlayerEvent.OnPlayPause), without showing the controls. */
    private fun setPaused(paused: Boolean) {
        if (controller.hasActivePlayIntent() == !paused) return
        controller.userPausedManually = paused
        if (paused) {
            controller.setPlaybackPaused(true)
            if (controller.isUsingMpvEngine()) {
                controller.stopProgressUpdates()
                controller.stopWatchProgressSaving()
                controller.emitPauseScrobbleForCurrentProgress()
            }
            controller.schedulePauseOverlay()
        } else {
            controller.cancelPauseOverlay()
            controller.setPlaybackPaused(false)
            if (controller.isUsingMpvEngine()) {
                controller.startProgressUpdates()
                controller.startWatchProgressSaving()
                controller.emitScrobbleStart()
            }
        }
    }
}

/** What the open stream would share, or why it cannot be (D056). */
internal sealed interface WatchPartyShareable {
    data class Yes(val media: WatchPartyMedia) : WatchPartyShareable
    data class No(val reason: WatchPartyUnshareable) : WatchPartyShareable
}

internal fun PlayerRuntimeController.watchPartyShareable(): WatchPartyShareable {
    val url = currentStreamUrl
    return when (
        val decision = WatchPartySharePolicy.decide(url, currentHeaders, isTorrentStream, LiveTvPlaybackRegistry.isLiveTv(url))
    ) {
        is WatchPartyShareDecision.NotShareable -> WatchPartyShareable.No(decision.reason)
        is WatchPartyShareDecision.Shareable -> WatchPartyShareable.Yes(
            WatchPartyMedia(
                url = url,
                headers = decision.headers,
                title = contentName ?: title,
                subtitle = currentEpisodeTitle,
                contentId = contentId,
                contentType = contentType,
                videoId = currentVideoId,
                season = currentSeason,
                episode = currentEpisode,
                poster = poster,
                backdrop = backdrop,
                logo = logo,
                streamName = streamName,
            ),
        )
    }
}

/**
 * Everything Watch Party draws over the player, as one hook in PlayerScreen: it ties the open
 * player to the room, shows the room badge with the controls and the panel the button opens.
 * Nothing at all while WATCH_PARTY is OFF.
 */
@Composable
internal fun BoxScope.WatchPartyPlayerLayer(
    viewModel: PlayerViewModel,
    showControls: Boolean,
    containerFocusRequester: FocusRequester,
) {
    val session = rememberWatchPartySession() ?: return
    val controller = viewModel.controller
    val adapter = remember(controller) { ControllerWatchPartyPlayer(controller) }
    val panelOpen by viewModel.watchPartyPanelOpen.collectAsStateWithLifecycle()

    // Ties the player to the room once it plays, again whenever the stream changes, and lets go of
    // it when the open stream cannot be shared (so the room never shares an earlier link). Live and
    // other streams without a known duration count as playing once loaded.
    LaunchedEffect(adapter) {
        var attachedUrl: String? = null
        while (true) {
            val ui = controller.uiState.value
            if (!ui.showLoadingOverlay) {
                when (val shareable = controller.watchPartyShareable()) {
                    is WatchPartyShareable.Yes -> if (ui.error == null && shareable.media.url != attachedUrl) {
                        session.attachPlayer(adapter, shareable.media)
                        attachedUrl = shareable.media.url
                    }
                    is WatchPartyShareable.No -> if (attachedUrl != null) {
                        session.detachPlayer(adapter)
                        attachedUrl = null
                    }
                }
                // A guest whose player cannot open the host's link (for example one locked to the
                // host's connection) sees why instead of a silent room.
                if (ui.error != null && WatchPartyReceivedStreams.contains(controller.currentStreamUrl)) {
                    session.reportStreamUnsupported()
                }
            }
            delay(1_000)
        }
    }
    DisposableEffect(adapter) {
        onDispose { session.detachPlayer(adapter) }
    }

    if ((showControls || watchPartyNoticeShown(session)) && !panelOpen) {
        WatchPartyBadge(
            session = session,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 32.dp)
                .zIndex(2.2f),
        )
    }
    var panelWasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(panelOpen) {
        if (panelOpen) {
            panelWasOpen = true
        } else if (panelWasOpen) {
            panelWasOpen = false
            delay(250)
            runCatching { containerFocusRequester.requestFocus() }
        }
    }
    WatchPartyPlayerPanel(
        session = session,
        visible = panelOpen,
        shareable = if (panelOpen) controller.watchPartyShareable() else null,
        onDismiss = { viewModel.watchPartyPanelOpen.value = false },
        modifier = Modifier.fillMaxSize().zIndex(2.7f),
    )
}

/** Whether the controls show the Watch Party button: only while WATCH_PARTY is on. */
@Composable
internal fun watchPartyButtonVisible(): Boolean = rememberWatchPartySession() != null

@Composable
private fun WatchPartyPlayerPanel(
    session: WatchPartySession,
    visible: Boolean,
    shareable: WatchPartyShareable?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by session.state.collectAsStateWithLifecycle()
    val firstButton = remember { FocusRequester() }
    // Creating a room is two steps: the host first sees exactly what the people who join receive.
    var confirming by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (!visible) confirming = false }
    LaunchedEffect(visible, state.status, confirming) {
        if (visible) {
            delay(120)
            runCatching { firstButton.requestFocus() }
        }
    }

    PlayerOverlayScaffold(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = NuvioTheme.spacing.xxxl, vertical = 36.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(480.dp)
                .clip(RoundedCornerShape(NuvioTheme.radii.xl))
                .background(NuvioTheme.colors.BackgroundElevated)
                .padding(NuvioTheme.spacing.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)) {
                Text(
                    text = stringResource(R.string.watch_party_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = NuvioTheme.colors.TextPrimary,
                )
                val inRoom = state.isActive || state.status == WatchPartyStatus.ERROR
                when {
                    !inRoom && confirming && shareable is WatchPartyShareable.Yes -> ConsentStep(
                        firstButton = firstButton,
                        onAgree = {
                            confirming = false
                            // The player is already attached (the loop above); the room shares it from now on.
                            session.createRoom(sharingConsented = true)
                        },
                        onCancel = { confirming = false },
                    )
                    !inRoom -> StartStep(
                        shareable = shareable,
                        firstButton = firstButton,
                        onCreate = { confirming = true },
                        onDismiss = onDismiss,
                    )
                    else -> RoomStep(
                        state = state,
                        notSharing = state.role == WatchPartyRole.HOST && shareable is WatchPartyShareable.No,
                        firstButton = firstButton,
                        onDismiss = onDismiss,
                        onLeave = { session.leaveRoom() },
                    )
                }
            }
        }
    }
}

@Composable
private fun StartStep(shareable: WatchPartyShareable?, firstButton: FocusRequester, onCreate: () -> Unit, onDismiss: () -> Unit) {
    val reason = (shareable as? WatchPartyShareable.No)?.reason
    Text(
        text = stringResource(
            when (reason) {
                null -> R.string.watch_party_create_description
                WatchPartyUnshareable.TORRENT, WatchPartyUnshareable.LOCAL, WatchPartyUnshareable.NOT_HTTP -> R.string.watch_party_not_shareable
                WatchPartyUnshareable.LIVE_TV -> R.string.watch_party_not_shareable_live_tv
                WatchPartyUnshareable.CREDENTIALS -> R.string.watch_party_not_shareable_credentials
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = NuvioTheme.colors.TextSecondary,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
        DialogButton(
            text = stringResource(R.string.watch_party_create),
            onClick = onCreate,
            isPrimary = true,
            enabled = shareable is WatchPartyShareable.Yes,
            modifier = if (shareable is WatchPartyShareable.Yes) Modifier.focusRequester(firstButton) else Modifier,
        )
        DialogButton(
            text = stringResource(R.string.watch_party_close),
            onClick = onDismiss,
            isPrimary = false,
            modifier = if (shareable is WatchPartyShareable.Yes) Modifier else Modifier.focusRequester(firstButton),
        )
    }
}

@Composable
private fun ConsentStep(firstButton: FocusRequester, onAgree: () -> Unit, onCancel: () -> Unit) {
    Text(
        text = stringResource(R.string.watch_party_consent),
        style = MaterialTheme.typography.bodyMedium,
        color = NuvioTheme.colors.TextPrimary,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
        DialogButton(
            text = stringResource(R.string.watch_party_consent_agree),
            onClick = onAgree,
            isPrimary = true,
            modifier = Modifier.focusRequester(firstButton),
        )
        DialogButton(text = stringResource(R.string.watch_party_consent_cancel), onClick = onCancel, isPrimary = false)
    }
}

@Composable
private fun RoomStep(state: WatchPartyState, notSharing: Boolean, firstButton: FocusRequester, onDismiss: () -> Unit, onLeave: () -> Unit) {
    if (state.status != WatchPartyStatus.ERROR) {
        Text(
            text = stringResource(if (state.role == WatchPartyRole.HOST) R.string.watch_party_share_code else R.string.watch_party_joined_code),
            style = MaterialTheme.typography.bodyMedium,
            color = NuvioTheme.colors.TextSecondary,
        )
        Text(
            text = formatWatchPartyCode(state.code),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 44.sp,
            letterSpacing = 4.sp,
            color = NuvioTheme.colors.Primary,
        )
    }
    Text(
        text = watchPartyStatusText(state),
        style = MaterialTheme.typography.bodyMedium,
        color = NuvioTheme.colors.TextPrimary,
    )
    if (notSharing) {
        Text(
            text = stringResource(R.string.watch_party_host_not_sharing),
            style = MaterialTheme.typography.bodyMedium,
            color = NuvioTheme.colors.TextSecondary,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
        DialogButton(
            text = stringResource(R.string.watch_party_close),
            onClick = onDismiss,
            isPrimary = true,
            modifier = Modifier.focusRequester(firstButton),
        )
        DialogButton(text = stringResource(R.string.watch_party_leave), onClick = onLeave, isPrimary = false)
    }
}

/** Whether the guest's notice stays up without the controls: the host's stream did not open here. */
@Composable
private fun watchPartyNoticeShown(session: WatchPartySession): Boolean {
    val state by session.state.collectAsStateWithLifecycle()
    return state.isActive && state.streamUnsupported
}

/** A small label while in a room: the code and how many are watching, or the guest's notice. */
@Composable
private fun WatchPartyBadge(session: WatchPartySession, modifier: Modifier = Modifier) {
    val state by session.state.collectAsStateWithLifecycle()
    if (!state.isActive) return
    Text(
        text = if (state.streamUnsupported) {
            stringResource(R.string.watch_party_stream_unsupported)
        } else {
            stringResource(R.string.watch_party_badge, formatWatchPartyCode(state.code), state.participants.size + 1)
        },
        style = MaterialTheme.typography.labelMedium,
        color = NuvioTheme.colors.TextPrimary,
        modifier = modifier
            .clip(RoundedCornerShape(NuvioTheme.radii.md))
            .background(NuvioTheme.colors.BackgroundElevated.copy(alpha = 0.75f))
            .padding(horizontal = NuvioTheme.spacing.md, vertical = NuvioTheme.spacing.xs),
    )
}
