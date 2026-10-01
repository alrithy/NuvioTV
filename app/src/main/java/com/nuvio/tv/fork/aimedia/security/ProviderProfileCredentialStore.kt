package com.nuvio.tv.fork.aimedia.security

// G13b (D063): FILE_PORT of Fornace/nuvio-ai @ 518af71 `app/src/main/…/core/media/provider/security/ProviderProfileCredentialStore.kt`; package renamed.

import com.nuvio.tv.core.profile.ProfileScopedCredentialStore
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

/**
 * Connects AI provider credential cleanup to official's profile lifecycle (G13b, D063). FILE_PORT of
 * Fornace/nuvio-ai @ 518af71 `security/ProviderProfileCredentialStore.kt`, adapted to official's
 * non-suspending [ProfileScopedCredentialStore]: deleting a profile deletes its keys and retires its
 * generation, so a later profile that reuses the numeric id can never decrypt the old credentials.
 * It runs whatever the AI opt-in says (keys saved while it was on must not outlive their profile),
 * and the vault is resolved lazily, so nothing touches the Keystore until a profile is deleted.
 */
@Singleton
class ProviderProfileCredentialStore @Inject constructor(
    private val vault: Lazy<ProviderCredentialVault>,
) : ProfileScopedCredentialStore {
    override fun removeProfile(profileId: Int) {
        runBlocking { vault.get().deleteProfile(profileId) }
    }

    override fun clearAllProfiles() {
        runBlocking { vault.get().deleteAllProfiles() }
    }
}
