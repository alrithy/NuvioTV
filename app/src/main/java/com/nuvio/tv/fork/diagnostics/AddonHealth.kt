package com.nuvio.tv.fork.diagnostics

/** Health of one add-on, derived from the outcome of requests the app already makes. */
enum class AddonHealthState {
    UNKNOWN,
    HEALTHY,
    SLOW,
    TIMEOUT,
    AUTH_ERROR,
    MANIFEST_ERROR,
    NO_STREAMS,
    REQUEST_ERROR;

    val isFailure: Boolean
        get() = this == TIMEOUT || this == AUTH_ERROR || this == MANIFEST_ERROR || this == REQUEST_ERROR
}

/** Latest observed health of one add-on. URLs, headers and error text are never kept. */
data class AddonHealth(
    val state: AddonHealthState,
    val consecutiveFailures: Int,
    val lastLatencyMs: Long,
)

enum class AddonRequest { MANIFEST, STREAMS }

/**
 * Maps a request outcome to an [AddonHealthState]. Outcome vocabulary and the slow threshold
 * follow ysosrs AddonHealthModel (45e0984); failures are split into the states Nuvio shows.
 */
object AddonHealthClassifier {
    const val SLOW_LATENCY_MS = 8_000L

    fun success(latencyMs: Long): AddonHealthState =
        if (latencyMs > SLOW_LATENCY_MS) AddonHealthState.SLOW else AddonHealthState.HEALTHY

    fun failure(request: AddonRequest, httpCode: Int?, message: String?): AddonHealthState = when {
        httpCode == 401 || httpCode == 403 -> AddonHealthState.AUTH_ERROR
        message.isTimeout() -> AddonHealthState.TIMEOUT
        request == AddonRequest.MANIFEST -> AddonHealthState.MANIFEST_ERROR
        httpCode == 404 -> AddonHealthState.NO_STREAMS
        else -> AddonHealthState.REQUEST_ERROR
    }

    private fun String?.isTimeout(): Boolean =
        this != null && (contains("timeout", ignoreCase = true) || contains("timed out", ignoreCase = true))
}
