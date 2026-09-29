package com.nuvio.tv.fork.audio

import androidx.media3.common.MimeTypes
import com.nuvio.tv.fork.audio.AudioPassthroughPolicy.Group
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPassthroughPolicyTest {

    private val bitstream = listOf(
        MimeTypes.AUDIO_AC3, MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC, MimeTypes.AUDIO_TRUEHD,
        MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_HD, MimeTypes.AUDIO_DTS_EXPRESS, MimeTypes.AUDIO_DTS_X,
        MimeTypes.AUDIO_AC4,
    )

    private val allOff = AudioPassthroughPolicy(
        allowAc3 = false, allowEac3 = false, allowTrueHd = false, allowDts = false, allowDtsHd = false,
    )

    @Test
    fun defaultDeniesNothing() {
        assertEquals(AudioPassthroughPolicy.ALLOW_ALL, AudioPassthroughPolicy())
        assertTrue(AudioPassthroughPolicy.ALLOW_ALL.allowsEverything())
        (bitstream + listOf(MimeTypes.AUDIO_RAW, null, "")).forEach {
            assertFalse(it.toString(), AudioPassthroughPolicy.ALLOW_ALL.deniesPassthrough(it))
        }
    }

    @Test
    fun eachSwitchDeniesOnlyItsGroup() {
        val cases = mapOf(
            AudioPassthroughPolicy(allowAc3 = false) to setOf(MimeTypes.AUDIO_AC3),
            AudioPassthroughPolicy(allowEac3 = false) to setOf(MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC),
            AudioPassthroughPolicy(allowTrueHd = false) to setOf(MimeTypes.AUDIO_TRUEHD),
            AudioPassthroughPolicy(allowDts = false) to setOf(MimeTypes.AUDIO_DTS),
            AudioPassthroughPolicy(allowDtsHd = false) to setOf(MimeTypes.AUDIO_DTS_HD),
        )
        cases.forEach { (policy, denied) ->
            bitstream.forEach { mime -> assertEquals("$policy $mime", mime in denied, policy.deniesPassthrough(mime)) }
            assertEquals(1, policy.deniedGroups().size)
        }
    }

    @Test
    fun formatsWithoutAnFfmpegDecoderAreNeverDenied() {
        assertFalse(allOff.deniesPassthrough(MimeTypes.AUDIO_DTS_EXPRESS))
        assertFalse(allOff.deniesPassthrough(MimeTypes.AUDIO_DTS_X))
        assertFalse(allOff.deniesPassthrough(MimeTypes.AUDIO_AC4))
        assertNull(AudioPassthroughPolicy.groupOf(MimeTypes.AUDIO_DTS_EXPRESS))
        assertEquals(Group.entries.toList(), allOff.deniedGroups())
    }

    @Test
    fun withoutSoftwareDecodersNothingIsDenied() {
        val policy = allOff.copy(softwareDecodersAvailable = false)
        bitstream.forEach { assertFalse(it, policy.deniesPassthrough(it)) }
    }
}
