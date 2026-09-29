package com.nuvio.tv.core.player.dvmkv

import androidx.media3.common.C
import androidx.media3.extractor.ExtractorInput
import com.nuvio.tv.fork.recovery.MkvResync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.EOFException

/**
 * G4c (feature 21): the resync gate and the Cluster scan used by [MatroskaExtractor] after
 * malformed cluster data. Static helpers, like the official dvmkv tests: the full extractor needs
 * a working `android.util.SparseArray`, which the stubbed unit-test android.jar does not provide.
 */
class MatroskaMalformedResyncTest {

    private val cluster = byteArrayOf(0x1F, 0x43, 0xB6.toByte(), 0x75)

    @Test
    fun resyncHelperFindsTheClusterIdAcrossBlockBoundaries() {
        val block = ByteArray(16).also { cluster.copyInto(it, destinationOffset = 9) }
        assertEquals(9, MkvResync.indexOfClusterId(block, block.size))
        assertEquals(-1, MkvResync.indexOfClusterId(block, 12))
        assertEquals(-1, MkvResync.indexOfClusterId(ByteArray(64), 64))
        assertEquals(MkvResync.BLOCK_BYTES - 3, MkvResync.advanceAfterMiss(MkvResync.BLOCK_BYTES))
    }

    @Test
    fun resyncRunsOnlyPastTheSeekMapWithinBudgetAndNeverOnATruncatedTail() {
        assertTrue(MatroskaExtractor.shouldResyncAfterMalformedData(true, true, 1, false))
        assertFalse(MatroskaExtractor.shouldResyncAfterMalformedData(false, true, 1, false))
        assertFalse(MatroskaExtractor.shouldResyncAfterMalformedData(true, false, 1, false))
        assertFalse(MatroskaExtractor.shouldResyncAfterMalformedData(true, true, 0, false))
        // Official truncated-tail handling wins.
        assertFalse(MatroskaExtractor.shouldResyncAfterMalformedData(true, true, 1, true))
    }

    @Test
    fun zeroFilledHoleIsSkippedToTheNextClusterId() {
        val data = ByteArray(1_000) + cluster + byteArrayOf(0x01, 0x02)
        val input = ByteInput(data)
        assertTrue(MatroskaExtractor.resyncToNextCluster(input, ByteArray(MkvResync.BLOCK_BYTES)))
        assertEquals(1_000L, input.position)
    }

    @Test
    fun aClusterIdStraddlingTwoBlocksIsFound() {
        // 16-byte blocks: the ID starts at 14 and ends in the second block.
        val data = ByteArray(14) + cluster + ByteArray(10)
        val input = ByteInput(data)
        assertTrue(MatroskaExtractor.resyncToNextCluster(input, ByteArray(16)))
        assertEquals(14L, input.position)
    }

    @Test
    fun noClusterBeforeTheEndOfInputGivesUp() {
        assertFalse(MatroskaExtractor.resyncToNextCluster(ByteInput(ByteArray(40_000)), ByteArray(16_384)))
        assertFalse(MatroskaExtractor.resyncToNextCluster(ByteInput(ByteArray(2)), ByteArray(16)))
    }

    private class ByteInput(private val data: ByteArray) : ExtractorInput {
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

        override fun getLength(): Long = data.size.toLong()

        override fun <E : Throwable> setRetryPosition(position: Long, e: E) {
            seekTo(position)
            throw e
        }
    }
}
