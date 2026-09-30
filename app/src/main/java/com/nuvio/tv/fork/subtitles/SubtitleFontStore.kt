package com.nuvio.tv.fork.subtitles

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class CustomSubtitleFont(
    val file: File,
    /** Family name from the font's `name` table; libass (mpv) matches fonts by it. */
    val familyName: String,
    val typeface: Typeface,
)

/**
 * One user-imported subtitle font in app storage (G6b, features 100, 102, 103, 104). FILE_PORT of
 * Reshaped `reshaped/subtitlefont/SubtitleFontStore.kt` @ 0ccf049.
 *
 * The font file is the setting: no file means the official subtitle font. Every import is
 * validated (size, TrueType/OpenType signature, Android loads it, a family name is readable)
 * before it replaces the current font, and a font that stops loading is dropped so playback falls
 * back to the official font. Local adaptations: HTTPS-only downloads with no redirect downgrade,
 * host-only logs without exception text, and nothing active while SUBTITLE_INTELLIGENCE is OFF.
 */
object SubtitleFontStore {
    private const val TAG = "SubtitleFontStore"
    private const val FONTS_DIR = "subtitle_fonts"
    private const val STAGING_PREFIX = ".import"

    private val _font = MutableStateFlow<CustomSubtitleFont?>(null)
    val font: StateFlow<CustomSubtitleFont?> = _font.asStateFlow()

    @Volatile
    private var loaded = false

    private val warmUpScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(false) // an https link may not redirect to cleartext
            .build()
    }

    fun fontsDir(context: Context): File =
        File(context.applicationContext.filesDir, FONTS_DIR).apply { mkdirs() }

    /** The imported font, loaded from disk on first use; null for the official font. */
    fun current(context: Context): CustomSubtitleFont? {
        if (!SubtitleIntelligence.enabled) return null
        if (!loaded) {
            synchronized(this) {
                if (!loaded) {
                    val files = fontsDir(context).listFiles().orEmpty()
                    // Staging files left behind by an import that was killed mid-copy.
                    files.filter { it.isFile && it.name.startsWith(STAGING_PREFIX) }.forEach { it.delete() }
                    val file = files.firstOrNull { it.isFile && !it.name.startsWith(".") }
                    val font = file?.let(::loadFont)
                    if (file != null && font == null) {
                        // A font that no longer loads: drop it so playback uses the official font.
                        file.delete()
                    }
                    _font.value = font
                    loaded = true
                }
            }
        }
        return _font.value
    }

    /** At launch: loads the font off the main thread so playback never parses it there. */
    fun warmUp(context: Context) {
        if (loaded || !SubtitleIntelligence.enabled) return
        val appContext = context.applicationContext
        warmUpScope.launch { runCatching { current(appContext) } }
    }

    /** The font if already loaded; never touches disk (starts the load instead). */
    private fun cached(context: Context): CustomSubtitleFont? {
        if (!SubtitleIntelligence.enabled) return null
        if (!loaded) warmUp(context)
        return _font.value
    }

    /**
     * The ExoPlayer subtitle typeface, keeping the bold setting; null keeps the official font.
     * Non-blocking: the player re-applies its style when [font] emits.
     */
    fun exoTypeface(context: Context, bold: Boolean): Typeface? {
        val custom = runCatching { cached(context)?.typeface }.getOrNull() ?: return null
        return if (bold) Typeface.create(custom, Typeface.BOLD) else custom
    }

    /**
     * libmpv options that make libass use the imported font; empty keeps the official font.
     * Non-blocking: relies on [warmUp] having run at app start.
     */
    fun mpvOptions(context: Context): List<Pair<String, String>> {
        val custom = runCatching { cached(context) }.getOrNull() ?: return emptyList()
        return listOf(
            "sub-fonts-dir" to fontsDir(context).path,
            "sub-font" to custom.familyName,
        )
    }

    suspend fun importFromUri(context: Context, uri: Uri): SubtitleFontImportResult =
        withContext(Dispatchers.IO) {
            val input = runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
                ?: return@withContext SubtitleFontImportResult.INVALID
            input.use { importFromStream(context, it, declaredLength = null, coroutineContext = coroutineContext) }
        }

    suspend fun importFromUrl(context: Context, url: String): SubtitleFontImportResult =
        withContext(Dispatchers.IO) {
            val trimmed = url.trim()
            if (!SubtitleFontImportPolicy.downloadUrlAllowed(trimmed)) {
                return@withContext SubtitleFontImportResult.DOWNLOAD_FAILED
            }
            val host = SubtitleFontImportPolicy.logHost(trimmed)
            val callContext = coroutineContext
            try {
                val request = Request.Builder().url(trimmed).get().build()
                val call = httpClient.newCall(request)
                // Closing the dialog cancels the coroutine: abort the socket so nothing imports later.
                val cancelHandle = callContext[Job]?.invokeOnCompletion { call.cancel() }
                try {
                    call.execute().use { response ->
                        if (!response.isSuccessful || !response.request.url.isHttps) {
                            Log.w(TAG, "Font download refused host=$host status=${response.code}")
                            return@withContext SubtitleFontImportResult.DOWNLOAD_FAILED
                        }
                        val body = response.body ?: return@withContext SubtitleFontImportResult.DOWNLOAD_FAILED
                        if (body.contentLength() > SubtitleFontFile.MAX_FONT_BYTES) {
                            return@withContext SubtitleFontImportResult.TOO_LARGE
                        }
                        body.byteStream().use {
                            importFromStream(context, it, declaredLength = null, coroutineContext = callContext)
                        }
                    }
                } finally {
                    cancelHandle?.dispose()
                }
            } catch (_: IllegalArgumentException) {
                Log.w(TAG, "Bad font URL host=$host")
                SubtitleFontImportResult.DOWNLOAD_FAILED
            } catch (_: IOException) {
                Log.w(TAG, "Font download failed host=$host")
                SubtitleFontImportResult.DOWNLOAD_FAILED
            }
        }

    /**
     * Copies [input] to a staging file, validates it and makes it the subtitle font. Blocking.
     * With [declaredLength], exactly that many bytes are read (an HTTP request body).
     */
    fun importFromStream(
        context: Context,
        input: InputStream,
        declaredLength: Long?,
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
    ): SubtitleFontImportResult {
        if (!SubtitleIntelligence.enabled) return SubtitleFontImportResult.INVALID
        if (declaredLength != null && declaredLength > SubtitleFontFile.MAX_FONT_BYTES) {
            return SubtitleFontImportResult.TOO_LARGE
        }
        val dir = fontsDir(context)
        val staging = try {
            File.createTempFile(STAGING_PREFIX, ".tmp", dir)
        } catch (_: IOException) {
            Log.w(TAG, "Font staging failed")
            return SubtitleFontImportResult.INVALID
        }
        try {
            val copied = try {
                copyLimited(input, staging, declaredLength, coroutineContext)
            } catch (_: IOException) {
                Log.w(TAG, "Font copy failed")
                return SubtitleFontImportResult.DOWNLOAD_FAILED
            }
            if (copied == null) return SubtitleFontImportResult.TOO_LARGE
            val extension = SubtitleFontFile.extension(staging) ?: return SubtitleFontImportResult.INVALID
            loadFont(staging) ?: return SubtitleFontImportResult.INVALID
            coroutineContext.ensureActive()
            synchronized(this) {
                dir.listFiles()
                    ?.filter { it.isFile && !it.name.startsWith(".") }
                    ?.forEach { it.delete() }
                val target = File(dir, "subtitle_font.$extension")
                if (!staging.renameTo(target)) return SubtitleFontImportResult.INVALID
                val font = loadFont(target)
                if (font == null) target.delete()
                _font.value = font
                loaded = true
                return if (font != null) SubtitleFontImportResult.IMPORTED else SubtitleFontImportResult.INVALID
            }
        } finally {
            if (staging.exists()) staging.delete()
        }
    }

    fun clear(context: Context) {
        synchronized(this) {
            fontsDir(context).listFiles()
                ?.filter { it.isFile && !it.name.startsWith(".") }
                ?.forEach { it.delete() }
            _font.value = null
            loaded = true
        }
    }

    /** Copies at most [SubtitleFontFile.MAX_FONT_BYTES]; the byte count, or null when too large. */
    private fun copyLimited(
        input: InputStream,
        target: File,
        declaredLength: Long?,
        coroutineContext: CoroutineContext,
    ): Long? {
        var total = 0L
        target.outputStream().use { output ->
            val buffer = ByteArray(64 * 1024)
            while (declaredLength == null || total < declaredLength) {
                coroutineContext.ensureActive()
                val wanted = if (declaredLength == null) {
                    buffer.size
                } else {
                    minOf(buffer.size.toLong(), declaredLength - total).toInt()
                }
                val read = input.read(buffer, 0, wanted)
                if (read < 0) break
                total += read
                if (total > SubtitleFontFile.MAX_FONT_BYTES) return null
                output.write(buffer, 0, read)
            }
        }
        if (declaredLength != null && total < declaredLength) throw IOException("Upload ended early")
        return total
    }

    private fun loadFont(file: File): CustomSubtitleFont? = runCatching {
        if (SubtitleFontFile.extension(file) == null) return@runCatching null
        val typeface = Typeface.createFromFile(file)
        if (typeface == null || typeface == Typeface.DEFAULT) return@runCatching null
        val family = SubtitleFontFile.familyName(file) ?: return@runCatching null
        CustomSubtitleFont(file = file, familyName = family, typeface = typeface)
    }.getOrElse {
        Log.w(TAG, "Unusable subtitle font")
        null
    }
}
