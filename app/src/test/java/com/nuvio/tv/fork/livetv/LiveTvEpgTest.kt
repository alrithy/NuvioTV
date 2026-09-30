package com.nuvio.tv.fork.livetv

import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reshaped `LiveTvPlaylistParserTest` @ 0ccf049 (guide cases), plus timestamps, cache, window and the repository. */
class LiveTvEpgTest {
    private val hour = 60L * 60 * 1000

    @Test
    fun guideIsReadAgainWhenACutChannelRunsOut() {
        fun slots(count: Int, length: Long) = (0 until count).map {
            LiveTvProgramme(title = "p$it", startEpochMs = it * length, stopEpochMs = (it + 1) * length)
        }
        val schedule = mapOf(
            "short" to slots(4, hour / 2), // cut, runs out after 2 h
            "ending" to slots(1, hour / 4), // the guide itself ends: no reason to read sooner
        )
        assertEquals(2 * hour, nextScheduleReadAt(schedule, setOf("short"), 0L, hour, 10 * hour))
        assertEquals(hour, nextScheduleReadAt(mapOf("tiny" to slots(4, hour / 10)), setOf("tiny"), 0L, hour, 10 * hour))
        assertEquals(10 * hour, nextScheduleReadAt(emptyMap(), emptySet(), 0L, hour, 10 * hour))
    }

    @Test
    fun nameKeysIgnoreCountryTagsQualityAndPunctuation() {
        assertEquals("bbcone", liveTvNameKey("UK: BBC One HD"))
        assertEquals("bbcone", liveTvNameKey("BBC ONE"))
        assertEquals("bbcone", liveTvNameKey("|UK| BBC-One FHD"))
        assertEquals("bbcone", liveTvNameKey("[UK] BBC One"))
        assertEquals("channel4+1", liveTvNameKey("Channel 4 +1"))
        assertEquals("abcnews", liveTvNameKey("ABC News"))
    }

    @Test
    fun guideMatchesByIdThenByNameAndKeepsAWindow() {
        val now = 10 * hour
        val channels = listOf(
            LiveTvChannel(id = "1", name = "BBC One HD", streamUrl = "a", tvgId = "bbc1.uk", guideKey = liveTvGuideKey("bbc1.uk", "BBC One HD")),
            LiveTvChannel(id = "2", name = "UK: ITV 1", streamUrl = "b", guideKey = liveTvGuideKey(null, "UK: ITV 1")),
            LiveTvChannel(id = "3", name = "Sky News", streamUrl = "c", tvgId = "wrong.id", guideKey = liveTvGuideKey("wrong.id", "Sky News")),
        )
        val builder = LiveTvScheduleBuilder(
            LiveTvGuideRequest.from(channels),
            now,
            LiveTvGuideWindow(pastMs = 2 * hour, maxPast = 1, aheadMs = 3 * hour, maxAhead = 2),
        )
        builder.channel("bbc1.uk", listOf("BBC One"), "https://logo/bbc.png")
        builder.channel("itv1.uk", listOf("ITV 1"), "https://logo/itv.png")
        builder.channel("skynews.uk", listOf("Sky News"), null)
        builder.channel("other.uk", listOf("Other"), null)
        assertEquals(null, builder.keysFor("other.uk"))
        listOf("bbc1.uk", "itv1.uk", "skynews.uk").forEach { id ->
            val keys = builder.keysFor(id)!!
            (7L..14L).forEach { h -> builder.add(keys, "$id $h", h * hour, (h + 1) * hour) }
        }
        val guide = builder.build()
        val bbc = guide.schedule.getValue(channels[0].guideKey)
        // One ended programme (the latest), then the one on now and the next: the cap is 2 ahead.
        assertEquals(listOf(9L, 10L, 11L).map { it * hour }, bbc.map { it.startEpochMs })
        assertTrue(channels[0].guideKey in guide.truncated)
        assertEquals(3, guide.schedule.getValue(channels[1].guideKey).size)
        assertEquals(3, guide.schedule.getValue(channels[2].guideKey).size)
        // None of these has a logo in the playlist, so the guide's is used.
        assertEquals("https://logo/itv.png", guide.logos[channels[1].guideKey])
    }

    @Test
    fun timestampsWithAndWithoutOffsetsAndTheProgrammeOnNow() {
        assertEquals(1_790_537_400_000L, LiveTvClock.parseXmlTvTimestamp("20260927213000 +0200"))
        assertEquals(1_790_537_400_000L, LiveTvClock.parseXmlTvTimestamp("202609271930 +0000"))
        assertNotNull(LiveTvClock.parseXmlTvTimestamp("20260927213000"))
        assertNull(LiveTvClock.parseXmlTvTimestamp("2026-09-27"))
        val on = LiveTvProgramme("On", 0, hour)
        val schedule = mapOf("a" to listOf(on, LiveTvProgramme("Next", hour, 2 * hour)))
        assertEquals(mapOf("a" to on), currentProgrammes(schedule, listOf("a", "b"), hour / 2))
        assertEquals(emptyMap<String, LiveTvProgramme>(), currentProgrammes(emptyMap(), listOf("a"), 0))
    }

    @Test
    fun theWindowFollowsTheConstrainedTier() {
        assertSame(LiveTvGuideWindow.LowMemory, LiveTvGuideWindow.forDevice(constrained = true))
        assertSame(LiveTvGuideWindow.Regular, LiveTvGuideWindow.forDevice(constrained = false))
    }

    @Test
    fun keptProgrammesServeOnlyWhileTheirGuidesAreUnchanged() {
        val dir = Files.createTempDirectory("guide").toFile()
        try {
            val guide = File(dir, "g.xml.gz").apply { writeText("x") }
            val cache = File(dir, LiveTvGuideCache.FILE_NAME)
            val now = guide.lastModified() + 1_000
            val schedule = mapOf("k" to listOf(LiveTvProgramme("T", now - hour, now + hour)))
            val key = LiveTvGuideCache.key(listOf("http://g"), setOf("k"), LiveTvGuideWindow.Regular)
            LiveTvGuideCache.write(cache, key, listOf(guide), LiveTvGuideCache.Entry(schedule, mapOf("k" to "http://l"), now + hour))
            val read = LiveTvGuideCache.read(cache, key, listOf(guide), now, 10 * hour)
            assertEquals(schedule, read?.schedule)
            assertEquals(mapOf("k" to "http://l"), read?.logos)
            assertNull(LiveTvGuideCache.read(cache, key + 1, listOf(guide), now, 10 * hour))
            assertNull(LiveTvGuideCache.read(cache, key, listOf(guide), now + 2 * hour, 10 * hour))
            guide.setLastModified(guide.lastModified() + 5_000)
            assertNull(LiveTvGuideCache.read(cache, key, listOf(guide), now, 10 * hour))
        } finally {
            dir.deleteRecursively()
        }
    }

    private class FakeGuideFiles(override val dir: File, private val programmes: (LiveTvGuideRequest) -> LiveTvGuide) : LiveTvGuideFiles {
        var downloads = 0
        var failDownloads = false

        override suspend fun download(url: String, headers: Map<String, String>, target: File) {
            downloads++
            if (failDownloads) throw IOException("HTTP 500 for $url")
            target.parentFile?.mkdirs()
            target.writeText("guide")
        }

        override suspend fun read(file: File, request: LiveTvGuideRequest, nowEpochMs: Long, window: LiveTvGuideWindow) = programmes(request)
    }

    @Test
    fun theRepositoryShowsWhatIsOnNowAndGuideLogos() = runBlocking {
        val dir = Files.createTempDirectory("guides").toFile()
        try {
            val now = LiveTvClock.nowEpochMs()
            val store = MemoryLiveTvStore(mapOf(1 to listOf(LiveTvSource("a", LiveTvSourceType.M3u, "http://list.example/a.m3u"))))
            val http = FakeLiveTvFetcher { url ->
                if ("list.example" !in url) throw IOException("unexpected")
                "#EXTM3U url-tvg=\"http://guide.example/g.xml\"\n#EXTINF:-1 tvg-id=\"one.uk\",One\nhttp://s/1\n#EXTINF:-1,Two\nhttp://s/2"
            }
            val oneKey = liveTvGuideKey("one.uk", "One")
            val files = FakeGuideFiles(dir) { request ->
                assertTrue(oneKey in request.keys)
                LiveTvGuide(
                    schedule = mapOf(oneKey to listOf(LiveTvProgramme("News", now - hour, now + hour), LiveTvProgramme("Film", now + hour, now + 3 * hour))),
                    logos = mapOf(oneKey to "http://logo/one.png"),
                    truncated = emptySet(),
                )
            }
            val repository = LiveTvRepository(store, store, http, MutableStateFlow(1), true, guideFiles = files, constrained = { false })
            repository.ensureLoaded()
            val state = withTimeout(5_000) { repository.state.first { it.currentProgrammes.isNotEmpty() && !it.isEpgLoading } }
            val one = state.channels.first { it.name == "One" }
            assertEquals("News", state.currentProgrammes[one.guideKey]?.title)
            assertEquals("http://logo/one.png", state.logoFor(one))
            assertEquals("Film", repository.nextProgramme(one.guideKey, now)?.title)
            assertEquals(1, files.downloads)
            assertTrue(File(dir, LiveTvGuideCache.FILE_NAME).isFile)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun aGuideThatCannotBeDownloadedLeavesTheListWithoutProgrammes() = runBlocking {
        val dir = Files.createTempDirectory("guides").toFile()
        try {
            val store = MemoryLiveTvStore(mapOf(1 to listOf(LiveTvSource("a", LiveTvSourceType.M3u, "http://list.example/a.m3u"))))
            val http = FakeLiveTvFetcher { "#EXTM3U url-tvg=\"http://guide.example/g.xml\"\n#EXTINF:-1,One\nhttp://s/1" }
            val files = FakeGuideFiles(dir) { error("not read") }.apply { failDownloads = true }
            val repository = LiveTvRepository(store, store, http, MutableStateFlow(1), true, guideFiles = files, constrained = { true })
            repository.ensureLoaded()
            val state = withTimeout(5_000) { repository.state.first { it.channels.isNotEmpty() && !it.isEpgLoading && files.downloads > 0 } }
            assertTrue(state.currentProgrammes.isEmpty())
            assertEquals(listOf("One"), state.channels.map { it.name })
        } finally {
            dir.deleteRecursively()
        }
    }
}
