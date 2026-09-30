package com.nuvio.tv.data.repository

import com.nuvio.tv.fork.skip.ForkSkipProvider
import com.nuvio.tv.fork.skip.ForkSkipProviders
import com.nuvio.tv.fork.skip.SkipProviderConfig
import com.nuvio.tv.fork.skip.SkipReport
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Superfork G9 closeout: a slow Simkl lookup (AniSkip / Anime-Skip ids) stays inside the 6 s deadline. */
class SkipIntroSimklDeadlineTest {

    private val fork = mockk<ForkSkipProviders> {
        coEvery { activeConfig() } returns SkipProviderConfig(enabled = setOf(ForkSkipProvider.SKIP_ME))
        coEvery { fetch(any(), any(), any(), any(), any(), any(), any(), any()) } returns listOf(
            SkipReport(60.0, 90.0, "opening", "intro", "skipme", 0.8),
        )
        every { combine(any(), any(), any()) } answers {
            secondArg<List<SkipReport>>().map { SkipInterval(it.startTime, it.endTime, it.type, it.provider) }
        }
    }

    private fun repository(simkl: SimklIdResolver) = SkipIntroRepository(
        mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), simkl,
        mockk(relaxed = true), mockk(relaxed = true), fork,
    )

    @Test
    fun `a stalled Simkl id lookup never holds back the other providers`() = runTest {
        val simkl = mockk<SimklIdResolver> {
            coEvery { resolveIdsForImdbEpisode(any(), any(), any()) } coAnswers { delay(60_000); null }
        }
        val result = repository(simkl).getSkipIntervals("tt0944947", 1, 2)
        assertEquals(listOf("skipme"), result.map { it.provider })
        assertTrue("finished at ${currentTime} ms", currentTime <= 6_000L)
    }

    @Test
    fun `a stalled episode mapping is bounded the same way`() = runTest {
        val simkl = mockk<SimklIdResolver> {
            coEvery { resolveIdsForImdbEpisode(any(), any(), any()) } returns
                SimklIdResolver.ResolvedIds(simklId = 1L, type = "anime", mal = "5114")
            coEvery { getEpisodeMapping(any(), any()) } coAnswers { delay(60_000); emptyList() }
        }
        repository(simkl).getSkipIntervals("tt0944947", 1, 2)
        assertTrue("finished at ${currentTime} ms", currentTime <= 6_000L)
    }
}
