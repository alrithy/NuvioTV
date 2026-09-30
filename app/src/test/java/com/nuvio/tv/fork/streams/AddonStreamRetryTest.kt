package com.nuvio.tv.fork.streams

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddonStreamRetryTest {

    @Test
    fun serverErrorsAndTimeoutsRetryOnce() {
        assertTrue(AddonStreamRetry.shouldRetry(503, "Service Unavailable", elapsedMs = 400, attempt = 0))
        assertTrue(AddonStreamRetry.shouldRetry(408, null, elapsedMs = 400, attempt = 0))
        assertTrue(AddonStreamRetry.shouldRetry(null, "timeout", elapsedMs = 400, attempt = 0))
        assertTrue(AddonStreamRetry.shouldRetry(null, "Read timed out", elapsedMs = 400, attempt = 0))
        assertFalse("never a second retry", AddonStreamRetry.shouldRetry(503, null, elapsedMs = 400, attempt = 1))
    }

    @Test
    fun clientErrorsAndRateLimitsNeverRetry() {
        listOf(400, 401, 403, 404, 429).forEach { code ->
            assertFalse("$code", AddonStreamRetry.shouldRetry(code, "error", elapsedMs = 100, attempt = 0))
        }
        assertFalse(AddonStreamRetry.shouldRetry(null, "Unable to resolve host", elapsedMs = 100, attempt = 0))
    }

    @Test
    fun slowFirstAttemptsAreNotRetried() {
        assertFalse(AddonStreamRetry.shouldRetry(503, null, elapsedMs = AddonStreamRetry.RETRY_WINDOW_MS + 1, attempt = 0))
        assertTrue(AddonStreamRetry.shouldRetry(503, null, elapsedMs = AddonStreamRetry.RETRY_WINDOW_MS, attempt = 0))
    }
}
