@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.mynetflix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.Icon
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.UserProfile
import com.nuvio.tv.ui.components.ProfileAvatarCircle
import com.nuvio.tv.ui.theme.netflixEpisodeLabel
import com.nuvio.tv.ui.util.contentTextDirection
import com.nuvio.tv.ui.theme.NetflixThemeTokens
import com.nuvio.tv.ui.theme.withNuvioDescenderRoom

@Composable
fun MyNetflixHubScreen(
    onOpenDetail: (itemId: String, itemType: String, addonBaseUrl: String?) -> Unit,
    onResume: (com.nuvio.tv.domain.model.WatchProgress) -> Unit,
    onOpenFullLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MyNetflixHubViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    MyNetflixHubContent(state, profile, onOpenDetail, onResume, onOpenFullLibrary, onOpenSearch, onOpenSettings)
}

/** Stateless so the TV instrumentation can render the real hub from deterministic fixtures. */
@Composable
internal fun MyNetflixHubContent(
    state: MyNetflixHubState,
    profile: UserProfile?,
    onOpenDetail: (String, String, String?) -> Unit,
    onResume: (com.nuvio.tv.domain.model.WatchProgress) -> Unit,
    onOpenFullLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val tokens = NetflixThemeTokens
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(tokens.background).testTag("my_netflix_hub"),
        contentPadding = PaddingValues(bottom = tokens.safeVerticalMargin * 2),
        verticalArrangement = Arrangement.spacedBy(NetflixThemeTokens.Hub.sectionGap),
    ) {
        item(key = "header") {
            // The header art is the profile's own most recent title, never stock imagery.
            val headerArt = state.rows.firstOrNull()?.cards?.firstOrNull { it.imageUrl != null }?.imageUrl
            MyNetflixHeader(profile, state.libraryCount, headerArt, onOpenFullLibrary, onOpenSettings)
        }
        if (state.isEmpty) {
            item(key = "empty") { MyNetflixEmpty(onOpenSearch) }
        }
        state.rows.forEach { row ->
            item(key = row.section.name) {
                MyNetflixSectionRow(row, onOpenDetail, onResume)
            }
        }
    }
}

@Composable
private fun MyNetflixHeader(
    profile: UserProfile?,
    libraryCount: Int,
    headerArt: String?,
    onOpenFullLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val tokens = NetflixThemeTokens
    val hub = NetflixThemeTokens.Hub
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(Modifier.fillMaxWidth().height(hub.headerArtHeight)) {
        if (headerArt != null) {
            val context = LocalContext.current
            val density = LocalDensity.current
            val config = LocalConfiguration.current
            val model = remember(headerArt, config.screenWidthDp) {
                // Bounded decode: header width at the canvas density, never the source resolution.
                ImageRequest.Builder(context).data(headerArt)
                    .size(with(density) { config.screenWidthDp.dp.roundToPx() }, with(density) { hub.headerArtHeight.roundToPx() })
                    .build()
            }
            AsyncImage(model, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = hub.headerArtAlpha })
            Box(Modifier.fillMaxSize().background(tokens.heroSideGradient(rtl)))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, tokens.background))))
        Row(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .padding(start = tokens.safeMargin, end = tokens.safeMargin, bottom = tokens.metadataGap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(hub.headerGap),
        ) {
            if (profile != null) {
                ProfileAvatarCircle(name = profile.name, colorHex = profile.avatarColorHex, size = hub.headerAvatarSize,
                    avatarImageUrl = profile.avatarUrl, imageCrossfade = false, avatarShape = RoundedCornerShape(tokens.profileRadius))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.metadataGap / 2)) {
                Text(
                    text = stringResource(R.string.netflix_nav_my_netflix),
                    style = TextStyle(fontFamily = tokens.fontFamily, fontSize = hub.titleSize, fontWeight = FontWeight.Bold).withNuvioDescenderRoom(),
                    color = tokens.textPrimary, maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.metadataGap)) {
                    if (profile != null) {
                        Text(
                            text = profile.name,
                            style = TextStyle(fontFamily = tokens.fontFamily, fontSize = hub.subtitleSize,
                                textDirection = profile.name.contentTextDirection()).withNuvioDescenderRoom(),
                            color = tokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // Full Library belongs with the profile identity as a quiet link, not an admin button.
                    MyNetflixAction(
                        label = if (libraryCount > 0) stringResource(R.string.netflix_hub_full_library_count, libraryCount)
                            else stringResource(R.string.netflix_hub_full_library),
                        onClick = onOpenFullLibrary, tag = "my_netflix_full_library",
                    )
                }
            }
            MyNetflixIconAction(Icons.Default.Settings, stringResource(R.string.nav_settings), onOpenSettings, tag = "my_netflix_settings")
        }
    }
}

@Composable
private fun MyNetflixIconAction(icon: ImageVector, label: String, onClick: () -> Unit, tag: String) {
    val tokens = NetflixThemeTokens
    androidx.tv.material3.IconButton(
        onClick = onClick,
        modifier = Modifier.size(NetflixThemeTokens.Hub.actionHeight + tokens.metadataGap).testTag(tag),
        colors = androidx.tv.material3.IconButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = tokens.textPrimary.copy(alpha = NetflixThemeTokens.TopNav.settingsAlpha),
            focusedContainerColor = tokens.focus.copy(alpha = NetflixThemeTokens.TopNav.focusFillAlpha),
            focusedContentColor = tokens.textPrimary,
        ),
        scale = androidx.tv.material3.IconButtonDefaults.scale(focusedScale = 1f),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(NetflixThemeTokens.TopNav.iconSize))
    }
}

@Composable
private fun MyNetflixAction(label: String, onClick: () -> Unit, tag: String) {
    val tokens = NetflixThemeTokens
    Button(
        onClick = onClick,
        modifier = Modifier.height(NetflixThemeTokens.Hub.actionHeight).testTag(tag),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(NetflixThemeTokens.TopNav.itemRadius)),
        colors = ButtonDefaults.colors(
            containerColor = tokens.focus.copy(alpha = .10f), contentColor = tokens.textPrimary,
            focusedContainerColor = tokens.focus, focusedContentColor = tokens.focusContent,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        contentPadding = PaddingValues(horizontal = tokens.previewPadding),
    ) {
        Text(label, style = TextStyle(fontFamily = tokens.fontFamily, fontSize = NetflixThemeTokens.Hub.actionTextSize, fontWeight = FontWeight.Medium).withNuvioDescenderRoom(), maxLines = 1)
    }
}

@Composable
private fun MyNetflixSectionRow(
    row: MyNetflixRow,
    onOpenDetail: (String, String, String?) -> Unit,
    onResume: (com.nuvio.tv.domain.model.WatchProgress) -> Unit,
) {
    val tokens = NetflixThemeTokens
    val title = when (row.section) {
        MyNetflixSection.CONTINUE_WATCHING -> stringResource(R.string.netflix_hub_continue_watching)
        MyNetflixSection.MY_LIST -> stringResource(R.string.netflix_my_list)
        MyNetflixSection.RECENTLY_WATCHED -> stringResource(R.string.netflix_hub_recently_watched)
    }
    Column(verticalArrangement = Arrangement.spacedBy(tokens.rowTitleGap)) {
        Text(
            text = title,
            style = TextStyle(fontFamily = tokens.fontFamily, fontSize = NetflixThemeTokens.Hub.rowTitleSize, fontWeight = FontWeight.Bold).withNuvioDescenderRoom(),
            color = tokens.textPrimary,
            modifier = Modifier.padding(horizontal = tokens.safeMargin),
        )
        LazyRow(
            modifier = Modifier.testTag("my_netflix_row_${row.section.name.lowercase()}"),
            contentPadding = PaddingValues(horizontal = tokens.safeMargin, vertical = tokens.focusEdgeReserve),
            horizontalArrangement = Arrangement.spacedBy(tokens.cardGap),
        ) {
            items(row.cards, key = { it.key }) { card ->
                MyNetflixCardView(card) {
                    val resume = card.resume
                    if (resume != null) onResume(resume) else onOpenDetail(card.contentId, card.contentType, card.addonBaseUrl)
                }
            }
        }
    }
}

@Composable
private fun MyNetflixCardView(card: MyNetflixCard, onClick: () -> Unit) {
    val tokens = NetflixThemeTokens
    val width = tokens.landscapeCardWidth
    val height = width / tokens.landscapeAspectRatio
    val context = LocalContext.current
    val density = LocalDensity.current
    val model = remember(card.imageUrl, width) {
        card.imageUrl?.let { url ->
            ImageRequest.Builder(context).data(url)
                .size(with(density) { width.roundToPx() }, with(density) { height.roundToPx() })
                .build()
        }
    }
    Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(tokens.metadataGap / 2)) {
        Card(
            onClick = onClick,
            modifier = Modifier.width(width).height(height).testTag("my_netflix_card_${card.key}"),
            shape = CardDefaults.shape(tokens.cardShape),
            colors = CardDefaults.colors(containerColor = tokens.surfaceRaised, focusedContainerColor = tokens.surfaceRaised),
            border = CardDefaults.border(focusedBorder = Border(
                androidx.compose.foundation.BorderStroke(tokens.focusedBorderWidth, tokens.focus), shape = tokens.cardShape)),
            scale = CardDefaults.scale(focusedScale = tokens.focusScale),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (model != null) {
                    AsyncImage(model, contentDescription = card.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    // Missing artwork: the title on a dark cinematic surface, never a bright placeholder.
                    Text(
                        card.title,
                        style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.metadata, fontWeight = FontWeight.Medium,
                            textDirection = card.title.contentTextDirection()).withNuvioDescenderRoom(),
                        color = tokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.Center).padding(tokens.previewPadding),
                    )
                }
                val progress = card.progress
                if (progress != null) {
                    Box(Modifier.fillMaxWidth().height(tokens.progressHeight * 4).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, tokens.background.copy(alpha = .6f)))))
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(tokens.progressHeight).background(Color.White.copy(alpha = .30f)))
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.coerceIn(.02f, 1f)).height(tokens.progressHeight)
                        .background(tokens.progress).testTag("my_netflix_progress"))
                }
            }
        }
        val season = card.season
        val episode = card.episode
        val caption = if (season != null && episode != null) netflixEpisodeLabel(season, episode, card.episodeTitle) else card.title
        Text(
            caption,
            style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.metadata, textDirection = caption.contentTextDirection()).withNuvioDescenderRoom(),
            color = tokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MyNetflixEmpty(onOpenSearch: () -> Unit) {
    com.nuvio.tv.ui.components.NetflixStatePanel(
        title = stringResource(R.string.netflix_hub_empty_title),
        body = androidx.compose.ui.text.AnnotatedString(stringResource(R.string.netflix_hub_empty_body)),
        modifier = Modifier.fillMaxWidth().padding(vertical = NetflixThemeTokens.Hub.sectionGap),
        icon = Icons.Default.VideoLibrary,
        actionLabel = stringResource(R.string.nav_search),
        onAction = onOpenSearch,
        actionModifier = Modifier.testTag("my_netflix_empty_search"),
    )
}
