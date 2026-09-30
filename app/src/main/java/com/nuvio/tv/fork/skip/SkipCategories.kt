package com.nuvio.tv.fork.skip

/**
 * Maps provider segment types onto official's categories (G9a) so every provider merges into the
 * segments the official player already understands. Pure.
 */
object SkipCategories {
    const val OPENING = "opening"
    const val ENDING = "ending"
    const val RECAP = "recap"
    const val MOVIE_CREDITS = "movie-credits"
    const val POST_CREDITS = "post-credits"

    /** Null = not a category official shows (preview and content warnings arrive with G9b). */
    fun categoryOf(type: String, isMovie: Boolean): String? = when (type.trim().lowercase()) {
        "intro", "op", "opening", "mixed-op" -> OPENING
        "recap" -> RECAP
        "outro", "ed", "ending", "mixed-ed", "credits" -> if (isMovie) MOVIE_CREDITS else ENDING
        "movie-credits" -> MOVIE_CREDITS
        "post-credits" -> POST_CREDITS
        else -> null
    }

    /** The type name official's player and settings use for a category. */
    fun officialType(category: String): String = when (category) {
        OPENING -> "intro"
        ENDING -> "outro"
        else -> category
    }

    /**
     * Official's rule, applied after merging: skipping movie credits must not also skip a
     * post-credits scene, so credits end where the scene starts (dropped if nothing is left).
     */
    fun guardPostCredits(merged: List<MergedSkip>): List<MergedSkip> {
        val scene = merged.firstOrNull { it.report.category == POST_CREDITS }?.report ?: return merged
        return merged.mapNotNull { item ->
            val credits = item.report
            if (credits.category != MOVIE_CREDITS) return@mapNotNull item
            val overlaps = scene.startTime < credits.endTime && scene.endTime > credits.startTime
            if (!overlaps) return@mapNotNull item
            val trimmed = credits.copy(endTime = scene.startTime)
            if (trimmed.endTime > trimmed.startTime) item.copy(report = trimmed) else null
        }
    }
}
