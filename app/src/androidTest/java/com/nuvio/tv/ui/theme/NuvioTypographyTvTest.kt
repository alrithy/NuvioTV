@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.domain.model.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.filters.SdkSuppress

/**
 * The root Nuvio typography owner renders the real, embedded Thmanyah Sans files (docs/PRIVATE_FONTS.md).
 * Runs in the Netflix Visual workflow, whose same-repository builds require the private font input.
 */
// Typeface.getWeight() needs API 28; the TV visual emulator runs API 31.
@SdkSuppress(minSdkVersion = 28)
@RunWith(AndroidJUnit4::class)
class NuvioTypographyTvTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val weights = listOf(
        FontWeight.Light to { ThmanyahFontResources.light() },
        FontWeight.Normal to { ThmanyahFontResources.regular() },
        FontWeight.Medium to { ThmanyahFontResources.medium() },
        FontWeight.Bold to { ThmanyahFontResources.bold() },
        FontWeight.Black to { ThmanyahFontResources.black() },
    )
    private val arabicKeyboard = "ابتثجحخدذرزسشصضطظعغفقكلمنهويءأإآةى"
    private val arabicDigits = "٠١٢٣٤٥٦٧٨٩"

    @Test fun rootThemeUsesNuvioFontFamily() {
        var tvStyles = emptyList<TextStyle>()
        var coreStyles = emptyList<TextStyle>()
        var themedWidth = 0; var platformWidth = 0
        compose.setContent {
            NuvioTheme(appTheme = AppTheme.WHITE) {
                val t = MaterialTheme.typography
                tvStyles = listOf(t.displayLarge, t.displayMedium, t.headlineLarge, t.headlineMedium, t.titleLarge, t.titleMedium,
                    t.titleSmall, t.bodyLarge, t.bodyMedium, t.bodySmall, t.labelLarge, t.labelMedium, t.labelSmall,
                    NuvioTheme.textStyles.nav, NuvioTheme.textStyles.button, NuvioTheme.textStyles.playerControl)
                val core = androidx.compose.material3.MaterialTheme.typography
                coreStyles = listOf(core.bodyLarge, core.bodyMedium, core.titleMedium, core.labelLarge)
                val measurer = rememberTextMeasurer()
                val sample = "Nuvio نوفيو Settings الإعدادات"
                themedWidth = measurer.measure(sample, t.bodyLarge).size.width
                platformWidth = measurer.measure(sample, t.bodyLarge.copy(fontFamily = FontFamily.SansSerif)).size.width
                Text(sample, style = t.bodyLarge, modifier = Modifier.testTag("sample"))
            }
        }
        compose.runOnIdle {
            assertTrue("BuildConfig.THMANYAH_EMBEDDED", BuildConfig.THMANYAH_EMBEDDED)
            assertTrue("generated resources embedded", ThmanyahFontResources.EMBEDDED)
            tvStyles.forEachIndexed { i, style -> assertTrue("tv style $i", style.fontFamily === NuvioFontFamily) }
            coreStyles.forEachIndexed { i, style -> assertTrue("material3 style $i", style.fontFamily === NuvioFontFamily) }
            tvStyles.forEach { assertEquals("no synthetic weights", FontSynthesis.None, it.fontSynthesis) }
            assertTrue("rendered width differs from platform Sans ($themedWidth vs $platformWidth)", themedWidth != platformWidth)
        }
        // A real default-theme Text is laid out with the Nuvio family.
        assertTrue(textLayout("sample").layoutInput.style.fontFamily === NuvioFontFamily)
    }

    @Test fun thmanyahWeightsMapToRealWeights() {
        assertTrue("Thmanyah Sans is embedded", ThmanyahFontResources.EMBEDDED)
        val resolver = createFontFamilyResolver(context)
        val inks = weights.map { (weight, resource) ->
            // The resource file itself carries that OS/2 weight: a real file per weight, not one file reused.
            val file = checkNotNull(ResourcesCompat.getFont(context, resource()))
            assertEquals("file weight for ${weight.weight}", weight.weight, file.weight)
            // Compose resolves the family to that same weight with synthesis disabled.
            val resolved = resolver.resolve(NuvioFontFamily, weight, FontStyle.Normal, FontSynthesis.None).value as Typeface
            assertEquals("resolved weight for ${weight.weight}", weight.weight, resolved.weight)
            ink(file, "نوفيو Nuvio")
        }
        // Heavier files draw strictly more ink: five visibly different real weights.
        inks.zipWithNext().forEachIndexed { i, (lighter, heavier) ->
            assertTrue("weight ${weights[i + 1].first.weight} ink $heavier > ${weights[i].first.weight} ink $lighter", heavier > lighter)
        }
        // There is no SemiBold file: 600 (used for buttons and tabs) uses the real Bold file, never fake bold.
        val semiBold = resolver.resolve(NuvioFontFamily, FontWeight.SemiBold, FontStyle.Normal, FontSynthesis.None).value as Typeface
        assertEquals(700, semiBold.weight)
    }

    @Test fun arabicGlyphsRenderWithoutTofu() {
        assertTrue("Thmanyah Sans is embedded", ThmanyahFontResources.EMBEDDED)
        val regular = checkNotNull(ResourcesCompat.getFont(context, ThmanyahFontResources.regular()))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = regular; textSize = 64f }
        val platform = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.SANS_SERIF; textSize = 64f }
        val ownGlyphs = (arabicKeyboard + arabicDigits + "،؛؟" + "0123456789").map(Char::toString)
        val signatures = mutableSetOf<String>()
        for (glyph in ownGlyphs) {
            assertTrue("$glyph has a glyph", paint.hasGlyph(glyph))
            val own = render(paint, glyph)
            // Drawn by Thmanyah Sans itself, not by the platform fallback.
            assertTrue("$glyph is drawn by Thmanyah Sans", !own.sameAs(render(platform, glyph)))
            if (glyph in arabicKeyboard.map(Char::toString)) assertTrue("$glyph is unique", signatures.add(signature(own)))
        }
        // Diacritics are drawn as marks on the letter (each one changes the rendering).
        val base = render(paint, "ب")
        for (mark in listOf("\u064E", "\u0650", "\u064F", "\u0651", "\u0652", "\u064B")) {
            assertTrue("diacritic U+${"%04X".format(mark[0].code)} renders", !render(paint, "ب$mark").sameAs(base))
        }
        // Contextual shaping: a joined word is not a row of isolated forms.
        assertTrue("joining forms", paint.measureText("بببب") < paint.measureText("ب") * 4 - 1f)
        // The lam-alef ligature is narrower than its two isolated letters.
        assertTrue("lam-alef ligature", paint.measureText("لا") < paint.measureText("ل") + paint.measureText("ا"))
        // Glyphs the family lacks use the controlled platform fallback, never tofu.
        for (symbol in listOf("→", "★", "▶", "پ", "گ")) assertTrue("$symbol falls back", paint.hasGlyph(symbol))
    }

    @Test fun mixedArabicLatinDoesNotClip() {
        val mixed = "الممر الشمالي: Northern Passage 2 · ٢٠٢٦ · 4K إِنَّ آخر"
        compose.setContent {
            NuvioTheme(appTheme = AppTheme.WHITE) {
                Column(Modifier.background(Color.Black).padding(8.dp)) {
                    val t = MaterialTheme.typography
                    for ((tag, style) in listOf("body" to t.bodyLarge, "title" to t.titleLarge, "heading" to t.headlineMedium,
                        "netflixDescription" to t.bodyLarge.copy(fontSize = NetflixThemeTokens.description,
                            lineHeight = NetflixThemeTokens.descriptionLineHeight), "small" to t.labelSmall.copy(fontSize = 12.sp))) {
                        Box(Modifier.background(Color.Black)) {
                            Text(mixed, style = style, color = Color.White, maxLines = 1, modifier = Modifier.testTag(tag))
                        }
                    }
                }
            }
        }
        for (tag in listOf("body", "title", "heading", "netflixDescription", "small")) {
            val layout = textLayout(tag)
            val size = compose.onNodeWithTag(tag).fetchSemanticsNode().size
            assertEquals("$tag on one line", 1, layout.lineCount)
            assertTrue("$tag not ellipsized", !layout.isLineEllipsized(0) && !layout.didOverflowWidth)
            assertTrue("$tag line fits its height", layout.getLineBottom(0) <= size.height + .5f)
            val image = compose.onNodeWithTag(tag).captureToImage()
            val rows = inkRows(image)
            assertTrue("$tag draws text", rows != null)
            assertTrue("$tag ascenders/diacritics not cut (${rows!!.first}..${rows.second} of ${image.height})",
                rows.first > 0 && rows.second < image.height - 1)
        }
    }

    private fun textLayout(tag: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private fun inkRows(image: ImageBitmap): Pair<Int, Int>? {
        val pixels = image.toPixelMap()
        var top = -1; var bottom = -1
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            val p = pixels[x, y]
            if (p.red > .45f && p.green > .45f && p.blue > .45f) { if (top < 0) top = y; bottom = y }
        }
        return if (top < 0) null else top to bottom
    }

    private fun render(paint: Paint, text: String): Bitmap {
        val bitmap = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawText(text, 40f, 84f, paint)
        return bitmap
    }

    private fun ink(typeface: Typeface, text: String): Int {
        val bitmap = Bitmap.createBitmap(900, 160, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawText(text, 20f, 110f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.typeface = typeface; textSize = 80f })
        var total = 0
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            for (pixel in row) total += pixel ushr 24
        }
        bitmap.recycle()
        return total
    }

    private fun signature(bitmap: Bitmap): String {
        val row = IntArray(bitmap.width)
        return buildString {
            for (y in 0 until bitmap.height step 4) {
                bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
                for (x in 0 until bitmap.width step 4) append(if (row[x] ushr 24 > 96) '1' else '0')
            }
        }
    }
}
