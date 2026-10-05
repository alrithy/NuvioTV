package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.domain.model.WatchProgress

/** A one-item projection published by the existing CW collector, never a second repository reader. */
internal data class HomeCompletedProgressSnapshot(
    val profileId: Int,
    val latestCompleted: WatchProgress?
)

internal fun homeCompletedProgressSnapshot(profileId: Int, progress: List<WatchProgress>): HomeCompletedProgressSnapshot =
    HomeCompletedProgressSnapshot(profileId, progress.asSequence()
        .filter { it.isCompleted() && it.contentId.isNotBlank() && it.name.isNotBlank() &&
            (it.contentType.equals("movie", true) || it.contentType.equals("series", true) || it.contentType.equals("tv", true)) }
        .maxByOrNull(WatchProgress::lastWatched))

/** A newly selected profile cannot use the previous profile's watch seed while its CW source warms. */
internal fun HomeCompletedProgressSnapshot?.progressForProfile(profileId: Int): List<WatchProgress> =
    this?.takeIf { it.profileId == profileId }?.latestCompleted?.let(::listOf).orEmpty()
