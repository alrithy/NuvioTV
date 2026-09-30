package com.nuvio.tv.fork.postplay

import kotlinx.coroutines.CancellationException

/**
 * Post-play recommendation sources (G9c, D054). [OFFICIAL] is official's own chain (the "More like
 * this" source: Trakt, TMDB or Simkl, TMDB when the tracker is not signed in) and stays the default.
 * The others are Cxsmo's (`PostPlayRecommendationSource` @ 3e0d0fa): Kurato AI and BingeCat AI are
 * catalogs of add-ons the user installed, MDBList is the user's MDBList watchlist. Pure.
 */
enum class ForkPostPlaySource(val key: String) {
    OFFICIAL("official"),
    AUTO("auto"),
    KURATO_AI("kurato_ai"),
    BINGECAT_AI("bingecat_ai"),
    MDBLIST("mdblist");

    companion object {
        fun fromKey(key: String?): ForkPostPlaySource = entries.firstOrNull { it.key == key } ?: OFFICIAL
    }
}

/** One lookup in a source chain. */
enum class PostPlayStep { KURATO_AI, BINGECAT_AI, OFFICIAL, MDBLIST }

object PostPlaySources {
    /** Cards once a fork source is chosen (144); official shows at most 4. */
    const val FORK_CARD_LIMIT = 20

    /** Catalog items fetched per add-on page (Cxsmo default 50, at most 100). */
    const val DEFAULT_CATALOG_PAGE_SIZE = 50
    const val MAX_CATALOG_PAGE_SIZE = 100

    /** Cxsmo pages through the whole catalog; the fork stops once filtering can still fill the cards. */
    const val MAX_CATALOG_PAGES = 3
    const val CATALOG_TARGET_ITEMS = FORK_CARD_LIMIT * 3

    /**
     * Lookup order per source. AUTO (143) tries the add-on AI catalogs, then official's chain (Cxsmo's
     * order); a named fork source falls back to official's chain when it gives nothing, the way
     * official falls back to TMDB when the chosen tracker is not signed in.
     */
    fun chain(source: ForkPostPlaySource): List<PostPlayStep> = when (source) {
        ForkPostPlaySource.OFFICIAL -> listOf(PostPlayStep.OFFICIAL)
        ForkPostPlaySource.AUTO -> listOf(PostPlayStep.KURATO_AI, PostPlayStep.BINGECAT_AI, PostPlayStep.OFFICIAL)
        ForkPostPlaySource.KURATO_AI -> listOf(PostPlayStep.KURATO_AI, PostPlayStep.OFFICIAL)
        ForkPostPlaySource.BINGECAT_AI -> listOf(PostPlayStep.BINGECAT_AI, PostPlayStep.OFFICIAL)
        ForkPostPlaySource.MDBLIST -> listOf(PostPlayStep.MDBLIST, PostPlayStep.OFFICIAL)
    }

    /** Cards shown: official's 4 for its own source, [FORK_CARD_LIMIT] for any fork source. */
    fun cardLimit(source: ForkPostPlaySource, officialLimit: Int): Int =
        if (source == ForkPostPlaySource.OFFICIAL) officialLimit else FORK_CARD_LIMIT

    /**
     * The first step whose [usable] result is non-empty; a step that fails or times out counts as
     * empty. [usable] applies the caller's filters (current, watched, unreleased) so a step whose
     * items are all filtered away does not end the chain.
     */
    suspend fun <T> firstNonEmpty(
        steps: List<PostPlayStep>,
        load: suspend (PostPlayStep) -> List<T>?,
        usable: suspend (List<T>) -> List<T> = { it },
    ): List<T> {
        for (step in steps) {
            val items = try {
                load(step)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            val kept = items?.takeIf { it.isNotEmpty() }?.let { usable(it) }.orEmpty()
            if (kept.isNotEmpty()) return kept
        }
        return emptyList()
    }

    /** Cxsmo `buildKuratoRecommendationQuery` @ 3e0d0fa. */
    fun kuratoQuery(title: String, genres: List<String>, isSeries: Boolean): String {
        val name = title.trim().ifBlank { "this title" }
        val kind = if (isSeries) "TV shows" else "movies"
        val hint = topGenres(genres).takeIf { it.isNotEmpty() }
            ?.let { " Focus on similar tone and genres such as ${it.joinToString(", ")}." }
            .orEmpty()
        return "Recommend $kind similar to \"$name\".$hint Return titles only, not episodes."
    }

    /** Cxsmo `buildBingeCatRecommendationQuery` @ 3e0d0fa. */
    fun bingeCatQuery(title: String, genres: List<String>, isSeries: Boolean): String {
        val name = title.trim().ifBlank { "this title" }
        val kind = if (isSeries) "TV shows" else "movies"
        val hint = topGenres(genres).takeIf { it.isNotEmpty() }
            ?.let { " with a similar tone and genres such as ${it.joinToString(", ")}" }
            .orEmpty()
        return "$kind like \"$name\"$hint"
    }

    private fun topGenres(genres: List<String>): List<String> =
        genres.asSequence().map(String::trim).filter(String::isNotBlank).distinct().take(3).toList()

    /**
     * Cxsmo `normalizeAddonTrailerUrl` @ 3e0d0fa: an add-on's trailer value as a YouTube URL, or null.
     * Bare 11-character ids become watch URLs; anything else must be an http(s) YouTube link.
     */
    fun addonTrailerUrl(value: String): String? {
        val trimmed = value.trim()
        if (YOUTUBE_ID.matches(trimmed)) return "https://www.youtube.com/watch?v=$trimmed"
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return null
        val lower = trimmed.lowercase()
        return trimmed.takeIf { "youtube.com/" in lower || "youtu.be/" in lower }
    }

    /** Distinct usable add-on trailer URLs, in the order given. */
    fun addonTrailerUrls(values: Sequence<String?>): List<String> =
        values.filterNotNull().map(String::trim).filter(String::isNotBlank).distinct()
            .mapNotNull(::addonTrailerUrl).distinct().toList()

    private val YOUTUBE_ID = Regex("^[a-zA-Z0-9_-]{11}$")
}
