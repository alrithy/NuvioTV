package com.nuvio.tv.fork.livetv

import android.content.Context
import android.util.Xml
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

/**
 * Guides in the app cache (`cache/live_tv`), downloaded with Live TV's own client and read with
 * Android's pull parser (G10c; Reshaped `LiveTvHttp.readFile` / `readXmlTvGuide` @ 0ccf049). A read
 * runs on the IO pool and is interrupted when cancelled; the parser checks for that as it goes.
 */
@Singleton
class LiveTvGuideStore internal constructor(context: Context, private val http: LiveTvHttp) : LiveTvGuideFiles {

    @Inject constructor(@ApplicationContext context: Context) : this(context, LiveTvHttp())

    override val dir: File = File(context.cacheDir, "live_tv")

    override suspend fun download(url: String, headers: Map<String, String>, target: File) =
        http.download(url, headers, target)

    override suspend fun read(file: File, request: LiveTvGuideRequest, nowEpochMs: Long, window: LiveTvGuideWindow): LiveTvGuide =
        runInterruptible(Dispatchers.IO) {
            file.inputStream().buffered(BUFFER_BYTES).use { buffered ->
                buffered.mark(2)
                val gzip = buffered.read() == 0x1f && buffered.read() == 0x8b
                buffered.reset()
                val input = if (gzip) GZIPInputStream(buffered, BUFFER_BYTES) else buffered
                readXmlTvGuide(Xml.newPullParser(), input, request, nowEpochMs, window)
            }
        }

    private companion object {
        const val BUFFER_BYTES = 64 * 1024
    }
}
