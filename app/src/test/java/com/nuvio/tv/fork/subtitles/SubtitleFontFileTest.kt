package com.nuvio.tv.fork.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

class SubtitleFontFileTest {

    /** Minimal sfnt: one `name` table with one family-name record (platform/encoding/language as given). */
    private fun sfnt(
        version: Int = 0x00010000,
        family: String = "Test Sans",
        platformId: Int = 3,
        encodingId: Int = 1,
        languageId: Int = 0x0409,
        nameId: Int = 1,
    ): ByteArray {
        val name = if (platformId == 1) family.toByteArray(Charsets.ISO_8859_1) else family.toByteArray(Charsets.UTF_16BE)
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).apply {
            writeInt(version)
            writeShort(1) // numTables
            writeShort(0); writeShort(0); writeShort(0)
            writeInt(0x6E616D65) // "name"
            writeInt(0) // checksum
            writeInt(28) // offset: 12-byte header + one 16-byte table record
            writeInt(6 + 12 + name.size)
            writeShort(0) // name table format
            writeShort(1) // count
            writeShort(6 + 12) // string storage offset
            writeShort(platformId); writeShort(encodingId); writeShort(languageId)
            writeShort(nameId); writeShort(name.size); writeShort(0)
            write(name)
        }
        return bytes.toByteArray()
    }

    private fun file(bytes: ByteArray): File =
        File.createTempFile("font", ".bin").apply { deleteOnExit(); writeBytes(bytes) }

    @Test
    fun trueTypeAndOpenTypeSignatures() {
        assertEquals("ttf", SubtitleFontFile.extension(file(sfnt(version = 0x00010000))))
        assertEquals("ttf", SubtitleFontFile.extension(file(sfnt(version = 0x74727565))))
        assertEquals("otf", SubtitleFontFile.extension(file(sfnt(version = 0x4F54544F))))
    }

    @Test
    fun nonFontsAndCollectionsAreRejected() {
        assertNull(SubtitleFontFile.extension(file("<html>not a font</html>".toByteArray())))
        assertNull(SubtitleFontFile.extension(file(sfnt(version = 0x74746366)))) // "ttcf" collection
        assertNull(SubtitleFontFile.extension(file(ByteArray(4))))
    }

    @Test
    fun familyNameFromWindowsRecord() {
        assertEquals("Test Sans", SubtitleFontFile.familyName(file(sfnt())))
    }

    @Test
    fun arabicFamilyNameIsReadAsUtf16() {
        assertEquals("نسخ عربي", SubtitleFontFile.familyName(file(sfnt(family = "نسخ عربي", languageId = 0x0401))))
    }

    @Test
    fun macRomanRecordIsTheFallback() {
        assertEquals("Mac Font", SubtitleFontFile.familyName(file(sfnt(family = "Mac Font", platformId = 1, encodingId = 0, languageId = 0))))
    }

    @Test
    fun missingFamilyRecordOrTruncatedFileGivesNull() {
        assertNull(SubtitleFontFile.familyName(file(sfnt(nameId = 4))))
        assertNull(SubtitleFontFile.familyName(file(sfnt().copyOf(30))))
        assertNull(SubtitleFontFile.familyName(file(ByteArray(0))))
    }
}
