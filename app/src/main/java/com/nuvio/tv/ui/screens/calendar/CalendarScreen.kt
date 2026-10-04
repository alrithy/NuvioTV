package com.nuvio.tv.ui.screens.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.R
import com.nuvio.tv.fork.discovery.CalendarDay
import com.nuvio.tv.fork.discovery.CalendarEpisode
import com.nuvio.tv.fork.discovery.CalendarFilter
import com.nuvio.tv.fork.discovery.CalendarRules
import com.nuvio.tv.fork.discovery.CalendarViewModel
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.theme.NuvioRadii
import com.nuvio.tv.ui.theme.NuvioTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CalendarCardWidth = 276.dp
private val CalendarCardHeight = 156.dp

/**
 * G9e Calendar (178–187): Cxsmo `CalendarScreen` @ 3e0d0fa (FILE_PORT). Days of the tracked series'
 * episodes 30 days back and 90 ahead; an unwatched episode whose previous one is unwatched keeps its
 * title and still hidden. Opening an episode goes to its show's detail page, focused on it.
 */
@Composable
fun CalendarScreen(
    showBuiltInHeader: Boolean = true,
    onNavigateToDetail: (itemId: String, itemType: String, returnFocusSeason: Int?, returnFocusEpisode: Int?) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contentFocusRequester = LocalContentFocusRequester.current
    val listState = rememberLazyListState()
    var scrolledToToday by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.days) {
        if (uiState.days.isNotEmpty() && !scrolledToToday) {
            scrolledToToday = true
            // Header is item 0, so a day's index is one more than in the list.
            val target = CalendarRules.firstUpcomingIndex(uiState.days, LocalDate.now())
            if (target > 0) listState.scrollToItem(target + 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(NuvioTheme.colors.Background)) {
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator(modifier = Modifier.size(48.dp))
            }
            uiState.days.isEmpty() && !uiState.isRefreshing -> EmptyCalendarState(
                selectedFilter = uiState.filter,
                onFilterSelect = viewModel::setFilter,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().focusRequester(contentFocusRequester),
                contentPadding = PaddingValues(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                item(key = "calendar_header") {
                    CalendarHeader(
                        selectedFilter = uiState.filter,
                        isRefreshing = uiState.isRefreshing,
                        onFilterSelect = viewModel::setFilter,
                        onRefresh = viewModel::refresh,
                        showTitle = showBuiltInHeader,
                        modifier = Modifier.padding(horizontal = 48.dp, vertical = 12.dp),
                    )
                }
                itemsIndexed(uiState.days, key = { _, day -> "day_${day.date}" }) { _, day ->
                    CalendarDaySection(
                        day = day,
                        onEpisodeClick = { episode ->
                            onNavigateToDetail(episode.showId, "series", episode.seasonNumber, episode.episodeNumber)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    selectedFilter: CalendarFilter,
    isRefreshing: Boolean,
    onFilterSelect: (CalendarFilter) -> Unit,
    onRefresh: () -> Unit,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showTitle) {
            Text(
                text = stringResource(R.string.calendar_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CalendarFilterChip(stringResource(R.string.calendar_filter_all), selectedFilter == CalendarFilter.ALL) {
                onFilterSelect(CalendarFilter.ALL)
            }
            CalendarFilterChip(stringResource(R.string.calendar_filter_upcoming), selectedFilter == CalendarFilter.UPCOMING) {
                onFilterSelect(CalendarFilter.UPCOMING)
            }
            CalendarFilterChip(stringResource(R.string.calendar_filter_past), selectedFilter == CalendarFilter.PAST) {
                onFilterSelect(CalendarFilter.PAST)
            }
            CalendarFilterChip(stringResource(R.string.calendar_refresh), selected = false, onClick = onRefresh)
            Spacer(modifier = Modifier.weight(1f))
            if (isRefreshing) LoadingIndicator(modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CalendarFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val colors = NuvioTheme.colors
    val shape = RoundedCornerShape(NuvioRadii.tokens.full)
    val container = when {
        focused -> Color.White.copy(alpha = 0.28f)
        selected -> colors.Secondary.copy(alpha = 0.22f)
        else -> Color.White.copy(alpha = 0.08f)
    }
    Card(
        onClick = onClick,
        modifier = Modifier.height(36.dp).onFocusChanged { focused = it.hasFocus },
        colors = CardDefaults.colors(containerColor = container, focusedContainerColor = container),
        border = CardDefaults.border(
            border = if (selected) Border(border = BorderStroke(1.dp, colors.Secondary.copy(alpha = 0.65f))) else Border.None,
            focusedBorder = Border(border = BorderStroke(2.dp, Color.White)),
        ),
        shape = CardDefaults.shape(shape = shape),
    ) {
        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected || focused) Color.White else Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun CalendarDaySection(day: CalendarDay, onEpisodeClick: (CalendarEpisode) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.padding(horizontal = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.height(22.dp).width(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(NuvioTheme.colors.Secondary),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = calendarDateHeader(day.date),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = calendarRelativeTag(day.date, day.episodes.size),
                color = Color.White.copy(alpha = 0.52f),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            )
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(day.episodes, key = { _, ep -> "ep_${ep.showId}_${ep.seasonNumber}_${ep.episodeNumber}" }) { _, episode ->
                CalendarEpisodeCard(episode = episode, onClick = { onEpisodeClick(episode) })
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CalendarEpisodeCard(episode: CalendarEpisode, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(10.dp)
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(durationMillis = 180), label = "calendarCardScale")
    Card(
        onClick = onClick,
        modifier = Modifier.width(CalendarCardWidth).height(CalendarCardHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.hasFocus },
        colors = CardDefaults.colors(containerColor = Color(0xFF141418), focusedContainerColor = Color(0xFF1C1C24)),
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = Border(border = BorderStroke(3.dp, Color(0xFFE5A00D)), shape = cardShape),
        ),
        shape = CardDefaults.shape(shape = cardShape),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CalendarEpisodeArtwork(episode)
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.96f)),
                    ),
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp).align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = episode.showTitle,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (episode.isWatched) {
                    Box(
                        modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF2E7D32)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.episodes_cd_watched),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp).align(Alignment.BottomStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE5A00D))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.season_episode_format, episode.seasonNumber, episode.episodeNumber),
                        color = Color(0xFF1E1400),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
                    )
                }
                Text(
                    text = if (episode.isSpoilerHidden) {
                        stringResource(R.string.calendar_spoiler_hidden)
                    } else {
                        episode.episodeTitle ?: stringResource(R.string.calendar_episode_fallback, episode.episodeNumber)
                    },
                    color = Color.White.copy(alpha = if (episode.isSpoilerHidden) 0.58f else 0.88f),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The spoiler still is never requested; show art is blurred where supported and darkened everywhere. */
@Composable
private fun CalendarEpisodeArtwork(episode: CalendarEpisode) {
    val context = LocalContext.current
    val candidates = remember(episode) { episode.artworkCandidates() }
    var index by remember(episode.episodeId, candidates) { mutableIntStateOf(0) }
    val url = candidates.getOrNull(index)
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF262637), Color(0xFF111118)))),
    ) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(url).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { if (index < candidates.lastIndex) index += 1 },
                modifier = Modifier.fillMaxSize().then(if (episode.isSpoilerHidden) Modifier.blur(18.dp) else Modifier),
            )
        }
        if (episode.isSpoilerHidden) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)))
        }
    }
}

@Composable
private fun EmptyCalendarState(
    selectedFilter: CalendarFilter,
    onFilterSelect: (CalendarFilter) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = stringResource(R.string.calendar_empty_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.calendar_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.60f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CalendarFilterChip(stringResource(R.string.calendar_filter_all), selectedFilter == CalendarFilter.ALL) {
                    onFilterSelect(CalendarFilter.ALL)
                }
                CalendarFilterChip(stringResource(R.string.calendar_refresh), selected = false, onClick = onRefresh)
            }
        }
    }
}

@Composable
private fun calendarDateHeader(date: LocalDate): String {
    val today = LocalDate.now()
    val locale = Locale.getDefault()
    val short = remember(date, locale) { date.format(DateTimeFormatter.ofPattern("MMMM d", locale)) }
    return when (CalendarRules.daysFrom(today, date)) {
        0L -> stringResource(R.string.calendar_today, short)
        1L -> stringResource(R.string.calendar_tomorrow, short)
        -1L -> stringResource(R.string.calendar_yesterday, short)
        else -> remember(date, locale) { date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale)) }
    }
}

@Composable
private fun calendarRelativeTag(date: LocalDate, count: Int): String {
    val episodes = LocalContext.current.resources.getQuantityString(R.plurals.calendar_episode_count, count, count)
    val days = CalendarRules.daysFrom(LocalDate.now(), date)
    return when {
        days < 0 -> stringResource(R.string.calendar_tag_past, episodes)
        days in 2..7 -> stringResource(R.string.calendar_tag_in_days, days.toInt(), episodes)
        else -> episodes
    }
}
