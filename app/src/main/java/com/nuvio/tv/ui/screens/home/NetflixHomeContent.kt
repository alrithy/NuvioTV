@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.nuvio.tv.ui.screens.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.fork.resource.AdaptiveResources
import com.nuvio.tv.ui.theme.NetflixThemeTokens
import com.nuvio.tv.ui.theme.netflixMetadataLine
import com.nuvio.tv.ui.util.contentTextDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import com.nuvio.tv.ui.theme.withNuvioDescenderRoom

/*
 * NETFLIX_THEME Home, per the maintainer reference correction (docs/NETFLIX_TV_2026_PARITY_AUDIT.md §0):
 *   TOP state:      top nav → large rounded hero CARD → category shortcuts → rows
 *   SCROLLED state: the hero scrolls away with content; the active row becomes the page, its focused
 *                   portrait poster expands INLINE into a landscape card with that item's facts beneath it,
 *                   and the next row stays partly visible.
 * Presentation only: rows, items, actions, load-more and focus saving are the existing Modern home owners.
 * There is no floating preview popup, so nothing can collide with the hero (the old 04b overlap).
 */

private const val HERO_KEY = "netflix_hero"
private const val CATEGORY_KEY = "netflix_categories"
private const val LOAD_MORE_THRESHOLD = 4
private const val LOGO_LOADING = 0
private const val LOGO_SHOWN = 1
private const val LOGO_FAILED = 2

/*
 * FOCUS COMFORT ZONE (docs/NETFLIX_REFERENCE_FIDELITY.md §Browse rows). The row scrolls only when the
 * focused card, at its final expanded width, would not sit inside the zone: the viewport minus a
 * neighbour "peek" on each side. Within the zone the viewport stays still, so a middle poster expands
 * in place. Offsets are LazyRow main-axis offsets measured from the reading start, so the same policy
 * maps to the mirrored physical bounds in RTL. The first and last items reach the edges because the
 * list clamps the scroll. The result depends only on positions, so repeated focus cannot oscillate.
 */
internal fun netflixComfortScrollDelta(
    finalStart: Float,
    finalEnd: Float,
    viewportStart: Float,
    viewportEnd: Float,
    peek: Float,
): Float {
    val span = finalEnd - finalStart
    // Never ask for a zone narrower than the card itself.
    val usablePeek = peek.coerceIn(0f, ((viewportEnd - viewportStart - span) / 2f).coerceAtLeast(0f))
    val zoneStart = viewportStart + usablePeek
    val zoneEnd = viewportEnd - usablePeek
    val delta = when {
        finalStart < zoneStart -> finalStart - zoneStart
        finalEnd > zoneEnd -> minOf(finalEnd - zoneEnd, finalStart - zoneStart)
        else -> 0f
    }
    return if (kotlin.math.abs(delta) < 1f) 0f else delta
}

/**
 * Predicts the focused card's final span: every earlier visible card settles to its idle width (the
 * previous selection collapsing moves this card toward the reading start). Null when the card is not
 * laid out; the caller then falls back to bringing it into view.
 */
internal fun netflixComfortDeltaFor(
    info: LazyListLayoutInfo,
    index: Int,
    landscapeRow: Boolean,
    portraitPx: Float,
    expandedPx: Float,
    peekPx: Float,
): Float? {
    val target = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return null
    val idlePx = if (landscapeRow) expandedPx else portraitPx
    val shift = info.visibleItemsInfo.filter { it.index < index }.sumOf { (idlePx - it.size).toDouble() }.toFloat()
    val start = target.offset + shift
    return netflixComfortScrollDelta(start, start + expandedPx, info.viewportStartOffset.toFloat(),
        info.viewportEndOffset.toFloat(), peekPx)
}

/** A row the reference draws with landscape cards rather than portrait posters. */
internal fun netflixRowIsLandscape(row: HeroCarouselRow): Boolean =
    row.items.list.any { it.payload is ModernPayload.ContinueWatching }

/** Genuine category shortcuts: the profile's own add-on catalogs that the See-all owner can open. */
internal fun netflixCategoryShortcuts(rows: List<HeroCarouselRow>): List<HeroCarouselRow> =
    rows.filter { !netflixRowIsLandscape(it) && !it.catalogId.isNullOrBlank() && !it.addonId.isNullOrBlank() && !it.apiType.isNullOrBlank() }
        .distinctBy { it.key }

@Composable
internal fun NetflixHomeContent(
    uiState: HomeUiState,
    rows: List<HeroCarouselRow>,
    focusState: HomeScreenFocusState,
    onNavigateToDetail: (String, String, String) -> Unit,
    onPlayClick: (String, String, String) -> Unit,
    onContinueWatchingClick: (ContinueWatchingItem) -> Unit,
    onLoadMoreCatalog: (String, String, String) -> Unit,
    onNavigateToCatalogSeeAll: (String, String, String) -> Unit,
    onItemFocus: (MetaPreview) -> Unit,
    onFocusedRowKeyChanged: (String?) -> Unit,
    onSaveFocusState: (Int, Int, String?, Map<String, String>, Map<String, Int>, Map<String, String>, Int, Int) -> Unit,
) {
    val tokens = NetflixThemeTokens
    val home = NetflixThemeTokens.Home
    val context = LocalContext.current
    val movieLabel = stringResource(R.string.type_movie)
    val seriesLabel = stringResource(R.string.type_series)
    val categories = remember(rows) { netflixCategoryShortcuts(rows) }
    val heroItem: ModernCarouselItem? = remember(uiState.heroItems, rows, movieLabel, seriesLabel) {
        uiState.heroItems.firstOrNull()?.let { item ->
            val source = CatalogRow("netflix_hero", "", item.sourceAddonBaseUrl.orEmpty(), "hero", "", item.type,
                rawType = item.apiType, items = emptyList(), hasMore = false)
            buildCatalogItem(item, source, true, 0, movieLabel, seriesLabel)
        } ?: rows.firstOrNull { !netflixRowIsLandscape(it) }?.items?.list?.firstOrNull()
    }
    val leadingItems = (if (heroItem != null) 1 else 0) + (if (categories.isNotEmpty()) 1 else 0)
    val columnState = rememberLazyListState()
    // Created lazily during composition, so a plain map (not snapshot state) caches each row's scroll state.
    val rowStates = remember { mutableMapOf<String, LazyListState>() }
    val focusedIndexByRow = remember { mutableStateMapOf<String, Int>() }
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    var activeRowKey by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val membership = uiState.posterLibraryMembership

    fun requesterFor(key: String) = requesters.getOrPut(key) { FocusRequester() }
    fun rowState(key: String) = rowStates.getOrPut(key) { LazyListState() }

    fun saveFocus() {
        val rowKey = activeRowKey
        val itemKeys = rows.associate { row -> row.key to (row.items.list.getOrNull(focusedIndexByRow[row.key] ?: 0)?.key ?: "") }
        val scrolls = rows.associate { row -> row.key to (rowStates[row.key]?.firstVisibleItemIndex ?: 0) }
        val rowIndex = rows.indexOfFirst { it.key == rowKey }
        onSaveFocusState(columnState.firstVisibleItemIndex, columnState.firstVisibleItemScrollOffset, rowKey,
            itemKeys, scrolls, emptyMap(), rowIndex, rowKey?.let { focusedIndexByRow[it] } ?: 0)
    }
    val latestSave by rememberUpdatedState(::saveFocus)
    DisposableEffect(Unit) { onDispose { latestSave() } }

    // Return from Details: bring the saved row/item into view, then focus that exact card.
    LaunchedEffect(Unit) {
        if (!focusState.hasSavedFocus) return@LaunchedEffect
        val rowKey = focusState.focusedRowKey ?: return@LaunchedEffect
        val rowIndex = rows.indexOfFirst { it.key == rowKey }.takeIf { it >= 0 } ?: return@LaunchedEffect
        val row = rows[rowIndex]
        val itemIndex = focusState.focusedItemKeyByRow[rowKey]
            ?.let { key -> row.items.list.indexOfFirst { it.key == key } }?.takeIf { it >= 0 } ?: 0
        focusedIndexByRow[rowKey] = itemIndex
        columnState.scrollToItem(leadingItems + rowIndex)
        rowState(rowKey).scrollToItem(itemIndex)
        val key = row.items.list[itemIndex].key
        repeat(home.restoreFrames) {
            withFrameNanos { }
            if (runCatching { requesterFor(key).requestFocus(FocusDirection.Enter) }.getOrDefault(false)) return@LaunchedEffect
        }
    }

    // Home owns its scrolling explicitly (two-state column, focus comfort zone in rows). The platform's
    // TV pivot bring-into-view would otherwise move the hero up as soon as Play gains focus and drag
    // rows toward a fixed pivot, fighting the comfort zone. The category strip keeps the default.
    val platformBringIntoView = LocalBringIntoViewSpec.current
    CompositionLocalProvider(LocalBringIntoViewSpec provides NetflixExplicitScroll) {
    LazyColumn(
        state = columnState,
        modifier = Modifier.fillMaxSize().background(tokens.background).testTag("netflix_home"),
        contentPadding = PaddingValues(bottom = home.bottomPadding),
        verticalArrangement = Arrangement.spacedBy(home.sectionGap),
    ) {
        if (heroItem != null) {
            item(key = HERO_KEY) {
                NetflixHeroCard(
                    item = heroItem,
                    inLibrary = heroItem.metaPreview?.let { membership[homeItemStatusKey(it.id, it.apiType)] } == true,
                    showImdbRatings = uiState.homeImdbRatingsVisibility.showRatings,
                    onFocused = {
                        activeRowKey = null; onFocusedRowKeyChanged(null); heroItem.metaPreview?.let(onItemFocus)
                        scope.launch { columnState.animateScrollToItem(0) }
                    },
                    onPlay = { heroAction(heroItem, onPlayClick, onContinueWatchingClick) },
                    onMoreInfo = { saveFocus(); heroAction(heroItem, onNavigateToDetail, onContinueWatchingClick) },
                )
            }
        }
        if (categories.isNotEmpty()) {
            item(key = CATEGORY_KEY) {
                CompositionLocalProvider(LocalBringIntoViewSpec provides platformBringIntoView) {
                    NetflixCategoryStrip(categories,
                        onFocused = {
                            if (activeRowKey != null) { activeRowKey = null; onFocusedRowKeyChanged(null) }
                            // The 400 dp hero leaves only a peek of the strip in the top state; a focused
                            // strip is revealed by the minimal scroll, keeping as much hero as possible.
                            scope.launch { columnState.revealItem(CATEGORY_KEY) }
                        }) { row ->
                        saveFocus()
                        onNavigateToCatalogSeeAll(row.catalogId!!, row.addonId!!, row.apiType!!)
                    }
                }
            }
        }
        itemsIndexed(rows, key = { _, row -> row.key }) { rowIndex, row ->
            NetflixBrowseRow(
                row = row,
                landscape = netflixRowIsLandscape(row),
                active = activeRowKey == row.key,
                listState = rowState(row.key),
                focusedIndex = focusedIndexByRow[row.key] ?: 0,
                membership = membership,
                showImdbRatings = uiState.homeImdbRatingsVisibility.showRatings,
                requesterFor = ::requesterFor,
                onItemFocused = { index, item ->
                    focusedIndexByRow[row.key] = index
                    if (activeRowKey != row.key) {
                        activeRowKey = row.key
                        onFocusedRowKeyChanged(row.key)
                    }
                    item.metaPreview?.let(onItemFocus)
                    // The focused row becomes the page: it rises under the top navigation, which pushes
                    // the hero out of view. Horizontal scrolling is the row's focus comfort zone (below).
                    scope.launch { columnState.animateScrollToItem(leadingItems + rowIndex) }
                    if (row.hasMore && !row.isLoading && index >= row.items.list.size - LOAD_MORE_THRESHOLD) {
                        val catalogId = row.catalogId; val addonId = row.addonId; val apiType = row.apiType
                        if (catalogId != null && addonId != null && apiType != null) onLoadMoreCatalog(catalogId, addonId, apiType)
                    }
                },
                onClick = { item ->
                    saveFocus()
                    when (val payload = item.payload) {
                        is ModernPayload.Catalog -> onNavigateToDetail(payload.itemId, payload.itemType, payload.addonBaseUrl)
                        is ModernPayload.ContinueWatching -> onContinueWatchingClick(payload.item)
                        is ModernPayload.CollectionFolder -> Unit
                    }
                },
            )
        }
    }
    }
}

/** Minimal scroll that shows the whole item [key]; nothing moves when it is already fully visible. */
private suspend fun LazyListState.revealItem(key: Any) {
    val info = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
    val overflow = info.offset + info.size - layoutInfo.viewportEndOffset
    val underflow = info.offset - layoutInfo.viewportStartOffset
    when {
        underflow < 0 -> animateScrollBy(underflow.toFloat())
        overflow > 0 -> animateScrollBy(overflow.toFloat())
    }
}

/** Scrolling in Netflix Home is decided by its own policies, never by the platform pivot. */
private object NetflixExplicitScroll : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

private fun heroAction(
    item: ModernCarouselItem,
    catalog: (String, String, String) -> Unit,
    continueWatching: (ContinueWatchingItem) -> Unit,
) {
    when (val payload = item.payload) {
        is ModernPayload.Catalog -> catalog(payload.itemId, payload.itemType, payload.addonBaseUrl)
        is ModernPayload.ContinueWatching -> continueWatching(payload.item)
        is ModernPayload.CollectionFolder -> Unit
    }
}

/** The reference hero: a large rounded media card inside the safe margins, not a full-bleed backdrop. */
@Composable
private fun NetflixHeroCard(
    item: ModernCarouselItem,
    inLibrary: Boolean,
    showImdbRatings: Boolean,
    onFocused: () -> Unit,
    onPlay: () -> Unit,
    onMoreInfo: () -> Unit,
) {
    val tokens = NetflixThemeTokens
    val home = NetflixThemeTokens.Home
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val preview = item.heroPreview
    val art = preview.backdrop ?: item.metaPreview?.background ?: item.imageUrl ?: preview.poster
    Box(
        Modifier.fillMaxWidth().padding(horizontal = home.heroInset).height(home.heroHeight)
            .clip(RoundedCornerShape(home.heroRadius)).background(tokens.surface)
            .onFocusChanged { if (it.hasFocus) onFocused() }
            .testTag("netflix_home_hero_card"),
    ) {
        // The decode never exceeds the AdaptiveResources tier's backdrop cap (LOW_RAM → 1280 px).
        val density = LocalDensity.current
        val capWidth = with(density) { netflixHomePreviewPolicy(AdaptiveResources.policy.tier).heroMaxWidthPx.toDp() }
        val requestWidth = if (home.heroRequestWidth < capWidth) home.heroRequestWidth else capWidth
        BoundedImage(art, requestWidth, home.heroHeight * (requestWidth / home.heroRequestWidth), Modifier.fillMaxSize())
        // Gradients only where the text needs them: the reading-start side and the lower edge.
        Box(Modifier.fillMaxSize().background(tokens.heroSideGradient(rtl)).graphicsAlpha(home.heroSideScrimAlpha))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, tokens.background.copy(alpha = home.heroBottomScrimAlpha)))))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth(home.heroTextWidthFraction).padding(home.heroPadding)
                .testTag("netflix_home_hero"),
            verticalArrangement = Arrangement.spacedBy(tokens.metadataGap),
        ) {
            // The real title logo is preferred; the text title stands in while it decodes and when it is
            // missing or fails, so the hero never shows an empty title slot.
            var logoState by remember(preview.logo) { mutableIntStateOf(if (preview.logo.isNullOrBlank()) LOGO_FAILED else LOGO_LOADING) }
            Box(if (logoState == LOGO_FAILED) Modifier else Modifier.height(home.heroLogoHeight), contentAlignment = Alignment.BottomStart) {
                if (logoState != LOGO_SHOWN) {
                    Text(
                        preview.title,
                        style = TextStyle(fontFamily = tokens.fontFamily, fontSize = home.heroTitleSize, fontWeight = FontWeight.Bold,
                            textDirection = preview.title.contentTextDirection()).withNuvioDescenderRoom(),
                        color = tokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (logoState != LOGO_FAILED) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(preview.logo).build(),
                        contentDescription = preview.title,
                        contentScale = ContentScale.Fit,
                        alignment = if (rtl) Alignment.BottomEnd else Alignment.BottomStart,
                        onSuccess = { logoState = LOGO_SHOWN },
                        onError = { logoState = LOGO_FAILED },
                        modifier = Modifier.height(home.heroLogoHeight).widthIn(max = home.heroLogoMaxWidth)
                            .graphicsAlpha(if (logoState == LOGO_SHOWN) 1f else 0f).testTag("netflix_hero_logo"),
                    )
                }
            }
            netflixCallout(item.payload, inLibrary)?.let { NetflixCalloutChip(it) }
            NetflixFactsLine(preview, showImdbRatings)
            // Reference: synopsis is not dominant in the initial hero state — one line at most.
            preview.description?.takeIf(String::isNotBlank)?.let {
                Text(it, style = TextStyle(fontFamily = tokens.fontFamily, fontSize = home.heroSynopsisSize,
                    textDirection = it.contentTextDirection()).withNuvioDescenderRoom(), color = tokens.textSecondary, maxLines = home.heroSynopsisLines,
                    overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.padding(top = tokens.metadataGap / 2), horizontalArrangement = Arrangement.spacedBy(tokens.actionGap)) {
                NetflixHeroButton(Icons.Default.PlayArrow, stringResource(R.string.hero_play), primary = true, onClick = onPlay,
                    modifier = Modifier.testTag("netflix_hero_play").focusRequester(LocalContentFocusRequester.current))
                NetflixHeroButton(Icons.Default.Info, stringResource(R.string.netflix_more_info), primary = false, onClick = onMoreInfo,
                    modifier = Modifier.testTag("netflix_hero_more_info"))
            }
        }
    }
}

private fun Modifier.graphicsAlpha(alpha: Float) = this.then(Modifier.graphicsLayer { this.alpha = alpha })

@Composable
private fun NetflixHeroButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, primary: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = NetflixThemeTokens
    Button(
        onClick = onClick,
        modifier = modifier.height(tokens.buttonHeight),
        shape = ButtonDefaults.shape(tokens.buttonShape),
        colors = ButtonDefaults.colors(
            containerColor = if (primary) tokens.focus else tokens.focus.copy(alpha = tokens.secondaryActionFillAlpha),
            contentColor = if (primary) tokens.focusContent else tokens.textPrimary,
            focusedContainerColor = tokens.focus, focusedContentColor = tokens.focusContent,
        ),
        border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(NetflixThemeTokens.Home.focusOutline, tokens.focus.copy(alpha = .9f)))),
        scale = ButtonDefaults.scale(focusedScale = tokens.episodeFocusScale),
        contentPadding = PaddingValues(horizontal = tokens.previewPadding * 1.5f),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = tokens.metadataGap).height(tokens.navigationIconSize))
        Text(label, style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.buttonText, fontWeight = FontWeight.Medium).withNuvioDescenderRoom())
    }
}

/** Category shortcuts below the hero: the profile's real catalogs, opened through See all. */
@Composable
private fun NetflixCategoryStrip(categories: List<HeroCarouselRow>, onFocused: () -> Unit, onOpen: (HeroCarouselRow) -> Unit) {
    val tokens = NetflixThemeTokens
    val home = NetflixThemeTokens.Home
    LazyRow(
        modifier = Modifier.onFocusChanged { if (it.hasFocus) onFocused() }.testTag("netflix_category_strip"),
        // Aligned with the hero card's edges, as in the reference top state.
        contentPadding = PaddingValues(horizontal = home.heroInset, vertical = home.focusOutline * 2),
        horizontalArrangement = Arrangement.spacedBy(home.categoryGap),
    ) {
        itemsIndexed(categories, key = { _, row -> "category:" + row.key }) { _, row ->
            Card(
                onClick = { onOpen(row) },
                modifier = Modifier.widthIn(min = home.categoryMinWidth, max = home.categoryMaxWidth).height(home.categoryHeight)
                    .testTag("netflix_category_${row.key}"),
                shape = CardDefaults.shape(RoundedCornerShape(home.categoryRadius)),
                colors = CardDefaults.colors(containerColor = tokens.surfaceRaised, focusedContainerColor = tokens.surfaceMuted),
                border = CardDefaults.border(focusedBorder = Border(BorderStroke(home.focusOutline, tokens.focus), shape = RoundedCornerShape(home.categoryRadius))),
                scale = CardDefaults.scale(focusedScale = 1f),
            ) {
                // Width follows the label (bounded), not a fixed settings-like block.
                Box(Modifier.fillMaxHeight().widthIn(min = home.categoryMinWidth).padding(horizontal = home.categoryPadding),
                    contentAlignment = Alignment.Center) {
                    Text(row.title, style = TextStyle(fontFamily = tokens.fontFamily, fontSize = home.categoryTextSize,
                        fontWeight = FontWeight.Medium, textDirection = row.title.contentTextDirection()).withNuvioDescenderRoom(),
                        color = tokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun NetflixBrowseRow(
    row: HeroCarouselRow,
    landscape: Boolean,
    active: Boolean,
    listState: LazyListState,
    focusedIndex: Int,
    membership: Map<String, Boolean>,
    showImdbRatings: Boolean,
    requesterFor: (String) -> FocusRequester,
    onItemFocused: (Int, ModernCarouselItem) -> Unit,
    onClick: (ModernCarouselItem) -> Unit,
) {
    val tokens = NetflixThemeTokens
    val home = NetflixThemeTokens.Home
    var focusedKey by remember(row.key) { mutableStateOf<String?>(null) }
    val rowScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var comfortJob by remember(row.key) { mutableStateOf<Job?>(null) }
    fun keepComfortable(index: Int) {
        comfortJob?.cancel()
        comfortJob = rowScope.launch {
            // Decide on settled geometry: under rapid key repeat, cards are mid-transform and the
            // focus system's own bring-into-view scroll is in flight; measuring then made the row
            // swing back. Waiting for the row to settle (bounded by the transform time) keeps the
            // decision a pure function of the final layout.
            val expandedPx = with(density) { home.rowCardExpandedWidth.toPx() }
            val idlePx = if (landscape) expandedPx else with(density) { home.rowCardIdleWidth.toPx() }
            withTimeoutOrNull(home.expandMillis + 240L) {
                snapshotFlow {
                    val visible = listState.layoutInfo.visibleItemsInfo
                    !listState.isScrollInProgress && visible.all { item ->
                        kotlin.math.abs(item.size - if (item.index == index) expandedPx else idlePx) < 1.5f
                    }
                }.first { it }
            }
            val delta = with(density) {
                netflixComfortDeltaFor(listState.layoutInfo, index, landscape,
                    portraitPx = home.rowCardIdleWidth.toPx(),
                    expandedPx = home.rowCardExpandedWidth.toPx(),
                    peekPx = (home.rowCardIdleWidth * home.comfortPeekFraction).toPx())
            }
            when {
                delta == null -> listState.animateScrollToItem(index)
                delta != 0f -> listState.animateScrollBy(delta, tween(if (AdaptiveResources.policy.isLowRam) 0 else home.expandMillis))
            }
        }
    }
    Column(Modifier.testTag("netflix_row_${row.key}"), verticalArrangement = Arrangement.spacedBy(tokens.rowTitleGap)) {
        Text(row.title, style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.rowHeader, fontWeight = FontWeight.Bold,
            textDirection = row.title.contentTextDirection()).withNuvioDescenderRoom(), color = tokens.textPrimary,
            modifier = Modifier.padding(horizontal = tokens.safeMargin))
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = tokens.safeMargin, vertical = home.focusOutline * 2),
            horizontalArrangement = Arrangement.spacedBy(home.cardGap),
        ) {
            itemsIndexed(row.items.list, key = { _, item -> item.key }) { index, item ->
                NetflixBrowseCard(
                    item = item,
                    landscapeRow = landscape,
                    requester = requesterFor(item.key),
                    onFocusChanged = { focused ->
                        if (focused) { focusedKey = item.key; keepComfortable(index); onItemFocused(index, item) }
                        else if (focusedKey == item.key) focusedKey = null
                    },
                    onClick = { onClick(item) },
                )
            }
        }
        // The focused item's facts live directly under the row, tied to that one item (audit §0 F).
        val focusedItem = row.items.list.firstOrNull { it.key == focusedKey }
            ?: row.items.list.getOrNull(focusedIndex)?.takeIf { active }
        if (active && focusedItem != null) {
            NetflixFocusedFacts(focusedItem, membership, showImdbRatings,
                Modifier.padding(horizontal = tokens.safeMargin).testTag("netflix_focused_facts_${focusedItem.key}"))
        }
    }
}

/**
 * Idle: a portrait poster. Focused: the same card widens INLINE to a landscape card at the row height
 * (siblings move aside inside the LazyRow; nothing floats). Landscape rows (Continue Watching) keep
 * their landscape shape and only gain the outline.
 */
@Composable
private fun NetflixBrowseCard(
    item: ModernCarouselItem,
    landscapeRow: Boolean,
    requester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val tokens = NetflixThemeTokens
    val home = NetflixThemeTokens.Home
    val lowRam = AdaptiveResources.policy.isLowRam
    var focused by remember { mutableStateOf(false) }
    val height = home.rowCardHeight
    val portraitWidth = home.rowCardIdleWidth
    val landscapeWidth = home.rowCardExpandedWidth
    val expanded = focused && !landscapeRow
    val targetWidth = if (landscapeRow || expanded) landscapeWidth else portraitWidth
    // Bounded, interruptible transform; a new focus target retargets the animation, never queues it.
    val width by animateDpAsState(targetWidth, tween(if (lowRam) 0 else home.expandMillis), label = "netflixInlineExpand")
    val preview = item.heroPreview
    val poster = preview.poster ?: item.metaPreview?.poster ?: item.imageUrl
    val wide = item.metaPreview?.landscapePoster ?: preview.backdrop ?: item.metaPreview?.background ?: item.imageUrl
    val shape = RoundedCornerShape(home.cardRadius)
    Card(
        onClick = onClick,
        modifier = Modifier.width(width).height(height)
            .zIndex(if (focused) 1f else 0f)
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused; onFocusChanged(it.isFocused) }
            .testTag("netflix_home_card_focus_${item.key}"),
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(containerColor = tokens.surfaceRaised, focusedContainerColor = tokens.surfaceRaised),
        border = CardDefaults.border(focusedBorder = Border(BorderStroke(home.focusOutline, tokens.focus.copy(alpha = tokens.focusOutlineAlpha)), shape = shape)),
        scale = CardDefaults.scale(focusedScale = 1f),
    ) {
        Box(Modifier.fillMaxSize()) {
            // A node holds one test tag (the card's identity), so the expanded state is marked inside it.
            if (expanded) Box(Modifier.fillMaxSize().testTag("netflix_inline_expanded"))
            if (landscapeRow) {
                BoundedImage(wide ?: poster, landscapeWidth, height, Modifier.fillMaxSize())
            } else {
                // The poster stays underneath while the landscape art decodes: no blank or wrong-art frame.
                BoundedImage(poster ?: wide, portraitWidth, height, Modifier.fillMaxSize())
                if (expanded) BoundedImage(wide ?: poster, landscapeWidth, height, Modifier.fillMaxSize())
            }
            if (expanded || landscapeRow) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent,
                    tokens.background.copy(alpha = .85f)))))
                Text(item.title, style = TextStyle(fontFamily = tokens.fontFamily, fontSize = home.cardTitleSize,
                    fontWeight = FontWeight.Medium, textDirection = item.title.contentTextDirection()).withNuvioDescenderRoom(),
                    color = tokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(tokens.previewPadding))
            }
            val progress = (item.payload as? ModernPayload.ContinueWatching)?.item?.let { cw ->
                (cw as? ContinueWatchingItem.InProgress)?.progress?.progressPercentage
            }
            if (progress != null) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(tokens.progressHeight).background(Color.White.copy(alpha = .3f)))
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.coerceIn(.02f, 1f)).height(tokens.progressHeight)
                    .background(tokens.progress))
            }
        }
    }
}

/** Title, factual callout, compact facts and a short synopsis for the one focused item. */
@Composable
private fun NetflixFocusedFacts(item: ModernCarouselItem, membership: Map<String, Boolean>, showImdbRatings: Boolean, modifier: Modifier) {
    val tokens = NetflixThemeTokens
    val home = NetflixThemeTokens.Home
    val preview = item.heroPreview
    val inLibrary = item.metaPreview?.let { membership[homeItemStatusKey(it.id, it.apiType)] } == true
    Column(modifier.widthIn(max = home.factsMaxWidth), verticalArrangement = Arrangement.spacedBy(tokens.metadataGap / 2)) {
        netflixCallout(item.payload, inLibrary)?.let { NetflixCalloutChip(it) }
        NetflixFactsLine(preview, showImdbRatings)
        preview.description?.takeIf(String::isNotBlank)?.let {
            Text(it, style = TextStyle(fontFamily = tokens.fontFamily, fontSize = home.factsSynopsisSize,
                lineHeight = home.factsSynopsisLineHeight, textDirection = it.contentTextDirection()).withNuvioDescenderRoom(),
                color = tokens.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Type · year · seasons/runtime · age · IMDb — each isolated for bidi, missing facts omitted. */
@Composable
internal fun NetflixFactsLine(preview: HeroPreview, showImdbRatings: Boolean) {
    val tokens = NetflixThemeTokens
    val duration = if (preview.isSeries && preview.seasonCount != null)
        pluralStringResource(R.plurals.netflix_seasons, preview.seasonCount, preview.seasonCount) else preview.runtimeText
    val facts = netflixMetadataLine(listOf(
        preview.contentTypeText, preview.yearText, duration, preview.ageRatingText,
        if (showImdbRatings) preview.imdbText?.takeIf(String::isNotBlank)?.let { "IMDb $it" } else null,
    ))
    if (facts.isNotBlank()) {
        Text(facts, style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.metadata, fontWeight = FontWeight.Medium).withNuvioDescenderRoom(),
            color = tokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** One bounded decode per surface size (AdaptiveResources owns the cap via the hero width token). */
@Composable
private fun BoundedImage(url: String?, width: Dp, height: Dp, modifier: Modifier) {
    if (url.isNullOrBlank()) return
    val context = LocalContext.current
    val density = LocalDensity.current
    val model = remember(url, width, height) {
        ImageRequest.Builder(context).data(url)
            .size(with(density) { width.roundToPx() }.coerceAtLeast(1), with(density) { height.roundToPx() }.coerceAtLeast(1))
            .build()
    }
    AsyncImage(model, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
}
