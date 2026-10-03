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
        contentPadding = PaddingValues(top = tokens.safeVerticalMargin, bottom = tokens.safeVerticalMargin * 2),
        verticalArrangement = Arrangement.spacedBy(tokens.Hub.sectionGap),
    ) {
        item(key = "header") {
            MyNetflixHeader(profile, state.libraryCount, onOpenFullLibrary, onOpenSettings)
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
    onOpenFullLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val tokens = NetflixThemeTokens
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.safeMargin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.Hub.headerGap),
    ) {
        if (profile != null) {
            ProfileAvatarCircle(name = profile.name, colorHex = profile.avatarColorHex, size = tokens.Hub.headerAvatarSize,
                avatarImageUrl = profile.avatarUrl, imageCrossfade = false, avatarShape = RoundedCornerShape(tokens.profileRadius))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.netflix_nav_my_netflix),
                style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.Hub.titleSize, fontWeight = FontWeight.Bold),
                color = tokens.textPrimary, maxLines = 1,
            )
            if (profile != null) {
                Text(
                    text = profile.name,
                    style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.Hub.subtitleSize,
                        textDirection = profile.name.contentTextDirection()),
                    color = tokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Real destinations only: the full Library (lists, providers, cloud) and Settings.
        MyNetflixAction(
            label = if (libraryCount > 0) stringResource(R.string.netflix_hub_full_library_count, libraryCount)
                else stringResource(R.string.netflix_hub_full_library),
            onClick = onOpenFullLibrary, tag = "my_netflix_full_library",
        )
        MyNetflixAction(stringResource(R.string.nav_settings), onOpenSettings, tag = "my_netflix_settings")
    }
}

@Composable
private fun MyNetflixAction(label: String, onClick: () -> Unit, tag: String) {
    val tokens = NetflixThemeTokens
    Button(
        onClick = onClick,
        modifier = Modifier.height(tokens.Hub.actionHeight).testTag(tag),
        shape = ButtonDefaults.shape(shape = tokens.buttonShape),
        colors = ButtonDefaults.colors(
            containerColor = tokens.surfaceMuted.copy(alpha = .72f), contentColor = tokens.textPrimary,
            focusedContainerColor = tokens.focus, focusedContentColor = tokens.focusContent,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        contentPadding = PaddingValues(horizontal = tokens.previewPadding),
    ) {
        Text(label, style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.metadata, fontWeight = FontWeight.SemiBold), maxLines = 1)
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
            style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.Hub.rowTitleSize, fontWeight = FontWeight.Bold),
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
                        style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.metadata, fontWeight = FontWeight.SemiBold,
                            textDirection = card.title.contentTextDirection()),
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
            style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.metadata, textDirection = caption.contentTextDirection()),
            color = tokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MyNetflixEmpty(onOpenSearch: () -> Unit) {
    val tokens = NetflixThemeTokens
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.safeMargin, vertical = tokens.Hub.sectionGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.Hub.headerGap),
    ) {
        Text(
            stringResource(R.string.netflix_hub_empty_title),
            style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.rowHeader, fontWeight = FontWeight.Bold),
            color = tokens.textPrimary, textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.netflix_hub_empty_body),
            style = TextStyle(fontFamily = tokens.fontFamily, fontSize = tokens.description),
            color = tokens.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = tokens.State.maxTextWidth),
        )
        MyNetflixAction(stringResource(R.string.nav_search), onOpenSearch, tag = "my_netflix_empty_search")
    }
}
