package com.nuvio.tv.fork.subtitles

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry

/**
 * Same-release timing reference for official AutoSync (G6a, feature 84).
 *
 * Official AutoSync compares the selected add-on subtitle with subtitles embedded in the video and
 * gives up when the file has none. Subtitles the stream itself provides (the stream response's
 * `subtitles`, `Subtitle.isStreamProvided`) ship with that exact release, so their timing matches
 * the video and they can serve as the reference instead. Only consulted on that failure path;
 * whenever an embedded reference exists official AutoSync is unchanged.
 */
object StreamSubtitleReference {
    /** Enough to cover a second language when the first one is sparse, without a download burst. */
    const val MAX_REFERENCES = 2

    /** Process-wide: registry defaults are fixed per process (as `AdaptiveResources.install`). */
    @JvmField
    val enabled: Boolean = FeatureRegistry().mode(FeatureId.SUBTITLE_INTELLIGENCE) != FeatureMode.OFF

    data class Candidate(
        val url: String,
        val language: String,
        val headers: Map<String, String>,
        val label: String? = null,
    )

    /**
     * Stream-provided subtitles usable as the reference for [selectedUrl]: never the target
     * itself, no blank or duplicate URLs, at most [MAX_REFERENCES], in the stream's order.
     */
    fun select(streamProvided: List<Candidate>, selectedUrl: String): List<Candidate> =
        streamProvided
            .filter { it.url.isNotBlank() && it.url != selectedUrl }
            .distinctBy { it.url }
            .take(MAX_REFERENCES)
}
