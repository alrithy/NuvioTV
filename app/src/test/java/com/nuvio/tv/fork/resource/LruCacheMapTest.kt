package com.nuvio.tv.fork.resource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ported from hackerslash/NuvioTV-Lite `LruCacheMapTest` @ 2afdcd05, plus null-value handling. */
class LruCacheMapTest {

    @Test
    fun evictsPastMaxSize() {
        val cache = lruCacheMap<Int, String>(2)
        cache[1] = "a"
        cache[2] = "b"
        cache[3] = "c"

        assertEquals(2, cache.size)
        assertFalse(cache.containsKey(1))
        assertTrue(cache.containsKey(3))
    }

    @Test
    fun aReadMakesAnEntryTheMostRecentlyUsed() {
        val cache = lruCacheMap<Int, String>(2)
        cache[1] = "a"
        cache[2] = "b"
        cache[1]
        cache[3] = "c"

        assertTrue(cache.containsKey(1))
        assertFalse(cache.containsKey(2))
    }

    @Test
    fun nullableValuesAreStoredLikeTheOfficialCachesExpect() {
        // SimklIdResolver caches a nullable ResolvedIds; get-then-let must still miss on null.
        val cache = lruCacheMap<String, String?>(2)
        cache["k"] = null
        assertTrue(cache.containsKey("k"))
        assertNull(cache["k"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsANonPositiveSize() {
        lruCacheMap<Int, Int>(0)
    }
}
