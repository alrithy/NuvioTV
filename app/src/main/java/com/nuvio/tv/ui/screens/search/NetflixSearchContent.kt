@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.nuvio.tv.ui.screens.search

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import kotlinx.coroutines.launch
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.DiscoverLocation
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.ui.components.EmptyScreenState
import com.nuvio.tv.ui.components.ErrorState
import com.nuvio.tv.ui.components.GridContentCard
import com.nuvio.tv.ui.components.LocalAlwaysBackdropWithLogo
import com.nuvio.tv.ui.components.LocalLandscapePosterMode
import com.nuvio.tv.ui.components.PosterCardStyle
import com.nuvio.tv.ui.screens.home.HeroBackdropState
import com.nuvio.tv.ui.theme.NetflixThemeTokens
import com.nuvio.tv.ui.util.contentTextDirection

/** A presentation adapter over SearchViewModel's existing live search, history and catalog owners. */
@Composable
internal fun NetflixSearchScreen(
    viewModel: SearchViewModel,
    onNavigateToDetail: (String, String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val watchedMovies by viewModel.watchedMovieIds.collectAsState()
    val watchedSeries by viewModel.watchedSeriesIds.collectAsState()
    var restoreSearchFocus by remember {
        mutableStateOf(viewModel.hasSavedSearchFocus && viewModel.savedFocusRowKey == NETFLIX_SEARCH_GRID_KEY)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && viewModel.hasSavedSearchFocus &&
                viewModel.savedFocusRowKey == NETFLIX_SEARCH_GRID_KEY) {
                restoreSearchFocus = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(viewModel) {
        viewModel.setDiscoverSessionEnabled(true)
        onDispose { viewModel.setDiscoverSessionEnabled(false, keepLoadedResults = true) }
    }
    LaunchedEffect(uiState.query, uiState.discoverLocation) {
        viewModel.setDiscoverSessionEnabled(true)
        if (uiState.query.isBlank()) {
            viewModel.ensureDiscoverLoaded()
        }
    }
    val resultCatalogs = remember(uiState.catalogRows) {
        buildMap {
            uiState.catalogRows.forEach { row ->
                row.items.forEachIndexed { index, item ->
                    putIfAbsent("${item.apiType}:${item.id}", row to index)
                }
            }
        }
    }
    val saveAndNavigate: (MetaPreview, String, Int) -> Unit = { item, addon, index ->
        viewModel.savedFocusRowKey = NETFLIX_SEARCH_GRID_KEY
        viewModel.savedFocusItemIndex = index
        viewModel.hasSavedSearchFocus = true
        viewModel.onEvent(SearchEvent.RememberSearchFromTextInput)
        HeroBackdropState.update(viewModel.getCachedBackdrop(item.id, item.apiType) ?: item.backdropUrl)
        onNavigateToDetail(item.id, item.apiType, addon)
    }
    NetflixSearchContent(
        uiState = uiState,
        watchedMovieIds = watchedMovies,
        watchedSeriesIds = watchedSeries,
        initialFocusedIndex = viewModel.savedFocusItemIndex.coerceAtLeast(0),
        restoreFocus = restoreSearchFocus,
        onEvent = viewModel::onEvent,
        onNavigateToDetail = saveAndNavigate,
        onItemFocused = { item, _ ->
            viewModel.prefetchMetaOnFocus(item.id, item.rawType)
            resultCatalogs["${item.apiType}:${item.id}"]?.let { (row, index) ->
                if (row.supportsSkip && row.hasMore && !row.isLoading && index >= row.items.size - NetflixThemeTokens.searchColumns) {
                    viewModel.onEvent(SearchEvent.LoadMoreCatalog(row.catalogId, row.addonId, row.apiType))
                }
            }
        },
        onItemLongPress = { item, addon -> viewModel.posterOptions.show(item, addon) },
        onFocusRestored = {
            restoreSearchFocus = false
            viewModel.hasSavedSearchFocus = false
        }
    )
    val options by viewModel.posterOptions.state.collectAsState()
    com.nuvio.tv.ui.components.posteroptions.PosterOptionsHost(
        state = options,
        controller = viewModel.posterOptions,
        onNavigateToDetail = { id, type, addon ->
            val results = netflixSearchResults(uiState.catalogRows)
            val index = results.indexOfFirst { it.item.id == id && it.item.apiType == type }.coerceAtLeast(0)
            viewModel.savedFocusRowKey = NETFLIX_SEARCH_GRID_KEY
            viewModel.savedFocusItemIndex = index
            viewModel.hasSavedSearchFocus = true
            HeroBackdropState.update(viewModel.getCachedBackdrop(id, type))
            onNavigateToDetail(id, type, addon)
        }
    )
}

internal const val NETFLIX_SEARCH_GRID_KEY = "netflix_search_grid"
private const val NETFLIX_SEARCH_RESTORE_FRAMES = 12

internal data class NetflixSearchResult(val item: MetaPreview, val addonBaseUrl: String) {
    val key: String get() = "${item.apiType}:${item.id}"
}

/** Keep the first add-on source for each identity, including equal IDs of different content types. */
internal fun netflixSearchResults(rows: List<CatalogRow>): List<NetflixSearchResult> {
    val seen = HashSet<String>()
    return buildList {
        rows.forEach { row ->
            row.items.forEach { item ->
                if (!item.id.startsWith("__placeholder_") && seen.add("${item.apiType}:${item.id}")) {
                    add(NetflixSearchResult(item, row.addonBaseUrl))
                }
            }
        }
    }
}

/** Deterministic TV screen; callbacks reuse Nuvio state rather than creating a search backend. */
@Composable
internal fun NetflixSearchContent(
    uiState: SearchUiState,
    watchedMovieIds: Set<String> = emptySet(),
    watchedSeriesIds: Set<String> = emptySet(),
    initialFocusedIndex: Int = 0,
    restoreFocus: Boolean = false,
    onEvent: (SearchEvent) -> Unit,
    onNavigateToDetail: (MetaPreview, String, Int) -> Unit,
    onItemFocused: (MetaPreview, Int) -> Unit = { _, _ -> },
    onItemLongPress: (MetaPreview, String) -> Unit = { _, _ -> },
    onFocusRestored: () -> Unit = {}
) {
    val tokens = NetflixThemeTokens
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val language = LocalConfiguration.current.locales[0].language
    var arabicKeyboard by rememberSaveable { mutableStateOf(language == "ar") }
    var resultsHaveFocus by remember { mutableStateOf(false) }
    val keyboardRequester = remember { FocusRequester() }
    val resultRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val fieldRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val discoverAddonBaseUrl = uiState.discoverCatalogs
        .firstOrNull { it.key == uiState.selectedDiscoverCatalogKey }?.addonBaseUrl.orEmpty()
    val results = remember(uiState.catalogRows, uiState.discoverResults, uiState.query, uiState.submittedQuery, discoverAddonBaseUrl) {
        if (uiState.query.isBlank() && uiState.submittedQuery.isBlank()) {
            uiState.discoverResults.distinctBy { "${it.apiType}:${it.id}" }
                .map { NetflixSearchResult(it, it.sourceAddonBaseUrl?.takeIf(String::isNotBlank) ?: discoverAddonBaseUrl) }
        } else netflixSearchResults(uiState.catalogRows)
    }
    LaunchedEffect(results) { resultRequesters.keys.retainAll(results.mapTo(HashSet()) { it.key }) }
    // Return to a composed cell after Back from a deeply scrolled grid. Item zero can be disposed.
    val resultEntryRequester = results.getOrNull(gridState.firstVisibleItemIndex)
        ?.let { resultRequesters.getOrPut(it.key) { FocusRequester() } }
    val moveToResults = {
        onEvent(SearchEvent.RememberSearchFromTextInput)
        resultEntryRequester?.let { runCatching { it.requestFocus() } }
        Unit
    }
    val latestOnFocusRestored by rememberUpdatedState(onFocusRestored)
    var screenHasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        repeat(2) { withFrameNanos { } }
        // Keep the keyboard usable while an asynchronous saved-result restoration is pending, but
        // never take focus back from a result the restoration already focused.
        if (!screenHasFocus) runCatching { keyboardRequester.requestFocus() }
    }
    // Results may arrive after the screen resumes; retire the restoration only after a cell owns focus.
    LaunchedEffect(restoreFocus, results.size, initialFocusedIndex) {
        if (!restoreFocus || results.isEmpty()) return@LaunchedEffect
        val index = initialFocusedIndex.coerceIn(results.indices)
        gridState.scrollToItem(index)
        // The target cell composes on a later frame. The Boolean overload reports whether focus
        // really moved (the no-argument one returns Unit, which left this restoration armed forever
        // and let later result updates pull focus back into the grid).
        repeat(NETFLIX_SEARCH_RESTORE_FRAMES) {
            withFrameNanos { }
            val requester = resultRequesters[results[index].key]
            val moved = requester != null && runCatching { requester.requestFocus(FocusDirection.Enter) }
                .onFailure { android.util.Log.d("NetflixSearchRestore", "requestFocus threw: ${it.javaClass.simpleName}: ${it.message}") }
                .getOrDefault(false)
            android.util.Log.d("NetflixSearchRestore", "index=$index requester=${requester != null} moved=$moved firstVisible=${gridState.firstVisibleItemIndex}")
            if (moved) {
                latestOnFocusRestored()
                return@LaunchedEffect
            }
        }
    }
    var lastGridQuery by remember { mutableStateOf(uiState.query) }
    LaunchedEffect(uiState.query) {
        if (lastGridQuery != uiState.query) {
            lastGridQuery = uiState.query
            gridState.scrollToItem(0)
        }
    }
    BackHandler(enabled = resultsHaveFocus) {
        runCatching { keyboardRequester.requestFocus() }
    }
    val gridScope = rememberCoroutineScope()
    BoxWithConstraints(
        Modifier.fillMaxSize().background(tokens.background)
            .padding(horizontal = tokens.safeVerticalMargin, vertical = tokens.safeVerticalMargin)
            .onFocusChanged { screenHasFocus = it.hasFocus }
            .testTag("netflix_search")
    ) {
        val available = maxWidth - tokens.searchKeyboardWidth - tokens.profileGap
        // Measured reference: four ~150×210 dp posters; never wider than the space allows.
        val cardWidth = minOf(tokens.searchPosterWidth,
            (available - tokens.searchResultGap * (tokens.searchColumns - 1)) / tokens.searchColumns)
        val gridWidth = cardWidth * tokens.searchColumns + tokens.searchResultGap * (tokens.searchColumns - 1)
        val cardStyle = remember(cardWidth) {
            PosterCardStyle(
                width = cardWidth,
                height = cardWidth * (tokens.searchPosterHeight / tokens.searchPosterWidth),
                cornerRadius = tokens.cardRadius,
                focusedBorderWidth = tokens.focusedBorderWidth,
                focusedScale = tokens.focusScale
            )
        }
        // Keyboard on the reading-start side, results grid against the far edge.
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(
                Modifier.width(tokens.searchKeyboardWidth).focusGroup(),
                verticalArrangement = Arrangement.spacedBy(tokens.searchKeyGap)
            ) {
                // Reference: a compact search icon + query line, not a boxed form field.
                Row(
                    Modifier.fillMaxWidth().height(tokens.searchKeySize + tokens.searchKeyGap * 2)
                        .drawBehind {
                            drawLine(tokens.textMuted, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), strokeWidth = 1f)
                        }
                        .testTag("netflix_search_field"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tokens.searchKeyGap * 3),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = tokens.textSecondary, modifier = Modifier.size(tokens.navigationIconSize))
                    BasicTextField(
                        value = uiState.query,
                        onValueChange = { onEvent(SearchEvent.QueryChanged(it)) },
                        modifier = Modifier.weight(1f).focusRequester(fieldRequester),
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = NetflixThemeTokens.fontFamily, fontSize = tokens.searchQuerySize,
                            color = tokens.textPrimary, textDirection = uiState.query.contentTextDirection()),
                        cursorBrush = SolidColor(tokens.focus),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onEvent(SearchEvent.SubmitSearch); moveToResults() }),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (uiState.query.isEmpty()) {
                                    Text(stringResource(R.string.search_placeholder), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        style = TextStyle(fontFamily = NetflixThemeTokens.fontFamily, fontSize = tokens.searchQuerySize, color = tokens.textMuted))
                                }
                                inner()
                            }
                        },
                    )
                }
                // Characters are ordered for reading in each script. Row applies logical RTL placement.
                val alphabet = if (arabicKeyboard) "ابتثجحخدذرزسشصضطظعغفقكلمنهويءأإآةى" else "abcdefghijklmnopqrstuvwxyz0123456789"
                CompositionLocalProvider(LocalLayoutDirection provides if (arabicKeyboard) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    alphabet.chunked(6).forEachIndexed { rowIndex, letters ->
                        Row(horizontalArrangement = Arrangement.spacedBy(tokens.searchKeyGap)) {
                            letters.forEachIndexed { column, letter ->
                                NetflixKeyboardKey(
                                    text = letter.toString(),
                                    modifier = Modifier.size(tokens.searchKeySize)
                                        .then(if (rowIndex == 0 && column == 0) Modifier.focusRequester(keyboardRequester) else Modifier)
                                        .focusProperties {
                                            val gridFacingColumn = if (isRtl == arabicKeyboard) letters.lastIndex else 0
                                            if (resultEntryRequester != null && column == gridFacingColumn) {
                                                if (isRtl) left = resultEntryRequester else right = resultEntryRequester
                                            }
                                        },
                                    onClick = { onEvent(SearchEvent.QueryChanged(uiState.query + letter)) }
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.searchKeyGap)) {
                    NetflixKeyboardKey(
                        text = if (arabicKeyboard) "ABC" else "أ ب ج",
                        modifier = Modifier.weight(1f),
                        onClick = { arabicKeyboard = !arabicKeyboard }
                    )
                    NetflixKeyboardKey(
                        text = "␣", contentDescription = stringResource(R.string.netflix_search_space),
                        modifier = Modifier.weight(1f),
                        onClick = { onEvent(SearchEvent.QueryChanged(uiState.query + " ")) }
                    )
                    Button(
                        onClick = { onEvent(SearchEvent.QueryChanged(uiState.query.dropLast(1))) },
                        modifier = Modifier.weight(1f).height(tokens.searchKeySize),
                        contentPadding = PaddingValues(tokens.cardGap), shape = ButtonDefaults.shape(shape = tokens.buttonShape),
                        colors = ButtonDefaults.colors(containerColor = tokens.surface, focusedContainerColor = tokens.focus, focusedContentColor = tokens.focusContent)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, stringResource(R.string.netflix_search_delete), Modifier.size(tokens.navigationIconSize))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.searchKeyGap)) {
                    NetflixKeyboardKey(stringResource(R.string.action_clear), Modifier.weight(1f)) {
                        onEvent(SearchEvent.QueryChanged(""))
                    }
                    Button(
                        onClick = { runCatching { fieldRequester.requestFocus() } },
                        modifier = Modifier.weight(1f).height(tokens.searchKeySize),
                        contentPadding = PaddingValues(tokens.cardGap), shape = ButtonDefaults.shape(shape = tokens.buttonShape),
                        colors = ButtonDefaults.colors(containerColor = tokens.surface, focusedContainerColor = tokens.focus, focusedContentColor = tokens.focusContent)
                    ) {
                        Icon(Icons.Default.Keyboard, stringResource(R.string.netflix_search_system_keyboard), Modifier.size(tokens.navigationIconSize))
                    }
                }
            }
            Column(Modifier.width(gridWidth)) {
                if (uiState.query.isBlank() && uiState.recentSearches.isNotEmpty()) {
                    Text(stringResource(R.string.search_recent_title), color = tokens.textSecondary, style = MaterialTheme.typography.labelLarge)
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = tokens.rowTitleGap),
                        horizontalArrangement = Arrangement.spacedBy(tokens.cardGap)
                    ) {
                        uiState.recentSearches.take(3).forEach { recent ->
                            NetflixKeyboardKey(recent, Modifier.weight(1f)) {
                                onEvent(SearchEvent.QueryChanged(recent)); onEvent(SearchEvent.SubmitSearch)
                            }
                        }
                        NetflixKeyboardKey(stringResource(R.string.search_recent_clear), Modifier.weight(1f)) {
                            onEvent(SearchEvent.ClearRecentSearches)
                        }
                    }
                }
                // The query already sits in the compact query line; only browse mode needs a header.
                if (uiState.query.isBlank()) {
                    Text(
                        text = stringResource(R.string.nav_discover),
                        color = tokens.textSecondary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = tokens.rowTitleGap)
                    )
                }
                val currentError = if (uiState.query.isBlank()) uiState.discoverError else uiState.error
                when {
                    currentError != null && results.isEmpty() -> ErrorState(currentError, onRetry = {
                        onEvent(if (uiState.query.isBlank()) SearchEvent.RetryDiscover else SearchEvent.Retry)
                    })
                    results.isEmpty() && (uiState.isSearching || uiState.discoverLoading || (uiState.query.trim().length >= MIN_SEARCH_QUERY_LENGTH && uiState.query.trim() != uiState.submittedQuery.trim())) -> {
                        LazyVerticalGrid(columns = GridCells.Fixed(tokens.searchColumns), horizontalArrangement = Arrangement.spacedBy(tokens.searchResultGap), verticalArrangement = Arrangement.spacedBy(tokens.rowGap)) {
                            items(tokens.searchColumns * 3) { Box(Modifier.fillMaxWidth().height(cardStyle.height).background(tokens.surface, tokens.cardShape)) }
                        }
                    }
                    results.isEmpty() -> EmptyScreenState(
                        title = stringResource(if (uiState.query.trim().length >= MIN_SEARCH_QUERY_LENGTH) R.string.search_no_results_title else R.string.search_start_title),
                        subtitle = stringResource(if (uiState.query.trim().length >= MIN_SEARCH_QUERY_LENGTH) R.string.search_no_results_subtitle else R.string.search_start_subtitle_no_discover),
                        icon = Icons.Default.Search
                    )
                    else -> CompositionLocalProvider(com.nuvio.tv.ui.components.LocalNetflixPortraitCards provides true,
                        LocalBringIntoViewSpec provides NetflixExplicitGridScroll) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(tokens.searchColumns), state = gridState,
                            modifier = Modifier.fillMaxSize().onFocusChanged { resultsHaveFocus = it.hasFocus }.testTag("netflix_search_results"),
                            horizontalArrangement = Arrangement.spacedBy(tokens.searchResultGap),
                            verticalArrangement = Arrangement.spacedBy(tokens.rowGap),
                            // Rows are aligned to the top inset when focus moves, and the tall bottom padding
                            // lets the last rows align too, so no poster row is ever cut by the grid's top edge.
                            contentPadding = PaddingValues(top = tokens.metadataGap, bottom = this@BoxWithConstraints.maxHeight)
                        ) {
                            itemsIndexed(results, key = { _, result -> result.key }, contentType = { _, _ -> "poster_result" }) { index, result ->
                                GridContentCard(
                                    item = result.item, posterCardStyle = cardStyle, showLabel = false,
                                    focusRequester = resultRequesters.getOrPut(result.key) { FocusRequester() },
                                    isWatched = if (result.item.apiType in listOf("series", "tv")) result.item.id in watchedSeriesIds else result.item.id in watchedMovieIds,
                                    modifier = Modifier.fillMaxWidth().testTag("netflix_search_result_$index")
                                        .onPreviewKeyEvent { event ->
                                            val towardKeyboard = if (isRtl) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                                            if (index % tokens.searchColumns == 0 && event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && event.nativeKeyEvent.keyCode == towardKeyboard) {
                                                keyboardRequester.requestFocus(); true
                                            } else false
                                        },
                                    onFocused = {
                                        // A focused poster that is not fully visible brings its whole row to the
                                        // grid's top edge, so no row is ever left half under the header.
                                        gridScope.launch {
                                            val info = gridState.layoutInfo
                                            val cell = info.visibleItemsInfo.firstOrNull { it.index == index }
                                            if (cell == null || cell.offset.y < 0 || cell.offset.y + cell.size.height > info.viewportEndOffset) {
                                                gridState.animateScrollToItem(index - index % tokens.searchColumns)
                                            }
                                        }
                                        onItemFocused(result.item, index)
                                        if (uiState.query.isBlank() && index >= results.size - tokens.searchColumns) onEvent(SearchEvent.LoadNextDiscoverResults)
                                    },
                                    onClick = { onNavigateToDetail(result.item, result.addonBaseUrl, index) },
                                    onLongPress = { onItemLongPress(result.item, result.addonBaseUrl) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetflixKeyboardKey(
    text: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(NetflixThemeTokens.searchKeySize)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentPadding = PaddingValues(0.dp),
        shape = ButtonDefaults.shape(shape = NetflixThemeTokens.buttonShape),
        colors = ButtonDefaults.colors(
            containerColor = NetflixThemeTokens.surface,
            contentColor = NetflixThemeTokens.textPrimary,
            focusedContainerColor = NetflixThemeTokens.focus,
            focusedContentColor = NetflixThemeTokens.focusContent
        ),
        scale = ButtonDefaults.scale(focusedScale = 1f)
    ) {
        // The Nuvio UI font (Thmanyah Sans covers every Arabic key) at a legible size, not squeezed by
        // button padding (measured reference ~15–16 sp).
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontFamily = NetflixThemeTokens.fontFamily, fontSize = NetflixThemeTokens.searchKeyGlyph, fontWeight = FontWeight.Medium))
        }
    }
}

/** The results grid scrolls only by its own row-aligned rule (onFocused), never by the TV pivot. */
private object NetflixExplicitGridScroll : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}
