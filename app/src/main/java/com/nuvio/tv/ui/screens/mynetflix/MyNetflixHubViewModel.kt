package com.nuvio.tv.ui.screens.mynetflix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.domain.model.UserProfile
import com.nuvio.tv.domain.repository.LibraryRepository
import com.nuvio.tv.domain.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * Reads the active profile's existing library and progress owners; it adds no backend, cache or
 * second data model. A failing source degrades to an empty section instead of blanking the hub.
 */
private const val HUB_SETTLE_MS = 180L

@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class MyNetflixHubViewModel @Inject constructor(
    libraryRepository: LibraryRepository,
    watchProgressRepository: WatchProgressRepository,
    private val profileManager: ProfileManager,
    liveTvRepository: com.nuvio.tv.fork.livetv.LiveTvRepository,
) : ViewModel() {

    /** Netflix has no Calendar or Live TV tab, so the hub is where both open from when enabled. */
    val calendarEnabled: Boolean = com.nuvio.tv.fork.discovery.CalendarRules.enabled
    val liveTvEnabled: StateFlow<Boolean> = liveTvRepository.menuEnabled
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    internal val state: StateFlow<MyNetflixHubState> = combine(
        libraryRepository.libraryItems.onStart { emit(emptyList()) }.catch { emit(emptyList()) },
        watchProgressRepository.continueWatching.onStart { emit(emptyList()) }.catch { emit(emptyList()) },
        watchProgressRepository.watchedItems.onStart { emit(emptyList()) }.catch { emit(emptyList()) },
        watchProgressRepository.allProgress.onStart { emit(emptyList()) }.catch { emit(emptyList()) },
    ) { library, continueWatching, watched, progress ->
        buildMyNetflixHub(library, continueWatching, watched, progress)
    }
        // Sources start empty so one slow owner cannot hold the hub; a short settle stops that start
        // from flashing the empty state before local data arrives.
        .debounce(HUB_SETTLE_MS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyNetflixHubState())

    val profile: StateFlow<UserProfile?> = profileManager.activeProfileId
        .map { id -> profileManager.profiles.value.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, profileManager.activeProfile)
}
