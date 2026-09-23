@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class
)

package com.nuvio.tv.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.R
import com.nuvio.tv.core.poster.withCustomPosterUrls
import com.nuvio.tv.core.util.withAppLocale
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.CinemaHomeSettings
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.legacyKey
import com.nuvio.tv.ui.components.ContinueWatchingOptionsDialog
import com.nuvio.tv.ui.components.TrailerPlayer
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.util.asStable
import com.nuvio.tv.ui.util.formatHeroRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

/*
 * Cinema View — an immersive, remote-first home layout.
 *
 * The whole screen is the focused title's artwork, slowly drifting behind a
 * spotlight (logo, metadata, synopsis). Rows sit in the lower half; the focused
 * row is pinned to the top of that area and every other row recedes. After a
 * period without input the chrome fades away ("ambient mode") and the artwork
 * plays like a screensaver until the next key press.
 */

private const val CINEMA_SPOTLIGHT_DEBOUNCE_MS = 160L
/** How long a card must hold focus before its trailer is fetched, and then played. */
private const val CINEMA_TRAILER_REQUEST_DELAY_MS = 1_200L
private const val CINEMA_TRAILER_START_DELAY_MS = 3_000L
private const val CINEMA_AMBIENT_SLIDE_MS = 12_000L
private const val CINEMA_LOAD_MORE_THRESHOLD = 5
private const val CINEMA_PLACEHOLDER_ID_PREFIX = "__placeholder_"
private const val CINEMA_FEATURED_ROW_KEY = "cinema_featured"
private const val CINEMA_CARD_ASPECT = 16f / 9f

private val CinemaCardWidth = 236.dp
private val CinemaCardHeight = CinemaCardWidth / CINEMA_CARD_ASPECT
private val CinemaRowHeaderHeight = 40.dp
private val CinemaRailVerticalPadding = 22.dp
private val CinemaHorizontalInset = 56.dp
private val CinemaCardShape = RoundedCornerShape(14.dp)

@Immutable
private sealed class CinemaRowSource {
    data object ContinueWatching : CinemaRowSource()
    data object Featured : CinemaRowSource()
    data class Catalog(val row: CatalogRow) : CinemaRowSource()
    data class Collection(val collectionId: String) : CinemaRowSource()
    /** Row whose catalog hasn't been fetched yet; [loadKey] is handed to the lazy loader. */
    data class Pending(val loadKey: String) : CinemaRowSource()
}

@Immutable
private data class CinemaRow(
    val key: String,
    val title: String,
    val items: List<ModernCarouselItem>,
    val source: CinemaRowSource
)

@Composable
fun CinemaHomeContent(
    uiState: HomeUiState,
    scrollToTopTrigger: Int = 0,
    onNavigateToDetail: (String, String, String) -> Unit,
    onContinueWatchingClick: (ContinueWatchingItem) -> Unit,
    onContinueWatchingStartFromBeginning: (ContinueWatchingItem) -> Unit = {},
    onContinueWatchingPlayManually: (ContinueWatchingItem) -> Unit = {},
    showContinueWatchingManualPlayOption: Boolean = false,
    onLoadMoreCatalog: (String, String, String) -> Unit,
    onRemoveContinueWatching: (String, Int?, Int?, Boolean) -> Unit,
    isCatalogItemWatched: (MetaPreview) -> Boolean = { false },
    onCatalogItemLongPress: (MetaPreview, String) -> Unit = { _, _ -> },
    onNavigateToFolderDetail: (String, String) -> Unit = { _, _ -> },
    onItemFocus: (MetaPreview) -> Unit = {},
    onPreloadAdjacentItem: (MetaPreview) -> Unit = {},
    enrichedPreviews: Map<String, MetaPreview> = emptyMap(),
    onFocusedRowKeyChanged: (String?) -> Unit = {},
    onRequestLazyCatalogLoad: (String) -> Unit = {},
    settings: CinemaHomeSettings = CinemaHomeSettings(),
    trailerPreviewUrls: Map<String, String> = emptyMap(),
    trailerPreviewAudioUrls: Map<String, String> = emptyMap(),
    trailerMuted: Boolean = true,
    onRequestTrailerPreview: (MetaPreview) -> Unit = {}
) {
    val context = LocalContext.current
    val localizedContext = remember(context) { context.withAppLocale() }
    val strContinueWatching = stringResource(R.string.continue_watching)
    val strFeatured = stringResource(R.string.cinema_featured_row)
    val strAirsDate = stringResource(R.string.cw_airs_date)
    val strUpcoming = stringResource(R.string.cw_upcoming)
    val strTypeMovie = stringResource(R.string.type_movie)
    val strTypeSeries = stringResource(R.string.type_series)
    val showImdbRatings = uiState.homeImdbRatingsVisibility.showRatings

    val strUpcomingSection = stringResource(R.string.upcoming_section_title)
    // Same gating and poster overrides as the other layouts apply to these rows.
    val continueWatchingItems = remember(
        uiState.continueWatchingEnabled,
        uiState.continueWatchingItems,
        uiState.customPosterUrlPattern
    ) {
        if (uiState.continueWatchingEnabled) {
            uiState.continueWatchingItems.withCustomPosterUrls(uiState.customPosterUrlPattern)
        } else {
            emptyList()
        }
    }
    val upcomingItems = remember(
        uiState.continueWatchingEnabled,
        uiState.upcomingItems,
        uiState.customPosterUrlPattern
    ) {
        if (uiState.continueWatchingEnabled) {
            uiState.upcomingItems.withCustomPosterUrls(uiState.customPosterUrlPattern)
        } else {
            emptyList()
        }
    }

    val rowCache = remember { CinemaRowCache() }
    val rows = remember(
        continueWatchingItems,
        upcomingItems,
        uiState.heroItems,
        uiState.heroSectionEnabled,
        uiState.homeRows,
        uiState.catalogRows,
        uiState.catalogTypeSuffixEnabled,
        uiState.showFullReleaseDate,
        showImdbRatings,
        localizedContext
    ) {
        buildCinemaRows(
            cache = rowCache,
            continueWatchingItems = continueWatchingItems,
            upcomingItems = upcomingItems,
            heroItems = if (uiState.heroSectionEnabled) uiState.heroItems else emptyList(),
            homeRows = uiState.homeRows.ifEmpty {
                uiState.catalogRows.filter { it.items.isNotEmpty() }.map { HomeRow.Catalog(it) }
            },
            showCatalogTypeSuffix = uiState.catalogTypeSuffixEnabled,
            showFullReleaseDate = uiState.showFullReleaseDate,
            showImdbRatings = showImdbRatings,
            strContinueWatching = strContinueWatching,
            strUpcomingSection = strUpcomingSection,
            strFeatured = strFeatured,
            strAirsDate = strAirsDate,
            strUpcoming = strUpcoming,
            strTypeMovie = strTypeMovie,
            strTypeSeries = strTypeSeries,
            context = localizedContext
        )
    }

    // Focus memory survives navigating to details and back.
    var focusedRowKey by rememberSaveable { mutableStateOf<String?>(null) }
    // One state per row: moving along a row only invalidates that row, not every row on screen.
    val anchorStates = rememberSaveable(saver = CinemaAnchorStatesSaver) { HashMap<String, MutableIntState>() }
    fun anchorFor(rowKey: String) = anchorStates.getOrPut(rowKey) { mutableIntStateOf(0) }
    val loadMoreRequestedAt = remember { HashMap<String, Int>() }
    var focusedItem by remember { mutableStateOf<ModernCarouselItem?>(null) }
    var spotlightItem by remember { mutableStateOf<ModernCarouselItem?>(null) }
    var optionsItem by remember { mutableStateOf<ContinueWatchingItem?>(null) }
    // Plain map: requesters are created lazily during composition and must not be snapshot writes.
    val rowFocusRequesters = remember { HashMap<String, FocusRequester>() }
    fun requesterFor(rowKey: String) = rowFocusRequesters.getOrPut(rowKey) { FocusRequester() }

    val fallbackItem = rows.firstOrNull()?.items?.firstOrNull { !it.isPlaceholder() }
    LaunchedEffect(focusedItem, fallbackItem) {
        val target = focusedItem ?: fallbackItem
        if (spotlightItem != null && target?.key != spotlightItem?.key) {
            // Don't flicker the whole screen while the remote is being held down.
            delay(CINEMA_SPOTLIGHT_DEBOUNCE_MS)
        }
        spotlightItem = target
    }
    // In ambient mode the screen becomes a slideshow; this is the slide currently shown.
    var ambientSlide by remember { mutableStateOf<ModernCarouselItem?>(null) }

    // TMDB / external-meta enrichment lands after focus; fold it into what the spotlight shows.
    val displayedSpotlight = remember(spotlightItem, ambientSlide, enrichedPreviews) {
        (ambientSlide ?: spotlightItem)?.let { item ->
            val enriched = item.metaPreview?.id?.let(enrichedPreviews::get)
            if (enriched == null) item else item.copy(heroPreview = item.heroPreview.withEnrichment(enriched))
        }
    }

    // Trailer autoplay: once a catalog card has held focus for a moment, fetch and play its trailer.
    var trailerItemId by remember { mutableStateOf<String?>(null) }
    var trailerRendered by remember { mutableStateOf(false) }
    val latestOnRequestTrailerPreview by rememberUpdatedState(onRequestTrailerPreview)
    val trailerCandidate = (focusedItem?.payload as? ModernPayload.Catalog)?.let { focusedItem?.metaPreview }
    LaunchedEffect(trailerCandidate?.id, settings.trailerAutoplayEnabled) {
        trailerItemId = null
        trailerRendered = false
        val candidate = trailerCandidate ?: return@LaunchedEffect
        if (!settings.trailerAutoplayEnabled) return@LaunchedEffect
        delay(CINEMA_TRAILER_REQUEST_DELAY_MS)
        latestOnRequestTrailerPreview(candidate)
        delay(CINEMA_TRAILER_START_DELAY_MS - CINEMA_TRAILER_REQUEST_DELAY_MS)
        trailerItemId = candidate.id
    }
    val trailerUrl = trailerItemId?.let { trailerPreviewUrls[it] }?.takeIf { it.isNotBlank() }
    val trailerAudioUrl = trailerItemId?.let { trailerPreviewAudioUrls[it] }

    // Ambient mode: fade the chrome away after a stretch without input.
    var lastInteractionAt by remember { mutableLongStateOf(0L) }
    var ambient by remember { mutableStateOf(false) }
    val ambientTimeoutMs = settings.ambientTimeoutSeconds * 1_000L
    val trailerActive = trailerUrl != null
    LaunchedEffect(lastInteractionAt, optionsItem, ambientTimeoutMs, trailerActive) {
        // Key events go to the dialog's window while it's open, so don't count that as idle,
        // and a playing trailer is something being watched, not an idle screen.
        if (optionsItem != null || ambientTimeoutMs <= 0L || trailerActive) return@LaunchedEffect
        delay(ambientTimeoutMs)
        ambient = true
    }
    LaunchedEffect(ambient) {
        if (!ambient) {
            ambientSlide = null
            return@LaunchedEffect
        }
        // Featured titles make the best slideshow; otherwise cycle the row that had focus.
        val pool = (rows.firstOrNull { it.source is CinemaRowSource.Featured }
            ?: rows.firstOrNull { it.key == focusedRowKey })
            ?.items
            ?.filter { !it.isPlaceholder() && !it.heroPreview.backdrop.isNullOrBlank() }
            .orEmpty()
        if (pool.size < 2) return@LaunchedEffect
        var index = pool.indexOfFirst { it.key == spotlightItem?.key }
        while (true) {
            delay(CINEMA_AMBIENT_SLIDE_MS)
            index = (index + 1) % pool.size
            ambientSlide = pool[index]
        }
    }
    val chromeAlpha by animateFloatAsState(
        targetValue = if (ambient) 0f else 1f,
        animationSpec = tween(durationMillis = if (ambient) 1_400 else 380),
        label = "cinemaChromeAlpha"
    )

    val columnState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val contentFocusRequester = LocalContentFocusRequester.current
    val latestOnFocusedRowKeyChanged by rememberUpdatedState(onFocusedRowKeyChanged)
    val latestOnRequestLazyCatalogLoad by rememberUpdatedState(onRequestLazyCatalogLoad)

    // Pin the focused row just below its header so the next row always peeks in underneath.
    // The focused card sits below its row header and the rail's top padding; keep both visible.
    val rowHeaderInsetPx = with(density) { (CinemaRowHeaderHeight + CinemaRailVerticalPadding).toPx() }
    val verticalPinSpec = remember(rowHeaderInsetPx, columnState) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                val distance = offset - rowHeaderInsetPx
                if (kotlin.math.abs(distance) < 1f) return 0f
                if (distance < 0f && !columnState.canScrollBackward) return 0f
                return distance
            }
        }
    }

    val firstFocusableRowIndex = rows.indexOfFirst { it.items.isNotEmpty() }

    fun focusRow(rowIndex: Int) {
        val row = rows.getOrNull(rowIndex) ?: return
        scope.launch {
            columnState.scrollToItem(rowIndex)
            withFrameNanos { }
            runCatching { requesterFor(row.key).requestFocus() }
        }
    }

    // Initial focus, or restore it when coming back from another screen.
    var initialFocusDone by remember { mutableStateOf(false) }
    val hasFocusableRow = firstFocusableRowIndex >= 0
    LaunchedEffect(hasFocusableRow) {
        if (initialFocusDone || !hasFocusableRow) return@LaunchedEffect
        initialFocusDone = true
        val restoreIndex = rows.indexOfFirst { it.key == focusedRowKey && it.items.isNotEmpty() }
            .takeIf { it >= 0 }
            ?: rows.indexOfFirst { it.items.isNotEmpty() }
        columnState.scrollToItem(restoreIndex)
        repeat(2) { withFrameNanos { } }
        runCatching { requesterFor(rows[restoreIndex].key).requestFocus() }
    }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) focusRow(firstFocusableRowIndex)
    }

    val focusedRowIndex = rows.indexOfFirst { it.key == focusedRowKey }
    // Back from deep in the rows jumps home to the first row before it can leave the screen.
    BackHandler(enabled = firstFocusableRowIndex >= 0 && focusedRowIndex > firstFocusableRowIndex && !ambient) {
        focusRow(firstFocusableRowIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NuvioTheme.colors.Background)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                lastInteractionAt = System.nanoTime()
                if (ambient) {
                    // The first press only wakes the screen up.
                    ambient = false
                    true
                } else {
                    false
                }
            }
    ) {
        CinemaBackdrop(
            imageUrl = displayedSpotlight?.let { it.heroPreview.backdrop ?: it.heroPreview.imageUrl ?: it.imageUrl },
            ambientProgress = { 1f - chromeAlpha },
            motionEnabled = settings.backdropMotionEnabled,
            modifier = Modifier.fillMaxSize(),
            video = {
                val playingId = trailerItemId
                if (trailerUrl != null && playingId != null && !ambient) {
                    key(playingId) {
                        TrailerPlayer(
                            trailerUrl = trailerUrl,
                            trailerAudioUrl = trailerAudioUrl,
                            isPlaying = true,
                            onEnded = {
                                trailerItemId = null
                                trailerRendered = false
                            },
                            onFirstFrameRendered = { trailerRendered = true },
                            muted = trailerMuted,
                            cropToFill = true,
                            overscanZoom = MODERN_TRAILER_OVERSCAN_ZOOM,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        )

        if (settings.clockEnabled) {
            CinemaClock(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 28.dp, end = CinemaHorizontalInset)
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val spotlightHeight = maxHeight * 0.52f
            Column(modifier = Modifier.fillMaxSize()) {
                CinemaSpotlight(
                    item = displayedSpotlight,
                    trailerPlaying = trailerRendered,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(spotlightHeight)
                        .graphicsLayer { alpha = chromeAlpha }
                        .padding(start = CinemaHorizontalInset, end = CinemaHorizontalInset, bottom = 12.dp)
                )

                CompositionLocalProvider(LocalBringIntoViewSpec provides verticalPinSpec) {
                    LazyColumn(
                        state = columnState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .graphicsLayer { alpha = chromeAlpha }
                            .focusRequester(contentFocusRequester)
                            .focusRestorer(),
                        contentPadding = PaddingValues(bottom = maxHeight * 0.4f)
                    ) {
                        itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                            if (row.source is CinemaRowSource.Pending) {
                                LaunchedEffect(row.key) {
                                    latestOnRequestLazyCatalogLoad(row.source.loadKey)
                                }
                            }
                            CinemaRowSection(
                                row = row,
                                isActive = row.key == focusedRowKey,
                                isAfterActive = focusedRowIndex in 0 until index,
                                anchorState = anchorFor(row.key),
                                anchorFocusRequester = requesterFor(row.key),
                                isCatalogItemWatched = isCatalogItemWatched,
                                onItemFocused = { itemIndex, item ->
                                    if (focusedRowKey != row.key) {
                                        focusedRowKey = row.key
                                        latestOnFocusedRowKeyChanged(row.key)
                                    }
                                    anchorFor(row.key).intValue = itemIndex
                                    focusedItem = item
                                    item.metaPreview?.let(onItemFocus)
                                    // Warm up the neighbour so its logo/backdrop are ready on the next press.
                                    row.items.getOrNull(itemIndex + 1)?.metaPreview?.let(onPreloadAdjacentItem)
                                    val source = row.source
                                    if (source is CinemaRowSource.Catalog &&
                                        source.row.hasMore &&
                                        !source.row.isLoading &&
                                        itemIndex >= row.items.size - CINEMA_LOAD_MORE_THRESHOLD &&
                                        loadMoreRequestedAt[row.key] != row.items.size
                                    ) {
                                        loadMoreRequestedAt[row.key] = row.items.size
                                        onLoadMoreCatalog(
                                            source.row.catalogId,
                                            source.row.addonId,
                                            source.row.apiType
                                        )
                                    }
                                },
                                onItemClick = { item ->
                                    when (val payload = item.payload) {
                                        is ModernPayload.ContinueWatching -> onContinueWatchingClick(payload.item)
                                        is ModernPayload.Catalog -> onNavigateToDetail(
                                            payload.itemId,
                                            payload.itemType,
                                            payload.addonBaseUrl
                                        )
                                        is ModernPayload.CollectionFolder -> onNavigateToFolderDetail(
                                            payload.collectionId,
                                            payload.folderId
                                        )
                                    }
                                },
                                onItemLongClick = { item ->
                                    when (val payload = item.payload) {
                                        is ModernPayload.ContinueWatching -> {
                                            optionsItem = payload.item
                                        }
                                        is ModernPayload.Catalog -> item.metaPreview?.let {
                                            onCatalogItemLongPress(it, payload.addonBaseUrl)
                                        }
                                        is ModernPayload.CollectionFolder -> Unit
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Ambient caption: a quiet reminder of what's on screen.
        val ambientTitle = displayedSpotlight?.heroPreview?.title.orEmpty()
        if (ambientTitle.isNotBlank()) {
            Text(
                text = ambientTitle,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = CinemaHorizontalInset, bottom = 40.dp)
                    .graphicsLayer { alpha = 1f - chromeAlpha }
            )
        }
    }

    val selectedOptionsItem = optionsItem
    if (selectedOptionsItem != null) {
        ContinueWatchingOptionsDialog(
            item = selectedOptionsItem,
            onDismiss = { optionsItem = null },
            onRemove = {
                onRemoveContinueWatching(
                    selectedOptionsItem.contentId(),
                    selectedOptionsItem.season(),
                    selectedOptionsItem.episode(),
                    selectedOptionsItem is ContinueWatchingItem.NextUp
                )
                optionsItem = null
            },
            onDetails = {
                onNavigateToDetail(selectedOptionsItem.contentId(), selectedOptionsItem.contentType(), "")
                optionsItem = null
            },
            onStartFromBeginning = {
                onContinueWatchingStartFromBeginning(selectedOptionsItem)
                optionsItem = null
            },
            showPlayManually = showContinueWatchingManualPlayOption,
            onPlayManually = {
                onContinueWatchingPlayManually(selectedOptionsItem)
                optionsItem = null
            }
        )
    }
}

/**
 * Remembers each built row alongside the data it was built from, so a change in one catalog
 * (a page load, an enrichment merge) only rebuilds that row instead of every card on screen.
 */
private class CinemaRowCache {
    private var config: List<Any?>? = null
    private val entries = HashMap<String, Pair<Any, CinemaRow>>()

    fun begin(config: List<Any?>) {
        if (config != this.config) {
            entries.clear()
            this.config = config
        }
    }

    fun get(key: String, source: Any, build: () -> CinemaRow): CinemaRow {
        val cached = entries[key]
        if (cached != null && (cached.first === source || cached.first == source)) return cached.second
        return build().also { entries[key] = source to it }
    }

    fun retainOnly(keys: Set<String>) {
        entries.keys.retainAll(keys)
    }
}

private fun buildCinemaRows(
    cache: CinemaRowCache,
    continueWatchingItems: List<ContinueWatchingItem>,
    upcomingItems: List<ContinueWatchingItem>,
    heroItems: List<MetaPreview>,
    homeRows: List<HomeRow>,
    showCatalogTypeSuffix: Boolean,
    showFullReleaseDate: Boolean,
    showImdbRatings: Boolean,
    strContinueWatching: String,
    strUpcomingSection: String,
    strFeatured: String,
    strAirsDate: String,
    strUpcoming: String,
    strTypeMovie: String,
    strTypeSeries: String,
    context: android.content.Context
): List<CinemaRow> {
    cache.begin(
        listOf(
            showCatalogTypeSuffix, showFullReleaseDate, showImdbRatings, strContinueWatching,
            strUpcomingSection, strFeatured, strAirsDate, strUpcoming, strTypeMovie, strTypeSeries, context
        )
    )

    fun continueWatchingRow(key: String, title: String, items: List<ContinueWatchingItem>) =
        cache.get(key, items) {
            CinemaRow(
                key = key,
                title = title,
                items = items.map { item ->
                    buildContinueWatchingItem(
                        item = item,
                        useLandscapePosters = true,
                        showImdbRatings = showImdbRatings,
                        airsDateTemplate = strAirsDate,
                        upcomingLabel = strUpcoming,
                        context = context
                    )
                },
                source = CinemaRowSource.ContinueWatching
            )
        }

    val rows = buildList {
        if (continueWatchingItems.isNotEmpty()) {
            add(continueWatchingRow(MODERN_CONTINUE_WATCHING_ROW_KEY, strContinueWatching, continueWatchingItems))
        }
        if (upcomingItems.isNotEmpty()) {
            add(continueWatchingRow(MODERN_UPCOMING_ROW_KEY, strUpcomingSection, upcomingItems))
        }

        // Enrichment is merged into the catalog rows, not into heroItems, so prefer the row copy.
        val latestById = HashMap<String, MetaPreview>()
        homeRows.forEach { row -> if (row is HomeRow.Catalog) row.row.items.forEach { latestById.putIfAbsent(it.id, it) } }
        val featuredItems = heroItems.map { latestById[it.id] ?: it }
        if (featuredItems.isNotEmpty()) {
            add(
                cache.get(CINEMA_FEATURED_ROW_KEY, featuredItems) {
                    val featuredRow = CatalogRow(
                        addonId = CINEMA_FEATURED_ROW_KEY,
                        addonName = "",
                        addonBaseUrl = featuredItems.firstNotNullOfOrNull { it.sourceAddonBaseUrl }.orEmpty(),
                        catalogId = CINEMA_FEATURED_ROW_KEY,
                        catalogName = strFeatured,
                        type = featuredItems.first().type,
                        items = featuredItems,
                        hasMore = false
                    )
                    CinemaRow(
                        key = CINEMA_FEATURED_ROW_KEY,
                        title = strFeatured,
                        items = featuredItems.mapIndexed { index, item ->
                            buildCatalogItem(
                                item = item,
                                row = featuredRow.copy(addonBaseUrl = item.sourceAddonBaseUrl ?: featuredRow.addonBaseUrl),
                                useLandscapePosters = true,
                                occurrence = index,
                                strTypeMovie = strTypeMovie,
                                strTypeSeries = strTypeSeries,
                                showFullReleaseDate = showFullReleaseDate,
                                showImdbRatings = showImdbRatings
                            )
                        },
                        source = CinemaRowSource.Featured
                    )
                }
            )
        }

        val seenKeys = HashSet<String>()
        homeRows.forEach { homeRow ->
            when (homeRow) {
                is HomeRow.Catalog -> {
                    val row = homeRow.row
                    val key = row.key()
                    if (!seenKeys.add(key)) return@forEach
                    val isPending = row.items.isEmpty() ||
                        row.items.all { it.id.startsWith(CINEMA_PLACEHOLDER_ID_PREFIX) }
                    if (isPending && !row.isLoading) return@forEach
                    add(
                        cache.get(key, row) {
                            val title = catalogRowTitle(row, showCatalogTypeSuffix, strTypeMovie, strTypeSeries)
                            if (isPending) {
                                CinemaRow(key, title, emptyList(), CinemaRowSource.Pending(row.legacyKey()))
                            } else {
                                val occurrences = HashMap<String, Int>()
                                CinemaRow(
                                    key = key,
                                    title = title,
                                    items = row.items.map { item ->
                                        val occurrence = occurrences.getOrDefault(item.id, 0)
                                        occurrences[item.id] = occurrence + 1
                                        buildCatalogItem(
                                            item = item,
                                            row = row,
                                            useLandscapePosters = true,
                                            occurrence = occurrence,
                                            strTypeMovie = strTypeMovie,
                                            strTypeSeries = strTypeSeries,
                                            showFullReleaseDate = showFullReleaseDate,
                                            showImdbRatings = showImdbRatings
                                        )
                                    },
                                    source = CinemaRowSource.Catalog(row)
                                )
                            }
                        }
                    )
                }

                is HomeRow.CollectionRow -> {
                    val collection = homeRow.collection
                    val key = "collection_${collection.id}"
                    if (collection.folders.isEmpty() || !seenKeys.add(key)) return@forEach
                    add(
                        cache.get(key, collection) {
                            CinemaRow(
                                key = key,
                                title = collection.title,
                                items = collection.folders.mapIndexed { index, folder ->
                                    buildCollectionFolderItem(collection, folder, index)
                                },
                                source = CinemaRowSource.Collection(collection.id)
                            )
                        }
                    )
                }

                is HomeRow.PlaceholderCatalog -> {
                    if (!seenKeys.add(homeRow.stableCatalogKey)) return@forEach
                    add(
                        CinemaRow(
                            key = homeRow.stableCatalogKey,
                            title = homeRow.displayTitle,
                            items = emptyList(),
                            source = CinemaRowSource.Pending(homeRow.catalogKey)
                        )
                    )
                }
            }
        }
    }
    cache.retainOnly(rows.mapTo(HashSet()) { it.key })
    return rows
}

private val CinemaAnchorStatesSaver = Saver<HashMap<String, MutableIntState>, HashMap<String, Int>>(
    save = { states -> HashMap(states.mapValues { it.value.intValue }) },
    restore = { saved -> HashMap(saved.mapValues { mutableIntStateOf(it.value) }) }
)

private fun HeroPreview.withEnrichment(meta: MetaPreview): HeroPreview = copy(
    logo = meta.logo?.takeIf { it.isNotBlank() } ?: logo,
    backdrop = meta.background?.takeIf { it.isNotBlank() } ?: backdrop,
    description = meta.description?.takeIf { it.isNotBlank() } ?: description,
    runtimeText = formatHeroRuntime(meta.runtime) ?: runtimeText,
    ageRatingText = meta.ageRating?.takeIf { it.isNotBlank() } ?: ageRatingText,
    imdbText = meta.imdbRating?.let { String.format("%.1f", it) } ?: imdbText,
    genres = if (meta.genres.isNotEmpty()) meta.genres.take(3).asStable() else genres
)

private fun ModernCarouselItem.isPlaceholder(): Boolean =
    metaPreview?.id?.startsWith(CINEMA_PLACEHOLDER_ID_PREFIX) == true

/** Scale/shift for the backdrop, read in the draw phase so the drift never recomposes. */
private class CinemaDrift(val scale: () -> Float, val shift: () -> Float) {
    companion object {
        val Still = CinemaDrift(scale = { 1.04f }, shift = { 0f })
    }
}

@Composable
private fun rememberCinemaDrift(): CinemaDrift {
    val transition = rememberInfiniteTransition(label = "cinemaDrift")
    // A slow Ken Burns push that never quite settles.
    val scale = transition.animateFloat(
        initialValue = 1.02f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cinemaDriftScale"
    )
    val shift = transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 34_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cinemaDriftShift"
    )
    return remember(scale, shift) { CinemaDrift(scale = { scale.value }, shift = { shift.value }) }
}

@Composable
private fun CinemaBackdrop(
    imageUrl: String?,
    ambientProgress: () -> Float,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
    video: @Composable () -> Unit = {}
) {
    val background = NuvioTheme.colors.Background
    // Brushes don't mirror on their own; keep the dark side behind the spotlight in RTL too.
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val drift = if (motionEnabled) rememberCinemaDrift() else CinemaDrift.Still

    Box(modifier = modifier) {
        Crossfade(
            targetState = imageUrl,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            label = "cinemaBackdropCrossfade"
        ) { url ->
            if (url.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxSize().background(background))
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = drift.scale()
                            scaleX = scale
                            scaleY = scale
                            translationX = drift.shift() * size.width * 0.012f
                        }
                )
            }
        }

        video()

        // Scrims soften as ambient mode takes over, so the artwork gets the whole screen.
        val scrimStrength = { 1f - (ambientProgress() * 0.75f) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimStrength() }
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to background.copy(alpha = 0.96f),
                            0.35f to background.copy(alpha = 0.78f),
                            0.65f to background.copy(alpha = 0.18f),
                            1.0f to Color.Transparent
                        ),
                        startX = if (isRtl) Float.POSITIVE_INFINITY else 0f,
                        endX = if (isRtl) 0f else Float.POSITIVE_INFINITY
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimStrength() }
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to background.copy(alpha = 0.45f),
                            0.22f to Color.Transparent,
                            0.48f to background.copy(alpha = 0.35f),
                            0.72f to background.copy(alpha = 0.92f),
                            1.0f to background
                        )
                    )
                )
        )
    }
}

@Composable
private fun CinemaSpotlight(
    item: ModernCarouselItem?,
    trailerPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // While a trailer plays the synopsis steps aside so the video has the screen.
    val synopsisAlpha by animateFloatAsState(
        targetValue = if (trailerPlaying) 0f else 1f,
        animationSpec = tween(durationMillis = 600),
        label = "cinemaSynopsisAlpha"
    )
    Box(modifier = modifier, contentAlignment = Alignment.BottomStart) {
        AnimatedContent(
            targetState = item,
            contentKey = { it?.key },
            transitionSpec = {
                (fadeIn(tween(420, delayMillis = 90)) +
                    slideInVertically(tween(420, delayMillis = 90)) { it / 14 })
                    .togetherWith(fadeOut(tween(180)))
            },
            label = "cinemaSpotlight"
        ) { target ->
            if (target == null) {
                Spacer(modifier = Modifier.fillMaxWidth())
            } else {
                CinemaSpotlightBody(target.heroPreview, target.payload, synopsisAlpha = { synopsisAlpha })
            }
        }
    }
}

@Composable
private fun CinemaSpotlightBody(
    preview: HeroPreview,
    payload: ModernPayload,
    synopsisAlpha: () -> Float = { 1f }
) {
    Column(
        modifier = Modifier.widthIn(max = 620.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val logo = preview.logo
        var logoFailed by remember(logo) { mutableStateOf(false) }
        if (!logo.isNullOrBlank() && !logoFailed) {
            AsyncImage(
                model = logo,
                contentDescription = preview.title,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomStart,
                onError = { logoFailed = true },
                modifier = Modifier
                    .heightIn(max = 120.dp)
                    .widthIn(max = 440.dp)
            )
        } else if (preview.title.isNotBlank()) {
            Text(
                text = preview.title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        val highlight = preview.secondaryHighlightText
        if (payload is ModernPayload.ContinueWatching && !highlight.isNullOrBlank()) {
            CinemaPill(text = highlight, emphasized = true)
        }

        CinemaMetaLine(preview)

        val description = preview.description
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { alpha = synopsisAlpha() }
            )
        }
    }
}

@Composable
private fun CinemaMetaLine(preview: HeroPreview) {
    val parts = buildList {
        preview.contentTypeText?.takeIf { it.isNotBlank() }?.let(::add)
        preview.yearText?.takeIf { it.isNotBlank() }?.let(::add)
        preview.runtimeText?.takeIf { it.isNotBlank() }?.let(::add)
        preview.genres.take(3).filter { it.isNotBlank() }.forEach(::add)
    }
    val rating = preview.imdbText
    val ageRating = preview.ageRatingText
    if (parts.isEmpty() && rating.isNullOrBlank() && ageRating.isNullOrBlank()) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!rating.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = NuvioTheme.colors.Rating,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = rating,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }
        }
        if (!ageRating.isNullOrBlank()) {
            CinemaPill(text = ageRating, emphasized = false)
        }
        if (parts.isNotEmpty()) {
            Text(
                text = parts.joinToString("  ·  "),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CinemaPill(text: String, emphasized: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (emphasized) NuvioTheme.colors.FocusRing.copy(alpha = 0.9f)
                else Color.White.copy(alpha = 0.14f)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            ),
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun CinemaClock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val timeFormat = remember(context) { android.text.format.DateFormat.getTimeFormat(context) }
    var now by remember { mutableStateOf(timeFormat.format(Date())) }
    LaunchedEffect(timeFormat) {
        while (true) {
            now = timeFormat.format(Date())
            // Wake up right after the next minute boundary.
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 50L)
        }
    }
    Text(
        text = now,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
        color = Color.White.copy(alpha = 0.9f),
        modifier = modifier
    )
}

@Composable
private fun CinemaRowSection(
    row: CinemaRow,
    isActive: Boolean,
    isAfterActive: Boolean,
    anchorState: MutableIntState,
    anchorFocusRequester: FocusRequester,
    isCatalogItemWatched: (MetaPreview) -> Boolean,
    onItemFocused: (Int, ModernCarouselItem) -> Unit,
    onItemClick: (ModernCarouselItem) -> Unit,
    onItemLongClick: (ModernCarouselItem) -> Unit
) {
    val rowAlpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            isAfterActive -> 0.55f
            else -> 0.35f
        },
        animationSpec = tween(durationMillis = 260),
        label = "cinemaRowAlpha"
    )
    val density = LocalDensity.current
    val startInsetPx = with(density) { CinemaHorizontalInset.toPx() }
    val rowState = rememberLazyListState(
        initialFirstVisibleItemIndex = anchorState.intValue.coerceIn(0, (row.items.size - 1).coerceAtLeast(0))
    )
    // Keep the focused card on a fixed left rail, like a TV guide.
    val horizontalRailSpec = remember(startInsetPx, rowState) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                val distance = offset - startInsetPx
                if (kotlin.math.abs(distance) < 1f) return 0f
                if (distance < 0f && !rowState.canScrollBackward) return 0f
                // Near the end, stop once the last card is fully visible.
                if (distance > 0f && offset + size + startInsetPx <= containerSize && !rowState.canScrollForward) return 0f
                return distance
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = rowAlpha }
            .padding(bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .height(CinemaRowHeaderHeight)
                .padding(start = CinemaHorizontalInset),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.2.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (row.items.isEmpty()) {
            CinemaSkeletonRow()
        } else {
            CompositionLocalProvider(LocalBringIntoViewSpec provides horizontalRailSpec) {
                LazyRow(
                    state = rowState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRestorer(),
                    // Room for the 1.09x focus scale plus the 18dp glow; lazy lists clip to their bounds.
                    contentPadding = PaddingValues(horizontal = CinemaHorizontalInset, vertical = CinemaRailVerticalPadding),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    itemsIndexed(row.items, key = { _, item -> item.key }) { index, item ->
                        val isAnchor = index == anchorState.intValue.coerceIn(0, row.items.size - 1)
                        CinemaCard(
                            item = item,
                            isWatched = item.metaPreview?.let(isCatalogItemWatched) == true,
                            modifier = if (isAnchor) Modifier.focusRequester(anchorFocusRequester) else Modifier,
                            onFocused = { onItemFocused(index, item) },
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CinemaCard(
    item: ModernCarouselItem,
    isWatched: Boolean,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRing = NuvioTheme.colors.FocusRing
    val payload = item.payload
    val progress = (payload as? ModernPayload.ContinueWatching)
        ?.item
        ?.let { it as? ContinueWatchingItem.InProgress }
        ?.progress
        ?.progressPercentage
    val cardImage = item.imageUrl ?: item.heroPreview.backdrop

    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .width(CinemaCardWidth)
            .height(CinemaCardHeight)
            .onFocusChanged { state ->
                if (state.isFocused != isFocused) {
                    isFocused = state.isFocused
                    if (state.isFocused) onFocused()
                }
            },
        shape = CardDefaults.shape(CinemaCardShape),
        colors = CardDefaults.colors(
            containerColor = NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = NuvioTheme.colors.BackgroundCard
        ),
        scale = CardDefaults.scale(focusedScale = 1.09f),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, focusRing),
                shape = CinemaCardShape
            )
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(elevationColor = focusRing.copy(alpha = 0.55f), elevation = 18.dp)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!cardImage.isNullOrBlank()) {
                AsyncImage(
                    model = cardImage,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Title caption: always shown when there is no artwork, otherwise only on focus.
            val showCaption = cardImage.isNullOrBlank() || isFocused
            val captionAlpha by animateFloatAsState(
                targetValue = if (showCaption) 1f else 0f,
                animationSpec = tween(220),
                label = "cinemaCardCaption"
            )
            if (captionAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = captionAlpha }
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.45f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.82f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, end = 12.dp, bottom = if (progress != null) 14.dp else 10.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = item.subtitle
                        if (!subtitle.isNullOrBlank() && payload !is ModernPayload.Catalog) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.cinema_watched),
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (progress != null && progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                            .background(focusRing)
                    )
                }
            }
        }
    }
}

@Composable
private fun CinemaSkeletonRow() {
    val shimmer = rememberInfiniteTransition(label = "cinemaSkeleton")
    val shimmerAlpha by shimmer.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cinemaSkeletonAlpha"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = CinemaHorizontalInset, top = CinemaRailVerticalPadding, bottom = CinemaRailVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .width(CinemaCardWidth)
                    .height(CinemaCardHeight)
                    .graphicsLayer { alpha = shimmerAlpha }
                    .clip(CinemaCardShape)
                    .background(NuvioTheme.colors.BackgroundCard)
            )
        }
    }
}
