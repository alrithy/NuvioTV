package com.nuvio.tv.fork.aimedia.security

// G13a (D063): FILE_PORT of Fornace/nuvio-ai @ 518af71 `app/src/main/…/core/media/provider/security/KeystoreBridge.kt`; package renamed.

/** AES-256-GCM output produced by [KeystoreBridge.encrypt]. */
class EncryptedPayload(val iv: ByteArray, val ciphertext: ByteArray) {
    fun snapshot(): EncryptedPayload = EncryptedPayload(iv.copyOf(), ciphertext.copyOf())
    override fun toString(): String =
        "EncryptedPayload(ivLength=${iv.size}, ciphertextLength=${ciphertext.size})"
}

/**
 * Injectable seam over the hardware-backed key provider used by
 * [ProviderCredentialVault]. The vault owns AAD computation and policy; the
 * bridge only turns (key, plaintext, AAD) into authenticated ciphertext and
 * back. Implementations MUST make GCM authentication cover the AAD, so that
 * ciphertexts swapped across installations, profile generations, providers or
 * records fail decryption.
 */
interface KeystoreBridge {
    fun encrypt(keyAlias: String, plaintext: ByteArray, aad: ByteArray): EncryptedPayload

    fun decrypt(keyAlias: String, payload: EncryptedPayload, aad: ByteArray): ByteArray

    fun hasKey(keyAlias: String): Boolean

    /** Returns aliases under an owned prefix for cleanup reconciliation. */
    fun aliases(prefix: String): Set<String>

    fun deleteKey(keyAlias: String): Boolean
}
