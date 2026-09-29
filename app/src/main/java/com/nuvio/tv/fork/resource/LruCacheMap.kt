package com.nuvio.tv.fork.resource

import java.util.Collections

/**
 * Bounded, thread-safe LRU map: an access-order LinkedHashMap that evicts the least-recently-used
 * entry past [maxSize], wrapped in a synchronized map. FILE_PORT of hackerslash/NuvioTV-Lite
 * `core/util/LruCacheMap.kt` @ 2afdcd05 (IMPORT_LEDGER G2c).
 */
fun <K, V> lruCacheMap(maxSize: Int): MutableMap<K, V> {
    require(maxSize > 0) { "maxSize must be positive" }
    return Collections.synchronizedMap(object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > maxSize
    })
}
