package com.nuvio.tv.fork.livetv

import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

class LiveTvXmlReaderTest {
    @Test
    fun malformedTailKeepsReadProgrammesButSignalsIncomplete() {
        val guide = read(cut = true)
        assertFalse(guide.complete)
        assertEquals("News", guide.schedule.values.single().single().title)
    }

    @Test
    fun closingXmlTvRootSignalsACompleteRead() {
        val guide = read(cut = false)
        assertTrue(guide.complete)
        assertEquals("News", guide.schedule.values.single().single().title)
    }

    private fun read(cut: Boolean): LiveTvGuide {
        // A pull-parser event seam: no Android Xml stub or new parser dependency in the app.
        data class Event(val type: Int, val name: String? = null, val depth: Int = 0)
        val events = listOf(
            Event(XmlPullParser.START_DOCUMENT), Event(XmlPullParser.START_TAG, "tv", 1),
            Event(XmlPullParser.START_TAG, "programme", 2), Event(XmlPullParser.START_TAG, "title", 3),
            Event(XmlPullParser.END_TAG, "title", 3), Event(XmlPullParser.END_TAG, "programme", 2),
            Event(XmlPullParser.END_TAG, "tv", 1), Event(XmlPullParser.END_DOCUMENT),
        )
        var index = 0
        val parser = Proxy.newProxyInstance(XmlPullParser::class.java.classLoader, arrayOf(XmlPullParser::class.java)) { _, method, args ->
            when (method.name) {
                "getEventType" -> events[index].type
                "getName" -> events[index].name
                "getDepth" -> events[index].depth
                "next" -> {
                    index++
                    if (cut && index == 6) throw XmlPullParserException("cut XML tail")
                    events[index].type
                }
                "nextText" -> { index++; "News" }
                "getAttributeValue" -> when (args[1]) {
                    "channel" -> "1"
                    "start" -> "20260927200000 +0000"
                    "stop" -> "20260927230000 +0000"
                    else -> null
                }
                else -> null
            }
        } as XmlPullParser
        val channel = LiveTvChannel("1", "One", "http://stream", tvgId = "1", guideKey = liveTvGuideKey("1", "One", "a"))
        return readXmlTvGuide(parser, "unused event seam".byteInputStream(), LiveTvGuideRequest.from(listOf(channel)),
            LiveTvClock.parseXmlTvTimestamp("20260927210000 +0000")!!, LiveTvGuideWindow.Regular)
    }
}
