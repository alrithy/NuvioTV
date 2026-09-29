package com.nuvio.tv.fork.video

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ysosrs Hdr10SeiInjector.selfTest (build → parse round trip + ordering) as unit tests. */
class Hdr10SeiInjectorTest {

    private fun unescape(nal: ByteArray, from: Int): ByteArray {
        val out = ArrayList<Byte>(nal.size)
        var zeros = 0
        var i = from
        while (i < nal.size) {
            val v = nal[i].toInt() and 0xFF
            if (zeros >= 2 && v == 0x03 && i + 1 < nal.size && (nal[i + 1].toInt() and 0xFF) <= 0x03) {
                zeros = 0; i++; continue
            }
            out.add(nal[i]); zeros = if (v == 0) zeros + 1 else 0; i++
        }
        return out.toByteArray()
    }

    private fun parse(nal: ByteArray): Pair<Int, ByteArray> {
        val rbsp = unescape(nal, 2)
        val size = rbsp[1].toInt() and 0xFF
        return (rbsp[0].toInt() and 0xFF) to rbsp.copyOfRange(2, 2 + size)
    }

    private fun u16(a: ByteArray, off: Int) = ((a[off].toInt() and 0xFF) shl 8) or (a[off + 1].toInt() and 0xFF)
    private fun u32(a: ByteArray, off: Int) =
        ((a[off].toLong() and 0xFF) shl 24) or ((a[off + 1].toLong() and 0xFF) shl 16) or
            ((a[off + 2].toLong() and 0xFF) shl 8) or (a[off + 3].toLong() and 0xFF)

    private fun ld(vararg nals: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        for (n in nals) { out.write(byteArrayOf(0, 0, 0, n.size.toByte())); out.write(n) }
        return out.toByteArray()
    }

    private fun nalTypes(a: ByteArray): List<Int> {
        val types = ArrayList<Int>()
        var pos = 0
        while (pos + 4 <= a.size) {
            val size = u32(a, pos).toInt(); val start = pos + 4
            if (size <= 0 || start + size > a.size) break
            types += (a[start].toInt() ushr 1) and 0x3F; pos = start + size
        }
        return types
    }

    private val sps = byteArrayOf((33 shl 1).toByte(), 0x01, 0x11, 0x22)
    private val slice = byteArrayOf((1 shl 1).toByte(), 0x01, 0x33, 0x44, 0x55)

    @Test
    fun mdcvRoundTripsBt2020D65AndPqLuminance() {
        val (type, payload) = parse(Hdr10SeiInjector.buildMdcvSeiNal(sourceMinPq = 62, sourceMaxPq = 3079))
        assertEquals(Hdr10SeiInjector.SEI_TYPE_MDCV, type)
        assertEquals(24, payload.size)
        assertEquals(Hdr10SeiInjector.PRIM_G_X, u16(payload, 0))
        assertEquals(Hdr10SeiInjector.WHITE_Y, u16(payload, 14))
        val maxLum = u32(payload, 16) // ~1000 nits in 0.0001 cd/m^2
        assertTrue("$maxLum", maxLum in 9_000_000L..11_000_000L)
    }

    @Test
    fun clliRoundTripsAndIsSuppressedWhenUnknown() {
        val (type, payload) = parse(Hdr10SeiInjector.buildClliSeiNal(617, 496)!!)
        assertEquals(Hdr10SeiInjector.SEI_TYPE_CLLI, type)
        assertEquals(617, u16(payload, 0))
        assertEquals(496, u16(payload, 2))
        assertNull(Hdr10SeiInjector.buildClliSeiNal(0, 0))
        assertEquals(1, Hdr10SeiInjector.buildSeiNals(RpuStaticMetadata(62, 3079, maxCll = 0, maxFall = 0)).size)
        assertEquals(2, Hdr10SeiInjector.buildSeiNals(RpuStaticMetadata(62, 3079, maxCll = 1000, maxFall = 400)).size)
        assertTrue(Hdr10SeiInjector.buildSeiNals(null).isEmpty())
    }

    @Test
    fun emulationPreventionSurvivesARoundTrip() {
        val (_, payload) = parse(Hdr10SeiInjector.wrapSeiNal(5, byteArrayOf(0, 0, 0, 1, 2)))
        assertArrayEquals(byteArrayOf(0, 0, 0, 1, 2), payload)
    }

    @Test
    fun seiGoesBeforeTheFirstSliceAndIsDetected() {
        val sample = ld(sps, slice)
        assertFalse(Hdr10SeiInjector.hasHdr10StaticSei(sample, sample.size, 4))
        val mdcv = Hdr10SeiInjector.buildMdcvSeiNal(62, 3079)
        val injected = Hdr10SeiInjector.injectLengthDelimited(sample, sample.size, 4, listOf(mdcv))
        assertEquals(listOf(33, 39, 1), nalTypes(injected))
        assertTrue(Hdr10SeiInjector.hasHdr10StaticSei(injected, injected.size, 4))
        assertArrayEquals(sample, Hdr10SeiInjector.injectLengthDelimited(sample, sample.size, 4, emptyList()))
    }

    @Test
    fun metadataLineAndStreamInfo() {
        assertEquals("MaxCLL 1000 · MaxFALL - · MDL ~1000 nits",
            RpuStaticMetadata(62, 3079, maxCll = 1000, maxFall = 0).toDiagnosticLine().replace(Regex("~\\d+"), "~1000"))
        assertEquals(RpuStaticMetadata(1, 2, null, 4, 5, null), RpuStaticMetadata.fromNative(intArrayOf(1, 2, -1, 4, 5, -1)))
        assertNull(RpuStaticMetadata.fromNative(intArrayOf(1, 2)))
        assertEquals(DvElType.FEL, DvElType.fromNative(2))
        assertNull(DvElType.fromNative(-1))
        DvStreamInfo.reset()
        assertNull(DvStreamInfo.hudLine())
        DvStreamInfo.elType = DvElType.FEL
        DvStreamInfo.hdr10SeiInjected = true
        assertEquals("FEL · HDR10 SEI added", DvStreamInfo.hudLine())
        DvStreamInfo.reset()
    }

    @Test
    fun pqEotfAnchors() {
        assertEquals(0.0, pqCodeToNits(0), 0.001)
        assertEquals(10_000.0, pqCodeToNits(4095), 1.0)
        assertTrue(pqCodeToNits(3079) in 950.0..1050.0)
    }
}
