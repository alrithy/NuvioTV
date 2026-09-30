package com.nuvio.tv.fork.skip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkipTextParsersTest {

    @Test
    fun videoSkipCuesKeepTheirActions() {
        val raw = """
            00:01:00 --> 00:01:30
            Violence - skip

            0:20:00.5 --> 0:20:04
            Profanity (audio)

            1:10:00 --> 1:10:10
            Nudity visual blur

            2:00 --> 1:00
            Violence backwards

            3:00 --> 3:10
            Unknown label
        """.trimIndent()
        val reports = SkipTextParsers.parseVideoSkip(raw, isMovie = true)
        assertEquals(
            listOf(
                Triple(SkipCategories.VIOLENCE, SkipReport.ACTION_SKIP, 60.0),
                Triple(SkipCategories.PROFANITY, SkipReport.ACTION_MUTE, 1200.5),
                Triple(SkipCategories.NUDITY, SkipReport.ACTION_WARN, 4200.0),
            ),
            reports.map { Triple(it.category, it.action, it.startTime) },
        )
        assertTrue(reports.all { it.provider == ForkSkipProvider.VIDEO_SKIP.key })
    }

    @Test
    fun notScareEntriesBecomeShortWarnings() {
        val html = """
            <html><head><script>var t = "12:34 Major";</script><style>.a{}</style></head>
            <body><ul><li>12:34 Major</li><li>1:02:03 Minor &amp; loud</li><li>no time here</li></ul></body></html>
        """.trimIndent()
        val reports = SkipTextParsers.parseNotScarePage(html)
        assertEquals(listOf(754.0 to 760.0, 3723.0 to 3727.0), reports.map { it.startTime to it.endTime })
        assertTrue(reports.all { it.action == SkipReport.ACTION_WARN && it.category == SkipCategories.JUMPSCARE })
    }

    @Test
    fun notScareSlugsMatchTheSite() {
        assertEquals("alien", SkipTextParsers.notScareSlug("Alien"))
        assertEquals("the-conjuring-2", SkipTextParsers.notScareSlug("The Conjuring 2"))
        assertEquals("amelie", SkipTextParsers.notScareSlug("Amélie"))
        assertEquals("tom-and-jerry", SkipTextParsers.notScareSlug("Tom & Jerry"))
        assertEquals("dont-breathe", SkipTextParsers.notScareSlug("Don't Breathe"))
    }

    @Test
    fun timestampsAndEntities() {
        assertEquals(5.5, SkipTextParsers.parseTimestamp("5.5")!!, 0.0)
        assertEquals(125.0, SkipTextParsers.parseTimestamp("2:05")!!, 0.0)
        assertEquals(3725.0, SkipTextParsers.parseTimestamp("1:02:05")!!, 0.0)
        assertNull(SkipTextParsers.parseTimestamp("1:x"))
        assertNull(SkipTextParsers.parseTimestamp("-3"))
        assertEquals("a & b < c", SkipTextParsers.decodeHtmlEntities("a &amp; b &lt; c"))
        assertEquals("A", SkipTextParsers.decodeHtmlEntities("&#65;"))
        assertEquals("\n\nvisible\n", SkipTextParsers.visibleHtmlText("<script>hidden</script><p>visible</p>"))
    }
}
