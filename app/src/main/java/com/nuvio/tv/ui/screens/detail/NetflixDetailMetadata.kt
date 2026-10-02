package com.nuvio.tv.ui.screens.detail

private val detailMinuteRuntime = Regex("^([\\p{Nd}]+)\\s*(?:m|min|mins|minutes?|دقيقة|دقائق)?$", RegexOption.IGNORE_CASE)
private val detailHourRuntime = Regex("^([\\p{Nd}]+)\\s*h(?:\\s*([\\p{Nd}]+)\\s*m(?:in)?)?$", RegexOption.IGNORE_CASE)
private val detailClockRuntime = Regex("^([\\p{Nd}]+):([\\p{Nd}]{2})$")

/** Normalize known provider runtime forms without guessing an unknown provider's units. */
internal fun netflixDetailRuntimeMinutes(runtime: String?): Int? {
    val value = runtime?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    fun String.minutesNumber(): Int? = map { it.digitToIntOrNull() ?: return null }
        .joinToString("").toIntOrNull()
    detailMinuteRuntime.matchEntire(value)?.let { match ->
        return match.groupValues[1].minutesNumber()?.takeIf { it > 0 }
    }
    val match = detailHourRuntime.matchEntire(value) ?: detailClockRuntime.matchEntire(value) ?: return null
    val hours = match.groupValues[1].minutesNumber() ?: return null
    val minutes = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.minutesNumber() ?: 0
    if (minutes !in 0..59) return null
    return (hours.toLong() * 60L + minutes).takeIf { it in 1..Int.MAX_VALUE.toLong() }?.toInt()
}
