package com.nuvio.tv.fork.skip

/**
 * Text-format parsers for the G9b providers (features 127–129). FILE_PORT of Cxsmo
 * `SkipMetadataParser.parseVideoSkip`, `parseNotScarePage`, `parseTimestamp`, `visibleHtmlText`,
 * `decodeHtmlEntities` and `slugifyNotScareTitle` @ 3e0d0fa. Pure: VideoSkip files and NotScare pages
 * are plain text / HTML, never executed or rendered.
 */
object SkipTextParsers {
    const val VIDEO_SKIP_CONFIDENCE = 0.72
    const val NOT_SCARE_CONFIDENCE = 0.76
    private const val MAJOR_SCARE_SECONDS = 6.0
    private const val MINOR_SCARE_SECONDS = 4.0

    private val VIDEO_SKIP_CUE = Regex("^(.+?)\\s+-->\\s+(.+?)\\s*$")
    private val NOT_SCARE_ENTRY = Regex("(?:^|\\n)\\s*((?:\\d{1,2}:)?\\d{1,2}:\\d{2})\\s+(Major|Minor)\\b", RegexOption.IGNORE_CASE)

    /**
     * VideoSkip `.skp` files: `start --> end` then a label line. Audio / dialog labels mute, visual /
     * blur labels only warn, the rest skip; labels naming none of our categories are dropped.
     */
    fun parseVideoSkip(raw: String, isMovie: Boolean): List<SkipReport> {
        val lines = raw.lineSequence().map { it.trim() }.toList()
        val result = ArrayList<SkipReport>()
        var index = 0
        while (index < lines.size - 1) {
            val cue = VIDEO_SKIP_CUE.matchEntire(lines[index])
            if (cue == null) {
                index++
                continue
            }
            val start = parseTimestamp(cue.groupValues[1])
            val end = parseTimestamp(cue.groupValues[2])
            val label = lines[index + 1]
            val category = SkipCategories.categoryOfLabel(label, isMovie)
            if (start != null && end != null && end > start && start >= 0 && label.isNotBlank() && category != null) {
                val action = when {
                    label.contains("audio", true) || label.contains("mute", true) || label.contains("dialog", true) -> SkipReport.ACTION_MUTE
                    label.contains("visual", true) || label.contains("blur", true) -> SkipReport.ACTION_WARN
                    else -> SkipReport.ACTION_SKIP
                }
                result += SkipReport(start, end, category, SkipCategories.officialType(category), ForkSkipProvider.VIDEO_SKIP.key, VIDEO_SKIP_CONFIDENCE, action)
                index += 2
            } else {
                index++
            }
        }
        return result
    }

    /** NotScare jump-scare pages: visible `m:ss Major|Minor` entries become short warn segments. */
    fun parseNotScarePage(rawHtml: String): List<SkipReport> {
        val plain = decodeHtmlEntities(visibleHtmlText(rawHtml))
        return NOT_SCARE_ENTRY.findAll(plain).mapNotNull { match ->
            val start = parseTimestamp(match.groupValues[1]) ?: return@mapNotNull null
            val major = match.groupValues[2].equals("Major", ignoreCase = true)
            SkipReport(
                startTime = start,
                endTime = start + if (major) MAJOR_SCARE_SECONDS else MINOR_SCARE_SECONDS,
                category = SkipCategories.JUMPSCARE,
                type = SkipCategories.JUMPSCARE,
                provider = ForkSkipProvider.NOT_SCARE.key,
                confidence = NOT_SCARE_CONFIDENCE,
                action = SkipReport.ACTION_WARN,
            )
        }.toList()
    }

    /** NotScare's page slug: `jump-scares-in-<title>-<year>`. */
    fun notScareSlug(title: String): String =
        java.text.Normalizer.normalize(title.trim(), java.text.Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace("&", " and ")
            .replace(Regex("[’']"), "")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

    /** `ss`, `m:ss` or `h:mm:ss`, with optional fractions. */
    fun parseTimestamp(value: String): Double? {
        val parts = value.trim().split(":").map { it.toDoubleOrNull() ?: return null }
        return when (parts.size) {
            1 -> parts[0]
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> null
        }?.takeIf { it.isFinite() && it >= 0 }
    }

    /** Visible text only: script and style bodies and every tag are dropped. */
    fun visibleHtmlText(html: String): String {
        val output = StringBuilder()
        val lower = html.lowercase()
        var cursor = 0
        while (cursor < html.length) {
            if (lower.startsWith("<script", cursor) || lower.startsWith("<style", cursor)) {
                val closing = if (lower.startsWith("<script", cursor)) "</script>" else "</style>"
                val end = lower.indexOf(closing, cursor + 7)
                if (end < 0) break
                cursor = end + closing.length
                output.append('\n')
                continue
            }
            if (html[cursor] == '<') {
                val end = html.indexOf('>', cursor + 1)
                if (end < 0) break
                cursor = end + 1
                output.append('\n')
                continue
            }
            val nextTag = html.indexOf('<', cursor).let { if (it < 0) html.length else it }
            output.append(html, cursor, nextTag)
            cursor = nextTag
        }
        return output.toString()
    }

    fun decodeHtmlEntities(value: String): String = value
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace(Regex("&#(\\d+);")) { match -> match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value }
}
