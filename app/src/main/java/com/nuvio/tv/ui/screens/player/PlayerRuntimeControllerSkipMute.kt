package com.nuvio.tv.ui.screens.player

/**
 * Superfork G9b (feature 122): a mute segment switches the audio off for its span and back on after
 * it; it never seeks and never shows the skip button, so skip and mute stay distinct. Runs on the
 * existing skip tick. ExoPlayer's volume is otherwise unused by the player; mpv uses its `mute`
 * property, separate from the `volume` that audio amplification owns.
 */
internal fun PlayerRuntimeController.updateSkipMute(positionMs: Long) {
    val shouldMute = muteActiveAt(skipIntervals, positionMs)
    if (shouldMute == skipMuteActive) return
    skipMuteActive = shouldMute
    if (isUsingMpvEngine()) {
        mpvView?.setMuted(shouldMute)
    } else {
        _exoPlayer?.volume = if (shouldMute) 0f else 1f
    }
}

/** True while [positionMs] is inside a mute segment. */
internal fun muteActiveAt(intervals: List<com.nuvio.tv.data.repository.SkipInterval>, positionMs: Long): Boolean {
    val positionSec = positionMs / 1000.0
    return intervals.any { it.action == "mute" && positionSec >= it.startTime && positionSec < it.endTime }
}
