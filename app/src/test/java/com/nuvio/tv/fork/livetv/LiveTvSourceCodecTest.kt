package com.nuvio.tv.fork.livetv

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveTvSourceCodecTest {

    @Test
    fun everySourceTypeRoundTrips() {
        val sources = listOf(
            LiveTvSource("m", LiveTvSourceType.M3u, "http://h/get.php?username=u&password=\"p\""),
            LiveTvSource("x", LiveTvSourceType.Xtream, "http://x", xtream = LiveTvXtreamSettings("http://x", "u", "p\\w")),
            LiveTvSource("s", LiveTvSourceType.Stalker, "http://s/c", stalker = LiveTvStalkerSettings("http://s/c", "00:1A", "l", "pw")),
        )
        assertEquals(sources, LiveTvSourceCodec.decode(LiveTvSourceCodec.encode(sources)))
    }

    @Test
    fun unreadableTextOrEntriesAreSkipped() {
        assertEquals(emptyList<LiveTvSource>(), LiveTvSourceCodec.decode(""))
        assertEquals(emptyList<LiveTvSource>(), LiveTvSourceCodec.decode("not json"))
        assertEquals(
            listOf("ok"),
            LiveTvSourceCodec.decode("""[{"id":"","type":"M3u"},{"id":"x","type":"Unknown"},{"id":"ok","type":"M3u","url":"u","extra":{"a":1}}]""")
                .map { it.id },
        )
    }
}
