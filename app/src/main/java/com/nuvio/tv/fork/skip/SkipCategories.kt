package com.nuvio.tv.fork.skip

/**
 * Maps provider segment types onto official's categories (G9a) plus the opt-in preview and
 * content-warning categories (G9b), so every provider merges into segments the official player
 * understands. Pure.
 */
object SkipCategories {
    const val OPENING = "opening"
    const val ENDING = "ending"
    const val RECAP = "recap"
    const val MOVIE_CREDITS = "movie-credits"
    const val POST_CREDITS = "post-credits"

    const val PREVIEW = "preview"
    const val JUMPSCARE = "jumpscare"
    const val NUDITY = "nudity"
    const val SEX = "sex"
    const val GORE = "gore"
    const val VIOLENCE = "violence"
    const val PROFANITY = "profanity"

    /** Official's categories: one segment each, as the official player expects. */
    val SINGLE: Set<String> = setOf(OPENING, ENDING, RECAP, MOVIE_CREDITS, POST_CREDITS)

    /** G9b: shown only for categories the user switched on (all off by default, D054). */
    val CONTENT_WARNINGS: List<String> = listOf(JUMPSCARE, NUDITY, SEX, GORE, VIOLENCE, PROFANITY)
    val OPTIONAL: List<String> = listOf(PREVIEW) + CONTENT_WARNINGS

    /** Official categories always pass; optional ones only when the user switched them on. */
    fun allowed(category: String, enabledOptional: Set<String>): Boolean =
        category in SINGLE || category in enabledOptional

    fun categoryOf(type: String, isMovie: Boolean): String? = when (val value = type.trim().lowercase()) {
        "intro", "op", "opening", "mixed-op" -> OPENING
        "recap" -> RECAP
        "outro", "ed", "ending", "mixed-ed", "credits" -> if (isMovie) MOVIE_CREDITS else ENDING
        "movie-credits" -> MOVIE_CREDITS
        "post-credits" -> POST_CREDITS
        else -> value.takeIf { it in OPTIONAL }
    }

    /**
     * Cxsmo `mapCategory` @ 3e0d0fa: a provider's free-text reason or label onto a category, or null
     * when it names none of ours (Cxsmo's "custom" is not imported).
     */
    fun categoryOfLabel(label: String, isMovie: Boolean): String? {
        val value = label.lowercase()
        return when {
            "jumpscare" in value || "jump scare" in value || "fright" in value || "scare" in value -> JUMPSCARE
            "nudity" in value -> NUDITY
            "sex" in value -> SEX
            "gore" in value -> GORE
            "violence" in value -> VIOLENCE
            "profan" in value || "language" in value || "curse" in value -> PROFANITY
            "intro" in value || "opening" in value -> OPENING
            "recap" in value -> RECAP
            "outro" in value || "ending" in value || "credit" in value -> if (isMovie) MOVIE_CREDITS else ENDING
            "preview" in value || "filler" in value -> PREVIEW
            else -> null
        }
    }

    /** The type name official's player and settings use for a category. */
    fun officialType(category: String): String = when (category) {
        OPENING -> "intro"
        ENDING -> "outro"
        else -> category
    }

    /**
     * What an external player may receive (official forwarding): plain skip segments of official's
     * categories only, never mute or content-warning ones it would silently skip.
     */
    fun forwardableToExternalPlayer(type: String, action: String): Boolean =
        action == SkipReport.ACTION_SKIP && type.trim().lowercase() !in OPTIONAL

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
