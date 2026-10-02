package com.nuvio.tv.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.EpisodeShuffleSettings
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.NextToWatch
import com.nuvio.tv.ui.components.SynopsisDescription
import com.nuvio.tv.ui.theme.NetflixThemeTokens
import com.nuvio.tv.ui.util.contentTextDirection
import com.nuvio.tv.ui.util.localizedGenreLabel
import java.util.Locale

/** The detail owner's alternate presentation. All actions still use its playback and list state. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun NetflixDetailHero(
    meta: Meta,
    nextToWatch: NextToWatch?,
    onPlayClick: () -> Unit,
    isPlayEnabled: Boolean,
    onPlayLongPress: (() -> Unit)?,
    onPlayFromBeginning: (() -> Unit)?,
    isInLibrary: Boolean,
    onToggleLibrary: () -> Unit,
    onLibraryLongPress: () -> Unit,
    isMovieWatched: Boolean,
    isMovieWatchedPending: Boolean,
    onToggleMovieWatched: () -> Unit,
    trailerAvailable: Boolean,
    onTrailerClick: () -> Unit,
    showRandomEpisodeButton: Boolean,
    episodeShuffle: EpisodeShuffleSettings,
    shuffleActionPending: Boolean,
    onRandomEpisodeClick: () -> Unit,
    randomEpisodeFocusRequester: FocusRequester?,
    isTrailerPlaying: Boolean,
    hideLogoDuringTrailer: Boolean,
    hideImdbRating: Boolean,
    tmdbRating: Float?,
    playButtonFocusRequester: FocusRequester?,
    restorePlayFocusToken: Int,
    onHeroActionFocused: () -> Unit,
    onPlayFocusRestored: () -> Unit,
    onShowFullDescription: () -> Unit,
    onTruncationChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    var logoFailed by remember(meta.logo) { mutableStateOf(false) }
    val logoRequest = remember(context, meta.logo) {
        meta.logo?.let { ImageRequest.Builder(context).data(it).crossfade(false).build() }
    }
    val genres = remember(meta.genres, context) {
        meta.genres.take(3).joinToString(" • ") { localizedGenreLabel(context, it) }
    }
    val cast = remember(meta.cast, meta.castMembers) {
        (meta.castMembers.map { it.name }.ifEmpty { meta.cast })
            .distinct().take(3).joinToString(", ")
    }
    val isSeries = meta.type == ContentType.SERIES || meta.type == ContentType.TV || meta.videos.isNotEmpty()
    val seasons = remember(meta.videos) { meta.videos.mapNotNull { it.season }.filter { it > 0 }.distinct().size }
    val runtimeMinutes = remember(meta.runtime) { netflixDetailRuntimeMinutes(meta.runtime) }
    val duration = if (isSeries && seasons > 0) {
        pluralStringResource(R.plurals.netflix_seasons, seasons, seasons)
    } else if (runtimeMinutes != null) {
        stringResource(R.string.netflix_runtime_minutes, runtimeMinutes)
    } else {
        null
    }
    val rating = if (!hideImdbRating) meta.imdbRating else null
    val ratingLabel = when {
        rating != null -> "IMDb ${String.format(Locale.getDefault(), "%.1f", rating)}/10"
        tmdbRating != null -> "TMDB ${String.format(Locale.getDefault(), "%.1f", tmdbRating)}/10"
        else -> null
    }
    val year = remember(meta.releaseInfo) { netflixDetailReleaseLabel(meta.releaseInfo) }

    Column(
        modifier = Modifier.fillMaxWidth()
            .height(screenHeight * NetflixThemeTokens.detailHeroHeightFraction)
            .padding(start = NetflixThemeTokens.safeMargin, end = NetflixThemeTokens.safeMargin,
                bottom = NetflixThemeTokens.safeVerticalMargin)
            .testTag("netflix_detail_hero"),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start
    ) {
        if (!hideLogoDuringTrailer || !isTrailerPlaying) {
            if (logoRequest != null && !logoFailed) {
                AsyncImage(
                    model = logoRequest,
                    contentDescription = meta.name,
                    onError = { logoFailed = true },
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth(NetflixThemeTokens.logoWidthFraction)
                        .height(NetflixThemeTokens.logoHeight)
                        .padding(bottom = NetflixThemeTokens.actionGap)
                )
            } else {
                Text(
                    text = meta.name,
                    style = MaterialTheme.typography.displayMedium.copy(textDirection = meta.name.contentTextDirection()),
                    color = NetflixThemeTokens.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(NetflixThemeTokens.detailContentWidthFraction)
                        .padding(bottom = NetflixThemeTokens.actionGap)
                )
            }
        }

        AnimatedVisibility(
            visible = !isTrailerPlaying,
            enter = fadeIn(tween(NetflixThemeTokens.screenTransitionMillis)),
            exit = fadeOut(tween(NetflixThemeTokens.screenTransitionMillis))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(NetflixThemeTokens.actionGap)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NetflixThemeTokens.metadataGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOfNotNull(ratingLabel, year, duration).forEach { value ->
                        Text(value, style = MaterialTheme.typography.labelLarge,
                            color = NetflixThemeTokens.textSecondary)
                    }
                    meta.ageRating?.trim()?.takeIf { it.isNotBlank() }?.let { age ->
                        Text(
                            age,
                            style = MaterialTheme.typography.labelMedium,
                            color = NetflixThemeTokens.textSecondary,
                            modifier = Modifier.border(BorderStroke(1.dp, NetflixThemeTokens.textMuted))
                                .padding(horizontal = NetflixThemeTokens.metadataGap)
                        )
                    }
                }
                if (genres.isNotBlank()) {
                    Text(genres, style = MaterialTheme.typography.labelLarge,
                        color = NetflixThemeTokens.textSecondary, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(NetflixThemeTokens.detailContentWidthFraction))
                }
                meta.description?.takeIf { it.isNotBlank() }?.let { description ->
                    SynopsisDescription(
                        description = description,
                        maxLines = 3,
                        onShowFullDescription = onShowFullDescription,
                        upFocusRequester = FocusRequester.Cancel,
                        downFocusRequester = playButtonFocusRequester,
                        onFocused = onHeroActionFocused,
                        onTruncationChanged = onTruncationChanged,
                        modifier = Modifier.fillMaxWidth(NetflixThemeTokens.detailContentWidthFraction)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NetflixThemeTokens.actionGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayButton(
                        text = if (isPlayEnabled) nextToWatch?.displayText ?: stringResource(R.string.hero_play)
                        else stringResource(R.string.playback_unavailable),
                        enabled = isPlayEnabled,
                        onClick = onPlayClick,
                        onLongPress = onPlayLongPress,
                        focusRequester = playButtonFocusRequester,
                        restoreFocusToken = restorePlayFocusToken,
                        onFocusRestored = { onHeroActionFocused(); onPlayFocusRestored() },
                        modifier = Modifier.height(NetflixThemeTokens.buttonHeight)
                    )
                    ActionIconButton(
                        icon = if (isInLibrary) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = stringResource(if (isInLibrary) R.string.hero_remove_from_library else R.string.hero_add_to_library),
                        onClick = onToggleLibrary,
                        onLongPress = onLibraryLongPress,
                        onFocused = onHeroActionFocused
                    )
                    if (!isSeries) {
                        ActionIconButton(
                            icon = if (isMovieWatched) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = stringResource(if (isMovieWatched) R.string.hero_mark_unwatched else R.string.hero_mark_watched),
                            onClick = onToggleMovieWatched,
                            enabled = !isMovieWatchedPending,
                            selected = isMovieWatched,
                            selectedContainerColor = NetflixThemeTokens.focus,
                            selectedContentColor = NetflixThemeTokens.focusContent,
                            onFocused = onHeroActionFocused
                        )
                    }
                    if (trailerAvailable) {
                        NetflixDetailSecondaryButton(stringResource(R.string.detail_tab_trailer), onTrailerClick, onHeroActionFocused)
                    }
                    if (nextToWatch?.isResume == true && onPlayFromBeginning != null) {
                        NetflixDetailSecondaryButton(
                            stringResource(R.string.cw_action_start_from_beginning),
                            onPlayFromBeginning, onHeroActionFocused
                        )
                    }
                    if (showRandomEpisodeButton) {
                        ActionIconButton(
                            icon = if (episodeShuffle.enabled) Icons.Default.Stop else Icons.Default.Shuffle,
                            contentDescription = stringResource(if (episodeShuffle.enabled) R.string.shuffle_stop else R.string.random_episode_title),
                            enabled = !shuffleActionPending,
                            onClick = onRandomEpisodeClick,
                            focusRequester = randomEpisodeFocusRequester,
                            onFocused = onHeroActionFocused
                        )
                    }
                }
                if (cast.isNotBlank()) {
                    Text(
                        stringResource(R.string.netflix_cast, cast),
                        style = MaterialTheme.typography.bodySmall.copy(textDirection = cast.contentTextDirection()),
                        color = NetflixThemeTokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(NetflixThemeTokens.detailContentWidthFraction)
                    )
                }
            }
        }
        if (isTrailerPlaying && !hideLogoDuringTrailer) {
            Text(stringResource(R.string.hero_press_back_trailer), style = MaterialTheme.typography.labelMedium,
                color = NetflixThemeTokens.textSecondary)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetflixDetailSecondaryButton(label: String, onClick: () -> Unit, onFocused: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(NetflixThemeTokens.buttonHeight).onFocusChanged { if (it.isFocused) onFocused() },
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(NetflixThemeTokens.buttonRadius)),
        colors = ButtonDefaults.colors(
            containerColor = NetflixThemeTokens.surfaceMuted,
            contentColor = NetflixThemeTokens.textPrimary,
            focusedContainerColor = NetflixThemeTokens.focus,
            focusedContentColor = NetflixThemeTokens.focusContent
        ),
        border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(NetflixThemeTokens.focusedBorderWidth, NetflixThemeTokens.focus))),
        scale = ButtonDefaults.scale(focusedScale = NetflixThemeTokens.episodeFocusScale),
        contentPadding = PaddingValues(horizontal = NetflixThemeTokens.actionGap)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** A factual Details panel; technical quality appears only after a real stream is selected. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun NetflixDetailFacts(
    meta: Meta,
    focusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    onShowFullDescription: () -> Unit
) {
    val context = LocalContext.current
    val facts = remember(meta.genres, meta.runtime, meta.country, meta.language, meta.awards, context) {
        listOfNotNull(
            meta.genres.takeIf { it.isNotEmpty() }?.joinToString(" • ") { localizedGenreLabel(context, it) },
            meta.runtime?.trim()?.takeIf { it.isNotBlank() && netflixDetailRuntimeMinutes(it) == null },
            listOfNotNull(meta.country?.takeIf { it.isNotBlank() }, meta.language?.takeIf { it.isNotBlank() }).joinToString(" • ").takeIf { it.isNotBlank() },
            meta.awards?.takeIf { it.isNotBlank() }
        )
    }
    val shape = RoundedCornerShape(NetflixThemeTokens.cardRadius)
    Card(
        onClick = onShowFullDescription,
        modifier = Modifier.fillMaxWidth().padding(horizontal = NetflixThemeTokens.safeMargin,
            vertical = NetflixThemeTokens.actionGap)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties { if (upFocusRequester != null) up = upFocusRequester }
            .testTag("netflix_detail_facts"),
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(containerColor = NetflixThemeTokens.surface,
            focusedContainerColor = NetflixThemeTokens.surfaceRaised),
        border = CardDefaults.border(focusedBorder = Border(BorderStroke(NetflixThemeTokens.focusedBorderWidth, NetflixThemeTokens.focus), shape = shape)),
        scale = CardDefaults.scale(focusedScale = 1f)
    ) {
        Column(modifier = Modifier.padding(NetflixThemeTokens.actionGap),
            verticalArrangement = Arrangement.spacedBy(NetflixThemeTokens.metadataGap)) {
            Text(meta.name, style = MaterialTheme.typography.titleMedium.copy(textDirection = meta.name.contentTextDirection()),
                color = NetflixThemeTokens.textPrimary)
            meta.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(description, style = MaterialTheme.typography.bodyMedium.copy(textDirection = description.contentTextDirection()),
                    color = NetflixThemeTokens.textSecondary, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            facts.forEach { fact ->
                Text(fact, style = MaterialTheme.typography.bodySmall.copy(textDirection = fact.contentTextDirection()),
                    color = NetflixThemeTokens.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
