package com.nuvio.tv.fork.seek

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Checks a Seekr key with the service before it is saved (G7a, feature 110). Port of Reshaped
 * `SeekrKeyPreferences.validate` @ 0ccf049. The key only goes to `api.seekr.tv`, in its
 * `X-API-Key` header, and is never logged.
 */
internal object SeekrKeyValidator {
    private const val VALIDATE_URL = "https://api.seekr.tv/v1/keys/validate"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(false) // the key header must not follow a redirect elsewhere
            .build()
    }

    /** True when Seekr accepts [key]; false when it rejects it or cannot be reached. */
    suspend fun validate(key: String): Boolean = withContext(Dispatchers.IO) {
        if (!looksLikeKey(key)) return@withContext false
        runCatching {
            val request = Request.Builder()
                .url(VALIDATE_URL)
                .header("X-API-Key", key.trim())
                .build()
            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful && isValidResponse(response.body?.string().orEmpty())
            }
        }.getOrDefault(false)
    }

    /** A key is one printable token; anything else is refused before it leaves the device. */
    fun looksLikeKey(key: String): Boolean {
        val trimmed = key.trim()
        return trimmed.length in 8..256 && trimmed.all { it in '!'..'~' }
    }

    fun isValidResponse(body: String): Boolean = body.replace(" ", "").contains("\"valid\":true")
}
