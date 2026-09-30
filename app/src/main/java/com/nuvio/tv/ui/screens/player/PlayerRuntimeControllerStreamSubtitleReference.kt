package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.fork.subtitles.StreamSubtitleReference

/**
 * G6a (feature 84): the playing stream's own subtitles as AutoSync's fallback reference, read when
 * the run starts. Empty while SUBTITLE_INTELLIGENCE is OFF, which keeps official AutoSync exactly.
 * Each subtitle keeps its own headers; stream request headers are never added.
 */
internal fun PlayerRuntimeController.autoSyncStreamSubtitleReferences(
    selectedUrl: String,
): List<StreamSubtitleReference.Candidate> {
    if (!StreamSubtitleReference.enabled) return emptyList()
    return StreamSubtitleReference.select(
        streamProvided = streamSubtitles.filter { it.isStreamProvided }.map { subtitle ->
            StreamSubtitleReference.Candidate(
                url = subtitle.url,
                language = subtitle.lang,
                headers = subtitle.headers.orEmpty(),
                label = subtitle.id,
            )
        },
        selectedUrl = selectedUrl,
    )
}
