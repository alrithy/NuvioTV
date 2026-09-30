package com.nuvio.tv.fork.skip

import android.util.Log
import com.nuvio.tv.data.repository.SkipInterval
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The fork skip providers beside official IntroDB / AniSkip / Anime-Skip (G9a, D054; features 117,
 * 124–126, 131–133). FILE_PORT of Cxsmo `SkipIntroRepository` fetchers @ 3e0d0fa; Cxsmo's TheIntroDB
 * and PublicMetaDB request strings (`"?$$query"`, `"Bearer $$apiKey"`) sent a literal `$` and are
 * fixed here. Each provider has its own timeout, so a slow or failing one never delays the rest;
 * nothing runs unless the user switched a provider on. Logs name the provider and status only
 * (keys travel in headers and are never logged).
 */
@Singleton
class ForkSkipProviders @Inject constructor(
    private val okHttpClient: OkHttpClient,
    moshi: Moshi,
    private val settings: SkipProviderSettings,
) {
    private val parsers = SkipProviderParsers(moshi)

    /** Empty when the group is OFF or no provider is active: official behavior stays untouched. */
    suspend fun activeConfig(): SkipProviderConfig =
        if (settings.featureEnabled) settings.configNow() else SkipProviderConfig.NONE

    suspend fun fetch(
        config: SkipProviderConfig,
        imdbId: String,
        season: Int?,
        episode: Int?,
        isMovie: Boolean,
        durationMs: Long?,
        title: String? = null,
        releaseYear: String? = null,
    ): List<SkipReport> = coroutineScope {
        // The id goes into JSON bodies and URLs, so only a plain IMDb id is ever sent.
        if (!imdbId.matches(IMDB_ID)) return@coroutineScope emptyList()
        config.active.map { provider ->
            async {
                withTimeoutOrNull(FORK_SKIP_PROVIDER_TIMEOUT_MS) {
                    try {
                        when (provider) {
                            ForkSkipProvider.SKIP_ME -> skipMe(imdbId, season, episode, isMovie, durationMs)
                            ForkSkipProvider.THE_INTRO_DB -> theIntroDb(imdbId, season, episode, isMovie, durationMs, config.keys[provider])
                            ForkSkipProvider.PUBLIC_META_DB -> publicMetaDb(imdbId, season, episode, isMovie, config.keys[provider].orEmpty())
                            ForkSkipProvider.MOVIE_HAVEN_DB -> if (isMovie) movieHavenDb(imdbId) else emptyList()
                            ForkSkipProvider.VIDEO_SKIP -> videoSkip(imdbId, title, isMovie, season, episode)
                            ForkSkipProvider.NOT_SCARE -> if (isMovie) notScare(title, releaseYear) else emptyList()
                        }
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (error: Exception) {
                        Log.d(TAG, "${provider.key} unavailable (${error.javaClass.simpleName})")
                        emptyList()
                    }
                } ?: emptyList<SkipReport>().also { Log.d(TAG, "${provider.key} timed out") }
            }
        }.awaitAll().flatten()
            // G9b: preview and content-warning segments only for the categories the user switched on.
            .filter { SkipCategories.allowed(it.category, config.categories) }
    }

    /**
     * Official results and fork reports merged by evidence (132, 133): one segment per category,
     * credits never covering a post-credits scene. Official providers weigh above a single-report fork
     * provider; a community segment backed by many submissions (SkipMe) or by several providers can
     * outweigh them, as in Cxsmo.
     */
    fun combine(official: List<SkipInterval>, fork: List<SkipReport>, isMovie: Boolean): List<SkipInterval> {
        val officialReports = official.mapNotNull { interval ->
            val category = SkipCategories.categoryOf(interval.type, isMovie) ?: return@mapNotNull null
            SkipReport(interval.startTime, interval.endTime, category, interval.type, interval.provider, officialConfidence(interval.provider))
        }
        val merged = SkipEvidenceMerge.bestPerCategory(SkipEvidenceMerge.merge(officialReports + fork))
        val guarded = if (isMovie) SkipCategories.guardPostCredits(merged) else merged
        return guarded.map {
            SkipInterval(it.report.startTime, it.report.endTime, it.report.type, it.report.provider, it.report.action)
        }
    }

    private suspend fun skipMe(imdbId: String, season: Int?, episode: Int?, isMovie: Boolean, durationMs: Long?): List<SkipReport> {
        // SkipMe's lookup requires the runtime; the player asks again once playback knows it.
        if (durationMs == null || durationMs <= 0) return emptyList()
        val lookup = buildString {
            append("[{\"imdb_id\":\"").append(imdbId).append('"')
            if (!isMovie && season != null && episode != null) append(",\"season\":").append(season).append(",\"episode\":").append(episode)
            append(",\"duration_ms\":").append(durationMs).append("}]")
        }
        val body = post(ForkSkipProvider.SKIP_ME, "https://db.skipme.workers.dev/v1/movies", lookup) ?: return emptyList()
        return parsers.parseSkipMe(body, isMovie, season, episode)
    }

    private suspend fun theIntroDb(imdbId: String, season: Int?, episode: Int?, isMovie: Boolean, durationMs: Long?, apiKey: String?): List<SkipReport> {
        val query = buildString {
            append("imdb_id=").append(imdbId)
            if (!isMovie && season != null && episode != null) append("&season=").append(season).append("&episode=").append(episode)
            if (durationMs != null && durationMs > 0) append("&duration_ms=").append(durationMs)
        }
        val headers = apiKey?.takeIf { it.isNotBlank() }?.let { mapOf("Authorization" to "Bearer $it") }.orEmpty()
        val body = get(ForkSkipProvider.THE_INTRO_DB, "https://api.theintrodb.org/v3/media?$query", headers) ?: return emptyList()
        return parsers.parseTheIntroDb(body, isMovie, durationMs)
    }

    private suspend fun publicMetaDb(imdbId: String, season: Int?, episode: Int?, isMovie: Boolean, apiKey: String): List<SkipReport> {
        if (apiKey.isBlank()) return emptyList()
        val headers = mapOf("Authorization" to "Bearer $apiKey")
        val mediaType = if (isMovie) "movie" else "tv"
        val tmdbId = get(
            ForkSkipProvider.PUBLIC_META_DB,
            "https://publicmetadb.com/api/external/mappings/lookup?id_type=imdb&id_value=$imdbId&media_type=$mediaType",
            headers,
        )?.let(parsers::parsePublicMetaDbMapping) ?: return emptyList()
        val url = buildString {
            append("https://publicmetadb.com/api/external/skips?tmdb_id=").append(tmdbId).append("&media_type=").append(mediaType)
            if (!isMovie && season != null && episode != null) append("&season=").append(season).append("&episode=").append(episode)
        }
        val body = get(ForkSkipProvider.PUBLIC_META_DB, url, headers) ?: return emptyList()
        return parsers.parsePublicMetaDb(body, isMovie)
    }

    /** G9b (127): the movie's MovieHavenDB scene file (a static JSON file per IMDb id). */
    private suspend fun movieHavenDb(imdbId: String): List<SkipReport> {
        val body = get(ForkSkipProvider.MOVIE_HAVEN_DB, "https://raw.githubusercontent.com/arman-kh/MovieHavenDB/master/movies/$imdbId.json", emptyMap())
            ?: return emptyList()
        return parsers.parseMovieHaven(body, isMovie = true)
    }

    /**
     * G9b (128): VideoSkip search by title, then the matching entries' `.skp` files (at most
     * [MAX_VIDEO_SKIP_DETAILS] entries and [MAX_VIDEO_SKIP_DOWNLOADS] files per entry, inside the
     * provider's one timeout). An entry matches by IMDb id (movies) or SxEy (episodes).
     */
    private suspend fun videoSkip(imdbId: String, title: String?, isMovie: Boolean, season: Int?, episode: Int?): List<SkipReport> {
        if (title.isNullOrBlank() || (!isMovie && (season == null || episode == null))) return emptyList()
        val query = java.net.URLEncoder.encode(title.trim(), "UTF-8")
        val search = get(ForkSkipProvider.VIDEO_SKIP, "$VIDEO_SKIP_BASE/exchange/search/?q=$query", HTML) ?: return emptyList()
        val details = Regex("/exchange/videos/\\d+/?").findAll(search).map { VIDEO_SKIP_BASE + it.value }
            .distinct().take(MAX_VIDEO_SKIP_DETAILS).toList()
        return details.flatMap { detailUrl ->
            val detail = get(ForkSkipProvider.VIDEO_SKIP, detailUrl, HTML)?.lowercase() ?: return@flatMap emptyList()
            val matches = if (isMovie) imdbId.lowercase() in detail
            else "s${season}e$episode" in detail || ("season $season" in detail && "episode $episode" in detail)
            if (!matches) return@flatMap emptyList()
            Regex("/exchange/skip/\\d+/download/?").findAll(detail).map { VIDEO_SKIP_BASE + it.value }
                .distinct().take(MAX_VIDEO_SKIP_DOWNLOADS).toList()
                .flatMap { url -> get(ForkSkipProvider.VIDEO_SKIP, url, emptyMap())?.let { SkipTextParsers.parseVideoSkip(it, isMovie) }.orEmpty() }
        }
    }

    /** G9b (129): NotScare's public jump-scare page for a movie, by title and year. */
    private suspend fun notScare(title: String?, releaseYear: String?): List<SkipReport> {
        val slug = title?.let(SkipTextParsers::notScareSlug)?.takeIf { it.isNotBlank() } ?: return emptyList()
        val year = releaseYear?.trim()?.takeIf { it.matches(YEAR) } ?: return emptyList()
        val body = get(ForkSkipProvider.NOT_SCARE, "https://notscare.me/movies/jump-scares-in-$slug-$year", HTML) ?: return emptyList()
        return SkipTextParsers.parseNotScarePage(body)
    }

    /** JSON is accepted unless [headers] name another `Accept` (HTML pages). */
    private suspend fun get(provider: ForkSkipProvider, url: String, headers: Map<String, String>): String? =
        execute(
            provider,
            Request.Builder().url(url).header("Accept", "application/json")
                .apply { headers.forEach { (name, value) -> header(name, value) } }.get(),
        )

    private suspend fun post(provider: ForkSkipProvider, url: String, json: String): String? =
        execute(provider, Request.Builder().url(url).header("Accept", "application/json").post(json.toRequestBody(JSON)))

    private suspend fun execute(provider: ForkSkipProvider, builder: Request.Builder): String? = withContext(Dispatchers.IO) {
        val request = builder.header("User-Agent", USER_AGENT).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.d(TAG, "${provider.key} HTTP ${response.code}")
                return@use null
            }
            val body = response.body ?: return@use null
            if (body.contentLength() > MAX_RESPONSE_BYTES) return@use null
            val source = body.source()
            source.request(MAX_RESPONSE_BYTES + 1)
            if (source.buffer.size > MAX_RESPONSE_BYTES) return@use null
            source.buffer.readUtf8()
        }
    }

    private companion object {
        const val TAG = "ForkSkip"
        val IMDB_ID = Regex("tt\\d+")
        val YEAR = Regex("\\d{4}")
        const val VIDEO_SKIP_BASE = "https://videoskip.herokuapp.com"
        const val MAX_VIDEO_SKIP_DETAILS = 8
        const val MAX_VIDEO_SKIP_DOWNLOADS = 6
        val HTML = mapOf("Accept" to "text/html,application/xhtml+xml,text/plain")
        const val MAX_RESPONSE_BYTES = 2L * 1024 * 1024
        const val USER_AGENT = "NuvioTV/skip-metadata"
        val JSON = "application/json; charset=utf-8".toMediaType()

        /** Official providers keep their order (IntroDB, Anime-Skip, AniSkip) over the fork ones. */
        fun officialConfidence(provider: String): Double = when (provider) {
            "introdb" -> 0.9
            "animeskip" -> 0.88
            "aniskip" -> 0.87
            else -> 0.8
        }
    }
}

/** Awaits [deferred] for at most [timeoutMs]; on timeout the request is cancelled (131). */
suspend fun <T> awaitWithin(deferred: Deferred<List<T>>, timeoutMs: Long): List<T> =
    withTimeoutOrNull(timeoutMs) { deferred.await() } ?: emptyList<T>().also { deferred.cancel() }

const val FORK_SKIP_PROVIDER_TIMEOUT_MS = 6_000L
