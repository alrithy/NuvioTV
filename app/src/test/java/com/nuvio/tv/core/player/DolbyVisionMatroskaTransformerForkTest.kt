package com.nuvio.tv.core.player

import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * G5d (feature 52): without the native bridge (JVM tests) every RPU conversion fails, which is
 * exactly the failure path under test: official forwards the raw RPU, the fork fix drops it.
 */
@UnstableApi
class DolbyVisionMatroskaTransformerForkTest {

    // dvcC: version 1.0, profile 7 (0x0E = 7 << 1), level 6.
    private val dv7Config = byteArrayOf(1, 0, 0x0E, 0x30)

    private val baseNal = byteArrayOf(0x02, 0x01, 0x11, 0x22, 0x33) // TRAIL_R slice
    private val rpuNal = byteArrayOf(0x7C, 0x01, 0x19, 0x08, 0x09) // unspec62 (RPU)

    private fun lengthDelimited(vararg nals: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        for (nal in nals) {
            out.write(byteArrayOf(0, 0, (nal.size shr 8).toByte(), nal.size.toByte()))
            out.write(nal)
        }
        return out.toByteArray()
    }

    private fun transform(forkDvFixes: Boolean, sample: ByteArray): ByteArray {
        val transformer = DolbyVisionMatroskaTransformer(
            config = DolbyVisionConversionConfig(active = true, forkDvFixes = forkDvFixes)
        )
        val out = transformer.transformHevcSample(sample, sample.size, 4, null, dv7Config)!!
        return out.copyOf(transformer.lastTransformedSampleLength())
    }

    @Test
    fun officialForwardsAFailedRpu() {
        val sample = lengthDelimited(baseNal, rpuNal)
        assertArrayEquals(sample, transform(forkDvFixes = false, sample))
    }

    @Test
    fun forkFixDropsAFailedRpuAndKeepsTheBaseLayer() {
        val sample = lengthDelimited(baseNal, rpuNal)
        assertArrayEquals(lengthDelimited(baseNal), transform(forkDvFixes = true, sample))
    }

    @Test
    fun failedBlockAdditionalRpuIsDroppedOnlyWithForkFix() {
        val official = DolbyVisionMatroskaTransformer(DolbyVisionConversionConfig(active = true))
        assertEquals(null, official.onDolbyVisionBlockAdditionalData(rpuNal, 1, dv7Config))
        val fork = DolbyVisionMatroskaTransformer(DolbyVisionConversionConfig(active = true, forkDvFixes = true))
        val stored = fork.onDolbyVisionBlockAdditionalData(rpuNal, 1, dv7Config)!!
        assertEquals(0, stored.size)
        val sample = lengthDelimited(baseNal)
        val out = fork.transformHevcSample(sample, sample.size, 4, stored, dv7Config)!!
        assertArrayEquals(sample, out.copyOf(fork.lastTransformedSampleLength()))
    }
}
