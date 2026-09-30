package com.nuvio.tv.fork.seek

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The user's own Seekr API key, per profile and on this device only (G7a, feature 110). FILE_PORT
 * of Cxsmo `data/local/SeekrCredentialsStore.kt` @ 3e0d0fa.
 *
 * The DataStore value is AES-GCM encrypted with an Android Keystore key, so a synced, backed-up or
 * copied settings file carries only ciphertext that no other device can open. There is no built-in
 * key (D052): without the user's key no Seekr track loads and on-device previews still work. The
 * plaintext key only reaches the Seekr client in this process and is never logged.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class SeekrKeyStore @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager
) {
    companion object {
        private const val FEATURE = "seekr_credentials"
        private const val KEY_ALIAS = "com.nuvio.tv.seekr.credentials.v1"
    }

    // Same alias and format as before the G9a extraction, so stored keys still decrypt.
    private val cipher = com.nuvio.tv.fork.foundation.KeystoreCipher(KEY_ALIAS)

    private val apiKeyKey = stringPreferencesKey("api_key")

    val apiKey: Flow<String> =
        profileManager.activeProfileId.flatMapLatest { profileId ->
            factory.get(profileId, FEATURE).data.map { prefs ->
                decryptOrEmpty(prefs[apiKeyKey])
            }
        }

    suspend fun setApiKey(value: String) {
        val normalized = value.trim()
        factory.get(profileManager.activeProfileId.value, FEATURE).edit { prefs ->
            if (normalized.isBlank()) {
                prefs.remove(apiKeyKey)
            } else {
                prefs[apiKeyKey] = encrypt(normalized)
            }
        }
    }

    private fun encrypt(value: String): String = cipher.encrypt(value)

    private fun decryptOrEmpty(value: String?): String = cipher.decryptOrEmpty(value)
}

/** For player-session objects built outside Hilt (see [SeekPreviewState]). */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface SeekrKeyStoreEntryPoint {
    fun seekrKeyStore(): SeekrKeyStore
}

internal fun seekrKeyStore(context: android.content.Context): SeekrKeyStore =
    dagger.hilt.android.EntryPointAccessors
        .fromApplication(context.applicationContext, SeekrKeyStoreEntryPoint::class.java)
        .seekrKeyStore()
