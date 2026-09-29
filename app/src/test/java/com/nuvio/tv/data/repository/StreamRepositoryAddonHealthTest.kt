package com.nuvio.tv.data.repository

import android.content.Context
import com.nuvio.tv.core.debrid.DebridStreamPresentation
import com.nuvio.tv.core.debrid.LocalDebridAvailabilityService
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.core.plugin.PluginManager
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.tmdb.TmdbService
import com.nuvio.tv.data.local.DebridSettingsDataStore
import com.nuvio.tv.data.remote.api.AddonApi
import com.nuvio.tv.data.remote.dto.StreamDto
import com.nuvio.tv.data.remote.dto.StreamResponseDto
import com.nuvio.tv.domain.model.Addon
import com.nuvio.tv.domain.model.AddonResource
import com.nuvio.tv.domain.model.AddonStreams
import com.nuvio.tv.domain.model.DebridSettings
import com.nuvio.tv.domain.repository.AddonRepository
import com.nuvio.tv.fork.diagnostics.AddonHealthState
import com.nuvio.tv.fork.diagnostics.AddonHealthTracker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.net.SocketTimeoutException

class StreamRepositoryAddonHealthTest {

    @Test
    fun `each addon outcome is recorded against its own addon and failures stay isolated`() = runTest {
        val tracker = AddonHealthTracker(enabled = true)
        val api = mockk<AddonApi>()
        coEvery { api.getStreams("$OK_URL/stream/movie/tt1.json") } returns Response.success(
            StreamResponseDto(streams = listOf(StreamDto(name = "Stream", url = "https://cdn.example/v.m3u8")))
        )
        coEvery { api.getStreams("$TIMEOUT_URL/stream/movie/tt1.json") } throws SocketTimeoutException("timeout")
        coEvery { api.getStreams("$EMPTY_URL/stream/movie/tt1.json") } returns Response.success(StreamResponseDto(streams = emptyList()))
        coEvery { api.getMeta(any()) } returns Response.error(404, "".toResponseBody(null))

        val repository = repository(api, tracker, listOf(addon("ok", OK_URL), addon("slow", TIMEOUT_URL), addon("empty", EMPTY_URL)))

        val last = repository.getStreamsFromAllAddons(type = "movie", videoId = "tt1", season = null, episode = null)
            .toList().last()

        assertEquals(listOf("ok"), (last as NetworkResult.Success).data.map { it.addonName })
        val health = tracker.health.value
        assertEquals(AddonHealthState.HEALTHY, health.getValue(OK_URL).state)
        assertEquals(AddonHealthState.TIMEOUT, health.getValue(TIMEOUT_URL).state)
        assertEquals(1, health.getValue(TIMEOUT_URL).consecutiveFailures)
        assertEquals(AddonHealthState.NO_STREAMS, health.getValue(EMPTY_URL).state)
    }

    @Test
    fun `http errors on stream requests are classified`() = runTest {
        val tracker = AddonHealthTracker(enabled = true)
        val api = mockk<AddonApi>()
        coEvery { api.getStreams("$OK_URL/stream/movie/tt1.json") } returns Response.error(401, "".toResponseBody(null))
        coEvery { api.getStreams("$EMPTY_URL/stream/movie/tt1.json") } returns Response.error(503, "".toResponseBody(null))

        repository(api, tracker, listOf(addon("auth", OK_URL), addon("down", EMPTY_URL)))
            .getStreamsFromAllAddons(type = "movie", videoId = "tt1", season = null, episode = null)
            .toList()

        assertEquals(AddonHealthState.AUTH_ERROR, tracker.health.value.getValue(OK_URL).state)
        assertEquals(AddonHealthState.REQUEST_ERROR, tracker.health.value.getValue(EMPTY_URL).state)
    }

    private fun repository(api: AddonApi, tracker: AddonHealthTracker, addons: List<Addon>): StreamRepositoryImpl {
        val addonRepository = mockk<AddonRepository>()
        every { addonRepository.getInstalledAddons() } returns flowOf(addons)

        val pluginManager = mockk<PluginManager>(relaxed = true)
        every { pluginManager.enabledScrapers } returns flowOf(emptyList())
        every { pluginManager.pluginsEnabled } returns flowOf(false)
        every { pluginManager.groupStreamsByRepository } returns flowOf(false)
        every { pluginManager.repositories } returns flowOf(emptyList())

        val profileManager = mockk<ProfileManager>(relaxed = true)
        every { profileManager.activeProfileId } returns MutableStateFlow(1)

        val debridSettingsDataStore = mockk<DebridSettingsDataStore>()
        every { debridSettingsDataStore.settings } returns flowOf(DebridSettings())
        val presentation = mockk<DebridStreamPresentation>()
        every { presentation.apply(any(), any<DebridSettings>(), any(), any()) } answers {
            firstArg<List<AddonStreams>>()
        }
        val availability = mockk<LocalDebridAvailabilityService>()
        coEvery { availability.markChecking(any()) } coAnswers { firstArg<List<AddonStreams>>() }
        coEvery { availability.annotateCachedAvailability(any()) } coAnswers { firstArg<List<AddonStreams>>() }

        return StreamRepositoryImpl(
            context = mockk<Context>(relaxed = true),
            api = api,
            addonRepository = addonRepository,
            pluginManager = pluginManager,
            profileManager = profileManager,
            debridSettingsDataStore = debridSettingsDataStore,
            tmdbService = mockk<TmdbService>(relaxed = true),
            debridStreamPresentation = presentation,
            localDebridAvailabilityService = availability,
            healthTracker = tracker
        )
    }

    private fun addon(name: String, baseUrl: String) = Addon(
        id = name,
        name = name,
        version = "1.0.0",
        description = null,
        logo = null,
        baseUrl = baseUrl,
        catalogs = emptyList(),
        types = emptyList(),
        resources = listOf(AddonResource(name = "stream", types = listOf("movie"), idPrefixes = listOf("tt")))
    )

    private companion object {
        const val OK_URL = "https://ok.example"
        const val TIMEOUT_URL = "https://timeout.example"
        const val EMPTY_URL = "https://empty.example"
    }
}
