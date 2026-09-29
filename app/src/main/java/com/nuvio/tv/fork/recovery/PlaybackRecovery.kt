package com.nuvio.tv.fork.recovery

/**
 * Dead-source failover decisions (G4a, feature 19). ALGORITHM_PORT of ysosrs
 * `PlayerRuntimeControllerErrorRecovery` @ 45e0984 (`isDeadSourceHttpError`,
 * `advanceToNextLiveSource`, MAX_DEAD_SOURCE_FAILOVERS).
 *
 * Dead means permanent for the URL: HTTP 404/410, or a body that is not media at all (the
 * container sniff failed and the official parsing probe found nothing better). HTTP 429 and
 * timeouts are deliberately not dead: debrid rate limits are transient.
 */
object DeadSourcePolicy {
    const val MAX_FAILOVERS = 3

    fun isDeadHttpStatus(status: Int?): Boolean = status == 404 || status == 410

    /**
     * Next source after [currentIndex] in the user's existing order whose URL is not dead.
     * A candidate without an http URL (torrent/magnet) is allowed, as upstream does.
     * Returns null when [failoversSoFar] reached the cap or nothing live remains.
     */
    fun nextLiveIndex(
        candidateUrls: List<String?>,
        currentIndex: Int,
        deadUrls: Set<String>,
        failoversSoFar: Int,
    ): Int? {
        if (failoversSoFar >= MAX_FAILOVERS || candidateUrls.isEmpty()) return null
        val start = if (currentIndex >= 0) currentIndex + 1 else 0
        return (start until candidateUrls.size).firstOrNull { index ->
            val url = candidateUrls[index]
            url == null || url !in deadUrls
        }
    }
}

/**
 * Startup watchdog decisions (G4a, feature 20). ALGORITHM_PORT of ysosrs
 * `scheduleStartupWatchdog` @ 45e0984: armed at `starting_stream`, disarmed at the first frame.
 * Every [TIMEOUT_MS] without a frame it re-arms while buffered-ahead data keeps growing and
 * another interval fits under [CEILING_MS]; otherwise it fires once with an honest reason.
 * It never stops the player, so a late first frame retracts the error.
 */
object StartupWatchdogPolicy {
    /** Worst legitimate first frame observed upstream was ~14.5 s from press. */
    const val TIMEOUT_MS = 20_000L
    const val CEILING_MS = 60_000L

    enum class Verdict { EXTEND, FIRE }

    enum class Reason(val key: String) {
        TRACKS_NOT_READ("tracks_not_read"),
        VIDEO_TRACK_UNSUPPORTED("video_track_unsupported"),
        NO_VIDEO_TRACK("no_video_track"),
        DECODER_UNRESPONSIVE("decoder_unresponsive"),
    }

    fun verdict(elapsedMs: Long, bufferedAheadMs: Long, lastBufferedAheadMs: Long): Verdict {
        val anotherIntervalFits = elapsedMs + TIMEOUT_MS <= CEILING_MS
        return if (bufferedAheadMs > lastBufferedAheadMs && anotherIntervalFits) Verdict.EXTEND else Verdict.FIRE
    }

    fun reason(tracksScanned: Boolean, hasVideoTrack: Boolean, videoTrackSelected: Boolean): Reason = when {
        !tracksScanned -> Reason.TRACKS_NOT_READ
        hasVideoTrack && !videoTrackSelected -> Reason.VIDEO_TRACK_UNSUPPORTED
        !hasVideoTrack -> Reason.NO_VIDEO_TRACK
        else -> Reason.DECODER_UNRESPONSIVE
    }
}
