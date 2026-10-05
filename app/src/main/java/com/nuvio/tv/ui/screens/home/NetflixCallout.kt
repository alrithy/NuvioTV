package com.nuvio.tv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.tv.material3.Icon
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.theme.NetflixThemeTokens
import com.nuvio.tv.ui.theme.netflixEpisodeToken
import com.nuvio.tv.ui.theme.netflixIsolate
import com.nuvio.tv.ui.theme.withNuvioDescenderRoom

/*
 * NETFLIX_THEME contextual callout slot (parity audit §5). It only ever states a fact a Nuvio owner
 * proves for this profile: real watch progress, a next-up episode, or My List membership. Rank, award,
 * "new season", "leaving soon" and match facts have no owner in Nuvio, so they are never produced.
 */

internal enum class NetflixCalloutKind { CONTINUE, NEXT_EPISODE, IN_MY_LIST }

@Immutable
internal data class NetflixCallout(val kind: NetflixCalloutKind, val season: Int? = null, val episode: Int? = null)

internal fun netflixCallout(payload: ModernPayload?, inLibrary: Boolean): NetflixCallout? {
    if (payload is ModernPayload.ContinueWatching) {
        return when (val item = payload.item) {
            is ContinueWatchingItem.InProgress -> NetflixCallout(NetflixCalloutKind.CONTINUE, item.progress.season, item.progress.episode)
            // A Mystery shuffle pick keeps its episode hidden (G9d).
            is ContinueWatchingItem.NextUp -> if (item.mystery) null
                else NetflixCallout(NetflixCalloutKind.NEXT_EPISODE, item.info.season, item.info.episode)
        }
    }
    return if (inLibrary) NetflixCallout(NetflixCalloutKind.IN_MY_LIST) else null
}

@Composable
internal fun NetflixCalloutChip(callout: NetflixCallout, modifier: Modifier = Modifier) {
    val tokens = NetflixThemeTokens.Callout
    val label = when (callout.kind) {
        NetflixCalloutKind.CONTINUE -> stringResource(R.string.netflix_callout_continue)
        NetflixCalloutKind.NEXT_EPISODE -> stringResource(R.string.netflix_callout_next_episode)
        NetflixCalloutKind.IN_MY_LIST -> stringResource(R.string.netflix_callout_in_my_list)
    }
    val season = callout.season
    val episode = callout.episode
    val text = if (season != null && episode != null && season > 0) "${netflixIsolate(label)} \u00B7 ${netflixEpisodeToken(season, episode)}" else label
    // An inline label that belongs to the metadata, not a technical badge: a slim accent mark for
    // watch state, a check for My List.
    Row(
        modifier = modifier.testTag("netflix_callout"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.gap),
    ) {
        if (callout.kind == NetflixCalloutKind.IN_MY_LIST) {
            Icon(Icons.Default.Check, contentDescription = null, tint = NetflixThemeTokens.textPrimary,
                modifier = Modifier.size(tokens.iconSize))
        } else {
            Box(Modifier.width(tokens.accentWidth).height(tokens.accentHeight)
                .clip(RoundedCornerShape(tokens.accentWidth)).background(NetflixThemeTokens.progress))
        }
        Text(text, style = TextStyle(fontFamily = NetflixThemeTokens.fontFamily, fontSize = tokens.textSize,
            fontWeight = FontWeight.Medium).withNuvioDescenderRoom(), color = NetflixThemeTokens.textPrimary, maxLines = 1)
    }
}
