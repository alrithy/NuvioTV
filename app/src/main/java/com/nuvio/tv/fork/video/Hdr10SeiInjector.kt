package com.nuvio.tv.fork.video

import kotlin.math.roundToLong

/**
 * HDR10 static metadata SEI (MDCV + CLLI) for a base layer whose Dolby Vision RPU was stripped, so
 * an HDR10 sink tone-maps against the master's real metadata (G5d, feature 54). FILE_PORT of ysosrs
 * `Hdr10SeiInjector` @ 45e0984 (its startup self-test became unit tests).
 *
 * Layouts: Rec. ITU-T H.265 D.2.28 (mastering_display_colour_volume, payloadType 137) and D.2.35
 * (content_light_level_info, 144). Mastering luminance comes from the RPU source_*_pq via the PQ
 * EOTF; MaxCLL/MaxFALL from L6 (CLLI omitted when both are 0 = unknown); primaries are BT.2020 +
 * D65, which the RPU does not carry. RPU parsing credit: quietvoid/libdovi (MIT).
 */
object Hdr10SeiInjector {

    private const val NAL_HEADER_BYTE0 = 0x4E // nal_unit_type 39 (prefix SEI), layer 0
    private const val NAL_HEADER_BYTE1 = 0x01 // nuh_temporal_id_plus1 = 1
    const val SEI_TYPE_MDCV = 137
    const val SEI_TYPE_CLLI = 144
    private const val VCL_NAL_MAX_TYPE = 31

    // BT.2020 primaries and D65 white in units of 0.00002, in the spec's G, B, R order.
    internal const val PRIM_G_X = 8500
    internal const val PRIM_G_Y = 39850
    private const val PRIM_B_X = 6550
    private const val PRIM_B_Y = 2300
    private const val PRIM_R_X = 35400
    private const val PRIM_R_Y = 14600
    private const val WHITE_X = 15635
    internal const val WHITE_Y = 16450

    /** MDCV prefix-SEI NAL, 24-byte payload; luminance in units of 0.0001 cd/m^2. */
    fun buildMdcvSeiNal(sourceMinPq: Int, sourceMaxPq: Int): ByteArray {
        val maxLum = (pqCodeToNits(sourceMaxPq) * 10_000.0).roundToLong().coerceIn(0L, 0xFFFFFFFFL)
        val minLum = (pqCodeToNits(sourceMinPq) * 10_000.0).roundToLong().coerceIn(0L, 0xFFFFFFFFL)
        val p = ByteArray(24)
        var i = 0
        i = putU16(p, i, PRIM_G_X); i = putU16(p, i, PRIM_G_Y)
        i = putU16(p, i, PRIM_B_X); i = putU16(p, i, PRIM_B_Y)
        i = putU16(p, i, PRIM_R_X); i = putU16(p, i, PRIM_R_Y)
        i = putU16(p, i, WHITE_X); i = putU16(p, i, WHITE_Y)
        i = putU32(p, i, maxLum); putU32(p, i, minLum)
        return wrapSeiNal(SEI_TYPE_MDCV, p)
    }

    /** CLLI prefix-SEI NAL, or null when both values are 0 (unknown). */
    fun buildClliSeiNal(maxCll: Int, maxFall: Int): ByteArray? {
        if (maxCll <= 0 && maxFall <= 0) return null
        val p = ByteArray(4)
        putU16(p, 0, maxCll.coerceIn(0, 0xFFFF))
        putU16(p, 2, maxFall.coerceIn(0, 0xFFFF))
        return wrapSeiNal(SEI_TYPE_CLLI, p)
    }

    /** MDCV always, CLLI when known; empty without metadata. */
    fun buildSeiNals(meta: RpuStaticMetadata?): List<ByteArray> {
        if (meta == null) return emptyList()
        val out = ArrayList<ByteArray>(2)
        out += buildMdcvSeiNal(meta.sourceMinPq, meta.sourceMaxPq)
        buildClliSeiNal(meta.maxCll ?: 0, meta.maxFall ?: 0)?.let { out += it }
        return out
    }

    // [2-byte header][escaped RBSP]; RBSP = type byte + size byte + payload + trailing bits
    // (type and size are < 255, so each is one byte).
    internal fun wrapSeiNal(payloadType: Int, payload: ByteArray): ByteArray {
        val rbsp = ByteArray(2 + payload.size + 1)
        rbsp[0] = payloadType.toByte()
        rbsp[1] = payload.size.toByte()
        System.arraycopy(payload, 0, rbsp, 2, payload.size)
        rbsp[rbsp.size - 1] = 0x80.toByte()
        val escaped = escapeRbsp(rbsp)
        val nal = ByteArray(2 + escaped.size)
        nal[0] = NAL_HEADER_BYTE0.toByte()
        nal[1] = NAL_HEADER_BYTE1.toByte()
        System.arraycopy(escaped, 0, nal, 2, escaped.size)
        return nal
    }

    // Emulation prevention: after two 0x00 bytes, a byte 0x00..0x03 gets a 0x03 before it.
    private fun escapeRbsp(rbsp: ByteArray): ByteArray {
        val out = ArrayList<Byte>(rbsp.size + 4)
        var zeros = 0
        for (b in rbsp) {
            val v = b.toInt() and 0xFF
            if (zeros >= 2 && v <= 0x03) {
                out.add(0x03.toByte())
                zeros = 0
            }
            out.add(b)
            zeros = if (v == 0x00) zeros + 1 else 0
        }
        return out.toByteArray()
    }

    /** Inserts [seiNals] before the first VCL NAL of a length-delimited sample (appends if none). */
    fun injectLengthDelimited(
        sample: ByteArray,
        sampleLen: Int,
        nalLengthFieldLength: Int,
        seiNals: List<ByteArray>,
    ): ByteArray {
        if (seiNals.isEmpty() || nalLengthFieldLength !in 1..4) return sample.copyOf(sampleLen)
        val insertAt = firstVclOffset(sample, sampleLen, nalLengthFieldLength)
        val extra = seiNals.sumOf { nalLengthFieldLength + it.size }
        val out = ByteArray(sampleLen + extra)
        System.arraycopy(sample, 0, out, 0, insertAt)
        var o = insertAt
        for (nal in seiNals) {
            o = putLengthField(out, o, nal.size, nalLengthFieldLength)
            System.arraycopy(nal, 0, out, o, nal.size)
            o += nal.size
        }
        System.arraycopy(sample, insertAt, out, o, sampleLen - insertAt)
        return out
    }

    private fun firstVclOffset(sample: ByteArray, sampleLen: Int, nlf: Int): Int {
        var pos = 0
        while (pos + nlf <= sampleLen) {
            val nalSize = readLengthField(sample, pos, nlf)
            val nalStart = pos + nlf
            if (nalSize <= 0 || nalStart + nalSize > sampleLen) break
            if (((sample[nalStart].toInt() ushr 1) and 0x3F) <= VCL_NAL_MAX_TYPE) return pos
            pos = nalStart + nalSize
        }
        return sampleLen
    }

    /** True when a length-delimited sample already carries an MDCV or CLLI prefix SEI. */
    fun hasHdr10StaticSei(sample: ByteArray, sampleLen: Int, nalLengthFieldLength: Int): Boolean {
        if (nalLengthFieldLength !in 1..4) return false
        var pos = 0
        while (pos + nalLengthFieldLength <= sampleLen) {
            val nalSize = readLengthField(sample, pos, nalLengthFieldLength)
            val nalStart = pos + nalLengthFieldLength
            if (nalSize <= 0 || nalStart + nalSize > sampleLen) break
            if (nalSize >= 3 && ((sample[nalStart].toInt() ushr 1) and 0x3F) == 39) {
                val payloadType = sample[nalStart + 2].toInt() and 0xFF
                if (payloadType == SEI_TYPE_MDCV || payloadType == SEI_TYPE_CLLI) return true
            }
            pos = nalStart + nalSize
        }
        return false
    }

    private fun putU16(a: ByteArray, off: Int, v: Int): Int {
        a[off] = ((v ushr 8) and 0xFF).toByte()
        a[off + 1] = (v and 0xFF).toByte()
        return off + 2
    }

    private fun putU32(a: ByteArray, off: Int, v: Long): Int {
        a[off] = ((v ushr 24) and 0xFF).toByte()
        a[off + 1] = ((v ushr 16) and 0xFF).toByte()
        a[off + 2] = ((v ushr 8) and 0xFF).toByte()
        a[off + 3] = (v and 0xFF).toByte()
        return off + 4
    }

    private fun readLengthField(a: ByteArray, off: Int, len: Int): Int {
        var v = 0
        for (k in 0 until len) v = (v shl 8) or (a[off + k].toInt() and 0xFF)
        return v
    }

    private fun putLengthField(a: ByteArray, off: Int, v: Int, len: Int): Int {
        for (k in 0 until len) a[off + k] = ((v ushr (8 * (len - 1 - k))) and 0xFF).toByte()
        return off + len
    }
}
