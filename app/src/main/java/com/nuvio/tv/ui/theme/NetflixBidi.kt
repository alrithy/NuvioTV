package com.nuvio.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import com.nuvio.tv.R

/*
 * NETFLIX_THEME bidi-safe metadata (parity audit §13.6, §19). Mixed tokens such as "S1 E2", "4K",
 * "IMDb 8.1", "1h 52m" and Latin titles inside Arabic text are isolated with Unicode FSI/PDI so the
 * paragraph's direction cannot reorder their digits and letters. Strings are never reversed by hand.
 */

private const val FSI = '⁨'
private const val PDI = '⁩'

/** First-strong isolate: the token keeps its own direction inside either paragraph direction. */
fun netflixIsolate(text: String): String = if (text.isEmpty()) text else "$FSI$text$PDI"

/** Joins non-blank metadata facts with a separator, each isolated; missing facts simply vanish. */
fun netflixMetadataLine(parts: List<String?>, separator: String = " · "): String =
    parts.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.joinToString(separator) { netflixIsolate(it) }

/** Locale-aware short episode token ("S1 E2" / "م1 ح2"), isolated so it never flips in RTL. */
@Composable
fun netflixEpisodeToken(season: Int, episode: Int): String =
    netflixIsolate(stringResource(R.string.netflix_episode_token, season, episode))

/** Episode token plus its real title when known. */
@Composable
fun netflixEpisodeLabel(season: Int, episode: Int, title: String?): String =
    netflixMetadataLine(listOf(stringResource(R.string.netflix_episode_token, season, episode), title), separator = " · ")

@Composable
fun isNetflixRtl(): Boolean = LocalLayoutDirection.current == LayoutDirection.Rtl
