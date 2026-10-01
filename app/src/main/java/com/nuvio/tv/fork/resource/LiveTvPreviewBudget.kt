package com.nuvio.tv.fork.resource

/**
 * G10f limits for the Live TV list's channel preview, from [AdaptiveResourcePolicy.liveTvPreviewBudget]
 * (Reshaped `LiveTvPreviewPlayer` @ 0ccf049, whose low-memory line is this policy's constrained tier).
 * One preview player at a time; a channel that only comes larger than [maxWidth] x [maxHeight] is not
 * decoded at all.
 */
data class LiveTvPreviewBudget(
    /** Whether previews are on until the viewer chooses: off on low-RAM devices. */
    val onByDefault: Boolean,
    val maxWidth: Int,
    val maxHeight: Int,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferAfterRebufferMs: Int,
    val targetBufferBytes: Int,
) {
    init {
        require(maxWidth > 0 && maxHeight > 0 && targetBufferBytes > 0)
        require(bufferForPlaybackMs <= minBufferMs && bufferAfterRebufferMs <= minBufferMs && minBufferMs <= maxBufferMs)
    }
}
