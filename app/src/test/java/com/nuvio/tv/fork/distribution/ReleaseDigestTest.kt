package com.nuvio.tv.fork.distribution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** G14b (306): GitHub's asset digest is parsed strictly and a mismatch or absence never verifies. */
class ReleaseDigestTest {
    // SHA-256("abc")
    private val abc = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    private fun sha(text: String) = ReleaseDigest.newDigest().digest(text.toByteArray())

    @Test
    fun githubDigestsAreParsedStrictly() {
        assertEquals(abc, ReleaseDigest.sha256Hex("sha256:$abc"))
        assertEquals(abc, ReleaseDigest.sha256Hex(" SHA256:${abc.uppercase()} "))
        assertNull(ReleaseDigest.sha256Hex(null))
        assertNull(ReleaseDigest.sha256Hex(abc))
        assertNull(ReleaseDigest.sha256Hex("sha1:a9993e364706816aba3e25717850c26c9cd0d89d"))
        assertNull(ReleaseDigest.sha256Hex("sha256:${abc.dropLast(1)}"))
        assertNull(ReleaseDigest.sha256Hex("sha256:${abc.dropLast(1)}g"))
    }

    @Test
    fun onlyTheExactPublishedDigestVerifies() {
        assertEquals(abc, ReleaseDigest.hex(sha("abc")))
        assertTrue(ReleaseDigest.verified(abc, sha("abc")))
        assertTrue(ReleaseDigest.verified(abc.uppercase(), sha("abc")))
        assertFalse(ReleaseDigest.verified(abc, sha("abd")))
        assertFalse("no published digest is a refusal", ReleaseDigest.verified(null, sha("abc")))
        assertFalse(ReleaseDigest.verified("", sha("abc")))
        assertFalse(ReleaseDigest.verified("sha256:$abc", sha("abc")))
    }
}
