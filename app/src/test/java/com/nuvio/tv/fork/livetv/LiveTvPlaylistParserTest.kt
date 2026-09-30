package com.nuvio.tv.fork.livetv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reshaped `LiveTvPlaylistParserTest` @ 0ccf049 (parser cases), plus header and direct-link cases. */
class LiveTvPlaylistParserTest {

    private fun parse(text: String) = parseM3uPlaylist(text.lineSequence())

    @Test
    fun readsChannelsGroupsLogosAndGuide() {
        val playlist = parse(
            """
            #EXTM3U url-tvg="https://guide.example/epg.xml.gz"
            #EXTINF:-1 tvg-id="one.uk" tvg-logo="https://logo/1.png" group-title="News",Channel One
            https://stream.example/1.m3u8
            #EXTINF:-1 group-title="Sports",Sport, HD
            #EXTVLCOPT:http-user-agent=Custom
            https://stream.example/2.ts|Referer=https://ref.example
            """.trimIndent(),
        )
        assertEquals(listOf("https://guide.example/epg.xml.gz"), playlist.epgUrls)
        assertEquals(2, playlist.channels.size)
        val one = playlist.channels[0]
        assertEquals("Channel One", one.name)
        assertEquals("one.uk", one.tvgId)
        assertEquals("https://logo/1.png", one.logoUrl)
        assertEquals("News", one.group)
        assertEquals(LIVE_TV_STREAM_HEADERS, one.headers)
        val two = playlist.channels[1]
        assertEquals("Sport, HD", two.name)
        assertEquals("https://stream.example/2.ts", two.streamUrl)
        assertEquals("Custom", two.headers["User-Agent"])
        assertEquals("https://ref.example", two.headers["Referer"])
    }

    @Test
    fun dropsDuplicatesAndCategorySeparators() {
        val playlist = parse(
            """
            #EXTM3U
            #EXTINF:-1,##### SPORTS #####
            https://stream.example/separator
            #EXTINF:-1,A
            https://stream.example/a
            #EXTINF:-1,A again
            https://stream.example/a
            """.trimIndent(),
        )
        assertEquals(listOf("A"), playlist.channels.map { it.name })
        assertEquals(listOf("m0"), playlist.channels.map { it.id })
    }

    @Test
    fun recognisesAnHlsStreamItself() {
        val playlist = parse(
            """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXTINF:6.0,
            segment1.ts
            """.trimIndent(),
        )
        assertTrue(playlist.isHlsStream)
        assertTrue(playlist.channels.isEmpty())
    }

    @Test
    fun extHttpHeadersApplyOnlyToTheNextEntry() {
        val playlist = parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-name="Named",
            #EXTHTTP:{"Referer":"https://a.example","X-Token":"t"}
            https://stream.example/a
            #EXTINF:-1,Plain
            https://stream.example/b
            """.trimIndent(),
        )
        assertEquals("Named", playlist.channels[0].name)
        assertEquals("https://a.example", playlist.channels[0].headers["Referer"])
        assertEquals("t", playlist.channels[0].headers["X-Token"])
        assertEquals(LIVE_TV_STREAM_HEADERS, playlist.channels[1].headers)
    }

    @Test
    fun unnamedEntriesAreNumberedAndCategoryHeadingsNeedHashesOnBothSides() {
        val playlist = parse("#EXTM3U\nhttps://s/1\n#EXTINF:-1,#1 Hits\nhttps://s/2")
        assertEquals(listOf("Channel 1", "#1 Hits"), playlist.channels.map { it.name })
        assertTrue(isLikelyCategoryHeading("### NEWS ###"))
        assertFalse(isLikelyCategoryHeading("#1 Hits"))
    }

    @Test
    fun directVideoLinksAreSingleChannels() {
        assertTrue("https://h/live/1.ts?token=x".looksLikeDirectVideoUrl())
        assertFalse("https://h/get.php?type=m3u".looksLikeDirectVideoUrl())
        assertFalse("https://h/list.m3u8".looksLikeDirectVideoUrl())
        assertEquals("1.ts", directStreamChannel("https://h/live/1.ts?token=x").name)
    }

    @Test
    fun oversizedListsStopAtTheCap() {
        val lines = sequence {
            yield("#EXTM3U")
            for (i in 0..LIVE_TV_MAX_CHANNELS_PER_SOURCE) yield("https://s/$i")
        }
        assertEquals(LIVE_TV_MAX_CHANNELS_PER_SOURCE, parseM3uPlaylist(lines).channels.size)
    }
}
