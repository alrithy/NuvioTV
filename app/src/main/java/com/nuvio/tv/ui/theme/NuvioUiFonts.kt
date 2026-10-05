package com.nuvio.tv.ui.theme

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Loads Thmanyah Sans from the encrypted pack the build generates ([NuvioUiFontPack], docs/PRIVATE_FONTS.md).
 *
 * The APK carries no font file: the five weights sit AES-CTR encrypted in one neutral asset. They are
 * decrypted once per process into direct memory buffers, checked against the licensed files' SHA-256
 * (the exact original bytes, unmodified), and handed to the platform as in-memory fonts. Nothing is
 * written to disk or cache. Each weight keeps the platform Sans as its per-glyph fallback, so glyphs
 * the family lacks (arrows, ★, Persian letters) never render as tofu.
 *
 * In-memory fonts need API 29; older devices, builds without the private input and a pack that fails
 * its checks use the platform Sans.
 */
object NuvioUiFonts {
    private const val TAG = "NuvioUiFonts"

    /** Whether this build carries the pack and this device can load fonts from memory. */
    val available: Boolean
        get() = NuvioUiFontPack.EMBEDDED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** Why the pack could not be used, or null once it loaded (or before the first load). */
    @Volatile var loadProblem: String? = null
        private set

    @Volatile private var loaded: Map<Int, Loaded>? = null

    private class Loaded(val font: Font, val typeface: Typeface)

    /** The Thmanyah Sans typeface for one of the five real weights, or the platform Sans. */
    fun typeface(context: Context, weight: Int): Typeface =
        load(context)[weight]?.typeface ?: platformSans(weight)

    private fun platformSans(weight: Int): Typeface =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Typeface.create(Typeface.SANS_SERIF, weight, false)
        else Typeface.create(Typeface.SANS_SERIF, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)

    /** The in-memory font for a weight; its style reports the weight read from the file itself. */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun font(context: Context, weight: Int): Font? = load(context)[weight]?.font

    private fun load(context: Context): Map<Int, Loaded> {
        loaded?.let { return it }
        return synchronized(this) {
            loaded ?: (
                if (NuvioUiFontPack.EMBEDDED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) decode(context.applicationContext.assets)
                else emptyMap()
            ).also { loaded = it }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun decode(assets: AssetManager): Map<Int, Loaded> {
        var packed = ByteArray(0)
        val key = NuvioUiFontPack.key()
        return try {
            packed = assets.open(NuvioUiFontPack.ASSET, AssetManager.ACCESS_STREAMING).use { it.readBytes() }
            val secret = SecretKeySpec(key, "AES")
            NuvioUiFontPack.WEIGHTS.indices.associate { index ->
                val weight = NuvioUiFontPack.WEIGHTS[index]
                val length = NuvioUiFontPack.LENGTHS[index]
                val iv = NuvioUiFontPack.iv(index)
                // Direct memory the platform font keeps; the decrypted bytes never touch a file.
                val buffer = ByteBuffer.allocateDirect(length)
                Cipher.getInstance("AES/CTR/NoPadding").run {
                    init(Cipher.DECRYPT_MODE, secret, IvParameterSpec(iv))
                    doFinal(ByteBuffer.wrap(packed, NuvioUiFontPack.OFFSETS[index], length), buffer)
                }
                iv.fill(0)
                buffer.flip()
                val digest = MessageDigest.getInstance("SHA-256").run { update(buffer.duplicate()); digest() }
                    .joinToString("") { "%02x".format(it) }
                check(digest == NuvioUiFontPack.SHA256[index]) { "weight $weight does not match the licensed file" }
                // No setWeight: the builder reads the weight from the file's OS/2 table.
                val font = Font.Builder(buffer).build()
                check(font.style.weight == weight) { "weight $weight file reports ${font.style.weight}" }
                val typeface = Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build())
                    .setStyle(font.style)
                    .setSystemFallback("sans-serif")
                    .build()
                weight to Loaded(font, typeface)
            }.also { loadProblem = null }
        } catch (error: Exception) {
            loadProblem = "${error.javaClass.simpleName}: ${error.message}"
            Log.e(TAG, "Thmanyah Sans pack unusable; using the platform Sans ($loadProblem)")
            emptyMap()
        } finally {
            packed.fill(0)
            key.fill(0)
        }
    }
}

/** One real Thmanyah Sans weight for Compose, served from [NuvioUiFonts]. */
@OptIn(ExperimentalTextApi::class)
internal class NuvioMemoryFont(override val weight: FontWeight) :
    AndroidFont(FontLoadingStrategy.Blocking, NuvioMemoryFontLoader, FontVariation.Settings()) {
    override val style: FontStyle = FontStyle.Normal

    override fun equals(other: Any?) = other is NuvioMemoryFont && other.weight == weight
    override fun hashCode() = weight.hashCode()
    override fun toString() = "NuvioMemoryFont(weight=${weight.weight})"
}

private object NuvioMemoryFontLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface =
        NuvioUiFonts.typeface(context, font.weight.weight)

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface = loadBlocking(context, font)
}
