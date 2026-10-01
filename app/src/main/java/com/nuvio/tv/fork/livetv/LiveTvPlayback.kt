package com.nuvio.tv.fork.livetv

/*
 * Live-only playback rules (G10d, features 233–236; D055). FILE_PORT of Reshaped
 * `LiveTvPlaybackRegistry`, `LiveTvLoadErrors`, `LiveEdgeRecovery` and `LiveTvFrameRate` @ 0ccf049,
 * split so the decisions are plain functions; the media3 glue is in `LiveTvPlaybackHooks`. Every
 * rule applies only to URLs Live TV handed to the player, so add-on and VOD streams keep official
 * behavior exactly.
 */

/**
 * Stream URLs Live TV has sent to the player, so the fork's playback extras (disk cache, speed
 * learning, AFR probing, AutoSync, seek previews) leave live channels alone, and the player overlay
 * knows it is showing a channel. Also remembers which list entry each came from (a Stalker link is
 * made per play). Memory only: the links carry account details and are never persisted or logged.
 */
object LiveTvPlaybackRegistry {
    private const val MAX_URLS = 16

    /** Playback URL to the list entry's channel key, most recent last. */
    @Volatile private var entries: List<Pair<String, Long>> = emptyList()

    @Synchronized
    fun register(playbackUrl: String, channelKey: Long) {
        if (playbackUrl.isBlank()) return
        entries = (entries.filterNot { it.first == playbackUrl } + (playbackUrl to channelKey)).takeLast(MAX_URLS)
    }

    fun isLiveTv(url: String?): Boolean = url != null && entries.any { it.first == url }

    /** The list entry's key for a URL the player is playing, or null when it is not a Live TV channel. */
    fun channelKeyFor(playbackUrl: String?): Long? =
        playbackUrl?.let { url -> entries.lastOrNull { it.first == url }?.second }

    @Synchronized
    internal fun clearForTest() {
        entries = emptyList()
    }
}

internal object LiveTvPlaybackRules {
    /** HTTP refusals retried this many times (0.7 s, 1.4 s, 2.1 s) before the error shows. */
    const val HTTP_RETRIES = 3
    const val HTTP_RETRY_STEP_MS = 700L

    /**
     * An IPTV server often refuses a channel only for a moment (the channel just left still counts
     * against a one-connection account, or a load balancer is busy), where Nuvio fails a file at once
     * on 400/401/403/404/410. Returns the delay before the next try, or null to give up (Media3's
     * `C.TIME_UNSET`); other errors keep Media3's default ([fallback]).
     */
    fun retryDelayMs(httpResponseCode: Int?, errorCount: Int, fallback: () -> Long?): Long? =
        if (httpResponseCode != null) {
            if (errorCount <= HTTP_RETRIES) HTTP_RETRY_STEP_MS * errorCount else null
        } else {
            fallback()
        }

    /** Rejoining the live edge after BEHIND_LIVE_WINDOW: a few times per minute at most. */
    class LiveEdgeRejoins(private val maxRejoins: Int = 3, private val windowMs: Long = 60_000L) {
        private var windowStartMs = Long.MIN_VALUE / 2
        private var rejoins = 0

        /** True when another rejoin is allowed at [nowMs]; a stream that keeps failing reaches the official handling. */
        @Synchronized
        fun tryAcquire(nowMs: Long): Boolean {
            if (nowMs - windowStartMs > windowMs) {
                windowStartMs = nowMs
                rejoins = 0
            }
            if (rejoins >= maxRejoins) return false
            rejoins++
            return true
        }
    }

    /** About a second of video: enough for a steady median, short enough to match early. */
    const val FRAME_SAMPLES = 48

    /**
     * The frame rate from presentation times of frames about to be shown: the median gap, so a
     * dropped frame or a timestamp jump doesn't move it. 0 when it is not a plausible rate.
     */
    fun frameRateFromTimes(timesUs: LongArray): Float {
        val gaps = (1 until timesUs.size).map { timesUs[it] - timesUs[it - 1] }.filter { it > 0 }.sorted()
        if (gaps.size < timesUs.size / 2) return 0f
        val fps = 1_000_000f / gaps[gaps.size / 2]
        return if (fps in 10f..121f) fps else 0f
    }
}
