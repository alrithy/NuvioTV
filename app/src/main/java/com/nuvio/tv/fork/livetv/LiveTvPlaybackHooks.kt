package com.nuvio.tv.fork.livetv

import android.media.MediaFormat
import android.os.Handler
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory

/**
 * The media3 side of the live-only rules (G10d; Reshaped `LiveTvLoadErrors`, `LiveTvTsFlags`,
 * `LiveEdgeRecovery`, `LiveTvFrameRate` @ 0ccf049). Each hook answers "official" for a URL Live TV
 * did not hand to the player ([LiveTvPlaybackRegistry]), so VOD and add-on streams are untouched.
 */
@OptIn(UnstableApi::class)
object LiveTvPlaybackHooks {

    private val retryPolicy = object : DefaultLoadErrorHandlingPolicy(
        DefaultLoadErrorHandlingPolicy.DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE,
    ) {
        override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
            val code = loadErrorInfo.exception.httpResponseCode() ?: return super.getRetryDelayMsFor(loadErrorInfo)
            return LiveTvPlaybackRules.retryDelayMs(code, loadErrorInfo.errorCount) { null } ?: C.TIME_UNSET
        }
    }

    /** Live TV's load retries for a Live TV [url] (feature 234), else [fallback]. */
    fun loadErrorPolicy(url: String, fallback: LoadErrorHandlingPolicy): LoadErrorHandlingPolicy =
        if (LiveTvPlaybackRegistry.isLiveTv(url)) retryPolicy else fallback

    /**
     * MPEG-TS reader flags to add for [url] (feature 235). Many broadcast restreams send H.264 with
     * IDR frames only every several seconds, or none at all; Media3 then waits for one, so the channel
     * starts late or times out. With this flag it starts on the first I-frame, as IPTV players and
     * Media3's own HLS reader do. Files keep Nuvio's flags.
     */
    fun extraTsFlags(url: String?): Int =
        if (LiveTvPlaybackRegistry.isLiveTv(url)) DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES else 0

    /** A live channel is never written to the VOD disk cache (it has no end). */
    fun allowsDiskCache(url: String?): Boolean = !LiveTvPlaybackRegistry.isLiveTv(url)

    private val rejoins = LiveTvPlaybackRules.LiveEdgeRejoins()

    /**
     * A paused or stalled live channel falls out of its playlist window (BEHIND_LIVE_WINDOW); the
     * player then only needs to jump back to the live edge. A few rejoins per minute at most, so a
     * channel that keeps failing still reaches Nuvio's own recovery. True when handled.
     */
    fun tryRejoinLiveEdge(url: String?, error: PlaybackException, player: Player?): Boolean {
        if (player == null || error.errorCode != PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) return false
        if (!LiveTvPlaybackRegistry.isLiveTv(url) || !rejoins.tryAcquire(SystemClock.elapsedRealtime())) return false
        player.seekToDefaultPosition()
        player.prepare()
        return true
    }

    /**
     * Plain MPEG-TS channels (and HLS without a FRAME-RATE tag) carry no frame rate in their track
     * format. For those the rate is read from the presentation times of the first frames the player
     * renders (no extra connection, no decoding) and handed to [onMeasured] on the player's thread;
     * the listener removes itself once measured.
     */
    fun measureFrameRate(player: ExoPlayer, onMeasured: (Float) -> Unit) {
        player.setVideoFrameMetadataListener(FrameMeter(player, onMeasured))
    }

    private class FrameMeter(private val player: ExoPlayer, private val onMeasured: (Float) -> Unit) : VideoFrameMetadataListener {
        private val times = LongArray(LiveTvPlaybackRules.FRAME_SAMPLES)
        private var count = 0

        // Called on the playback thread for each frame about to be shown.
        override fun onVideoFrameAboutToBeRendered(
            presentationTimeUs: Long,
            releaseTimeNs: Long,
            format: Format,
            mediaFormat: MediaFormat?,
        ) {
            if (count >= times.size) return
            times[count++] = presentationTimeUs
            if (count < times.size) return
            val fps = LiveTvPlaybackRules.frameRateFromTimes(times)
            Handler(player.applicationLooper).post {
                runCatching { player.clearVideoFrameMetadataListener(this) }
                if (fps > 0f) onMeasured(fps)
            }
        }
    }

    private fun Throwable.httpResponseCode(): Int? {
        var current: Throwable? = this
        while (current != null) {
            if (current is HttpDataSource.InvalidResponseCodeException) return current.responseCode
            current = current.cause
        }
        return null
    }
}
