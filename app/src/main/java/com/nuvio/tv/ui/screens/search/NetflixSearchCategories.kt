@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.theme.NetflixThemeTokens
import com.nuvio.tv.ui.theme.withNuvioDescenderRoom
import com.nuvio.tv.ui.util.localizedGenreLabel

/**
 * NETFLIX_THEME: Netflix TV has no Categories tab; genres are browsed inside Search. Type and genre
 * chips drive Nuvio's existing discover session, so the grid below shows real add-on results.
 */
@Composable
internal fun NetflixSearchCategories(
    uiState: SearchUiState,
    onEvent: (SearchEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = NetflixThemeTokens
    val types = uiState.discoverCatalogs.map { it.type }.distinct().filter { it == "movie" || it == "series" }
    val genres = uiState.discoverCatalogs.firstOrNull { it.key == uiState.selectedDiscoverCatalogKey }?.genres.orEmpty()
    Column(modifier.fillMaxWidth().testTag("netflix_search_categories"), verticalArrangement = Arrangement.spacedBy(tokens.metadataGap)) {
        Text(
            text = stringResource(R.string.netflix_search_categories),
            color = tokens.textSecondary,
            style = TextStyle(fontFamily = tokens.fontFamily, fontSize = NetflixThemeTokens.searchKeyGlyph, fontWeight = FontWeight.Medium).withNuvioDescenderRoom(),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(tokens.cardGap)) {
            items(types, key = { "type_$it" }) { type ->
                NetflixCategoryChip(
                    label = stringResource(if (type == "movie") R.string.nav_movies else R.string.netflix_nav_shows),
                    selected = uiState.selectedDiscoverType == type,
                    tag = "netflix_search_type_$type",
                ) { if (uiState.selectedDiscoverType != type) onEvent(SearchEvent.SelectDiscoverType(type)) }
            }
            if (genres.isNotEmpty()) {
                item(key = "genre_all") {
                    NetflixCategoryChip(stringResource(R.string.netflix_search_all_genres), uiState.selectedDiscoverGenre == null, "netflix_search_genre_all") {
                        if (uiState.selectedDiscoverGenre != null) onEvent(SearchEvent.SelectDiscoverGenre(null))
                    }
                }
            }
            items(genres, key = { "genre_$it" }) { genre ->
                NetflixCategoryChip(localizedGenreLabel(genre), uiState.selectedDiscoverGenre == genre, "netflix_search_genre") {
                    if (uiState.selectedDiscoverGenre != genre) onEvent(SearchEvent.SelectDiscoverGenre(genre))
                }
            }
        }
    }
}

@Composable
private fun NetflixCategoryChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val tokens = NetflixThemeTokens
    Button(
        onClick = onClick,
        modifier = Modifier.height(NetflixThemeTokens.searchKeySize).testTag(tag),
        shape = ButtonDefaults.shape(shape = RoundedPill),
        colors = ButtonDefaults.colors(
            containerColor = if (selected) tokens.textPrimary.copy(alpha = .22f) else tokens.surface,
            contentColor = tokens.textPrimary,
            focusedContainerColor = tokens.focus,
            focusedContentColor = tokens.focusContent,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        contentPadding = PaddingValues(horizontal = tokens.previewPadding),
    ) {
        Text(label, maxLines = 1,
            style = TextStyle(fontFamily = tokens.fontFamily, fontSize = NetflixThemeTokens.searchKeyGlyph,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium).withNuvioDescenderRoom())
    }
}

private val RoundedPill = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
