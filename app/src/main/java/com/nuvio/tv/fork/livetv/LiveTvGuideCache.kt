package com.nuvio.tv.fork.livetv

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * G10c: FILE_PORT of Reshaped `reshaped/livetv/LiveTvGuideCache.kt` @ 0ccf049, adapted to a v2
 * digest key including source-to-guide association. The programmes kept from the last complete
 * guide read, saved small beside the guides. Opening Live
 * TV again (after it was let go while unused, or after the app was closed) reads this in a moment
 * instead of going through the whole XMLTV file, which is often 100+ MB. It only serves while
 * the guides it came from are unchanged and none of its channels has run out of programmes.
 */
internal object LiveTvGuideCache {
    const val FILE_NAME = "guide_kept.bin.gz"
    private const val VERSION = 2
    private const val MAX_TITLE = 1_000

    class Entry(val schedule: LiveTvSchedule, val logos: Map<String, String>, val nextReadAtMs: Long)

    /** What the saved programmes were read for: the guides, the channels and how much is kept. */
    fun key(epgUrls: List<String>, guideKeys: Set<String>, window: LiveTvGuideWindow,
        channelsByGuide: Map<String, Set<String>> = emptyMap(),
    ): String = liveTvGuideDigest(buildList {
        addAll(listOf(window.pastMs.toString(), window.aheadMs.toString(), window.maxPast.toString(), window.maxAhead.toString()))
        add(guideKeys.size.toString())
        addAll(guideKeys.sorted())
        epgUrls.forEach { url ->
            add(url)
            val keys = channelsByGuide[url].orEmpty()
            add(keys.size.toString())
            addAll(keys.sorted())
        }
    })

    /** The saved programmes, or null when they are missing, stale or for other guides or channels. */
    fun read(file: File, key: String, guideFiles: List<File>, nowMs: Long, maxGuideAgeMs: Long): Entry? = runCatching {
        if (!file.isFile) return null
        DataInputStream(GZIPInputStream(file.inputStream().buffered(), 16 * 1024)).use { input ->
            if (input.readInt() != VERSION || input.readUTF() != key) return null
            val nextReadAtMs = input.readLong()
            if (nowMs >= nextReadAtMs) return null
            val files = input.readInt()
            if (files != guideFiles.size) return null
            guideFiles.forEach { guide ->
                val modified = input.readLong()
                // A guide downloaded since, or due to be downloaded again, means a full read.
                if (guide.lastModified() != modified || nowMs - modified !in 0 until maxGuideAgeMs) return null
            }
            val logos = HashMap<String, String>()
            repeat(input.readInt()) { logos[input.readUTF()] = input.readUTF() }
            val channels = input.readInt()
            val schedule = HashMap<String, List<LiveTvProgramme>>(channels * 2)
            repeat(channels) {
                val guideKey = input.readUTF()
                val count = input.readInt()
                schedule[guideKey] = List(count) { LiveTvProgramme(input.readUTF(), input.readLong(), input.readLong()) }
            }
            Entry(schedule, logos, nextReadAtMs)
        }
    }.getOrNull()

    /** Saves a full read's result; a failed save only means the next opening reads the XML again. */
    fun write(file: File, key: String, guideFiles: List<File>, entry: Entry) {
        val temp = File(file.path + ".tmp")
        runCatching {
            DataOutputStream(GZIPOutputStream(temp.outputStream().buffered(), 16 * 1024)).use { out ->
                out.writeInt(VERSION)
                out.writeUTF(key)
                out.writeLong(entry.nextReadAtMs)
                out.writeInt(guideFiles.size)
                guideFiles.forEach { out.writeLong(it.lastModified()) }
                out.writeInt(entry.logos.size)
                entry.logos.forEach { (guideKey, logo) ->
                    out.writeUTF(guideKey)
                    out.writeUTF(logo)
                }
                out.writeInt(entry.schedule.size)
                entry.schedule.forEach { (guideKey, programmes) ->
                    out.writeUTF(guideKey)
                    out.writeInt(programmes.size)
                    programmes.forEach { programme ->
                        out.writeUTF(programme.title.take(MAX_TITLE))
                        out.writeLong(programme.startEpochMs)
                        out.writeLong(programme.stopEpochMs)
                    }
                }
            }
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
        }.onFailure {
            temp.delete()
            file.delete()
        }
    }
}
