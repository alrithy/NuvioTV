package com.nuvio.tv.fork.diagnostics

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddonHealthTest {

    @Test
    fun successIsHealthyUntilSlowThreshold() {
        assertEquals(AddonHealthState.HEALTHY, AddonHealthClassifier.success(0))
        assertEquals(AddonHealthState.HEALTHY, AddonHealthClassifier.success(AddonHealthClassifier.SLOW_LATENCY_MS))
        assertEquals(AddonHealthState.SLOW, AddonHealthClassifier.success(AddonHealthClassifier.SLOW_LATENCY_MS + 1))
    }

    @Test
    fun authErrorsWinForAnyRequest() {
        AddonRequest.entries.forEach { request ->
            assertEquals(AddonHealthState.AUTH_ERROR, AddonHealthClassifier.failure(request, 401, "timeout"))
            assertEquals(AddonHealthState.AUTH_ERROR, AddonHealthClassifier.failure(request, 403, null))
        }
    }

    @Test
    fun timeoutsAreRecognisedFromOkHttpMessages() {
        listOf("timeout", "Read timed out", "connect timed out", "SocketTimeout").forEach { message ->
            assertEquals(message, AddonHealthState.TIMEOUT, AddonHealthClassifier.failure(AddonRequest.STREAMS, null, message))
        }
    }

    @Test
    fun manifestFailuresAreManifestErrors() {
        assertEquals(AddonHealthState.MANIFEST_ERROR, AddonHealthClassifier.failure(AddonRequest.MANIFEST, 500, "Server Error"))
        assertEquals(AddonHealthState.MANIFEST_ERROR, AddonHealthClassifier.failure(AddonRequest.MANIFEST, 404, "Not Found"))
    }

    @Test
    fun streamNotFoundMeansNoStreamsOtherwiseRequestError() {
        assertEquals(AddonHealthState.NO_STREAMS, AddonHealthClassifier.failure(AddonRequest.STREAMS, 404, "Not Found"))
        assertEquals(AddonHealthState.REQUEST_ERROR, AddonHealthClassifier.failure(AddonRequest.STREAMS, 502, "Bad Gateway"))
        assertEquals(AddonHealthState.REQUEST_ERROR, AddonHealthClassifier.failure(AddonRequest.STREAMS, null, null))
    }

    @Test
    fun noStreamsIsNotAFailure() {
        assertFalse(AddonHealthState.NO_STREAMS.isFailure)
        assertFalse(AddonHealthState.SLOW.isFailure)
        assertTrue(AddonHealthState.TIMEOUT.isFailure)
    }

    @Test
    fun trackerCountsConsecutiveFailuresPerAddonAndResetsOnSuccess() {
        val tracker = AddonHealthTracker(enabled = true)
        tracker.record(A, AddonHealthState.TIMEOUT, 10_000)
        tracker.record(A, AddonHealthState.REQUEST_ERROR, 50)
        tracker.record(B, AddonHealthState.HEALTHY, 120)

        assertEquals(AddonHealth(AddonHealthState.REQUEST_ERROR, 2, 50), tracker.health.value[A])
        assertEquals(AddonHealth(AddonHealthState.HEALTHY, 0, 120), tracker.health.value[B])

        tracker.record(A, AddonHealthState.NO_STREAMS, 300)
        assertEquals(AddonHealth(AddonHealthState.NO_STREAMS, 0, 300), tracker.health.value[A])
    }

    @Test
    fun trackerIgnoresUnknownOutcomes() {
        val tracker = AddonHealthTracker(enabled = true)
        tracker.record(A, AddonHealthState.UNKNOWN, 1)
        assertTrue(tracker.health.value.isEmpty())
    }

    @Test
    fun disabledTrackerRecordsNothing() {
        AddonHealthTracker.DISABLED.record(A, AddonHealthState.TIMEOUT, 1)
        assertTrue(AddonHealthTracker.DISABLED.health.value.isEmpty())

        val off = AddonHealthTracker(FeatureRegistry(mapOf(FeatureId.UNIFIED_DIAGNOSTICS to FeatureMode.OFF)))
        off.record(A, AddonHealthState.TIMEOUT, 1)
        assertTrue(off.health.value.isEmpty())
    }

    @Test
    fun defaultRegistryEnablesPassiveTracking() {
        val tracker = AddonHealthTracker(FeatureRegistry())
        tracker.record(A, AddonHealthState.HEALTHY, 1)
        assertEquals(AddonHealthState.HEALTHY, tracker.health.value[A]?.state)
    }

    private companion object {
        const val A = "https://a.example/manifest-base"
        const val B = "https://b.example"
    }
}
