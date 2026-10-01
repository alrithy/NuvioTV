package com.nuvio.tv.fork.distribution

import java.security.MessageDigest

/**
 * G14b (D064; feature 306): an update APK is installed only when its SHA-256 equals the digest
 * GitHub publishes for the release asset (`"digest": "sha256:<hex>"`). A missing or malformed
 * digest is a refusal, never a pass.
 */
object ReleaseDigest {
    private const val PREFIX = "sha256:"
    private const val HEX_LENGTH = 64

    /** The asset's SHA-256 as 64 lowercase hex digits, or null when GitHub gave none usable. */
    fun sha256Hex(githubDigest: String?): String? {
        val value = githubDigest?.trim() ?: return null
        if (!value.startsWith(PREFIX, ignoreCase = true)) return null
        return value.substring(PREFIX.length).lowercase().takeIf(::isSha256Hex)
    }

    fun newDigest(): MessageDigest = MessageDigest.getInstance("SHA-256")

    /** Whether [actual] (raw SHA-256 bytes) is the published [expectedHex]; constant time. */
    fun verified(expectedHex: String?, actual: ByteArray): Boolean {
        val expected = expectedHex?.trim()?.lowercase()?.takeIf(::isSha256Hex) ?: return false
        return MessageDigest.isEqual(expected.toByteArray(Charsets.US_ASCII), hex(actual).toByteArray(Charsets.US_ASCII))
    }

    private fun isSha256Hex(value: String): Boolean =
        value.length == HEX_LENGTH && value.all { it in '0'..'9' || it in 'a'..'f' }

    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    /** Thrown after the unverified file has been deleted; the message is shown on the update banner. */
    class UnverifiedUpdate : IllegalStateException(
        "The downloaded update does not match the checksum published with the release, so it was deleted.",
    )
}
