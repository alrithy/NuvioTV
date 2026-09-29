package com.nuvio.tv.core.player.dvmkv

import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.ParserException
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import com.nuvio.tv.fork.recovery.MkvResync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.EOFException

/**
 * G4c (feature 21) fixtures: a Matroska file whose cluster data holds a zero-filled hole (an
 * incomplete Usenet article). Built in memory, EBML element by element.
 */
class MatroskaMalformedResyncTest {

    /** Declared length of a large remote file: the hole is far from the tail. */
    private val largeFileLength = 4L * 1024 * 1024 * 1024

    @Test
    fun resyncHelperFindsTheClusterIdAcrossBlockBoundaries() {
        val block = ByteArray(16).also { CLUSTER.copyInto(it, destinationOffset = 9) }
        assertEquals(9, MkvResync.indexOfClusterId(block, block.size))
        assertEquals(-1, MkvResync.indexOfClusterId(block, 12))
        assertEquals(-1, MkvResync.indexOfClusterId(ByteArray(64), 64))
        assertEquals(MkvResync.BLOCK_BYTES - 3, MkvResync.advanceAfterMiss(MkvResync.BLOCK_BYTES))
    }

    @Test
    fun zeroFilledBlockInsideAClusterIsSkippedToTheNextCluster() {
        val file = mkv(corruptClusters = 1, goodClusterTimecodeMs = 1_000)
        val output = extract(file, largeFileLength)
        assertEquals(listOf(1_000_000L), output.sampleTimesUs)
    }

    @Test
    fun theResyncBudgetStillFailsAPervasivelyDamagedFile() {
        assertEquals(1, extract(mkv(corruptClusters = MkvResync.MAX_ATTEMPTS), largeFileLength).sampleTimesUs.size)
        try {
            extract(mkv(corruptClusters = MkvResync.MAX_ATTEMPTS + 1), largeFileLength)
            fail("expected ParserException once the budget is spent")
        } catch (expected: ParserException) {
            assertTrue(expected.message.orEmpty().contains("No valid varint length mask found"))
        }
    }

    @Test
    fun aHoleNearTheTailKeepsTheOfficialTruncatedTailHandling() {
        val file = mkv(corruptClusters = 1)
        // The whole fixture is within the official tail budget: official ends the stream there.
        val output = extract(file, file.size.toLong())
        assertEquals(emptyList<Long>(), output.sampleTimesUs)
    }

    @Test
    fun corruptionBeforeTheSeekMapIsNotResynced() {
        val file = ebmlHeader() + segment(
            info() + element(TRACKS, element(TRACK_ENTRY, byteArrayOf(0xD7.toByte(), 0x00)))
        )
        try {
            extract(file, largeFileLength)
            fail("expected ParserException before the seek map")
        } catch (expected: ParserException) {
            // Official behavior.
        }
    }

    // --- fixture builders ---

    private fun mkv(corruptClusters: Int, goodClusterTimecodeMs: Int = 0): ByteArray {
        val body = ByteArrayOutputStream()
        body.write(info())
        body.write(tracks())
        repeat(corruptClusters) {
            // Cluster timecode, then a SimpleBlock whose content was zero-filled.
            body.write(element(CLUSTER, element(TIMECODE, byteArrayOf(0x00)) + element(SIMPLE_BLOCK, ByteArray(24))))
        }
        body.write(
            element(
                CLUSTER,
                element(TIMECODE, uint16(goodClusterTimecodeMs)) + element(SIMPLE_BLOCK, simpleBlock())
            )
        )
        return ebmlHeader() + segment(body.toByteArray())
    }

    private fun ebmlHeader() = element(EBML, element(DOC_TYPE, "matroska".toByteArray()))

    private fun segment(content: ByteArray) = element(SEGMENT, content)

    private fun info() = element(INFO, element(TIMECODE_SCALE, uint24(1_000_000)))

    private fun tracks() = element(
        TRACKS,
        element(
            TRACK_ENTRY,
            element(TRACK_NUMBER, byteArrayOf(0x01)) +
                element(TRACK_UID, byteArrayOf(0x01)) +
                element(TRACK_TYPE, byteArrayOf(0x02)) +
                element(CODEC_ID, "A_AC3".toByteArray()) +
                element(AUDIO, element(CHANNELS, byteArrayOf(0x02)) + element(SAMPLING_FREQUENCY, float32(48_000f)))
        )
    )

    /** Track 1, relative timecode 0, keyframe, 8 payload bytes. */
    private fun simpleBlock() = byteArrayOf(0x81.toByte(), 0x00, 0x00, 0x80.toByte()) + ByteArray(8) { 0x0B }

    private fun element(id: ByteArray, content: ByteArray): ByteArray = id + size8(content.size.toLong()) + content

    /** 8-byte EBML size varint: 0x01 marker then 7 bytes. */
    private fun size8(size: Long) = ByteArray(8) { i -> if (i == 0) 0x01 else (size shr (8 * (7 - i))).toByte() }

    private fun uint16(value: Int) = byteArrayOf((value shr 8).toByte(), value.toByte())

    private fun uint24(value: Int) = byteArrayOf((value shr 16).toByte(), (value shr 8).toByte(), value.toByte())

    private fun float32(value: Float) = java.nio.ByteBuffer.allocate(4).putFloat(value).array()

    // --- extraction harness ---

    private fun extract(file: ByteArray, declaredLength: Long): RecordingOutput {
        val extractor = MatroskaExtractor(/* flags= */ 0)
        val output = RecordingOutput()
        extractor.init(output)
        val input = ByteInput(file, declaredLength)
        val holder = PositionHolder()
        var guard = 0
        while (true) {
            when (extractor.read(input, holder)) {
                Extractor.RESULT_END_OF_INPUT -> return output
                Extractor.RESULT_SEEK -> input.seekTo(holder.position)
            }
            check(++guard < 10_000) { "extractor did not finish" }
        }
    }

    private class RecordingOutput : ExtractorOutput {
        val sampleTimesUs = mutableListOf<Long>()

        override fun track(id: Int, type: Int): TrackOutput = object : TrackOutput {
            override fun format(format: Format) = Unit

            override fun sampleData(input: DataReader, length: Int, allowEndOfInput: Boolean, sampleDataPart: Int): Int {
                val scratch = ByteArray(length)
                var read = 0
                while (read < length) {
                    val result = input.read(scratch, read, length - read)
                    if (result == C.RESULT_END_OF_INPUT) {
                        if (allowEndOfInput) return C.RESULT_END_OF_INPUT
                        throw EOFException()
                    }
                    read += result
                }
                return read
            }

            override fun sampleData(data: ParsableByteArray, length: Int, sampleDataPart: Int) {
                data.skipBytes(length)
            }

            override fun sampleMetadata(
                timeUs: Long,
                flags: Int,
                size: Int,
                offset: Int,
                cryptoData: TrackOutput.CryptoData?,
            ) {
                sampleTimesUs += timeUs
            }
        }

        override fun endTracks() = Unit

        override fun seekMap(seekMap: SeekMap) = Unit
    }

    /** In-memory input; [declaredLength] is what `getLength()` reports (a large remote file). */
    private class ByteInput(private val data: ByteArray, private val declaredLength: Long) : ExtractorInput {
        private var position = 0
        private var peekPosition = 0

        fun seekTo(target: Long) {
            position = target.toInt()
            peekPosition = position
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= data.size) return C.RESULT_END_OF_INPUT
            val count = minOf(length, data.size - position)
            System.arraycopy(data, position, buffer, offset, count)
            position += count
            peekPosition = position
            return count
        }

        override fun readFully(target: ByteArray, offset: Int, length: Int, allowEndOfInput: Boolean): Boolean {
            if (length == 0) return true
            if (position >= data.size && allowEndOfInput) return false
            if (position + length > data.size) throw EOFException()
            System.arraycopy(data, position, target, offset, length)
            position += length
            peekPosition = position
            return true
        }

        override fun readFully(target: ByteArray, offset: Int, length: Int) {
            readFully(target, offset, length, false)
        }

        override fun skip(length: Int): Int {
            if (position >= data.size) return C.RESULT_END_OF_INPUT
            val count = minOf(length, data.size - position)
            position += count
            peekPosition = position
            return count
        }

        override fun skipFully(length: Int, allowEndOfInput: Boolean): Boolean {
            if (position >= data.size && allowEndOfInput && length > 0) return false
            if (position + length > data.size) throw EOFException()
            position += length
            peekPosition = position
            return true
        }

        override fun skipFully(length: Int) {
            skipFully(length, false)
        }

        override fun peek(target: ByteArray, offset: Int, length: Int): Int {
            if (peekPosition >= data.size) return C.RESULT_END_OF_INPUT
            val count = minOf(length, data.size - peekPosition)
            System.arraycopy(data, peekPosition, target, offset, count)
            peekPosition += count
            return count
        }

        override fun peekFully(target: ByteArray, offset: Int, length: Int, allowEndOfInput: Boolean): Boolean {
            if (peekPosition >= data.size && allowEndOfInput && length > 0) return false
            if (peekPosition + length > data.size) throw EOFException()
            System.arraycopy(data, peekPosition, target, offset, length)
            peekPosition += length
            return true
        }

        override fun peekFully(target: ByteArray, offset: Int, length: Int) {
            peekFully(target, offset, length, false)
        }

        override fun advancePeekPosition(length: Int, allowEndOfInput: Boolean): Boolean {
            if (peekPosition >= data.size && allowEndOfInput && length > 0) return false
            if (peekPosition + length > data.size) throw EOFException()
            peekPosition += length
            return true
        }

        override fun advancePeekPosition(length: Int) {
            advancePeekPosition(length, false)
        }

        override fun resetPeekPosition() {
            peekPosition = position
        }

        override fun getPeekPosition(): Long = peekPosition.toLong()

        override fun getPosition(): Long = position.toLong()

        override fun getLength(): Long = declaredLength

        override fun <E : Throwable> setRetryPosition(position: Long, e: E) {
            seekTo(position)
            throw e
        }
    }

    private companion object {
        fun id(vararg bytes: Int) = ByteArray(bytes.size) { bytes[it].toByte() }

        val EBML = id(0x1A, 0x45, 0xDF, 0xA3)
        val DOC_TYPE = id(0x42, 0x82)
        val SEGMENT = id(0x18, 0x53, 0x80, 0x67)
        val INFO = id(0x15, 0x49, 0xA9, 0x66)
        val TIMECODE_SCALE = id(0x2A, 0xD7, 0xB1)
        val TRACKS = id(0x16, 0x54, 0xAE, 0x6B)
        val TRACK_ENTRY = id(0xAE)
        val TRACK_NUMBER = id(0xD7)
        val TRACK_UID = id(0x73, 0xC5)
        val TRACK_TYPE = id(0x83)
        val CODEC_ID = id(0x86)
        val AUDIO = id(0xE1)
        val CHANNELS = id(0x9F)
        val SAMPLING_FREQUENCY = id(0xB5)
        val CLUSTER = id(0x1F, 0x43, 0xB6, 0x75)
        val TIMECODE = id(0xE7)
        val SIMPLE_BLOCK = id(0xA3)
    }
}
