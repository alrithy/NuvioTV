package com.nuvio.tv.fork.subtitles

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * TrueType/OpenType checks for a user font (G6b, feature 103). FILE_PORT of the private helpers in
 * Reshaped `reshaped/subtitlefont/SubtitleFontStore.kt` @ 0ccf049 (`fontExtension`,
 * `readFontFamilyName`); pure `java.io`, so a malformed file returns null instead of throwing.
 */
object SubtitleFontFile {
    const val MAX_FONT_BYTES = 20L * 1024 * 1024
    private const val SFNT_TRUETYPE = 0x00010000
    private const val SFNT_TRUE = 0x74727565 // "true"
    private const val SFNT_OTTO = 0x4F54544F // "OTTO"
    private const val TAG_NAME = 0x6E616D65 // "name"
    private const val MAX_TABLES = 512

    /** "ttf" or "otf" from the sfnt signature; null when it is not a single TrueType/OpenType font. */
    fun extension(file: File): String? = try {
        if (file.length() < 12) {
            null
        } else {
            when (RandomAccessFile(file, "r").use { it.readInt() }) {
                SFNT_TRUETYPE, SFNT_TRUE -> "ttf"
                SFNT_OTTO -> "otf"
                else -> null
            }
        }
    } catch (_: IOException) {
        null
    }

    /**
     * Family name (name ID 1) from the `name` table; libass (mpv) matches fonts by it. Prefers a
     * Unicode or Windows US-English record, then any Unicode record, then a Mac Roman one.
     */
    fun familyName(file: File): String? = try {
        RandomAccessFile(file, "r").use(::readFamilyName)
    } catch (_: IOException) {
        null
    }

    private fun readFamilyName(raf: RandomAccessFile): String? {
        val fileLength = raf.length()
        val version = raf.readInt()
        if (version != SFNT_TRUETYPE && version != SFNT_OTTO && version != SFNT_TRUE) return null
        val numTables = raf.readUnsignedShort()
        if (numTables > MAX_TABLES) return null
        raf.skipBytes(6)
        var nameOffset = -1L
        repeat(numTables) {
            val tag = raf.readInt()
            raf.skipBytes(4)
            val offset = raf.readInt().toLong() and 0xFFFFFFFFL
            raf.skipBytes(4)
            if (tag == TAG_NAME) nameOffset = offset
        }
        if (nameOffset < 0 || nameOffset >= fileLength) return null
        raf.seek(nameOffset)
        raf.skipBytes(2)
        val count = raf.readUnsignedShort()
        val stringsOffset = nameOffset + raf.readUnsignedShort()
        var fallback: String? = null
        for (i in 0 until count) {
            val recordOffset = nameOffset + 6 + i * 12L
            if (recordOffset + 12 > fileLength) break
            raf.seek(recordOffset)
            val platformId = raf.readUnsignedShort()
            val encodingId = raf.readUnsignedShort()
            val languageId = raf.readUnsignedShort()
            val nameId = raf.readUnsignedShort()
            val length = raf.readUnsignedShort()
            val offset = raf.readUnsignedShort()
            if (nameId != 1 || length == 0) continue
            if (stringsOffset + offset + length > fileLength) continue
            val bytes = ByteArray(length)
            raf.seek(stringsOffset + offset)
            raf.readFully(bytes)
            when {
                platformId == 3 || platformId == 0 -> {
                    val name = String(bytes, Charsets.UTF_16BE).trim()
                    if (name.isNotEmpty() && (platformId == 0 || languageId == 0x0409 || encodingId == 0)) {
                        return name
                    }
                    if (name.isNotEmpty() && fallback == null) fallback = name
                }
                platformId == 1 && encodingId == 0 && fallback == null ->
                    fallback = String(bytes, Charsets.ISO_8859_1).trim().ifEmpty { null }
            }
        }
        return fallback
    }
}
