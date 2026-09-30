package com.nuvio.tv.fork.streams

import java.net.URI

/**
 * Where and when to use AIOStreams' opt-in progressive endpoint (G8a, features 147, 151). Port of
 * the URL rules in Cxsmo `StreamRepositoryImpl` @ 3e0d0fa (`isProgressiveAioStreamsUrl`, the
 * `stream-progressive/<type>/<id>.ndjson` path). Pure, so capability detection is unit-tested.
 */
object ProgressiveAioStreamsRules {
    /** The add-on opts in through its manifest URL query: `client=nuvio-progressive`. */
    fun isProgressive(addonBaseUrl: String): Boolean {
        val query = addonBaseUrl.trimEnd('/').substringAfter('?', "")
        return query.split('&').any { pair ->
            val parts = pair.split('=', limit = 2)
            parts.size == 2 && parts[0].equals("client", ignoreCase = true) &&
                parts[1].equals("nuvio-progressive", ignoreCase = true)
        }
    }

    /**
     * The NDJSON endpoint next to the add-on's ordinary `stream/<type>/<id>.json`, keeping the
     * add-on's own query (its configuration) exactly as official keeps it.
     */
    fun progressiveUrl(addonBaseUrl: String, encodedType: String, encodedVideoId: String): String {
        val clean = addonBaseUrl.trimEnd('/')
        val queryStart = clean.indexOf('?')
        val basePath = if (queryStart >= 0) clean.substring(0, queryStart).trimEnd('/') else clean
        val baseQuery = if (queryStart >= 0) clean.substring(queryStart) else ""
        return "$basePath/stream-progressive/$encodedType/$encodedVideoId.ndjson$baseQuery"
    }

    /** NDJSON keep-alive and comment lines carry no event. */
    fun isEventLine(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.isNotEmpty() && !trimmed.startsWith(":")
    }

    /**
     * Host only, for logs: add-on manifest URLs often carry the user's configuration (debrid
     * keys, tokens) in their path or query, which must never reach a log.
     */
    fun logHost(url: String): String = runCatching { URI(url).host }.getOrNull() ?: "?"
}
