package com.nuvio.tv.domain.model

import androidx.compose.runtime.Immutable

/** Options that only apply to the Cinema home layout. */
@Immutable
data class CinemaHomeSettings(
    /** Slow Ken Burns drift on the full-screen artwork. */
    val backdropMotionEnabled: Boolean = true,
    /** Seconds without input before ambient mode; 0 turns ambient mode off. */
    val ambientTimeoutSeconds: Int = DEFAULT_AMBIENT_TIMEOUT_SECONDS,
    val clockEnabled: Boolean = true,
    /** Play the focused title's trailer behind the spotlight after a short pause. */
    val trailerAutoplayEnabled: Boolean = true
) {
    companion object {
        const val DEFAULT_AMBIENT_TIMEOUT_SECONDS = 30
        val AMBIENT_TIMEOUT_OPTIONS = listOf(0, 15, 30, 60, 120)
    }
}
