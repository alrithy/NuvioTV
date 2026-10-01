package com.nuvio.tv.fork.security

/**
 * G14a (D064; feature 305): what a URL may show in a log line. Add-on, stream, subtitle and plugin
 * URLs carry configuration, API keys and signed tokens in their path, query and user info, so only
 * the scheme, host and port survive; anything that is not a URL is hidden whole. Official's
 * `urlForLog()` delegates here.
 */
object LogRedaction {
    fun url(value: String?): String {
        if (value == null) return "(null)"
        val text = value.trim()
        if (text.isEmpty()) return "(blank)"
        val colon = text.indexOf(':')
        if (colon <= 0) return HIDDEN
        val scheme = text.substring(0, colon)
        if (!scheme[0].isLetter() || !scheme.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }) {
            return HIDDEN
        }
        if (!text.startsWith("://", colon)) return "${scheme.lowercase()}:$HIDDEN"
        val authority = text.substring(colon + 3)
            .takeWhile { it != '/' && it != '?' && it != '#' && it != '\\' }
            .substringAfterLast('@')
        if (authority.isEmpty()) return "${scheme.lowercase()}://$HIDDEN"
        return "${scheme.lowercase()}://${authority.lowercase()}"
    }

    private const val HIDDEN = "(redacted)"
}
