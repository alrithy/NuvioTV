package com.nuvio.tv.fork.seek

import android.graphics.Bitmap
import android.util.Log
import com.nuvio.tv.fork.seek.local.LocalPreviewTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs [SeekrCalibration] for one session (G7b): the reference frames are the on-device
 * keyframe thumbnails of the playing file, the candidates are the Seekr thumbnails around each of
 * them. Only lookups of already prefetched sprite sheets and small luma grids; no seeking, no new
 * requests beyond the sheets Seekr previews load anyway. Called only while no manual Preview Sync
 * offset is set, so Seekr lookups are at the raw position.
 */
internal object SeekrCalibrationRunner {
    private const val TAG = "SeekrCalibration"
    private const val ANCHORS = 6
    private const val TIMEOUT_MS = 10_000L

    suspend fun run(
        local: LocalPreviewTrack,
        seekr: BoundedSeekrTrack,
        suggestedOffsetMs: Long,
    ): SeekrCalibration.Result = withTimeoutOrNull(TIMEOUT_MS) {
        withContext(Dispatchers.Default) {
            val frames = local.calibrationFrames(ANCHORS)
            if (frames.size < SeekrCalibration.MIN_ANCHORS) return@withContext SeekrCalibration.Result()
            val anchors = frames.mapNotNull { (frameMs, frame) -> anchorFor(frameMs, frame, seekr, suggestedOffsetMs) }
            SeekrCalibration.estimate(anchors).also { result ->
                Log.i(
                    TAG,
                    "offset=${result.offsetMs} confidence=${"%.2f".format(result.confidence)} " +
                        "anchors=${result.anchorsUsed}/${anchors.size} accepted=${result.accepted}",
                )
            }
        }
    } ?: SeekrCalibration.Result()

    private suspend fun anchorFor(
        frameMs: Long,
        frame: Bitmap,
        seekr: BoundedSeekrTrack,
        suggestedOffsetMs: Long,
    ): SeekrCalibration.Anchor? {
        val reference = luma(frame) ?: return null
        val atFrame = seekr.thumbnailFor(frameMs) ?: return null
        val cueIntervalMs = (atFrame.cueEndMs - atFrame.cueStartMs).coerceAtLeast(1_000L)
        // One candidate per distinct Seekr cue: offsets landing on the same tile would tie and
        // make every match look ambiguous.
        val seenCues = HashSet<Long>()
        val candidates = SeekrCalibration.candidateOffsets(cueIntervalMs, suggestedOffsetMs).mapNotNull { offsetMs ->
            val position = frameMs + offsetMs
            if (position < 0L) return@mapNotNull null
            val thumbnail = seekr.thumbnailFor(position) ?: return@mapNotNull null
            if (!seenCues.add(thumbnail.cueStartMs)) return@mapNotNull null
            SeekrCalibration.Candidate(offsetMs, SeekrCalibration.similarity(reference, luma(thumbnail.bitmap)))
        }
        return SeekrCalibration.Anchor(frameMs, candidates).takeIf { candidates.isNotEmpty() }
    }

    private fun luma(bitmap: Bitmap): DoubleArray? {
        if (bitmap.isRecycled) return null
        return runCatching {
            SeekrCalibration.lumaGrid(bitmap.width, bitmap.height) { x, y -> bitmap.getPixel(x, y) }
        }.getOrNull()
    }
}
