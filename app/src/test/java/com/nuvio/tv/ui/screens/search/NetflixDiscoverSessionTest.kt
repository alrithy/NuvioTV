package com.nuvio.tv.ui.screens.search

import android.content.Context
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.data.local.DiscoverSelectionDataStore
import com.nuvio.tv.data.local.LayoutPreferenceDataStore
import com.nuvio.tv.data.local.SearchHistoryDataStore
import com.nuvio.tv.data.local.WatchedSeriesStateHolder
import com.nuvio.tv.domain.model.Addon
import com.nuvio.tv.domain.model.CatalogDescriptor
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.DiscoverLocation
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.domain.repository.AddonRepository
import com.nuvio.tv.domain.repository.CatalogRepository
import com.nuvio.tv.domain.repository.WatchProgressRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetflixDiscoverSessionTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `Netflix categories work with Discover off and preserve the saved catalog`() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.viewModel()
        runCurrent()
        viewModel.setDiscoverSessionEnabled(true, "series")
        viewModel.ensureDiscoverLoaded()
        advanceUntilIdle()

        assertEquals(DiscoverLocation.OFF, viewModel.uiState.value.discoverLocation)
        assertEquals("series", viewModel.uiState.value.selectedDiscoverType)
        assertEquals(listOf("series-item"), viewModel.uiState.value.discoverResults.map { it.id })
        coVerify(exactly = 0) { fixture.selection.setSelectedCatalogKey(any()) }

        viewModel.setDiscoverSessionEnabled(false, keepLoadedResults = true)
        assertEquals(1, viewModel.uiState.value.discoverResults.size)
        viewModel.setDiscoverSessionEnabled(true, "series")
        viewModel.ensureDiscoverLoaded()
        advanceUntilIdle()
        assertEquals(listOf("series"), fixture.requestedTypes)

        // Changing away from Netflix restores the user's disabled Discover state.
        viewModel.setDiscoverSessionEnabled(false)
        assertTrue(viewModel.uiState.value.discoverResults.isEmpty())
        assertFalse(viewModel.uiState.value.discoverInitialized)
    }

    @Test
    fun `Discover network errors expose retry and successful retry clears the error`() = runTest {
        val fixture = Fixture().apply { failCatalog = true }
        val viewModel = fixture.viewModel()
        runCurrent()
        viewModel.setDiscoverSessionEnabled(true, "movie")
        viewModel.ensureDiscoverLoaded()
        advanceUntilIdle()
        assertEquals("catalog unavailable", viewModel.uiState.value.discoverError)
        assertFalse(viewModel.uiState.value.discoverLoading)

        fixture.failCatalog = false
        viewModel.onEvent(SearchEvent.RetryDiscover)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.discoverError)
        assertEquals(listOf("movie-item"), viewModel.uiState.value.discoverResults.map { it.id })
    }

    @Test
    fun `ending a forced session cancels pending catalog initialization`() = runTest {
        val lookupGate = CompletableDeferred<Unit>()
        val fixture = Fixture(lookupGate)
        val viewModel = fixture.viewModel()
        runCurrent()
        viewModel.setDiscoverSessionEnabled(true, "series")
        viewModel.ensureDiscoverLoaded()
        runCurrent()
        assertTrue(viewModel.uiState.value.discoverLoading)

        viewModel.setDiscoverSessionEnabled(false)
        lookupGate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.discoverLoading)
        assertFalse(viewModel.uiState.value.discoverInitialized)
        assertTrue(viewModel.uiState.value.discoverCatalogs.isEmpty())
        assertTrue(fixture.requestedTypes.isEmpty())
    }

    private class Fixture(private val lookupGate: CompletableDeferred<Unit>? = null) {
        val selection = mockk<DiscoverSelectionDataStore>(relaxed = true)
        val requestedTypes = mutableListOf<String>()
        var failCatalog = false

        fun viewModel(): SearchViewModel {
            val addon = Addon(
                id = "addon", name = "Addon", version = "1", description = null, logo = null,
                baseUrl = "https://example.test", resources = emptyList(),
                types = listOf(ContentType.MOVIE, ContentType.SERIES),
                catalogs = listOf(
                    CatalogDescriptor(type = ContentType.MOVIE, id = "movies", name = "Movies"),
                    CatalogDescriptor(type = ContentType.SERIES, id = "series", name = "Series")
                )
            )
            val addons = mockk<AddonRepository>()
            every { addons.getInstalledAddons() } returns flow {
                lookupGate?.await()
                emit(listOf(addon))
            }
            coEvery { selection.getSelectedCatalogKey() } returns "addon_movie_movies"
            val catalogs = mockk<CatalogRepository>()
            every { catalogs.getCatalog(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } answers {
                val type = arg<String>(5)
                requestedTypes += type
                if (failCatalog) flowOf(NetworkResult.Error("catalog unavailable")) else {
                    val contentType = if (type == "series") ContentType.SERIES else ContentType.MOVIE
                    flowOf(NetworkResult.Success(CatalogRow(
                        addonId = addon.id, addonName = addon.name, addonBaseUrl = addon.baseUrl,
                        catalogId = arg(3), catalogName = arg(4), type = contentType,
                        items = listOf(MetaPreview(
                            id = "$type-item", type = contentType, name = "Title", poster = null,
                            posterShape = PosterShape.LANDSCAPE, background = null, logo = null,
                            description = null, releaseInfo = null, imdbRating = null, genres = emptyList()
                        )), hasMore = false
                    )))
                }
            }
            val layout = mockk<LayoutPreferenceDataStore>()
            every { layout.discoverLocation } returns flowOf(DiscoverLocation.OFF)
            every { layout.posterCardWidthDp } returns flowOf(126)
            every { layout.posterLabelsEnabled } returns flowOf(true)
            every { layout.catalogAddonNameEnabled } returns flowOf(true)
            every { layout.posterCardHeightDp } returns flowOf(189)
            every { layout.posterCardCornerRadiusDp } returns flowOf(12)
            every { layout.catalogTypeSuffixEnabled } returns flowOf(true)
            every { layout.hideUnreleasedContent } returns flowOf(false)
            val history = mockk<SearchHistoryDataStore>(relaxed = true)
            every { history.recentSearches } returns flowOf(emptyList())
            val progress = mockk<WatchProgressRepository>()
            every { progress.observeWatchedMovieIds() } returns flowOf(emptySet())
            val watched = mockk<WatchedSeriesStateHolder>()
            every { watched.fullyWatchedSeriesIds } returns MutableStateFlow(emptySet())
            return SearchViewModel(
                addonRepository = addons, catalogRepository = catalogs, metaRepository = mockk(relaxed = true),
                discoverSelectionDataStore = selection, layoutPreferenceDataStore = layout,
                searchHistoryDataStore = history, watchProgressRepository = progress,
                watchedSeriesStateHolder = watched, posterOptions = mockk(relaxed = true),
                context = mockk<Context>(relaxed = true)
            )
        }
    }
}
