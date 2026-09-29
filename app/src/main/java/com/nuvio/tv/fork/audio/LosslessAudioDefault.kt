package com.nuvio.tv.fork.audio

/** The track facts the lossless default reads; filled from the player's own track list. */
data class AudioTrackCandidate(
    val index: Int,
    val codec: String?,
    val name: String?,
    val language: String?,
    val channelCount: Int?,
)

/**
 * Default audio selection to the best lossless track (G5b, feature 37). ALGORITHM_PORT of ysosrs
 * `LosslessAudioTrackDefault` @ 45e0984.
 *
 * Remuxes carry a lossless track (TrueHD, DTS-HD MA, FLAC, PCM) next to a lossy one, and the
 * engines' defaults follow container flags and language only. This runs only when the user turned
 * "Prefer lossless audio" on (D048) and no explicit, remembered or carried-over selection exists.
 *
 * Language rule: the preference applies strictly within the first preferred language that has
 * any track; a lossless track is never chosen over the user's language. Classification is
 * conservative: a track is ranked only when its codec or name proves the tier (mpv reports plain
 * "dts" for every DTS flavour, so it needs an MA hint in the name). ExoPlayer's DTS-HD covers MA
 * and the lossy HRA profile; in remux practice it is MA, so it ranks as lossless (upstream
 * trade-off). Commentary tracks are never the default.
 */
object LosslessAudioDefault {
    const val TIER_TRUEHD = 4
    const val TIER_DTS_HD_MA = 3
    const val TIER_FLAC = 2
    const val TIER_PCM = 1

    private val commentaryHints = listOf("commentary", "descriptive", "description", "narration", "narrator")
    private val maWord = Regex("\\bma\\b")

    /** Lossless tier, or null when the track is not provably lossless. */
    fun losslessTier(codec: String?, name: String?): Int? {
        val c = codec?.trim()?.lowercase().orEmpty()
        val n = name?.trim()?.lowercase().orEmpty()
        if (c == "truehd" || c == "mlp" || "truehd" in n || "true hd" in n) return TIER_TRUEHD
        val nameSaysMa = "dts-hd ma" in n || "dts hd ma" in n || "dtshd ma" in n || "master audio" in n ||
            maWord.containsMatchIn(n)
        if (c == "dts-hd") return TIER_DTS_HD_MA
        if (c == "dts" && nameSaysMa) return TIER_DTS_HD_MA
        if (c.isEmpty() && nameSaysMa && "dts" in n) return TIER_DTS_HD_MA
        if (c == "flac" || c == "alac" || "flac" in n) return TIER_FLAC
        if (c == "pcm" || c == "wav" || c.startsWith("pcm_") || "lpcm" in n) return TIER_PCM
        return null
    }

    fun isCommentaryLike(name: String?): Boolean {
        val n = name?.lowercase() ?: return false
        return commentaryHints.any { it in n }
    }

    /**
     * Index the default should select, or null when the engine's pick should stand.
     * [languageMatches] is (trackLanguage, preferredLanguage) and tolerates ISO 639 variants.
     */
    fun pickDefaultIndex(
        tracks: List<AudioTrackCandidate>,
        preferredLanguages: List<String>,
        languageMatches: (String?, String) -> Boolean,
    ): Int? {
        if (tracks.isEmpty()) return null
        val pool = preferredLanguages.asSequence()
            .map { lang -> tracks.filter { languageMatches(it.language, lang) } }
            .firstOrNull { it.isNotEmpty() }
            ?: tracks
        val ranked = pool.mapNotNull { track -> losslessTier(track.codec, track.name)?.let { track to it } }
        if (ranked.isEmpty()) return null
        val byTierDesc = ranked.groupBy({ it.second }, { it.first }).toSortedMap(compareByDescending { it })
        for ((_, tierTracks) in byTierDesc) {
            val clean = tierTracks.filterNot { isCommentaryLike(it.name) }
            if (clean.isNotEmpty()) {
                return clean.sortedWith(
                    compareByDescending<AudioTrackCandidate> { it.channelCount ?: 0 }.thenBy { it.index }
                ).first().index
            }
        }
        return null
    }
}
