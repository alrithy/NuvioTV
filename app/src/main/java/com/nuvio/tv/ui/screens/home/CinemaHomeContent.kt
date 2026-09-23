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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.nuvio.tv.core.util.withAppLocale
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.legacyKey
import com.nuvio.tv.ui.components.ContinueWatchingOptionsDialog
import com.nuvio.tv.ui.theme.NuvioTheme
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
private const val CINEMA_AMBIENT_IDLE_MS = 25_000L
private const val CINEMA_LOAD_MORE_THRESHOLD = 5
private const val CINEMA_PLACEHOLDER_ID_PREFIX = "__placeholder_"
private const val CINEMA_FEATURED_ROW_KEY = "cinema_featured"
private const val CINEMA_CARD_ASPECT = 16f / 9f

private val CinemaCardWidth = 236.dp
private val CinemaCardHeight = CinemaCardWidth / CINEMA_CARD_ASPECT
private val CinemaRowHeaderHeight = 40.dp
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
    onFocusedRowKeyChanged: (String?) -> Unit = {},
    onRequestLazyCatalogLoad: (String) -> Unit = {}
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

    val continueWatchingItems = if (uiState.continueWatchingEnabled) {
        uiState.continueWatchingItems
    } else {
        emptyList()
    }

    val rows = remember(
        continueWatchingItems,
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
            continueWatchingItems = continueWatchingItems,
            heroItems = if (uiState.heroSectionEnabled) uiState.heroItems else emptyList(),
            homeRows = uiState.homeRows.ifEmpty {
                uiState.catalogRows.filter { it.items.isNotEmpty() }.map { HomeRow.Catalog(it) }
            },
            showCatalogTypeSuffix = uiState.catalogTypeSuffixEnabled,
            showFullReleaseDate = uiState.showFullReleaseDate,
            showImdbRatings = showImdbRatings,
            strContinueWatching = strContinueWatching,
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
    val anchorIndexByRow = rememberSaveable(saver = CinemaAnchorMapSaver) { mutableStateMapOf<String, Int>() }
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

    // Ambient mode: fade the chrome away after a stretch without input.
    var lastInteractionAt by remember { mutableLongStateOf(0L) }
    var ambient by remember { mutableStateOf(false) }
    LaunchedEffect(lastInteractionAt, optionsItem) {
        // Key events go to the dialog's window while it's open, so don't count that as idle.
        if (optionsItem != null) return@LaunchedEffect
        delay(CINEMA_AMBIENT_IDLE_MS)
        ambient = true
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
    val rowHeaderInsetPx = with(density) { CinemaRowHeaderHeight.toPx() }
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
            imageUrl = spotlightItem?.let { it.heroPreview.backdrop ?: it.heroPreview.imageUrl ?: it.imageUrl },
            ambientProgress = 1f - chromeAlpha,
            modifier = Modifier.fillMaxSize()
        )

        CinemaClock(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 28.dp, end = CinemaHorizontalInset)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val spotlightHeight = maxHeight * 0.52f
            Column(modifier = Modifier.fillMaxSize()) {
                CinemaSpotlight(
                    item = spotlightItem,
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
                                anchorIndex = anchorIndexByRow[row.key] ?: 0,
                                anchorFocusRequester = requesterFor(row.key),
                                isCatalogItemWatched = isCatalogItemWatched,
                                onItemFocused = { itemIndex, item ->
                                    if (focusedRowKey != row.key) {
                                        focusedRowKey = row.key
                                        latestOnFocusedRowKeyChanged(row.key)
                                    }
                                    anchorIndexByRow[row.key] = itemIndex
                                    focusedItem = item
                                    item.metaPreview?.let(onItemFocus)
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
        val ambientTitle = spotlightItem?.heroPreview?.title.orEmpty()
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

private fun buildCinemaRows(
    continueWatchingItems: List<ContinueWatchingItem>,
    heroItems: List<MetaPreview>,
    homeRows: List<HomeRow>,
    showCatalogTypeSuffix: Boolean,
    showFullReleaseDate: Boolean,
    showImdbRatings: Boolean,
    strContinueWatching: String,
    strFeatured: String,
    strAirsDate: String,
    strUpcoming: String,
    strTypeMovie: String,
    strTypeSeries: String,
    context: android.content.Context
): List<CinemaRow> = buildList {
    if (continueWatchingItems.isNotEmpty()) {
        add(
            CinemaRow(
                key = MODERN_CONTINUE_WATCHING_ROW_KEY,
                title = strContinueWatching,
                items = continueWatchingItems.map { item ->
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
        )
    }

    if (heroItems.isNotEmpty()) {
        val featuredRow = CatalogRow(
            addonId = CINEMA_FEATURED_ROW_KEY,
            addonName = "",
            addonBaseUrl = heroItems.firstNotNullOfOrNull { it.sourceAddonBaseUrl }.orEmpty(),
            catalogId = CINEMA_FEATURED_ROW_KEY,
            catalogName = strFeatured,
            type = heroItems.first().type,
            items = heroItems,
            hasMore = false
        )
        add(
            CinemaRow(
                key = CINEMA_FEATURED_ROW_KEY,
                title = strFeatured,
                items = heroItems.mapIndexed { index, item ->
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
                val title = catalogRowTitle(row, showCatalogTypeSuffix, strTypeMovie, strTypeSeries)
                if (isPending) {
                    if (!row.isLoading) return@forEach
                    add(CinemaRow(key, title, emptyList(), CinemaRowSource.Pending(row.legacyKey())))
                } else {
                    val occurrences = HashMap<String, Int>()
                    add(
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
                    )
                }
            }

            is HomeRow.CollectionRow -> {
                val collection = homeRow.collection
                val key = "collection_${collection.id}"
                if (collection.folders.isEmpty() || !seenKeys.add(key)) return@forEach
                add(
                    CinemaRow(
                        key = key,
                        title = collection.title,
                        items = collection.folders.mapIndexed { index, folder ->
                            buildCollectionFolderItem(collection, folder, index)
                        },
                        source = CinemaRowSource.Collection(collection.id)
                    )
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

private val CinemaAnchorMapSaver = Saver<SnapshotStateMap<String, Int>, HashMap<String, Int>>(
    save = { HashMap(it) },
    restore = { saved -> mutableStateMapOf<String, Int>().apply { putAll(saved) } }
)

private fun ModernCarouselItem.isPlaceholder(): Boolean =
    metaPreview?.id?.startsWith(CINEMA_PLACEHOLDER_ID_PREFIX) == true

@Composable
private fun CinemaBackdrop(
    imageUrl: String?,
    ambientProgress: Float,
    modifier: Modifier = Modifier
) {
    val background = NuvioTheme.colors.Background
    val drift = rememberInfiniteTransition(label = "cinemaDrift")
    // A slow Ken Burns push that never quite settles.
    val driftScale by drift.animateFloat(
        initialValue = 1.02f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cinemaDriftScale"
    )
    val driftShift by drift.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 34_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cinemaDriftShift"
    )

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
                            scaleX = driftScale
                            scaleY = driftScale
                            translationX = driftShift * size.width * 0.012f
                        }
                )
            }
        }

        // Scrims soften as ambient mode takes over, so the artwork gets the whole screen.
        val scrimStrength = 1f - (ambientProgress * 0.75f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimStrength }
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to background.copy(alpha = 0.96f),
                            0.35f to background.copy(alpha = 0.78f),
                            0.65f to background.copy(alpha = 0.18f),
                            1.0f to Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimStrength }
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
    modifier: Modifier = Modifier
) {
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
                CinemaSpotlightBody(target.heroPreview, target.payload)
            }
        }
    }
}

@Composable
private fun CinemaSpotlightBody(
    preview: HeroPreview,
    payload: ModernPayload
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
                overflow = TextOverflow.Ellipsis
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
    anchorIndex: Int,
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
        initialFirstVisibleItemIndex = anchorIndex.coerceIn(0, (row.items.size - 1).coerceAtLeast(0))
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
            .padding(bottom = 18.dp)
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
                    contentPadding = PaddingValues(horizontal = CinemaHorizontalInset, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    itemsIndexed(row.items, key = { _, item -> item.key }) { index, item ->
                        val isAnchor = index == anchorIndex.coerceIn(0, row.items.size - 1)
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
            .padding(start = CinemaHorizontalInset, top = 10.dp, bottom = 10.dp),
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
