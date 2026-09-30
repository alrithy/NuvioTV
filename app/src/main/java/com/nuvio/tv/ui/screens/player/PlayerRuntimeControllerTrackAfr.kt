package com.nuvio.tv.ui.screens.player

import android.os.Build
import android.util.Log
import com.nuvio.tv.core.player.FrameRateUtils
import com.nuvio.tv.data.local.FrameRateMatchingMode
import com.nuvio.tv.data.local.InternalPlayerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** G5e (feature 59): whole track-AFR step (switch wait + settle); timer-released, never event-gated. */
internal const val TRACK_AFR_ABSOLUTE_DEADLINE_MS = 8_000L

/** G5e (feature 59): hold after a real display-mode switch so A/V does not start inside the HDMI transition. */
internal const val TRACK_AFR_SETTLE_HOLD_MS = 2_000L

/**
 * G5e (feature 58): ExoPlayer frame-rate matching from the track's reported frame rate when the
 * official preflight probe found none (moov-at-end MP4, probe timeout, unreadable host). ALGORITHM_PORT
 * of ysosrs `maybeRunTrackFormatAfr` @ 45e0984, reduced to a fallback: a preflight detection always
 * wins, and the official probing preflight is kept (ysosrs replaced it with a cache-only one).
 *
 * The switch runs before the first frame: playback start is held ([afrTrackSwitchInFlight], checked
 * by the start sites in initializePlayer) for the switch plus [TRACK_AFR_SETTLE_HOLD_MS], and released
 * by [TRACK_AFR_ABSOLUTE_DEADLINE_MS] at the latest. The detection is cached so the next play of the
 * same title takes the official preflight hit path (switch before prepare).
 */
internal fun PlayerRuntimeController.maybeRunTrackFormatAfr(rawFps: Float, width: Int?, height: Int?) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val state = _uiState.value
    val player = _exoPlayer
    val action = com.nuvio.tv.fork.video.TrackAfrPolicy.decide(
        enabled = trackAfrEnabled,
        exoPlayerEngine = currentInternalPlayerEngine == InternalPlayerEngine.EXOPLAYER && player != null,
        afrOn = state.frameRateMatchingMode != FrameRateMatchingMode.OFF,
        alreadyAttempted = trackAfrAttemptedForStream,
        rawFps = rawFps,
        preflightDetected = state.detectedFrameRateSource == FrameRateSource.PROBE,
        preflightRunning = state.afrProbeRunning,
        playbackRunning = hasRenderedFirstFrame && player?.isPlaying == true,
        liveTv = isLiveTvPlayback, // Superfork G10d (236)
    )
    when (action) {
        com.nuvio.tv.fork.video.TrackAfrAction.SKIP -> return
        com.nuvio.tv.fork.video.TrackAfrAction.DEFER -> {
            // Decided when the preflight finishes (onAfrPreflightFinished).
            pendingTrackAfr = Triple(rawFps, width, height)
            return
        }
        else -> Unit
    }
    trackAfrAttemptedForStream = true
    pendingTrackAfr = null
    if (action == com.nuvio.tv.fork.video.TrackAfrAction.IGNORE_IMPLAUSIBLE) {
        Log.w(PlayerRuntimeController.TAG, "TRACK_AFR: ignoring implausible frame rate ${rawFps}fps")
        return
    }
    if (action == com.nuvio.tv.fork.video.TrackAfrAction.TOO_LATE) {
        Log.d(PlayerRuntimeController.TAG, "TRACK_AFR: playback already running, not switching (fps=$rawFps)")
        return
    }
    if (action == com.nuvio.tv.fork.video.TrackAfrAction.RUN_LIVE) {
        switchDisplayForLiveTv(rawFps, width, height)
        return
    }
    val activity = currentHostActivity() ?: return

    afrTrackSwitchInFlight = true
    // Engage the hold even if the first READY already primed playWhenReady (the frame is not yet
    // playing, checked above); the release re-applies the start.
    player?.playWhenReady = false
    val generation = afrTrackGeneration
    scope.launch {
        try {
            withTimeoutOrNull(TRACK_AFR_ABSOLUTE_DEADLINE_MS) {
                val settings = playerSettingsDataStore.playerSettings.first()
                val snapped = FrameRateUtils.snapToStandardRate(rawFps)
                val target = FrameRateUtils.refineFrameRateForDisplay(
                    activity = activity,
                    detectedFps = snapped,
                    prefer23976Near24 = rawFps in 23.95f..23.999f
                )
                FrameRateUtils.cacheFrameRate(
                    currentStreamUrl,
                    currentHeaders,
                    FrameRateUtils.FrameRateDetection(raw = rawFps, snapped = snapped, videoWidth = width, videoHeight = height),
                    currentFilename
                )
                val initialModeId = withContext(Dispatchers.Main) { activity.window?.decorView?.display?.mode?.modeId }
                Log.i(PlayerRuntimeController.TAG, "TRACK_AFR: raw=$rawFps snapped=$snapped target=$target size=${width}x$height")
                val result = FrameRateUtils.matchFrameRateAndWait(
                    activity = activity,
                    frameRate = target,
                    videoWidth = width,
                    videoHeight = height,
                    resolutionMatchingEnabled = settings.resolutionMatchingEnabled
                ) ?: return@withTimeoutOrNull
                _uiState.update {
                    it.copy(
                        detectedFrameRateRaw = rawFps,
                        detectedFrameRate = snapped,
                        detectedFrameRateSource = FrameRateSource.TRACK,
                        displayModeInfo = DisplayModeInfo(
                            width = result.appliedMode.physicalWidth,
                            height = result.appliedMode.physicalHeight,
                            refreshRate = result.appliedMode.refreshRate
                        ),
                        showDisplayModeInfo = true
                    )
                }
                if (initialModeId != null && initialModeId != result.appliedMode.modeId) {
                    Log.i(PlayerRuntimeController.TAG, "TRACK_AFR: display switched; holding start ${TRACK_AFR_SETTLE_HOLD_MS}ms")
                    delay(TRACK_AFR_SETTLE_HOLD_MS)
                }
            } ?: Log.w(PlayerRuntimeController.TAG, "TRACK_AFR: deadline ${TRACK_AFR_ABSOLUTE_DEADLINE_MS}ms reached; releasing start")
        } finally {
            // Only the current stream's attempt may release the hold.
            if (afrTrackGeneration == generation) {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) {
                    afrTrackSwitchInFlight = false
                    resumePlaybackAfterTrackAfrHold()
                }
            }
        }
    }
}

/** Called when the official preflight ends; runs a track-AFR decision it deferred. */
internal fun PlayerRuntimeController.onAfrPreflightFinished() {
    val pending = pendingTrackAfr ?: return
    pendingTrackAfr = null
    maybeRunTrackFormatAfr(pending.first, pending.second, pending.third)
}

/** Per-stream reset; also invalidates an in-flight attempt of the previous stream. */
internal fun PlayerRuntimeController.resetTrackAfrForNewStream() {
    afrTrackGeneration++
    trackAfrAttemptedForStream = false
    pendingTrackAfr = null
    afrTrackSwitchInFlight = false
}

/**
 * Re-applies the start the hold suppressed: the start sites stood down while [afrTrackSwitchInFlight],
 * so start now unless the user paused, the playback was opened paused, or the player is going away.
 */
private fun PlayerRuntimeController.resumePlaybackAfterTrackAfrHold() {
    val player = _exoPlayer ?: return
    if (userPausedManually || startPausedForCurrentPlayback || isReleasingPlayer || player.playWhenReady) return
    if (player.playbackState != androidx.media3.common.Player.STATE_READY) return
    Log.d(PlayerRuntimeController.TAG, "TRACK_AFR: hold released; starting playback")
    player.playWhenReady = true
    player.play()
}
