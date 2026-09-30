package com.nuvio.tv.fork.skip

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SkipProviderDeadlineTest {
    @Test
    fun `provider deadlines run concurrently even when results are awaited in order`() = runTest {
        val slow = async { boundedProvider(true) { delay(30_000); listOf("slow") } }
        val second = async { boundedProvider(true) { delay(30_000); listOf("second") } }
        val fast = async { boundedProvider(true) { delay(10); listOf("fast") } }
        assertEquals(listOf("fast"), slow.await() + second.await() + fast.await())
        assertEquals(6_000L, currentTime)
    }

    @Test
    fun `official-only provider keeps its original timeout contract`() = runTest {
        assertEquals(listOf("official"), boundedProvider(false) { delay(7_000); listOf("official") })
        assertEquals(7_000L, currentTime)
    }
}
