package com.nuvio.tv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/** P0 TCL C6K Detail crash: an em line height (every Thmanyah theme style) must convert without throwing. */
class LineHeightDpTest {
    private val tv = object : Density {
        override val density = 2f
        override val fontScale = 1f
    }

    @Test fun emLineHeightResolvesAgainstTheFontSize() {
        val style = TextStyle(fontSize = 20.sp, lineHeight = NuvioMinLineHeightEm.em)
        assertEquals(31f, style.lineHeightDp(tv).value, 0.001f)
    }

    @Test fun spLineHeightIsUsedAsIs() {
        assertEquals(24f, TextStyle(fontSize = 16.sp, lineHeight = 24.sp).lineHeightDp(tv).value, 0.001f)
    }

    @Test fun unspecifiedLineHeightKeepsTheThmanyahMinimum() {
        val style = TextStyle(fontSize = 10.sp, lineHeight = TextUnit.Unspecified)
        assertEquals(15.5f, style.lineHeightDp(tv).value, 0.001f)
    }
}
