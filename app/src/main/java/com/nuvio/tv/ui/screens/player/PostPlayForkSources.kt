package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.data.mdblist.MDBLIST_WATCHLIST_KEY
import com.nuvio.tv.data.mdblist.MdbListTrackingLibraryProvider
import com.nuvio.tv.domain.model.Addon
import com.nuvio.tv.domain.model.CatalogDescriptor
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.repository.AddonRepository
import com.nuvio.tv.domain.repository.CatalogRepository
import com.nuvio.tv.fork.postplay.ForkPostPlaySettings
import com.nuvio.tv.fork.postplay.ForkPostPlaySource
import com.nuvio.tv.fork.postplay.PostPlaySources
import com.nuvio.tv.fork.postplay.PostPlayStep
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * G9c fork post-play sources beside official's chain (140–143). Kurato AI and BingeCat AI are
 * catalogs of add-ons the user installed, queried through official `CatalogRepository` (no new
 * endpoint); MDBList is the watchlist official's MDBList library already syncs. A source that is not
 * set up gives null, so the chain moves on.
 */
@Singleton
class PostPlayForkSources @Inject constructor(
    private val settings: ForkPostPlaySettings,
    private val addonRepository: AddonRepository,
    private val catalogRepository: CatalogRepository,
    private val mdbListLibrary: MdbListTrackingLibraryProvider,
) {
    val featureEnabled: Boolean get() = settings.featureEnabled

    suspend fun source(): ForkPostPlaySource = settings.sourceNow()

    suspend fun load(step: PostPlayStep, meta: Meta, contentType: ContentType): List<MetaPreview>? = when (step) {
        PostPlayStep.KURATO_AI, PostPlayStep.BINGECAT_AI -> withTimeoutOrNull(CATALOG_TIMEOUT_MS) {
            loadAiCatalog(step, meta, contentType)
        }
        PostPlayStep.MDBLIST -> withTimeoutOrNull(MDBLIST_TIMEOUT_MS) { loadMdbListWatchlist(contentType) }
        PostPlayStep.OFFICIAL -> null
    }

    private suspend fun loadAiCatalog(step: PostPlayStep, meta: Meta, contentType: ContentType): List<MetaPreview>? {
        val (addon, catalog) = findAiCatalog(addonRepository.getInstalledAddons().first(), contentType, step) ?: return null
        val isSeries = contentType == ContentType.SERIES
        val query = if (step == PostPlayStep.KURATO_AI) {
            PostPlaySources.kuratoQuery(meta.name, meta.genres, isSeries)
        } else {
            PostPlaySources.bingeCatQuery(meta.name, meta.genres, isSeries)
        }
        val pageSize = (catalog.pageSize ?: PostPlaySources.DEFAULT_CATALOG_PAGE_SIZE).coerceIn(1, PostPlaySources.MAX_CATALOG_PAGE_SIZE)
        val items = LinkedHashMap<String, MetaPreview>()
        for (page in 0 until PostPlaySources.MAX_CATALOG_PAGES) {
            val result = catalogRepository.getCatalog(
                addonBaseUrl = addon.baseUrl,
                addonId = addon.id,
                addonName = addon.displayName,
                catalogId = catalog.id,
                catalogName = catalog.name,
                type = catalog.apiType,
                skip = page * pageSize,
                skipStep = pageSize,
                extraArgs = mapOf("search" to query),
                supportsSkip = true,
            ).first { it !is NetworkResult.Loading }
            val row = when (result) {
                is NetworkResult.Success -> result.data
                else -> null
            } ?: break
            val before = items.size
            row.items.forEach { items.putIfAbsent("${it.apiType}:${it.id}".lowercase(), it) }
            if (items.size == before || !row.hasMore || row.items.size < pageSize ||
                items.size >= PostPlaySources.CATALOG_TARGET_ITEMS
            ) {
                break
            }
        }
        return items.values.toList()
    }

    /** Cxsmo's MDBList source is the user's watchlist (`loadMdbListCandidates` @ 3e0d0fa). */
    private suspend fun loadMdbListWatchlist(contentType: ContentType): List<MetaPreview>? {
        if (!mdbListLibrary.isAuthenticated.first()) return null
        val isMovie = contentType == ContentType.MOVIE
        return mdbListLibrary.items.first()
            .asSequence()
            .filter { MDBLIST_WATCHLIST_KEY in it.listKeys }
            .filter { (resolvePostPlayContentType(it.type) == ContentType.MOVIE) == isMovie }
            .map { it.toMetaPreview().copy(imdbId = it.imdbId) }
            .distinctBy { "${it.apiType}:${it.id}" }
            .toList()
    }

    private companion object {
        const val CATALOG_TIMEOUT_MS = 15_000L
        const val MDBLIST_TIMEOUT_MS = 10_000L
    }
}

/**
 * Cxsmo `findKuratoAiCatalog` / `findBingeCatAiCatalog` @ 3e0d0fa: the enabled add-on catalog for
 * [step] and [contentType], exact catalog id first, add-ons whose id or name names the service first.
 */
internal fun findAiCatalog(
    addons: List<Addon>,
    contentType: ContentType,
    step: PostPlayStep,
): Pair<Addon, CatalogDescriptor>? {
    val isSeries = contentType == ContentType.SERIES
    val (exactId, idPart, names) = when (step) {
        PostPlayStep.KURATO_AI -> Triple(
            if (isSeries) "kurato-ai-discover-series" else "kurato-ai-discover-movie",
            "kurato-ai-discover",
            listOf("kurato"),
        )
        PostPlayStep.BINGECAT_AI -> Triple(
            if (isSeries) "aicat_search_series" else "aicat_search_movie",
            "aicat_search",
            listOf("aicat", "bingecat"),
        )
        else -> return null
    }
    val preferred = addons.filter { it.enabled }.sortedByDescending { addon ->
        names.any { name ->
            addon.id.contains(name, true) || addon.name.contains(name, true) || addon.displayName.contains(name, true)
        }
    }
    fun matchesType(catalog: CatalogDescriptor) = resolvePostPlayContentType(catalog.apiType, catalog.type) == contentType
    preferred.forEach { addon ->
        addon.catalogs.firstOrNull { it.id.equals(exactId, true) && matchesType(it) }?.let { return addon to it }
    }
    return preferred.firstNotNullOfOrNull { addon ->
        addon.catalogs.firstOrNull { it.id.contains(idPart, true) && matchesType(it) }?.let { addon to it }
    }
}
