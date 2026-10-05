package com.nuvio.tv.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.R

/**
 * Add-ons name their catalogs in English ("Popular", "Top Rated", "Genres"). Well-known names are shown in
 * the app language; anything else (brand names such as Netflix or Cinemeta, custom titles) is left as is.
 */
@Composable
fun localizedCatalogName(name: String): String {
    val context = LocalContext.current
    return localizedCatalogName(context, name)
}

fun localizedCatalogName(context: Context, name: String): String {
    val resource = catalogNameResource(name) ?: return localizedGenreLabel(context, name)
    return context.getString(resource)
}

@StringRes
internal fun catalogNameResource(name: String): Int? =
    when (name.lowercase().trim().replace(Regex("\\s+"), " ")) {
        "popular" -> R.string.catalog_name_popular
        "trending" -> R.string.catalog_name_trending
        "trending today" -> R.string.catalog_name_trending_today
        "trending this week" -> R.string.catalog_name_trending_week
        "top rated" -> R.string.catalog_name_top_rated
        "most watched" -> R.string.catalog_name_most_watched
        "anticipated" -> R.string.catalog_name_anticipated
        "box office" -> R.string.catalog_name_box_office
        "top 10" -> R.string.catalog_name_top_10
        "new" -> R.string.catalog_name_new
        "new releases" -> R.string.catalog_name_new_releases
        "latest" -> R.string.catalog_name_latest
        "featured" -> R.string.catalog_name_featured
        "recommended" -> R.string.catalog_name_recommended
        "now playing" -> R.string.catalog_name_now_playing
        "upcoming" -> R.string.catalog_name_upcoming
        "airing today" -> R.string.catalog_name_airing_today
        "on the air" -> R.string.catalog_name_on_the_air
        "discover" -> R.string.catalog_name_discover
        "streaming" -> R.string.catalog_name_streaming
        "genres" -> R.string.catalog_name_genres
        "studios" -> R.string.catalog_name_studios
        "networks" -> R.string.catalog_name_networks
        "decades" -> R.string.catalog_name_decades
        "year" -> R.string.catalog_name_year
        "language" -> R.string.catalog_name_language
        "collections" -> R.string.catalog_name_collections
        "movies" -> R.string.catalog_name_movies
        "series", "shows", "tv shows" -> R.string.catalog_name_series
        else -> null
    }
