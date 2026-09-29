package com.nuvio.tv.ui.screens.player

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.nuvio.tv.R
import com.nuvio.tv.fork.recovery.DeadSourcePolicy
import com.nuvio.tv.fork.recovery.StartupWatchdogPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// G4a (features 19, 20): dead-source failover and the startup watchdog, ported from
// ysosrs 45e0984 (PlayerRuntimeControllerErrorRecovery / PlayerRuntimeControllerObservers).
// Everything here is inert while REMUX_PERFORMANCE is OFF (playbackRecoveryEnabled).

/** HTTP 404/410: permanent for this URL, no probe or same-URL retry can help. */
internal fun PlayerRuntimeController.isDeadSourceHttpError(error: PlaybackException): Boolean =
    DeadSourcePolicy.isDeadHttpStatus(error.findInvalidResponseCodeException()?.responseCode)

/**
 * Dead HTTP status, or a container sniff failure (3003 with UnrecognizedInputFormatException):
 * the body is not media at all (an HTML page behind HTTP 200, a .rar/.zip payload).
 */
internal fun PlayerRuntimeController.isDeadSourcePlaybackError(error: PlaybackException): Boolean {
    if (isDeadSourceHttpError(error)) return true
    if (error.errorCode != PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED) return false
    var cause: Throwable? = error
    while (cause != null) {
        if (cause is androidx.media3.exoplayer.source.UnrecognizedInputFormatException) return true
        cause = cause.cause
    }
    return false
}

/**
 * Marks the current URL dead for this player session and switches to the next live source in the
 * user's existing order, keeping the playback position. Bounded by
 * [DeadSourcePolicy.MAX_FAILOVERS]; returns false (caller shows the error) when disabled, capped
 * or out of sources.
 */
internal fun PlayerRuntimeController.advanceToNextLiveSource(detailedError: String): Boolean {
    if (!playbackRecoveryEnabled) return false
    deadSourceStreamUrls.add(currentStreamUrl)
    val state = _uiState.value
    val streams = state.sourceAllStreams
    val currentIdx = findCurrentStreamIndex(
        streams = streams,
        currentStreamInfoHash = state.currentStreamInfoHash,
        currentStreamFileIdx = state.currentStreamFileIdx,
        currentStreamAddonName = state.currentStreamAddonName,
        currentStreamUrl = state.currentStreamUrl,
        currentStreamName = state.currentStreamName
    )
    if (currentIdx >= 0) streams.getOrNull(currentIdx)?.getStreamUrl()?.let { deadSourceStreamUrls.add(it) }

    val nextIdx = DeadSourcePolicy.nextLiveIndex(
        candidateUrls = streams.map { it.getStreamUrl() },
        currentIndex = currentIdx,
        deadUrls = deadSourceStreamUrls,
        failoversSoFar = deadSourceFailoverCount
    )
    if (nextIdx == null) {
        Log.w(
            PlayerRuntimeController.TAG,
            "Dead-source failover unavailable (count=$deadSourceFailoverCount/${DeadSourcePolicy.MAX_FAILOVERS}, " +
                "sources=${streams.size}); surfacing error"
        )
        return false
    }
    val next = streams[nextIdx]
    deadSourceFailoverCount++
    val attemptNo = deadSourceFailoverCount
    Log.w(
        PlayerRuntimeController.TAG,
        "Dead source ($detailedError) - failing over to next source " +
            "($attemptNo/${DeadSourcePolicy.MAX_FAILOVERS}): host=${next.getStreamUrl()?.failoverHost()}"
    )
    val savedPosition = _exoPlayer?.currentPosition?.takeIf { it > 0L } ?: 0L
    errorRetryJob?.cancel()
    scope.launch {
        _uiState.update {
            it.copy(
                error = null,
                showPauseOverlay = false,
                showLoadingOverlay = it.loadingOverlayEnabled,
                loadingMessage = context.getString(
                    R.string.player_dead_source_failover,
                    attemptNo,
                    DeadSourcePolicy.MAX_FAILOVERS
                ),
                pendingSeekPosition = if (savedPosition > 0L) savedPosition else it.pendingSeekPosition
            )
        }
        switchToSourceStream(next)
    }
    return true
}

/** A startup failure that exhausted the official ladders treats the URL as dead (ysosrs Task 1.6). */
internal fun PlayerRuntimeController.attemptStartupExhaustedSourceFailover(detailedError: String): Boolean {
    if (!playbackRecoveryEnabled || hasRenderedFirstFrame) return false
    Log.w(PlayerRuntimeController.TAG, "Startup recovery exhausted; attempting next-source failover for: $detailedError")
    return advanceToNextLiveSource(detailedError)
}

internal fun PlayerRuntimeController.cancelStartupWatchdog() {
    startupWatchdogJob?.cancel()
    startupWatchdogJob = null
}

/**
 * Armed at `starting_stream` on the ExoPlayer path; see [StartupWatchdogPolicy]. It surfaces an
 * actionable error instead of an endless spinner and deliberately does not retry or stop the
 * player (a wedged vendor decoder just hangs again; a late first frame retracts the error).
 */
internal fun PlayerRuntimeController.scheduleStartupWatchdog() {
    cancelStartupWatchdog()
    if (!playbackRecoveryEnabled) return
    startupWatchdogJob = scope.launch {
        val armedAtMs = System.currentTimeMillis()
        var lastBufferedAheadMs = 0L
        while (isActive) {
            delay(StartupWatchdogPolicy.TIMEOUT_MS)
            if (hasRenderedFirstFrame) return@launch
            val livePlayer = _exoPlayer ?: return@launch
            val elapsedMs = System.currentTimeMillis() - armedAtMs
            val bufferedAheadMs = livePlayer.totalBufferedDuration.coerceAtLeast(0L)
            if (StartupWatchdogPolicy.verdict(elapsedMs, bufferedAheadMs, lastBufferedAheadMs) ==
                StartupWatchdogPolicy.Verdict.EXTEND
            ) {
                Log.w(
                    PlayerRuntimeController.TAG,
                    "STARTUP_WATCHDOG: no first frame ${elapsedMs}ms, buffered-ahead " +
                        "${lastBufferedAheadMs}ms -> ${bufferedAheadMs}ms; extending"
                )
                lastBufferedAheadMs = bufferedAheadMs
                continue
            }
            val reason = StartupWatchdogPolicy.reason(
                tracksScanned = hasScannedTextTracksOnce,
                hasVideoTrack = currentStreamHasVideoTrack,
                videoTrackSelected = currentVideoTrackSelected
            )
            Log.w(
                PlayerRuntimeController.TAG,
                "STARTUP_WATCHDOG: no first frame ${elapsedMs}ms (state=${livePlayer.playbackState.playbackStateName()} " +
                    "bufferedAheadMs=$bufferedAheadMs reason=${reason.key}); surfacing error"
            )
            val message = context.getString(reason.messageRes())
            _uiState.update { if (it.error == null) it.copy(error = message, showLoadingOverlay = false) else it }
            return@launch
        }
    }
}

/** A first frame after the watchdog fired disproves it: retract exactly the watchdog's error. */
internal fun PlayerRuntimeController.retractStartupWatchdogErrorAfterFirstFrame() {
    if (!playbackRecoveryEnabled) return
    val watchdogMessages = StartupWatchdogPolicy.Reason.entries.map { context.getString(it.messageRes()) }.toSet()
    var retracted = false
    _uiState.update {
        if (it.error != null && it.error in watchdogMessages) {
            retracted = true
            it.copy(error = null)
        } else {
            it
        }
    }
    if (retracted) Log.w(PlayerRuntimeController.TAG, "STARTUP_WATCHDOG: first frame after fire; error retracted")
}

private fun StartupWatchdogPolicy.Reason.messageRes(): Int = when (this) {
    StartupWatchdogPolicy.Reason.VIDEO_TRACK_UNSUPPORTED -> R.string.player_error_startup_video_track_unsupported
    StartupWatchdogPolicy.Reason.TRACKS_NOT_READ,
    StartupWatchdogPolicy.Reason.NO_VIDEO_TRACK -> R.string.player_error_startup_no_stream_data
    StartupWatchdogPolicy.Reason.DECODER_UNRESPONSIVE -> R.string.player_error_startup_timeout
}

/** Host only: never log full URLs (tokens, signed query strings). */
private fun String.failoverHost(): String = runCatching { java.net.URI(this).host }.getOrNull() ?: "unknown"

private fun Int.playbackStateName(): String = when (this) {
    Player.STATE_IDLE -> "IDLE"
    Player.STATE_BUFFERING -> "BUFFERING"
    Player.STATE_READY -> "READY"
    Player.STATE_ENDED -> "ENDED"
    else -> toString()
}
