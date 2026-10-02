package com.nuvio.tv.ui.screens.home

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.core.text.BidiFormatter
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.LibraryEntry
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.TmdbSettings
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.domain.model.WatchedItem
import com.nuvio.tv.fork.resource.MemoryTier
import com.nuvio.tv.ui.util.asStable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

private const val MY_LIST_ITEM_LIMIT = 24
private const val RECOMMENDATION_SETTLE_MS = 650L
private val RECOMMENDATION_TYPES = setOf("movie", "series", "tv")

@Immutable
internal data class NetflixRecommendationSeed(
    val contentId: String,
    val contentType: ContentType,
    val title: String,
    val language: String
)

@Immutable
internal data class NetflixRecommendations(
    val seed: NetflixRecommendationSeed,
    val items: List<MetaPreview>
)

@Immutable
internal data class NetflixHomeSources(
    val enabled: Boolean = false,
    val libraryItems: List<LibraryEntry> = emptyList(),
    val recommendations: NetflixRecommendations? = null,
    val profileId: Int? = null
)

internal fun netflixRecommendationLimit(tier: MemoryTier): Int = when (tier) {
    MemoryTier.STANDARD -> 12
    MemoryTier.CONSTRAINED -> 8
    MemoryTier.LOW_RAM -> 6
}

/** Only completed/explicitly watched history can label a shelf "Because you watched". */
internal fun netflixRecommendationSeed(
    watched: List<WatchedItem>,
    progress: List<WatchProgress>,
    settings: TmdbSettings,
    localeTag: String
): NetflixRecommendationSeed? {
    if (!settings.enabled || !settings.useMoreLikeThis) return null
    val candidates = watched.asSequence().map { Triple(it.contentId, it.contentType, it.title) to it.watchedAt } +
        progress.asSequence().filter { it.isCompleted() }
            .map { Triple(it.contentId, it.contentType, it.name) to it.lastWatched }
    val latest = candidates.filter { (item, _) ->
        item.first.isNotBlank() && item.third.isNotBlank() &&
            item.second.lowercase() in RECOMMENDATION_TYPES
    }.maxByOrNull { it.second }?.first ?: return null
    return NetflixRecommendationSeed(
        contentId = latest.first,
        contentType = if (latest.second.equals("movie", true)) ContentType.MOVIE else ContentType.SERIES,
        title = latest.third,
        language = settings.language.ifBlank { localeTag.ifBlank { "en" } }
    )
}

/**
 * Theme gating stops both source subscriptions and recommendation work when another theme is active.
 * One latest seed, one small memo and collectLatest cancellation bound the work within each profile.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal fun netflixHomeSourcesFlow(
    themeEnabled: Flow<Boolean>,
    libraryItems: Flow<List<LibraryEntry>>,
    seeds: Flow<NetflixRecommendationSeed?>,
    fetchRecommendations: suspend (NetflixRecommendationSeed) -> List<MetaPreview>
): Flow<NetflixHomeSources> {
    var memo: NetflixRecommendations? = null
    return themeEnabled.distinctUntilChanged().flatMapLatest { enabled ->
        if (!enabled) return@flatMapLatest flowOf(NetflixHomeSources())
        val recommendations: Flow<NetflixRecommendations?> = seeds.distinctUntilChanged().flatMapLatest { seed ->
            flow<NetflixRecommendations?> {
                emit(null)
                if (seed == null) return@flow
                val cached = memo?.takeIf { it.seed == seed }
                if (cached != null) {
                    emit(cached)
                    return@flow
                }
                delay(RECOMMENDATION_SETTLE_MS)
                val items = try {
                    fetchRecommendations(seed)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    emptyList()
                }
                val result = NetflixRecommendations(seed, items.distinctBy { "${it.apiType}:${it.id}" })
                memo = result
                emit(result)
            }
        }
        combine(
            libraryItems.map { items ->
                items.asSequence().filter { it.id.isNotBlank() && it.name.isNotBlank() }
                    .distinctBy { "${it.type}:${it.id}" }.take(MY_LIST_ITEM_LIMIT).toList()
            },
            recommendations
        ) { library, related -> NetflixHomeSources(true, library, related) }
            .onStart { emit(NetflixHomeSources(enabled = true)) }
    }
}

/** Adapts existing library/TMDB facts into the same Modern card/focus pipeline as add-on catalogs. */
internal fun buildNetflixSourceRows(
    sources: NetflixHomeSources,
    context: Context,
    showFullReleaseDate: Boolean,
    showImdbRatings: Boolean
): List<HeroCarouselRow> {
    if (!sources.enabled) return emptyList()
    val movieLabel = context.getString(R.string.type_movie)
    val seriesLabel = context.getString(R.string.type_series)
    fun buildRow(key: String, title: String, items: List<Pair<MetaPreview, String>>): HeroCarouselRow = HeroCarouselRow(
        key = key,
        title = title,
        globalRowIndex = -1,
        items = items.map { (item, origin) ->
            val row = CatalogRow(
                addonId = key, addonName = "", addonBaseUrl = origin,
                catalogId = key, catalogName = title, type = item.type, rawType = item.apiType,
                items = emptyList(), hasMore = false
            )
            buildCatalogItem(item, row, useLandscapePosters = true, occurrence = 0,
                strTypeMovie = movieLabel, strTypeSeries = seriesLabel,
                showFullReleaseDate = showFullReleaseDate, showImdbRatings = showImdbRatings)
                .copy(key = "$key:${item.apiType}:${item.id}")
        }.asStable()
    )
    return buildList {
        if (sources.libraryItems.isNotEmpty()) {
            add(buildRow("netflix_my_list", context.getString(R.string.netflix_my_list),
                sources.libraryItems.map { it.toMetaPreview() to it.addonBaseUrl.orEmpty() }))
        }
        sources.recommendations?.takeIf { it.items.isNotEmpty() }?.let { related ->
            val isRtl = context.resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
            val title = BidiFormatter.getInstance(isRtl).unicodeWrap(related.seed.title)
            add(buildRow("netflix_because_you_watched", context.getString(R.string.netflix_because_you_watched, title),
                related.items.map { it to it.sourceAddonBaseUrl.orEmpty() }))
        }
    }
}
