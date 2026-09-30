package com.nuvio.tv.fork.seek

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
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
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }

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

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return "${cipher.iv.toBase64()}.${cipher.doFinal(value.toByteArray(Charsets.UTF_8)).toBase64()}"
    }

    private fun decryptOrEmpty(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return runCatching {
            val separator = value.indexOf('.')
            require(separator > 0 && separator < value.lastIndex)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(GCM_TAG_BITS, value.substring(0, separator).fromBase64())
            )
            cipher.doFinal(value.substring(separator + 1).fromBase64())
                .toString(Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
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
