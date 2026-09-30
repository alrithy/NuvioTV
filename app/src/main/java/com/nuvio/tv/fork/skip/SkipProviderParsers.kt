package com.nuvio.tv.fork.skip

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlin.math.ln

/** Skip providers the fork adds beside official IntroDB / AniSkip / Anime-Skip (G9a, D054). */
enum class ForkSkipProvider(val key: String, val requiresKey: Boolean) {
    SKIP_ME("skipme", requiresKey = false),
    THE_INTRO_DB("theintrodb", requiresKey = false),
    PUBLIC_META_DB("publicmetadb", requiresKey = true),
}

@JsonClass(generateAdapter = true)
data class SkipMeTimeDto(
    @Json(name = "start_ms") val startMs: Long? = null,
    @Json(name = "end_ms") val endMs: Long? = null,
    @Json(name = "submissions") val submissions: Int? = null,
)

@JsonClass(generateAdapter = true)
data class SkipMeSegmentDto(
    @Json(name = "season") val season: Int? = null,
    @Json(name = "episode") val episode: Int? = null,
    @Json(name = "segment") val segment: String? = null,
    @Json(name = "start_ms") val startMs: Long? = null,
    @Json(name = "end_ms") val endMs: Long? = null,
    @Json(name = "submissions") val submissions: Int? = null,
)

@JsonClass(generateAdapter = true)
data class SkipMeItemDto(
    @Json(name = "segments") val segments: List<SkipMeSegmentDto>? = null,
    @Json(name = "intro") val intro: List<SkipMeTimeDto>? = null,
    @Json(name = "recap") val recap: List<SkipMeTimeDto>? = null,
    @Json(name = "credits") val credits: List<SkipMeTimeDto>? = null,
    @Json(name = "preview") val preview: List<SkipMeTimeDto>? = null,
)

@JsonClass(generateAdapter = true)
data class TheIntroDbTimeDto(
    @Json(name = "start_ms") val startMs: Any? = null,
    @Json(name = "end_ms") val endMs: Any? = null,
    @Json(name = "start") val start: Any? = null,
    @Json(name = "end") val end: Any? = null,
)

@JsonClass(generateAdapter = true)
data class TheIntroDbMediaDto(
    @Json(name = "intro") val intro: List<TheIntroDbTimeDto>? = null,
    @Json(name = "recap") val recap: List<TheIntroDbTimeDto>? = null,
    @Json(name = "credits") val credits: List<TheIntroDbTimeDto>? = null,
    @Json(name = "preview") val preview: List<TheIntroDbTimeDto>? = null,
)

@JsonClass(generateAdapter = true)
data class PublicMetaDbMappingResultDto(@Json(name = "tmdb_id") val tmdbId: Long? = null)

@JsonClass(generateAdapter = true)
data class PublicMetaDbMappingDto(@Json(name = "results") val results: List<PublicMetaDbMappingResultDto>? = null)

@JsonClass(generateAdapter = true)
data class PublicMetaDbSkipDto(
    @Json(name = "intro_start_ms") val introStartMs: Long? = null,
    @Json(name = "intro_end_ms") val introEndMs: Long? = null,
    @Json(name = "credits_start_ms") val creditsStartMs: Long? = null,
    @Json(name = "credits_end_ms") val creditsEndMs: Long? = null,
)

@JsonClass(generateAdapter = true)
data class PublicMetaDbSkipsDto(@Json(name = "items") val items: List<PublicMetaDbSkipDto>? = null)

/**
 * Response parsers for the G9a providers. FILE_PORT of Cxsmo `SkipMetadataParser`
 * (`parseSkipMe`, `parseTheIntroDb`, `parsePublicMetaDb*`, `timeSeconds`, `parseClock`) @ 3e0d0fa,
 * moved from org.json to Moshi. Confidences are Cxsmo's; any malformed body yields no reports.
 */
class SkipProviderParsers(moshi: Moshi) {
    private val skipMeAdapter = moshi.adapter<List<SkipMeItemDto>>(
        Types.newParameterizedType(List::class.java, SkipMeItemDto::class.java),
    )
    private val theIntroDbAdapter = moshi.adapter(TheIntroDbMediaDto::class.java)
    private val mappingAdapter = moshi.adapter(PublicMetaDbMappingDto::class.java)
    private val publicMetaDbAdapter = moshi.adapter(PublicMetaDbSkipsDto::class.java)

    fun parseSkipMe(raw: String, isMovie: Boolean, season: Int?, episode: Int?): List<SkipReport> {
        val item = runCatching { skipMeAdapter.fromJson(raw) }.getOrNull()?.firstOrNull() ?: return emptyList()
        val provider = ForkSkipProvider.SKIP_ME.key
        if (!isMovie && item.segments != null) {
            return item.segments.mapNotNull { segment ->
                if (segment.season != season || segment.episode != episode) return@mapNotNull null
                val type = segment.segment?.trim()?.lowercase() ?: return@mapNotNull null
                report(segment.startMs, segment.endMs, type, provider, skipMeConfidence(segment.submissions), isMovie)
            }
        }
        return listOf("intro" to item.intro, "recap" to item.recap, "credits" to item.credits, "preview" to item.preview)
            .flatMap { (type, times) ->
                times.orEmpty().mapNotNull { report(it.startMs, it.endMs, type, provider, skipMeConfidence(it.submissions), isMovie) }
            }
    }

    fun parseTheIntroDb(raw: String, isMovie: Boolean, durationMs: Long?): List<SkipReport> {
        val media = runCatching { theIntroDbAdapter.fromJson(raw) }.getOrNull() ?: return emptyList()
        val provider = ForkSkipProvider.THE_INTRO_DB.key
        return listOf("intro" to media.intro, "recap" to media.recap, "credits" to media.credits, "preview" to media.preview)
            .flatMap { (type, times) ->
                times.orEmpty().mapNotNull { time ->
                    val start = seconds(time.startMs, time.start) ?: 0.0
                    val end = seconds(time.endMs, time.end) ?: durationMs?.takeIf { it > 0 }?.div(1000.0) ?: return@mapNotNull null
                    reportSeconds(start, end, type, provider, THE_INTRO_DB_CONFIDENCE, isMovie)
                }
            }
    }

    fun parsePublicMetaDbMapping(raw: String): Long? =
        runCatching { mappingAdapter.fromJson(raw) }.getOrNull()?.results?.firstOrNull()?.tmdbId?.takeIf { it > 0 }

    fun parsePublicMetaDb(raw: String, isMovie: Boolean): List<SkipReport> {
        val items = runCatching { publicMetaDbAdapter.fromJson(raw) }.getOrNull()?.items.orEmpty()
        val provider = ForkSkipProvider.PUBLIC_META_DB.key
        return items.flatMap { item ->
            listOfNotNull(
                report(item.introStartMs, item.introEndMs, "intro", provider, PUBLIC_META_DB_CONFIDENCE, isMovie),
                report(item.creditsStartMs, item.creditsEndMs, "credits", provider, PUBLIC_META_DB_CONFIDENCE, isMovie),
            )
        }
    }

    private fun report(startMs: Long?, endMs: Long?, type: String, provider: String, confidence: Double, isMovie: Boolean): SkipReport? {
        if (startMs == null || endMs == null || startMs < 0 || endMs <= startMs) return null
        return reportSeconds(startMs / 1000.0, endMs / 1000.0, type, provider, confidence, isMovie)
    }

    private fun reportSeconds(start: Double, end: Double, type: String, provider: String, confidence: Double, isMovie: Boolean): SkipReport? {
        if (!start.isFinite() || !end.isFinite() || start < 0 || end <= start) return null
        val category = SkipCategories.categoryOf(type, isMovie) ?: return null
        return SkipReport(start, end, category, SkipCategories.officialType(category), provider, confidence)
    }

    companion object {
        const val THE_INTRO_DB_CONFIDENCE = 0.86
        const val PUBLIC_META_DB_CONFIDENCE = 0.82

        /** Cxsmo: more community submissions, more confidence (0.72 for one, capped at 0.99). */
        fun skipMeConfidence(submissions: Int?): Double =
            (0.72 + (ln((submissions ?: 1).coerceAtLeast(0) + 1.0) / ln(2.0)) * 0.08).coerceAtMost(0.99)

        /** Milliseconds first; else seconds as a number or an `m:ss` / `h:mm:ss` clock. */
        fun seconds(millis: Any?, secondsValue: Any?): Double? {
            millis?.toString()?.toDoubleOrNull()?.let { return it / 1000.0 }
            val raw = secondsValue?.toString() ?: return null
            raw.toDoubleOrNull()?.let { return it }
            return parseClock(raw)
        }

        fun parseClock(value: String): Double? {
            val parts = value.trim().split(":").map { it.toDoubleOrNull() ?: return null }
            return when (parts.size) {
                2 -> parts[0] * 60 + parts[1]
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                else -> null
            }
        }
    }
}
