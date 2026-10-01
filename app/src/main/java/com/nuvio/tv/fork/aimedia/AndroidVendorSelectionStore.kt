package com.nuvio.tv.fork.aimedia

// G13b (D063): FILE_PORT of Fornace/nuvio-ai @ 518af71 `app/src/main/…/core/media/provider/host/VendorSelectionStore.kt`; package renamed.

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Device-local JSON file under no restrictive permissions, holding only
 * non-secret vendor choices. One JSON object keyed by provider id.
 */
class AndroidVendorSelectionStore(context: Context) : VendorSelectionStore {

    private val file: File = File(context.filesDir, FILE_NAME)

    @Synchronized
    override fun load(providerId: String): VendorSelection? {
        if (!file.exists()) return null
        return runCatching {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val entry = root.optJSONObject(providerId) ?: return null
            val vendorId = entry.optString(KEY_VENDOR_ID)
            if (vendorId.isBlank()) return null
            val aux = mutableMapOf<String, String>()
            val auxRoot = entry.optJSONObject(KEY_AUX)
            if (auxRoot != null) {
                val keys = auxRoot.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    aux[key] = auxRoot.optString(key)
                }
            }
            VendorSelection(vendorId = vendorId, auxFields = aux)
        }.getOrNull()
    }

    @Synchronized
    override fun save(providerId: String, selection: VendorSelection) {
        val entry = JSONObject()
            .put(KEY_VENDOR_ID, selection.vendorId)
        if (selection.auxFields.isNotEmpty()) {
            val aux = JSONObject()
            selection.auxFields.forEach { (key, value) -> aux.put(key, value) }
            entry.put(KEY_AUX, aux)
        }
        mutateRoot { root -> root.put(providerId, entry) }
    }

    @Synchronized
    override fun clear(providerId: String) {
        mutateRoot { root -> root.remove(providerId) }
    }

    private fun mutateRoot(block: (JSONObject) -> Unit) {
        val root = runCatching {
            if (file.exists()) JSONObject(file.readText(Charsets.UTF_8)) else JSONObject()
        }.getOrDefault(JSONObject())
        block(root)
        file.parentFile?.mkdirs()
        file.writeText(root.toString(), Charsets.UTF_8)
    }

    companion object {
        private const val FILE_NAME = "provider_vendor_selections.json"
        private const val KEY_VENDOR_ID = "vendorId"
        private const val KEY_AUX = "aux"
    }
}
