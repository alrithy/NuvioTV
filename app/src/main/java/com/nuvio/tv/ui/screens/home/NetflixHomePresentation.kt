package com.nuvio.tv.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.nuvio.tv.R
import com.nuvio.tv.fork.resource.MemoryTier
import com.nuvio.tv.ui.theme.NetflixThemeTokens
import com.nuvio.tv.ui.theme.NetflixPresentationPolicy
import com.nuvio.tv.ui.theme.netflixPresentationPolicy
import com.nuvio.tv.ui.util.contentTextDirection
import com.nuvio.tv.ui.util.localizedGenreLabel
import com.nuvio.tv.ui.components.TrailerPlayer
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.fork.resource.AdaptiveResources
import kotlin.math.roundToInt

/** Presentation policy reads the one installed resource owner; it performs no device probing. */
internal fun netflixHomePreviewPolicy(tier: MemoryTier): NetflixPresentationPolicy = netflixPresentationPolicy(tier)

/** Deterministic title composition, shared by the live Modern hero and the screenshot harness. */
@Composable
internal fun NetflixHeroTitleContent(
    preview: HeroPreview,
    onPlay: () -> Unit,
    onMoreInfo: () -> Unit,
    modifier: Modifier = Modifier,
    playFocusRequester: FocusRequester? = null,
    onDownToRows: (() -> Unit)? = null,
    showImdbRatings: Boolean = true,
    callout: NetflixCallout? = null
) {
    val tokens = NetflixThemeTokens
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val logoWidthPx = with(density) { (screenWidthDp.dp * tokens.heroMetadataWidthFraction).roundToPx() }
    val logoModel = remember(context, preview.logo, density, logoWidthPx) {
        preview.logo?.let { url ->
            ImageRequest.Builder(context).data(url)
                .size(logoWidthPx, with(density) { tokens.logoHeight.roundToPx() })
                .build()
        }
    }
    var logoFailed by remember(preview.logo) { mutableStateOf(false) }
    Column(
        modifier = modifier.testTag("netflix_home_hero"),
        verticalArrangement = Arrangement.spacedBy(tokens.metadataGap)
    ) {
        if (callout != null) NetflixCalloutChip(callout)
        if (logoModel != null && !logoFailed) {
            AsyncImage(
                model = logoModel,
                contentDescription = preview.title,
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth().height(tokens.logoHeight),
                onError = { logoFailed = true }
            )
        } else {
            Text(
                preview.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = tokens.heroTitle,
                    fontWeight = FontWeight.Bold,
                    textDirection = preview.title.contentTextDirection()
                ),
                color = tokens.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        NetflixPreviewMetadata(preview, showImdbRatings = showImdbRatings)
        val genres = remember(preview.genres, context) {
            preview.genres.list.take(3).joinToString(" • ") { localizedGenreLabel(context, it) }
        }
        if (genres.isNotBlank()) {
            Text(genres, style = MaterialTheme.typography.labelMedium, color = tokens.textPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        preview.description?.takeIf(String::isNotBlank)?.let { description ->
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = description.contentTextDirection()),
                color = tokens.textPrimary,
                maxLines = tokens.descriptionMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.actionGap)) {
            NetflixHomeAction(
                icon = Icons.Default.PlayArrow,
                label = stringResource(R.string.hero_play),
                onClick = onPlay,
                primary = true,
                modifier = Modifier.testTag("netflix_hero_play")
                    .then(if (playFocusRequester != null) Modifier.focusRequester(playFocusRequester) else Modifier),
                onDownToRows = onDownToRows
            )
            NetflixHomeAction(
                icon = Icons.Default.Info,
                label = stringResource(R.string.netflix_more_info),
                onClick = onMoreInfo,
                modifier = Modifier.testTag("netflix_hero_more_info"),
                onDownToRows = onDownToRows
            )
        }
    }
}

/** Hero-only catalogs still use the existing Modern backdrop owner and the same genuine actions. */
@Composable
internal fun NetflixHeroOnlyContent(
    item: MetaPreview,
    onPlay: (String, String, String) -> Unit,
    onMoreInfo: (String, String, String) -> Unit,
    onItemFocus: (MetaPreview) -> Unit,
    showImdbRatings: Boolean
) {
    val tokens = NetflixThemeTokens
    val context = LocalContext.current
    val density = LocalDensity.current
    val policy = netflixHomePreviewPolicy(AdaptiveResources.policy.tier)
    val focusRequester = LocalContentFocusRequester.current
    val movieLabel = stringResource(R.string.type_movie)
    val seriesLabel = stringResource(R.string.type_series)
    val carouselItem = remember(item, movieLabel, seriesLabel) {
        val origin = item.sourceAddonBaseUrl.orEmpty()
        val source = CatalogRow("netflix_hero", "", origin, "hero", "", item.type,
            rawType = item.apiType, items = emptyList(), hasMore = false)
        buildCatalogItem(item, source, true, 0, movieLabel, seriesLabel)
    }
    val scene = rememberUpdatedState(ModernHeroSceneState(
        heroBackdrop = item.backdropUrl,
        preview = carouselItem.heroPreview,
        enrichmentActive = false,
        shouldPlayTrailer = false,
        trailerFirstFrameRendered = false,
        trailerUrl = null,
        trailerAudioUrl = null,
        trailerPlaybackKey = null,
        trailerMuted = true,
        fullScreenBackdrop = true
    ))
    val sceneProvider = remember { { scene.value } }
    BoxWithConstraints(Modifier.fillMaxSize().background(tokens.background)) {
        val requestWidth = with(density) { maxWidth.roundToPx() }.coerceAtMost(policy.heroMaxWidthPx)
        val requestHeight = (requestWidth / tokens.landscapeAspectRatio).roundToInt()
        ModernHeroScene(sceneProvider, { true }, tokens.background, Modifier.fillMaxSize(),
            requestWidth, requestHeight, onTrailerEnded = {}, onFirstFrameRendered = {})
        NetflixHeroTitleContent(carouselItem.heroPreview,
            onPlay = { onPlay(item.id, item.apiType, item.sourceAddonBaseUrl.orEmpty()) },
            onMoreInfo = { onMoreInfo(item.id, item.apiType, item.sourceAddonBaseUrl.orEmpty()) },
            playFocusRequester = focusRequester,
            showImdbRatings = showImdbRatings,
            modifier = Modifier.align(Alignment.CenterStart)
                .padding(start = tokens.safeMargin, end = tokens.safeMargin)
                .fillMaxWidth(tokens.heroMetadataWidthFraction)
                .onFocusChanged { if (it.hasFocus) onItemFocus(item) })
    }
}

/** One focused artwork request, with authentic Nuvio library/play callbacks and no video owner. */
@Composable
internal fun NetflixExpandedCardContent(
    item: ModernCarouselItem,
    onPlay: () -> Unit,
    onLibrary: () -> Unit,
    onMoreInfo: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = NetflixThemeTokens.landscapeCardWidth * NetflixThemeTokens.expandedScale,
    inLibrary: Boolean = false,
    libraryPending: Boolean = false,
    playFocusRequester: FocusRequester? = null,
    onActionFocused: (Int) -> Unit = {},
    showImdbRatings: Boolean = true,
    trailerPreviewUrl: String? = null,
    trailerPreviewAudioUrl: String? = null,
    trailerMuted: Boolean = true,
    onTrailerEnded: () -> Unit = {}
) {
    val tokens = NetflixThemeTokens
    val context = LocalContext.current
    val density = LocalDensity.current
    val imageUrl = item.metaPreview?.landscapePoster ?: item.heroPreview.backdrop ?: item.imageUrl
    val imageModel = remember(imageUrl, context, density, width) {
        imageUrl?.let { url ->
            ImageRequest.Builder(context).data(url)
                .size(with(density) { width.roundToPx() }, with(density) { (width / tokens.landscapeAspectRatio).roundToPx() })
                .build()
        }
    }
    Column(
        modifier = modifier.width(width).clip(tokens.cardShape)
            .background(tokens.surfaceRaised).testTag("netflix_expanded_card")
    ) {
        Box(Modifier.fillMaxWidth().height(width / tokens.landscapeAspectRatio)) {
            AsyncImage(imageModel, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (!trailerPreviewUrl.isNullOrBlank()) {
                TrailerPlayer(trailerUrl = trailerPreviewUrl, trailerAudioUrl = trailerPreviewAudioUrl,
                    isPlaying = true, onEnded = onTrailerEnded, muted = trailerMuted,
                    cropToFill = true, modifier = Modifier.fillMaxSize())
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(tokens.heroBottomStops.map { it.second })))
            var logoFailed by remember(item.heroPreview.logo) { mutableStateOf(false) }
            if (!item.heroPreview.logo.isNullOrBlank() && !logoFailed) {
                val logoModel = remember(item.heroPreview.logo, context, density, width) {
                    ImageRequest.Builder(context).data(item.heroPreview.logo)
                        .size(with(density) { width.roundToPx() }, with(density) { tokens.buttonHeight.roundToPx() }).build()
                }
                AsyncImage(logoModel, item.title, contentScale = ContentScale.Fit, alignment = Alignment.CenterStart,
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .height(tokens.logoHeight / 2).padding(tokens.previewPadding),
                    onError = { logoFailed = true })
            } else {
                Text(item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold,
                        textDirection = item.title.contentTextDirection()),
                    color = tokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(tokens.previewPadding))
            }
        }
        Column(Modifier.padding(tokens.previewPadding), verticalArrangement = Arrangement.spacedBy(tokens.metadataGap)) {
            netflixCallout(item.payload, inLibrary)?.let { NetflixCalloutChip(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.cardGap)) {
                NetflixHomeAction(Icons.Default.PlayArrow, stringResource(R.string.hero_play), onPlay,
                    primary = true, iconOnly = true,
                    modifier = Modifier.testTag("netflix_card_play")
                        .then(if (playFocusRequester != null) Modifier.focusRequester(playFocusRequester) else Modifier)
                        .onFocusChanged { if (it.isFocused) onActionFocused(0) })
                NetflixHomeAction(if (inLibrary) Icons.Default.Check else Icons.Default.Add,
                    stringResource(if (inLibrary) R.string.hero_remove_from_library else R.string.hero_add_to_library),
                    onLibrary, iconOnly = true, enabled = !libraryPending,
                    modifier = Modifier.testTag("netflix_card_library")
                        .onFocusChanged { if (it.isFocused) onActionFocused(1) })
                NetflixHomeAction(Icons.Default.Info, stringResource(R.string.netflix_more_info), onMoreInfo,
                    iconOnly = true, modifier = Modifier.testTag("netflix_card_more_info")
                        .onFocusChanged { if (it.isFocused) onActionFocused(2) })
            }
            NetflixPreviewMetadata(item.heroPreview, showImdbRatings = showImdbRatings)
            val genres = remember(item.heroPreview.genres, context) {
                item.heroPreview.genres.list.take(2).joinToString(" • ") { localizedGenreLabel(context, it) }
            }
            if (genres.isNotBlank()) Text(genres, style = MaterialTheme.typography.labelSmall,
                color = tokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun NetflixPreviewMetadata(preview: HeroPreview, showImdbRatings: Boolean) {
    val tokens = NetflixThemeTokens
    FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.metadataGap),
        verticalArrangement = Arrangement.spacedBy(tokens.cardGap)) {
        if (showImdbRatings) preview.imdbText?.takeIf(String::isNotBlank)?.let {
            Text("IMDb $it", style = MaterialTheme.typography.labelMedium, color = tokens.textPrimary, maxLines = 1)
        }
        preview.yearText?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary, maxLines = 1)
        }
        preview.ageRatingText?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary, maxLines = 1,
                modifier = Modifier.border(BorderStroke(tokens.focusedBorderWidth, tokens.textMuted))
                    .padding(horizontal = tokens.cardGap))
        }
        val duration = if (preview.isSeries && preview.seasonCount != null) {
            pluralStringResource(R.plurals.netflix_seasons, preview.seasonCount, preview.seasonCount)
        } else preview.runtimeText
        duration?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetflixHomeAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    iconOnly: Boolean = false,
    enabled: Boolean = true,
    onDownToRows: (() -> Unit)? = null
) {
    val tokens = NetflixThemeTokens
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.height(tokens.buttonHeight)
            .then(if (iconOnly) Modifier.width(tokens.buttonHeight) else Modifier)
            .then(if (onDownToRows != null) Modifier.onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                    onDownToRows(); true
                } else false
            } else Modifier),
        shape = ButtonDefaults.shape(tokens.buttonShape),
        colors = ButtonDefaults.colors(
            containerColor = if (primary) tokens.focus else tokens.surfaceMuted,
            contentColor = if (primary) tokens.focusContent else tokens.textPrimary,
            focusedContainerColor = tokens.focus,
            focusedContentColor = tokens.focusContent
        ),
        scale = ButtonDefaults.scale(focusedScale = tokens.focusScale),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = if (iconOnly) tokens.cardGap else tokens.previewPadding,
            vertical = tokens.cardGap)
    ) {
        Icon(icon, contentDescription = if (iconOnly) label else null,
            modifier = Modifier.size(tokens.navigationIconSize))
        if (!iconOnly) Text(label, style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = tokens.cardGap))
    }
}
