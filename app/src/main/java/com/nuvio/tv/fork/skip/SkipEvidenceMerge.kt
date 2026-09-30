package com.nuvio.tv.fork.skip

/** One provider's report of a segment (G9a). Times in seconds. */
data class SkipReport(
    val startTime: Double,
    val endTime: Double,
    /** Grouping key: official category (opening / ending / recap / movie-credits / post-credits). */
    val category: String,
    /** The provider's raw type, kept for the interval the player receives. */
    val type: String,
    val provider: String,
    val confidence: Double,
    /** Skip and mute never merge with each other (G9b). */
    val action: String = ACTION_SKIP,
) {
    companion object {
        const val ACTION_SKIP = "skip"

        /** G9b (122): lower the volume for the span, never seek. */
        const val ACTION_MUTE = "mute"

        /** G9b (121): offer the skip button only, never act on its own. */
        const val ACTION_WARN = "warn"
    }
}

/** A merged segment and how many distinct providers back it. */
data class MergedSkip(val report: SkipReport, val providers: List<String>)

/**
 * ALGORITHM_PORT of Cxsmo `mergeSkipIntervals` @ 3e0d0fa (features 132, 133): reports of the same
 * category and action that overlap within [toleranceSeconds] form one group; its times are the
 * confidence-weighted mean, its confidence the mean plus a small multi-provider bonus. Different
 * actions stay apart. Pure.
 */
object SkipEvidenceMerge {
    const val DEFAULT_TOLERANCE_SECONDS = 2.0
    private const val MIN_WEIGHT = 0.01
    private const val BONUS_PER_EXTRA_PROVIDER = 0.04
    private const val MAX_BONUS = 0.12
    private const val MAX_CONFIDENCE = 0.995

    fun merge(reports: List<SkipReport>, toleranceSeconds: Double = DEFAULT_TOLERANCE_SECONDS): List<MergedSkip> {
        val groups = mutableListOf<MutableList<SkipReport>>()
        reports
            .filter { it.startTime.isFinite() && it.endTime.isFinite() && it.startTime >= 0 && it.endTime > it.startTime }
            .sortedWith(compareBy<SkipReport> { it.startTime }.thenByDescending { it.confidence })
            .forEach { report ->
                val group = groups.firstOrNull { candidate ->
                    candidate.any { other ->
                        other.category == report.category && other.action == report.action &&
                            other.startTime <= report.endTime + toleranceSeconds &&
                            report.startTime <= other.endTime + toleranceSeconds
                    }
                }
                if (group == null) groups += mutableListOf(report) else group += report
            }
        return groups.map { group ->
            val strongest = group.maxWith(compareBy<SkipReport> { it.confidence }.thenBy { -it.startTime })
            val providers = group.map { it.provider }.distinct()
            if (group.size == 1) return@map MergedSkip(strongest, providers)
            val weights = group.map { it.confidence.coerceIn(0.0, 1.0).coerceAtLeast(MIN_WEIGHT) }
            val total = weights.sum()
            val start = group.indices.sumOf { group[it].startTime * weights[it] } / total
            val end = group.indices.sumOf { group[it].endTime * weights[it] } / total
            val confidence = if (providers.size > 1) {
                (group.map { it.confidence.coerceIn(0.0, 1.0) }.average() +
                    minOf(MAX_BONUS, (providers.size - 1) * BONUS_PER_EXTRA_PROVIDER)).coerceAtMost(MAX_CONFIDENCE)
            } else {
                strongest.confidence.coerceIn(0.0, 1.0)
            }
            MergedSkip(
                strongest.copy(startTime = start, endTime = end.coerceAtLeast(start + 0.001), confidence = confidence),
                providers,
            )
        }.sortedWith(compareBy<MergedSkip> { it.report.startTime }.thenBy { it.report.endTime })
    }

    /**
     * One segment per official category (the player shows one button per category): the merged
     * group with the highest confidence, then the most providers, then the earliest. Preview and
     * content-warning segments (G9b) all stay: a film can have many jump scares.
     */
    fun bestPerCategory(merged: List<MergedSkip>): List<MergedSkip> {
        val (single, repeated) = merged.partition { it.report.category in SkipCategories.SINGLE }
        return (single.groupBy { it.report.category to it.report.action }.values
            .map { candidates ->
                candidates.maxWith(
                    compareBy<MergedSkip> { it.report.confidence }
                        .thenBy { it.providers.size }
                        .thenBy { -it.report.startTime },
                )
            } + repeated)
            .sortedBy { it.report.startTime }
    }
}
