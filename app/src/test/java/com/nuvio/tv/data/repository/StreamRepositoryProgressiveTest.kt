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
import com.nuvio.tv.fork.diagnostics.AddonHealthTracker
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.fork.streams.ProgressiveAioStreamsClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** Superfork G8a: progressive AIOStreams through the official add-on loop, and its fallback. */
class StreamRepositoryProgressiveTest {

    private fun stream(name: String) = Stream(
        name = name, title = null, description = null, url = "https://cdn.example/$name.mkv", ytId = null,
        infoHash = null, fileIdx = null, externalUrl = null, behaviorHints = null, addonName = "aio", addonLogo = null,
    )

    @Test
    fun `cumulative snapshots replace the add-on group and the completed list wins`() = runTest {
        val api = mockk<AddonApi>()
        val progressive = mockk<ProgressiveAioStreamsClient>()
        coEvery { progressive.fetch(any(), any(), any(), any(), any(), any()) } coAnswers {
            val onSnapshot = arg<suspend (List<Stream>) -> Unit>(5)
            onSnapshot(listOf(stream("a")))
            onSnapshot(listOf(stream("b"), stream("a")))
            listOf(stream("b"), stream("a"), stream("c"))
        }

        val emissions = repository(api, progressive, listOf(addon("aio", PROGRESSIVE_URL)))
            .getStreamsFromAllAddons(type = "movie", videoId = "tt1", season = null, episode = null)
            .toList()
            .filterIsInstance<NetworkResult.Success<List<AddonStreams>>>()

        val last = emissions.last().data.single()
        assertEquals(listOf("b", "a", "c"), last.streams.map { it.name })
        // The second snapshot replaced the first rather than being appended to it.
        assertTrue(emissions.any { success -> success.data.single().streams.map { it.name } == listOf("b", "a") })
        coVerify(exactly = 0) { api.getStreams(any()) }
    }

    @Test
    fun `an unavailable progressive endpoint falls back to the official request`() = runTest {
        val api = mockk<AddonApi>()
        coEvery { api.getStreams("$PROGRESSIVE_BASE/stream/movie/tt1.json?client=nuvio-progressive") } returns Response.success(
            StreamResponseDto(streams = listOf(StreamDto(name = "official", url = "https://cdn.example/o.m3u8")))
        )
        val progressive = mockk<ProgressiveAioStreamsClient>()
        coEvery { progressive.fetch(any(), any(), any(), any(), any(), any()) } returns null

        val last = repository(api, progressive, listOf(addon("aio", PROGRESSIVE_URL)))
            .getStreamsFromAllAddons(type = "movie", videoId = "tt1", season = null, episode = null)
            .toList().last()

        assertEquals(listOf("official"), (last as NetworkResult.Success).data.single().streams.map { it.name })
    }

    @Test
    fun `add-ons that do not opt in never use the progressive endpoint`() = runTest {
        val api = mockk<AddonApi>()
        coEvery { api.getStreams("$PLAIN_URL/stream/movie/tt1.json") } returns Response.success(
            StreamResponseDto(streams = listOf(StreamDto(name = "plain", url = "https://cdn.example/p.m3u8")))
        )
        val progressive = mockk<ProgressiveAioStreamsClient>()

        repository(api, progressive, listOf(addon("plain", PLAIN_URL)))
            .getStreamsFromAllAddons(type = "movie", videoId = "tt1", season = null, episode = null)
            .toList()

        coVerify(exactly = 0) { progressive.fetch(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { api.getStreams(any()) }
    }

    @Test
    fun `a server error is retried once and a client error never`() = runTest {
        val api = mockk<AddonApi>()
        coEvery { api.getStreams("$PLAIN_URL/stream/movie/tt1.json") } returnsMany listOf(
            Response.error(503, "".toResponseBody(null)),
            Response.success(StreamResponseDto(streams = listOf(StreamDto(name = "retried", url = "https://cdn.example/r.m3u8")))),
        )
        coEvery { api.getStreams("$AUTH_URL/stream/movie/tt1.json") } returns Response.error(401, "".toResponseBody(null))

        val last = repository(api, null, listOf(addon("plain", PLAIN_URL), addon("auth", AUTH_URL)))
            .getStreamsFromAllAddons(type = "movie", videoId = "tt1", season = null, episode = null)
            .toList().last()

        assertEquals(listOf("retried"), (last as NetworkResult.Success).data.flatMap { group -> group.streams.map { it.name } })
        coVerify(exactly = 2) { api.getStreams("$PLAIN_URL/stream/movie/tt1.json") }
        coVerify(exactly = 1) { api.getStreams("$AUTH_URL/stream/movie/tt1.json") }
    }

    private fun repository(api: AddonApi, progressive: ProgressiveAioStreamsClient?, addons: List<Addon>): StreamRepositoryImpl {
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
            healthTracker = AddonHealthTracker(enabled = true),
            progressiveAioStreams = progressive
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
        const val PROGRESSIVE_BASE = "https://aio.example/cfg"
        const val PROGRESSIVE_URL = "$PROGRESSIVE_BASE?client=nuvio-progressive"
        const val PLAIN_URL = "https://plain.example"
        const val AUTH_URL = "https://auth.example"
    }
}
