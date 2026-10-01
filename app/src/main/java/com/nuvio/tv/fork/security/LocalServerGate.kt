package com.nuvio.tv.fork.security

import fi.iki.elonen.NanoHTTPD

/**
 * G14a (D064): [LocalServerAccess] for a NanoHTTPD server. The first line of an official server's
 * `serve()` is `access.gate(session)?.let { return it }`; null means serve the request as before.
 * Denials carry a fixed text and never echo the request.
 */
fun LocalServerAccess.gate(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response? {
    val headers = session.headers
    val decision = decide(
        method = session.method?.name.orEmpty(),
        keyParam = session.parameters[LocalServerAccess.KEY_PARAM]?.firstOrNull(),
        cookieHeader = headers["cookie"],
        origin = headers["origin"],
        referer = headers["referer"],
        host = headers["host"],
    )
    return when (decision) {
        LocalServerDecision.ALLOW -> null
        LocalServerDecision.ENTER ->
            NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.REDIRECT_SEE_OTHER, NanoHTTPD.MIME_PLAINTEXT, "")
                .apply { entryHeaders(session.uri.orEmpty()).forEach { (name, value) -> addHeader(name, value) } }
        LocalServerDecision.DENY_SESSION -> forbidden(SESSION_TEXT)
        LocalServerDecision.DENY_ORIGIN -> forbidden(ORIGIN_TEXT)
    }
}

private fun forbidden(text: String): NanoHTTPD.Response =
    NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.FORBIDDEN, "text/plain; charset=utf-8", text)
        .apply { addHeader("Cache-Control", "no-store") }

private const val SESSION_TEXT = "Open this page by scanning the QR code shown on the TV."
private const val ORIGIN_TEXT = "Requests from other sites are not accepted."
