package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.ui.util.asStable

/**
 * NETFLIX_THEME Movies / Shows: Netflix gives both tabs the Home layout (hero, rows, focused
 * expansion) narrowed to one type. Rows keep only cards of that type; a row left empty disappears,
 * except a catalog of that type that is still loading so its skeleton keeps its place.
 */
internal fun ModernHomePresentationState.filteredToType(type: String): ModernHomePresentationState {
    val filtered = rows.list.mapNotNull { row ->
        val items = row.items.list.filter { item -> matchesContentType(type, item.contentType()) }
        when {
            items.isNotEmpty() -> if (items.size == row.items.list.size) row else row.copy(items = items.asStable())
            row.isLoading && matchesContentType(type, row.apiType) -> row
            else -> null
        }
    }
    return ModernHomePresentationState(rows = filtered.asStable(), lookups = buildCarouselRowLookups(filtered))
}

internal fun matchesContentType(type: String, candidate: String?): Boolean =
    if (isSeriesType(type)) isSeriesType(candidate) else candidate.equals(type, ignoreCase = true)

private fun ModernCarouselItem.contentType(): String? = when (val payload = payload) {
    is ModernPayload.Catalog -> payload.itemType
    is ModernPayload.ContinueWatching -> when (val item = payload.item) {
        is ContinueWatchingItem.InProgress -> item.progress.contentType
        is ContinueWatchingItem.NextUp -> item.info.contentType
    }
    is ModernPayload.CollectionFolder -> null
}
