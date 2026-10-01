package com.nuvio.tv.fork.aimedia

// G13b (D063): FILE_PORT of Fornace/nuvio-ai @ 518af71 `app/src/main/…/core/media/provider/host/VendorSelectionStore.kt`; package renamed.

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/** Non-secret per-provider vendor choice (vendor id plus any auxiliary auth fields). */
data class VendorSelection(
    val vendorId: String,
    val auxFields: Map<String, String> = emptyMap(),
)

/** Persists vendor selections outside the encrypted vault; they contain no secrets. */
interface VendorSelectionStore {
    fun load(providerId: String): VendorSelection?
    fun save(providerId: String, selection: VendorSelection)
    fun clear(providerId: String)
}

/**
 * Credential envelope stored in the vault: one JSON document carrying the
 * vendor id, the API key and any auxiliary auth fields, so the provider engine
 * receives everything needed to call its vendor in one sealed payload.
 */
object CredentialEnvelope {
    const val KEY_VENDOR_ID = "vendorId"
    const val KEY_API_KEY = "apiKey"

    fun build(vendorId: String, apiKey: CharArray, auxFields: Map<String, String>): ByteArray {
        require(vendorId.isNotBlank()) { "vendorId must not be blank" }
        require(apiKey.isNotEmpty()) { "apiKey must not be empty" }
        auxFields.keys.forEach { key ->
            require(key.isNotBlank() && key != KEY_VENDOR_ID && key != KEY_API_KEY) {
                "reserved aux field: $key"
            }
        }
        // kotlinx.serialization instead of Fornace's org.json, so the envelope is built the same way
        // in JVM unit tests (where android.jar's org.json is a stub) and on the device.
        val document = buildJsonObject {
            put(KEY_VENDOR_ID, JsonPrimitive(vendorId))
            put(KEY_API_KEY, JsonPrimitive(String(apiKey)))
            auxFields.forEach { (key, value) -> put(key, JsonPrimitive(value)) }
        }
        return document.toString().toByteArray(Charsets.UTF_8)
    }
}
