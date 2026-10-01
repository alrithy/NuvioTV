package com.nuvio.tv.ui.screens.player

import android.util.Log
import androidx.media3.common.Format
import com.nuvio.tv.core.player.FrameRateUtils
import com.nuvio.tv.data.local.FrameRateMatchingMode
import com.nuvio.tv.fork.livetv.LiveTvPlaybackHooks
import com.nuvio.tv.fork.livetv.LiveTvPlaybackRegistry
import java.util.WeakHashMap
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * G10d (features 234–236, D055): the player side of Live TV. Every function returns at once for a
 * stream Live TV did not hand to the player, so add-on and VOD playback keep official behavior.
 * Reshaped `LiveTvFrameRate` @ 0ccf049, adapted onto the G5e track-AFR owner.
 */

/** Whether the player is showing a Live TV channel. */
internal val PlayerRuntimeController.isLiveTvPlayback: Boolean
    get() = LiveTvPlaybackRegistry.isLiveTv(currentStreamUrl)

/**
 * The official AFR preflight downloads the start of the stream over extra connections before
 * playback (up to 18 s). A live channel has no start, and many IPTV accounts allow one connection,
 * so the probe could get the channel itself refused. Live TV skips it (ExoPlayer only) and the G5e
 * track-AFR fallback matches from the track, or from the measured rate. True when skipped.
 */
internal fun PlayerRuntimeController.skipAfrPreflightForLiveTv(url: String): Boolean {
    if (isUsingMpvEngine() || !LiveTvPlaybackRegistry.isLiveTv(url)) return false
    _uiState.update {
        it.copy(detectedFrameRateRaw = 0f, detectedFrameRate = 0f, detectedFrameRateSource = null, afrProbeRunning = false)
    }
    return true
}

/** The track-AFR generation each player screen last started a frame meter for (weak: a closed screen is not kept). */
private val liveTvFrameMeters = WeakHashMap<PlayerRuntimeController, Int>()

/**
 * A Live TV channel whose video track reports no frame rate (plain MPEG-TS): measure it from the
 * first rendered frames, once per stream, and hand it to the G5e track-AFR decision.
 */
internal fun PlayerRuntimeController.maybeMeasureLiveTvFrameRate(format: Format) {
    if (!isLiveTvPlayback || isUsingMpvEngine() || trackAfrAttemptedForStream) return
    if (_uiState.value.frameRateMatchingMode == FrameRateMatchingMode.OFF) return
    val player = _exoPlayer ?: return
    val generation = afrTrackGeneration
    synchronized(liveTvFrameMeters) {
        if (liveTvFrameMeters.put(this, generation) == generation) return
    }
    val url = currentStreamUrl
    val width = format.width.takeIf { it > 0 }
    val height = format.height.takeIf { it > 0 }
    LiveTvPlaybackHooks.measureFrameRate(player) { fps ->
        // A channel switched meanwhile: its own measurement replaces this one.
        if (afrTrackGeneration != generation || currentStreamUrl != url || _exoPlayer !== player) return@measureFrameRate
        Log.i(PlayerRuntimeController.TAG, "LIVE_TV_AFR: measured ${fps}fps")
        maybeRunTrackFormatAfr(fps, width, height)
    }
}

/**
 * G5e's RUN_LIVE: a Live TV channel already playing switches the display once, without the start
 * hold (a live stream has nothing to hold), as IPTV players do. Not cached: the rate belongs to what
 * is on the channel now.
 */
internal fun PlayerRuntimeController.switchDisplayForLiveTv(rawFps: Float, width: Int?, height: Int?) {
    val activity = currentHostActivity() ?: return
    val generation = afrTrackGeneration
    scope.launch {
        val settings = playerSettingsDataStore.playerSettings.first()
        val snapped = FrameRateUtils.snapToStandardRate(rawFps)
        val target = FrameRateUtils.refineFrameRateForDisplay(
            activity = activity,
            detectedFps = snapped,
            prefer23976Near24 = rawFps in 23.95f..23.999f,
        )
        if (afrTrackGeneration != generation) return@launch
        val result = FrameRateUtils.matchFrameRateAndWait(
            activity = activity,
            frameRate = target,
            videoWidth = width,
            videoHeight = height,
            resolutionMatchingEnabled = settings.resolutionMatchingEnabled,
        ) ?: return@launch
        _uiState.update {
            it.copy(
                detectedFrameRateRaw = rawFps,
                detectedFrameRate = snapped,
                detectedFrameRateSource = FrameRateSource.TRACK,
                displayModeInfo = DisplayModeInfo(
                    width = result.appliedMode.physicalWidth,
                    height = result.appliedMode.physicalHeight,
                    refreshRate = result.appliedMode.refreshRate,
                ),
                showDisplayModeInfo = true,
            )
        }
    }
}
