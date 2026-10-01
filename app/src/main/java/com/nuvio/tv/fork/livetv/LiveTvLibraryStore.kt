package com.nuvio.tv.fork.livetv

import kotlinx.coroutines.flow.Flow

/** Where a profile keeps its Live TV choices and the menu switch (G10b); a fake replaces it in tests. */
interface LiveTvLibraryStore {
    suspend fun library(profileId: Int): LiveTvLibrary

    suspend fun saveLibrary(profileId: Int, library: LiveTvLibrary)

    /** Whether Live TV shows in the menu for [profileId]; off until the user turns it on (D055). */
    fun menuEnabled(profileId: Int): Flow<Boolean>

    suspend fun setMenuEnabled(profileId: Int, enabled: Boolean)

    /** The list's channel preview choices for [profileId] (G10f). */
    fun previewChoice(profileId: Int): Flow<LiveTvPreviewChoice>

    suspend fun setPreviewChoice(profileId: Int, choice: LiveTvPreviewChoice)
}
