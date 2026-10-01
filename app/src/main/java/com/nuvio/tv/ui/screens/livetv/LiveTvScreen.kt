@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.livetv

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.fork.livetv.LiveTvChannel
import com.nuvio.tv.fork.livetv.LiveTvFilterKeys
import com.nuvio.tv.fork.livetv.LiveTvOrganisation
import com.nuvio.tv.fork.livetv.LiveTvProgramme
import com.nuvio.tv.fork.livetv.LiveTvSource
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Live TV (G10b, features 208, 219–225): categories on the left, channels in the middle, the last
 * channel on top. FILE_PORT of Reshaped `ui/reshaped/livetv/LiveTvScreen.kt` @ 0ccf049, keyed by
 * channel key; what is on now, its progress and time left come from the guide (G10c); the preview
 * (G10f) joins it in its slice.
 * Rows are plain and fixed height (no blur, no images larger than drawn) so thousands of channels
 * scroll smoothly on low-end TVs.
 */
@Composable
fun LiveTvScreen(
    onPlay: (String) -> Unit,
    showBuiltInHeader: Boolean = true,
    viewModel: LiveTvViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val repository = viewModel.repository
    // Before the state is read, so a list let go while unused shows as loading, never as empty.
    remember(viewModel) { viewModel.ensureLoaded() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        // Back from the background after a long while: the list may have been let go meanwhile.
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_START) viewModel.ensureLoaded() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val state by repository.state.collectAsStateWithLifecycle()
    // A release that raced the screen coming back leaves an empty, idle state: load again.
    LaunchedEffect(state.isLoaded, state.isLoading, state.hasSource) {
        if (!state.isLoaded && !state.isLoading && !state.hasSource) viewModel.ensureLoaded()
    }
    val library = state.library
    val scope = rememberCoroutineScope()
    var filterKey by rememberSaveable { mutableStateOf(LiveTvFilterKeys.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var launching by remember { mutableStateOf(false) }
    val channelListState = rememberLazyListState()
    val channelFocus = remember { FocusRequester() }
    val categoryFocus = remember { FocusRequester() }
    val started = lifecycleOwner.lifecycle.currentStateAsState().value.isAtLeast(Lifecycle.State.STARTED)
    // Under the pill menu the screen starts below it, as Settings does, so the pill never covers the search field.
    val topPadding = if (showBuiltInHeader) NuvioTheme.spacing.xl else 68.dp

    // A category that was hidden, or a source that was removed, falls back to all channels.
    LaunchedEffect(library.hiddenGroups, state.sources) {
        if (LiveTvOrganisation.isStale(filterKey, state)) filterKey = LiveTvFilterKeys.ALL
    }

    // Filtered off the main thread: lists can hold tens of thousands of channels.
    val filterInput = LiveTvFilterInput(state.channels, library, filterKey, query)
    val visibleChannels = viewModel.visibleChannels
    val filtering = !viewModel.isFilteredFor(filterInput)
    LaunchedEffect(state.channels, library.favorites, library.hiddenGroups, library.hiddenChannels, filterKey, query) {
        if (viewModel.isFilteredFor(filterInput)) return@LaunchedEffect
        if (query.isNotEmpty()) delay(200) // typing
        val filtered = withContext(Dispatchers.Default) {
            LiveTvOrganisation.filter(filterInput.channels, filterInput.library, filterInput.filterKey, filterInput.query)
        }
        viewModel.setVisible(filterInput, filtered)
    }
    // Back to the top only when the category changes, not each time the screen comes back.
    var scrolledForKey by rememberSaveable { mutableStateOf(filterKey) }
    LaunchedEffect(filterKey) {
        if (scrolledForKey != filterKey) {
            scrolledForKey = filterKey
            channelListState.scrollToItem(0)
        }
    }

    val recentChannel = remember(state.channels, library.recent) { repository.recentChannel(state) }
    val showsRecentRow = recentChannel != null && filterKey == LiveTvFilterKeys.ALL && query.isEmpty()
    // Back from the player: focus the channel last watched (or the first one), scrolled into view.
    var focusTargetKey by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(visibleChannels, filtering) {
        if (!viewModel.restoreFocusOnReturn || filtering || visibleChannels.isEmpty()) return@LaunchedEffect
        viewModel.restoreFocusOnReturn = false
        val index = visibleChannels.indexOfFirst { it.key == library.recent?.key }.coerceAtLeast(0)
        focusTargetKey = visibleChannels[index].key
        val itemIndex = index + if (showsRecentRow) 1 else 0
        if (channelListState.layoutInfo.visibleItemsInfo.none { it.index == itemIndex }) {
            channelListState.scrollToItem((itemIndex - 2).coerceAtLeast(0))
        }
        withFrameNanos { }
        withFrameNanos { }
        runCatching { channelFocus.requestFocus() }
    }
    val minuteClock = rememberLiveTvMinuteClock()
    // Stays set until the player has taken over the screen.
    LaunchedEffect(started) { if (!started) launching = false }

    // Unfavouriting a channel in Favorites removes its row: focus moves to the next one (or the
    // category when none is left) instead of falling back to the top of the categories.
    var refocusAfterRemoval by remember { mutableStateOf(false) }
    LaunchedEffect(visibleChannels, filtering) {
        if (!refocusAfterRemoval || filtering) return@LaunchedEffect
        refocusAfterRemoval = false
        val target = if (visibleChannels.any { it.key == focusTargetKey }) channelFocus else categoryFocus
        repeat(10) {
            withFrameNanos { }
            if (runCatching { target.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }
    val toggleFavorite: (LiveTvChannel) -> Unit = { channel ->
        if (filterKey == LiveTvFilterKeys.FAVORITES && channel.key in library.favorites) {
            val index = visibleChannels.indexOfFirst { it.key == channel.key }
            focusTargetKey = (visibleChannels.getOrNull(index + 1) ?: visibleChannels.getOrNull(index - 1))?.key
            refocusAfterRemoval = true
        }
        repository.toggleFavorite(channel)
    }

    val play: (LiveTvChannel) -> Unit = { channel ->
        if (!launching) {
            launching = true
            // G10e: zapping in the player stays in the list the channel was picked from (all shown channels when it is not in it).
            repository.setZapList(visibleChannels, filterKey.takeIf { query.isEmpty() })
            scope.launch {
                try {
                    val route = viewModel.playerRoute(channel)
                    viewModel.restoreFocusOnReturn = true
                    onPlay(route)
                } catch (cancel: CancellationException) {
                    launching = false
                    throw cancel
                } catch (_: Exception) {
                    launching = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NuvioTheme.colors.Background),
    ) {
        if (!state.isLoading && !state.hasSource) {
            LiveTvEmptyState(onAddSource = { showSourceDialog = true })
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = NuvioTheme.spacing.xxxl, end = NuvioTheme.spacing.xl, top = topPadding),
                horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xl),
            ) {
                val visibleGroups = remember(state.groups, library.hiddenGroups) { state.visibleGroups }
                LiveTvCategoryColumn(
                    showHeader = showBuiltInHeader,
                    channelCount = state.channels.size,
                    sources = if (state.sources.size > 1) state.sources else emptyList(),
                    groups = visibleGroups,
                    groupNames = library.groupNames,
                    selectedKey = filterKey,
                    selectedFocus = categoryFocus,
                    onSelect = { filterKey = it },
                    modifier = Modifier.width(260.dp).fillMaxHeight(),
                )
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm),
                    ) {
                        LiveTvTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = stringResource(R.string.live_tv_search),
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                            modifier = Modifier.weight(1f),
                        )
                        LiveTvPillButton(
                            text = stringResource(R.string.live_tv_refresh),
                            onClick = { repository.refresh() },
                            enabled = !state.isLoading,
                        )
                        LiveTvPillButton(
                            text = stringResource(R.string.live_tv_edit_categories),
                            onClick = { showCategoryDialog = true },
                        )
                        LiveTvPillButton(
                            text = stringResource(R.string.live_tv_sources),
                            onClick = { showSourceDialog = true },
                        )
                    }
                    Spacer(Modifier.height(NuvioTheme.spacing.md))
                    val failedSource = state.sources.firstOrNull { it.id in state.sourceErrors }
                    val status = when {
                        state.isLoading -> stringResource(R.string.live_tv_loading)
                        failedSource != null -> stringResource(
                            R.string.live_tv_source_error,
                            failedSource.label,
                            state.sourceErrors[failedSource.id]?.message(context).orEmpty(),
                        )
                        state.error != null -> state.error?.message(context)
                        state.isEpgLoading -> stringResource(R.string.live_tv_guide_loading)
                        else -> null
                    }
                    if (status != null) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = if ((failedSource != null || state.error != null) && !state.isLoading) {
                                NuvioTheme.colors.Error
                            } else {
                                NuvioTheme.colors.TextSecondary
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = NuvioTheme.spacing.sm),
                        )
                    }
                    LazyColumn(
                        state = channelListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = NuvioTheme.spacing.xxl, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (recentChannel != null && showsRecentRow) {
                            item(key = "recent", contentType = "recent") {
                                LiveTvRecentRow(
                                    channel = recentChannel,
                                    logo = state.logoFor(recentChannel),
                                    programme = state.currentProgrammes[recentChannel.guideKey],
                                    clock = minuteClock,
                                    groupName = liveTvGroupName(recentChannel.group, library.groupNames),
                                    onClick = { play(recentChannel) },
                                )
                            }
                        }
                        items(visibleChannels, key = { it.id }, contentType = { "channel" }) { channel ->
                            LiveTvChannelRow(
                                channel = channel,
                                logo = state.logoFor(channel),
                                programme = state.currentProgrammes[channel.guideKey],
                                clock = minuteClock,
                                isFavorite = channel.key in library.favorites,
                                groupName = liveTvGroupName(channel.group, library.groupNames),
                                onClick = { play(channel) },
                                onLongClick = { toggleFavorite(channel) },
                                modifier = if (channel.key == focusTargetKey) Modifier.focusRequester(channelFocus) else Modifier,
                            )
                        }
                        if (visibleChannels.isEmpty() && !filtering && state.isLoaded && !state.isLoading) {
                            item(key = "empty") {
                                Text(
                                    text = stringResource(
                                        if (filterKey == LiveTvFilterKeys.FAVORITES) R.string.live_tv_no_favorites else R.string.live_tv_no_channels_found,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NuvioTheme.colors.TextSecondary,
                                    modifier = Modifier.padding(vertical = NuvioTheme.spacing.lg),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSourceDialog) {
        LiveTvSourceDialog(repository = repository, onDismiss = { showSourceDialog = false })
    }
    if (showCategoryDialog) {
        LiveTvCategoryDialog(repository = repository, onDismiss = { showCategoryDialog = false })
    }
}

@Composable
private fun LiveTvCategoryColumn(
    showHeader: Boolean,
    channelCount: Int,
    sources: List<LiveTvSource>,
    groups: List<String>,
    groupNames: Map<String, String>,
    selectedKey: String,
    selectedFocus: FocusRequester,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedModifier = Modifier.focusRequester(selectedFocus)
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.live_tv_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = if (showHeader) NuvioTheme.colors.TextPrimary else Color.Transparent,
        )
        Text(
            text = stringResource(R.string.live_tv_channel_count, channelCount),
            style = MaterialTheme.typography.labelLarge,
            color = NuvioTheme.colors.TextTertiary,
            modifier = Modifier.padding(top = 2.dp, bottom = NuvioTheme.spacing.md),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = NuvioTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = LiveTvFilterKeys.ALL) {
                LiveTvCategoryItem(stringResource(R.string.live_tv_all_channels), selectedKey == LiveTvFilterKeys.ALL, selectedModifier) {
                    onSelect(LiveTvFilterKeys.ALL)
                }
            }
            item(key = LiveTvFilterKeys.FAVORITES) {
                LiveTvCategoryItem(stringResource(R.string.live_tv_favorites), selectedKey == LiveTvFilterKeys.FAVORITES, selectedModifier) {
                    onSelect(LiveTvFilterKeys.FAVORITES)
                }
            }
            // With several sources, each can be browsed on its own.
            items(sources, key = { LiveTvFilterKeys.source(it.id) }) { source ->
                val key = LiveTvFilterKeys.source(source.id)
                LiveTvCategoryItem(source.label, selectedKey == key, selectedModifier) { onSelect(key) }
            }
            if (sources.isNotEmpty()) {
                item(key = "\u0000divider") {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(NuvioTheme.colors.TextPrimary.copy(alpha = 0.08f)),
                    )
                }
            }
            items(groups, key = { it }) { group ->
                LiveTvCategoryItem(liveTvGroupLabel(group, groupNames), selectedKey == group, selectedModifier) { onSelect(group) }
            }
        }
    }
}

/** A category: selecting happens on focus, so the list follows the remote. */
@Composable
private fun LiveTvCategoryItem(label: String, selected: Boolean, selectedModifier: Modifier, onSelect: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused) {
        if (focused && !selected) {
            delay(250) // passing over a category does not re-filter
            onSelect()
        }
    }
    val shape = RoundedCornerShape(10.dp)
    Card(
        onClick = onSelect,
        modifier = (if (selected) selectedModifier else Modifier)
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(
            containerColor = if (selected) NuvioTheme.colors.TextPrimary.copy(alpha = 0.08f) else Color.Transparent,
            focusedContainerColor = NuvioTheme.colors.TextPrimary,
        ),
        scale = CardDefaults.scale(focusedScale = 1.02f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (focused) Color.Black else if (selected) NuvioTheme.colors.TextPrimary else NuvioTheme.colors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun LiveTvRecentRow(
    channel: LiveTvChannel,
    logo: String?,
    programme: LiveTvProgramme?,
    clock: State<Long>,
    groupName: String?,
    onClick: () -> Unit,
) {
    LiveTvRowCard(onClick = onClick, onLongClick = null, tall = true) { focused ->
        LiveTvLogo(url = logo, name = channel.name, width = 88.dp, height = 54.dp)
        Column(modifier = Modifier.weight(1f).padding(start = NuvioTheme.spacing.md)) {
            Text(
                text = stringResource(R.string.live_tv_continue).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.5.sp,
                color = if (focused) Color.Black.copy(alpha = 0.6f) else NuvioTheme.colors.TextTertiary,
            )
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (focused) Color.Black else NuvioTheme.colors.TextPrimary,
            )
            ProgrammeLine(programme, clock, focused, fallback = groupName ?: channel.group)
        }
    }
}

@Composable
private fun LiveTvChannelRow(
    channel: LiveTvChannel,
    logo: String?,
    programme: LiveTvProgramme?,
    clock: State<Long>,
    isFavorite: Boolean,
    groupName: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiveTvRowCard(onClick = onClick, onLongClick = onLongClick, tall = false, modifier = modifier) { focused ->
        LiveTvLogo(url = logo, name = channel.name, width = 66.dp, height = 40.dp)
        Column(modifier = Modifier.weight(1f).padding(start = NuvioTheme.spacing.md)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (focused) Color.Black else NuvioTheme.colors.TextPrimary,
            )
            ProgrammeLine(programme, clock, focused, fallback = groupName ?: channel.group)
        }
        if (isFavorite) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = stringResource(R.string.live_tv_favorites),
                tint = if (focused) Color.Black else NuvioTheme.colors.TextSecondary,
                modifier = Modifier.padding(start = NuvioTheme.spacing.sm).size(18.dp),
            )
        }
    }
}

/** What is on now, a thin progress bar and the time left, or the category when there is no guide. */
@Composable
private fun ProgrammeLine(programme: LiveTvProgramme?, clock: State<Long>, focused: Boolean, fallback: String = "") {
    val secondary = if (focused) Color.Black.copy(alpha = 0.65f) else NuvioTheme.colors.TextSecondary
    if (programme == null) {
        if (fallback.isNotBlank()) {
            Text(text = fallback, style = MaterialTheme.typography.bodySmall, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        return
    }
    Text(
        text = programme.title,
        style = MaterialTheme.typography.bodySmall,
        color = secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        LiveTvProgressBar(
            programme = programme,
            clock = clock,
            fill = if (focused) Color.Black else NuvioTheme.colors.TextPrimary,
            track = if (focused) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.12f),
            modifier = Modifier.weight(1f, fill = false).widthIn(max = 160.dp).fillMaxWidth(),
        )
        Text(
            text = liveTvTimeLeft(programme, clock),
            style = MaterialTheme.typography.labelSmall,
            color = if (focused) Color.Black.copy(alpha = 0.55f) else NuvioTheme.colors.TextTertiary,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun LiveTvRowCard(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    tall: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val container by animateColorAsState(
        if (focused) NuvioTheme.colors.TextPrimary else NuvioTheme.colors.TextPrimary.copy(alpha = 0.035f),
        label = "liveTvRow",
    )
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(containerColor = container, focusedContainerColor = container),
        scale = CardDefaults.scale(focusedScale = 1.015f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (tall) 76.dp else 58.dp)
                .padding(horizontal = NuvioTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content(focused)
        }
    }
}

@Composable
private fun LiveTvEmptyState(onAddSource: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(
        modifier = Modifier.fillMaxSize().padding(NuvioTheme.spacing.xxxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.live_tv_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = NuvioTheme.colors.TextPrimary,
        )
        Text(
            text = stringResource(R.string.live_tv_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = NuvioTheme.colors.TextSecondary,
            modifier = Modifier.widthIn(max = 520.dp).padding(top = NuvioTheme.spacing.sm, bottom = NuvioTheme.spacing.lg),
            textAlign = TextAlign.Center,
        )
        LiveTvPillButton(
            text = stringResource(R.string.live_tv_add_source),
            onClick = onAddSource,
            modifier = Modifier.focusRequester(focus),
        )
    }
}
