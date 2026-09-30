package com.nuvio.tv.fork.seek

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekrKeyValidatorTest {

    @Test
    fun onlyASinglePrintableTokenLooksLikeAKey() {
        assertTrue(SeekrKeyValidator.looksLikeKey("sk_live_0123456789abcdef"))
        assertTrue(SeekrKeyValidator.looksLikeKey("  sk_live_0123456789abcdef  "))
        assertFalse(SeekrKeyValidator.looksLikeKey("short"))
        assertFalse(SeekrKeyValidator.looksLikeKey("two words here"))
        assertFalse(SeekrKeyValidator.looksLikeKey("line\nbreak-injection-attempt"))
        assertFalse(SeekrKeyValidator.looksLikeKey("x".repeat(300)))
    }

    @Test
    fun onlyAnExplicitValidTrueAccepts() {
        assertTrue(SeekrKeyValidator.isValidResponse("""{"valid": true, "plan": "free"}"""))
        assertFalse(SeekrKeyValidator.isValidResponse("""{"valid": false}"""))
        assertFalse(SeekrKeyValidator.isValidResponse(""))
        assertFalse(SeekrKeyValidator.isValidResponse("<html>valid true</html>"))
    }
}
