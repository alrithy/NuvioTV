package com.nuvio.tv.fork.audio.mat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G13c (45, D063): the pure halves of the MAT path. ysosrs 45e0984 ships no tests for them; these
 * pin the TrueHD access-unit framing (FFmpeg truehd_core / mlp_parser rules) and the packer's frame
 * contract with synthetic access units. Real bitstreams on an eARC receiver are HV-G13-3.
 */
class MatFramingTest {

    /** A synthetic TrueHD access unit: low 12 bits of word 0 are its length in 16-bit words. */
    private fun accessUnit(bytes: Int, frameTime: Int = 0, majorSync: Boolean = false, fill: Int = 0x11): ByteArray {
        require(bytes % 2 == 0 && bytes / 2 <= 0x0FFF)
        val au = ByteArray(bytes) { fill.toByte() }
        val word0 = 0xF000 or (bytes / 2)
        au[0] = (word0 ushr 8).toByte(); au[1] = word0.toByte()
        au[2] = (frameTime ushr 8).toByte(); au[3] = frameTime.toByte()
        if (majorSync) {
            au[4] = 0xF8.toByte(); au[5] = 0x72; au[6] = 0x6F; au[7] = 0xBA.toByte()
            au[8] = 0x00 // ratebits 0 (48 kHz family)
        }
        return au
    }

    @Test
    fun aChunkWithSeveralAccessUnitsYieldsEachWhole() {
        val framer = TrueHdAuFramer()
        val chunk = accessUnit(40) + accessUnit(64, fill = 0x22)
        val units = mutableListOf<ByteArray>()
        assertEquals(2, framer.feed(chunk, 0, chunk.size) { units += it; 0 })
        assertEquals(listOf(40, 64), units.map { it.size })
        assertEquals(0x22.toByte(), units[1][10])
    }

    @Test
    fun anAccessUnitSplitAcrossChunksIsEmittedOnceComplete() {
        val framer = TrueHdAuFramer()
        val au = accessUnit(100)
        val units = mutableListOf<ByteArray>()
        assertEquals(0, framer.feed(au, 0, 1) { units += it; 0 })
        assertEquals(0, framer.feed(au, 1, 40) { units += it; 0 })
        assertEquals(1, framer.feed(au, 41, 59) { units += it; 0 })
        assertTrue(au.contentEquals(units.single()))
    }

    @Test
    fun aMalformedLengthStopsInsteadOfLooping() {
        val framer = TrueHdAuFramer()
        val chunk = accessUnit(20) + byteArrayOf(0xF0.toByte(), 0x00, 1, 2, 3, 4)
        var count = 0
        assertEquals(1, framer.feed(chunk, 0, chunk.size) { count++; 0 })
        assertEquals(1, count)
    }

    @Test
    fun resetDropsAPartialAccessUnit() {
        val framer = TrueHdAuFramer()
        val au = accessUnit(50)
        framer.feed(au, 0, 30) { 0 }
        framer.reset()
        val next = accessUnit(20)
        val units = mutableListOf<ByteArray>()
        assertEquals(1, framer.feed(next, 0, next.size) { units += it; 0 })
        assertEquals(20, units.single().size)
    }

    @Test
    fun majorSyncIsRecognisedWithEitherLowBit() {
        assertTrue(TrueHdAuFramer.isMajorSync(accessUnit(20, majorSync = true)))
        val other = accessUnit(20, majorSync = true).also { it[7] = 0xBB.toByte() }
        assertTrue(TrueHdAuFramer.isMajorSync(other))
        assertFalse(TrueHdAuFramer.isMajorSync(accessUnit(20)))
        assertFalse(TrueHdAuFramer.isMajorSync(ByteArray(6)))
    }

    @Test
    fun thePackerIgnoresRuntsAndWaitsForAMajorSync() {
        val packer = MatPacker()
        assertFalse(packer.packTrueHD(ByteArray(8), 8))
        val plain = accessUnit(200, frameTime = 40)
        assertFalse(packer.packTrueHD(plain, plain.size))
        assertNull(packer.getOutputFrame())
    }

    @Test
    fun aSteadyStreamBecomesFullSizeMatFrames() {
        val packer = MatPacker()
        val frames = mutableListOf<ByteArray>()
        // 48 kHz family: 40 samples per access unit, 24 access units per 61,440-byte MAT frame.
        for (i in 0 until 72) {
            val au = accessUnit(400, frameTime = i * 40, majorSync = i == 0)
            if (packer.packTrueHD(au, au.size)) {
                while (true) frames += packer.getOutputFrame() ?: break
            }
        }
        assertTrue("got ${frames.size} frames", frames.size >= 2)
        frames.forEach { assertEquals(MatPacker.MAT_BUFFER_SIZE, it.size) }
    }
}
