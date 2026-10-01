package com.nuvio.tv.fork.aimedia.security

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G13b (D063): official's profile deletion reaches the AI vault, so a later profile that reuses the
 * numeric id cannot read the old keys, and nothing touches the vault until a profile is deleted.
 */
class ProviderProfileCredentialStoreTest {
    private val keystore = FakeKeystoreBridge()
    private val cipherStore = RecordingCipherTextStore()
    private val generations = ProfileGenerationStore(InMemoryProfileGenerationStorage())
    private val vault = ProviderCredentialVault(
        keystoreBridge = keystore,
        cipherTextStore = cipherStore,
        installIdentity = InstallIdentity(InMemoryInstallationIdStorage()),
        profileGenerationStore = generations,
    )
    private var resolved = 0
    private val store = ProviderProfileCredentialStore { resolved++; vault }
    private val meta = CredentialRecordMeta(profileId = 4, providerId = "openai", recordId = "primary")

    @Test
    fun theVaultIsOnlyResolvedWhenAProfileIsDeleted() {
        assertEquals(0, resolved)
        store.removeProfile(9)
        assertEquals(1, resolved)
    }

    @Test
    fun deletingAProfileRemovesItsKeysAndRetiresItsGeneration() = runBlocking {
        vault.store(meta, ProviderSecret.copyOf("sk-old".toByteArray(Charsets.UTF_8)))
        val before = generations.generationOf(4)
        assertTrue(vault.contains(meta))

        store.removeProfile(4)

        assertFalse(vault.contains(meta))
        assertTrue(cipherStore.records.isEmpty())
        assertNotEquals("a reused profile id gets a new generation", before, generations.generationOf(4))
    }

    @Test
    fun clearingAllProfilesRemovesEveryKey() = runBlocking {
        vault.store(meta, ProviderSecret.copyOf("sk-a".toByteArray(Charsets.UTF_8)))
        vault.store(meta.copy(profileId = 5), ProviderSecret.copyOf("sk-b".toByteArray(Charsets.UTF_8)))

        store.clearAllProfiles()

        assertTrue(cipherStore.records.isEmpty())
        assertTrue(keystore.aliases("").isEmpty())
    }
}
