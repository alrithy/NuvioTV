package com.nuvio.tv.fork.video

/** What the track-format AFR fallback (G5e, feature 58) does for one reported video frame rate. */
enum class TrackAfrAction {
    /** Not applicable (group off, MPV, AFR off, already decided, or the preflight already matched). */
    SKIP,
    /** The official preflight probe is still running; decide when it finishes. */
    DEFER,
    /** A rate under the floor is not real content; decided, nothing to do. */
    IGNORE_IMPLAUSIBLE,
    /** Playback already runs; a mid-playback mode switch is the black-screen risk this avoids. */
    TOO_LATE,
    /** Hold the start, switch, settle, release. */
    RUN,
}

object TrackAfrPolicy {
    const val MIN_FPS = 20f

    fun decide(
        enabled: Boolean,
        exoPlayerEngine: Boolean,
        afrOn: Boolean,
        alreadyAttempted: Boolean,
        rawFps: Float,
        preflightDetected: Boolean,
        preflightRunning: Boolean,
        playbackRunning: Boolean,
    ): TrackAfrAction = when {
        !enabled || !exoPlayerEngine || !afrOn || alreadyAttempted || rawFps <= 0f || preflightDetected -> TrackAfrAction.SKIP
        preflightRunning -> TrackAfrAction.DEFER
        rawFps < MIN_FPS -> TrackAfrAction.IGNORE_IMPLAUSIBLE
        playbackRunning -> TrackAfrAction.TOO_LATE
        else -> TrackAfrAction.RUN
    }
}
