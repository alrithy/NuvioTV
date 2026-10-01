package com.nuvio.tv.fork.livetv

import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
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
    fun guideFilesAndCacheKeysDoNotShareJavaHashCollisions() {
        val a = "http://guide.example/Aa"
        val b = "http://guide.example/BB"
        assertEquals(a.hashCode(), b.hashCode())
        org.junit.Assert.assertNotEquals(liveTvGuideFileName(a), liveTvGuideFileName(b))
        org.junit.Assert.assertNotEquals(
            LiveTvGuideCache.key(listOf(a), setOf("k"), LiveTvGuideWindow.Regular),
            LiveTvGuideCache.key(listOf(b), setOf("k"), LiveTvGuideWindow.Regular),
        )
    }

    @Test
    fun variantsRetainEveryNameAndFallbackLogoWhileSharingAnId() {
        val key = liveTvGuideKey("wrong", "Alias A", "source")
        val channels = listOf(
            LiveTvChannel("a", "Alias A", "a", tvgId = "wrong", logoUrl = "http://logo/a", guideKey = key),
            LiveTvChannel("b", "Alias B", "b", tvgId = "wrong", guideKey = key),
        )
        val builder = LiveTvScheduleBuilder(LiveTvGuideRequest.from(channels), hour, LiveTvGuideWindow.Regular)
        builder.channel("actual", listOf("Alias B"), "http://logo/guide")
        assertEquals(listOf(key), builder.keysFor("actual"))
        builder.add(builder.keysFor("actual")!!, "Show", 0, 2 * hour)
        val guide = builder.build()
        assertEquals("Show", guide.schedule[key]?.single()?.title)
        assertEquals("http://logo/guide", guide.logos[key])
    }

    @Test
    fun guideIdMatchingFeedsSourceScopedKeys() {
        val a = LiveTvChannel("a", "One", "a", tvgId = "1", guideKey = liveTvGuideKey("1", "One", "source-a"))
        val b = LiveTvChannel("b", "Two", "b", tvgId = "1", guideKey = liveTvGuideKey("1", "Two", "source-b"))
        org.junit.Assert.assertNotEquals(a.guideKey, b.guideKey)
        for (channel in listOf(a, b)) {
            val builder = LiveTvScheduleBuilder(LiveTvGuideRequest.from(listOf(channel)), hour, LiveTvGuideWindow.Regular)
            assertEquals(listOf(channel.guideKey), builder.keysFor("1"))
        }
    }

    @Test
    fun exactAndNameMatchedVariantsBothReceiveTheSameGuideChannel() {
        val a = LiveTvChannel("a", "One HD", "a", tvgId = "1", guideKey = liveTvGuideKey("1", "One HD", "s"))
        val b = LiveTvChannel("b", "One SD", "b", guideKey = liveTvGuideKey(null, "One SD", "s"))
        val builder = LiveTvScheduleBuilder(LiveTvGuideRequest.from(listOf(a, b)), hour, LiveTvGuideWindow.Regular)
        builder.channel("1", listOf("One"), null)
        assertEquals(setOf(a.guideKey, b.guideKey), builder.keysFor("1")!!.toSet())
    }

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
            val oneKey = liveTvGuideKey("one.uk", "One", "a")
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
    @Test
    fun leavingTheScreenCancelsAnActiveGuideDownload() = runBlocking {
        val dir = Files.createTempDirectory("guide-leave").toFile()
        try {
            val store = MemoryLiveTvStore(mapOf(1 to listOf(LiveTvSource("a", LiveTvSourceType.M3u, "http://list/a"))))
            val http = FakeLiveTvFetcher { "#EXTM3U url-tvg=\"http://guide/g\"\n#EXTINF:-1,One\nhttp://s/1" }
            val entered = CompletableDeferred<Unit>()
            val cancelled = CompletableDeferred<Unit>()
            val files = object : LiveTvGuideFiles {
                override val dir = dir
                override suspend fun download(url: String, headers: Map<String, String>, target: File) {
                    entered.complete(Unit)
                    try { awaitCancellation() } finally { cancelled.complete(Unit) }
                }
                override suspend fun read(file: File, request: LiveTvGuideRequest, nowEpochMs: Long, window: LiveTvGuideWindow) = error("not read")
            }
            val repository = LiveTvRepository(store, store, http, MutableStateFlow(1), true, guideFiles = files)
            val collector = async { repository.state.collect { } }
            repository.ensureLoaded()
            withTimeout(5_000) { entered.await() }
            collector.cancelAndJoin()
            withTimeout(2_000) { cancelled.await() }
        } finally { dir.deleteRecursively() }
    }

    @Test
    fun separateSourceGuidesWithTheSameIdKeepDifferentProgrammes() = runBlocking {
        val dir = Files.createTempDirectory("guide-source").toFile()
        try {
            val store = MemoryLiveTvStore(mapOf(1 to listOf(
                LiveTvSource("a", LiveTvSourceType.M3u, "http://list/a"),
                LiveTvSource("b", LiveTvSourceType.M3u, "http://list/b"),
            )))
            val http = FakeLiveTvFetcher { url ->
                val source = url.substringAfterLast('/')
                "#EXTM3U url-tvg=\"http://guide/$source\"\n#EXTINF:-1 tvg-id=\"1\",Station $source\nhttp://s/$source"
            }
            val now = LiveTvClock.nowEpochMs()
            val files = FakeGuideFiles(dir) { request ->
                val key = request.keys.single()
                LiveTvGuide(mapOf(key to listOf(LiveTvProgramme(key.substringBefore('\u0000'), now - hour, now + hour))), emptyMap(), emptySet())
            }
            val repository = LiveTvRepository(store, store, http, MutableStateFlow(1), true, guideFiles = files)
            repository.ensureLoaded()
            val state = withTimeout(5_000) { repository.state.first { it.currentProgrammes.size == 2 && !it.isEpgLoading } }
            assertEquals(setOf("a", "b"), state.currentProgrammes.values.map { it.title }.toSet())
            assertEquals(2, files.downloads)
        } finally { dir.deleteRecursively() }
    }

    @Test
    fun partialGuidesAreDisplayedButNeverSavedAsComplete() = runBlocking {
        val dir = Files.createTempDirectory("guide-partial").toFile()
        try {
            val store = MemoryLiveTvStore(mapOf(1 to listOf(LiveTvSource("a", LiveTvSourceType.M3u, "http://list/a"))))
            val http = FakeLiveTvFetcher { "#EXTM3U url-tvg=\"http://guide/g\"\n#EXTINF:-1 tvg-id=\"1\",One\nhttp://s/1" }
            val now = LiveTvClock.nowEpochMs()
            val files = FakeGuideFiles(dir) { request ->
                LiveTvGuide(mapOf(request.keys.single() to listOf(LiveTvProgramme("Partial", now - hour, now + hour))), emptyMap(), emptySet(), complete = false)
            }
            val repository = LiveTvRepository(store, store, http, MutableStateFlow(1), true, guideFiles = files)
            repository.ensureLoaded()
            val state = withTimeout(5_000) { repository.state.first { it.currentProgrammes.isNotEmpty() && !it.isEpgLoading } }
            assertEquals("Partial", state.currentProgrammes.values.single().title)
            org.junit.Assert.assertFalse(File(dir, LiveTvGuideCache.FILE_NAME).exists())
        } finally { dir.deleteRecursively() }
    }

    @Test
    fun refreshDownloadsGuidesEvenWhenEverySourceReloadFails() = runBlocking {
        val dir = Files.createTempDirectory("guide-refresh").toFile()
        try {
            val store = MemoryLiveTvStore(mapOf(1 to listOf(LiveTvSource("a", LiveTvSourceType.M3u, "http://list/a"))))
            var failSource = false
            val http = FakeLiveTvFetcher {
                if (failSource) throw IOException("source unavailable")
                "#EXTM3U url-tvg=\"http://guide/g\"\n#EXTINF:-1,One\nhttp://s/1"
            }
            val files = FakeGuideFiles(dir) { LiveTvGuide(emptyMap(), emptyMap(), emptySet()) }
            val repository = LiveTvRepository(store, store, http, MutableStateFlow(1), true, guideFiles = files)
            val collector = async { repository.state.collect { } }
            try {
                repository.ensureLoaded()
                withTimeout(5_000) { repository.state.first { it.channels.isNotEmpty() && !it.isEpgLoading } }
                assertEquals(1, files.downloads)
                failSource = true
                repository.refresh()
                withTimeout(5_000) { repository.state.first { files.downloads >= 2 && !it.isEpgLoading && it.sourceErrors.isNotEmpty() } }
                assertEquals(1, repository.state.value.channels.size)
            } finally { collector.cancelAndJoin() }
        } finally { dir.deleteRecursively() }
    }

    @Test
    fun partialGuideIsDownloadedAgainAtFailureRetryInsteadOfTenHours() = runBlocking {
        val dir = Files.createTempDirectory("guide-retry").toFile()
        try {
            val store = MemoryLiveTvStore(mapOf(1 to listOf(LiveTvSource("a", LiveTvSourceType.M3u, "http://list/a"))))
            val http = FakeLiveTvFetcher { "#EXTM3U url-tvg=\"http://guide/g\"\n#EXTINF:-1,One\nhttp://s/1" }
            val clock = java.util.concurrent.atomic.AtomicLong(LiveTvClock.nowEpochMs())
            val now = clock.get()
            var reads = 0
            val files = FakeGuideFiles(dir) { request ->
                val complete = ++reads > 1
                LiveTvGuide(mapOf(request.keys.single() to listOf(LiveTvProgramme(if (complete) "Recovered" else "Partial", now - hour, now + hour))),
                    emptyMap(), emptySet(), complete = complete)
            }
            val repository = LiveTvRepository(store, store, http, MutableStateFlow(1), true,
                guideFiles = files, epgClock = clock::get, epgTickMs = 5)
            val collector = async { repository.state.collect { } }
            try {
                repository.ensureLoaded()
                withTimeout(5_000) { repository.state.first { it.currentProgrammes.isNotEmpty() && !it.isEpgLoading } }
                assertEquals(1, files.downloads)
                clock.addAndGet(LiveTvRepository.EPG_RETRY_MS + 1)
                withTimeout(5_000) { repository.state.first { it.currentProgrammes.values.any { p -> p.title == "Recovered" } } }
                assertEquals(2, files.downloads)
                withTimeout(5_000) { while (!File(dir, LiveTvGuideCache.FILE_NAME).exists()) kotlinx.coroutines.delay(5) }
            } finally { collector.cancelAndJoin() }
        } finally { dir.deleteRecursively() }
    }

}
