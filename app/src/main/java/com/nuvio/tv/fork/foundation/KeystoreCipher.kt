package com.nuvio.tv.fork.foundation

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-GCM with a non-exportable Android Keystore key, for provider keys the user types in (Seekr in
 * G7a, skip providers in G9a). FILE_PORT of the cipher in Cxsmo `SeekrCredentialsStore` /
 * `SkipProviderCredentialsStore` @ 3e0d0fa: values are stored as `base64(iv).base64(ciphertext)`, so
 * a synced, backed-up or copied settings file carries only ciphertext no other device can open.
 * Plaintext never leaves the process and is never logged.
 */
class KeystoreCipher(private val keyAlias: String) {

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return "${cipher.iv.toBase64()}.${cipher.doFinal(value.toByteArray(Charsets.UTF_8)).toBase64()}"
    }

    /** Empty for a missing, foreign or corrupted value (it can never be read back, only replaced). */
    fun decryptOrEmpty(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return runCatching {
            val separator = value.indexOf('.')
            require(separator > 0 && separator < value.lastIndex)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(GCM_TAG_BITS, value.substring(0, separator).fromBase64()),
            )
            cipher.doFinal(value.substring(separator + 1).fromBase64()).toString(Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}
