package com.nuvio.tv.core.logging

fun String?.rawForLog(): String =
    this ?: "(null)"

/** G14a (D064): scheme, host and port only; path, query and user info never reach a log. */
fun String?.urlForLog(): String = com.nuvio.tv.fork.security.LogRedaction.url(this)

fun String?.bodySnippetForLog(maxLength: Int = Int.MAX_VALUE): String {
    val value = this ?: return "(null)"
    if (value.isBlank()) return value
    return if (value.length <= maxLength) value else "${value.take(maxLength)}..."
}

fun Throwable.diagnosticSummary(): String {
    val parts = mutableListOf<String>()
    var current: Throwable? = this
    while (current != null && parts.size < 6) {
        val name = current.javaClass.simpleName.ifBlank { current.javaClass.name }
        val message = current.message.bodySnippetForLog()
        parts.add("$name: $message")
        current = current.cause
    }
    return parts.joinToString(" <- ")
}
