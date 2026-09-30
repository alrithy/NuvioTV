package com.nuvio.tv.fork.skip

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry
import com.nuvio.tv.fork.foundation.KeystoreCipher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** What the fork skip providers may do for the active profile (G9a). */
data class SkipProviderConfig(
    val enabled: Set<ForkSkipProvider> = emptySet(),
    /** Decrypted in memory only; never logged or synced. */
    val keys: Map<ForkSkipProvider, String> = emptyMap(),
    /** G9b: optional categories (preview, content warnings) the user switched on; none by default. */
    val categories: Set<String> = emptySet(),
    val profileId: Int = 0,
    /** Non-secret revision, changed atomically with the encrypted credentials. */
    val credentialRevision: String = "",
) {
    /** A provider runs only when switched on and, where required, given a key. */
    val active: Set<ForkSkipProvider>
        get() = enabled.filterTo(LinkedHashSet()) { !it.requiresKey || !keys[it].isNullOrBlank() }

    /** Cache key part: results differ with the active providers and enabled categories. */
    fun cacheKey(): String =
        "$profileId:$credentialRevision|" + active.sortedBy { it.key }.joinToString(",") { it.key } +
            "|" + categories.sorted().joinToString(",")

    companion object {
        val NONE = SkipProviderConfig()
    }
}

/**
 * Per-profile skip provider switches (all off by default, D054) and API keys, in `fork_skip_providers`.
 * Keys are AES-GCM encrypted with a Keystore key (Cxsmo `SkipProviderCredentialsStore` @ 3e0d0fa via
 * [KeystoreCipher]). With DISCOVERY_SKIP_RECOMMENDATIONS OFF nothing is active.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class SkipProviderSettings @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager,
    registry: FeatureRegistry,
) {
    val featureEnabled: Boolean = registry.mode(FeatureId.DISCOVERY_SKIP_RECOMMENDATIONS) != FeatureMode.OFF

    private val cipher = KeystoreCipher(KEY_ALIAS)
    private val enabledKeys = ForkSkipProvider.entries.associateWith { booleanPreferencesKey("enabled_${it.key}") }
    private val apiKeys = ForkSkipProvider.entries.associateWith { stringPreferencesKey("api_key_${it.key}") }
    private val categoryKeys = SkipCategories.OPTIONAL.associateWith { booleanPreferencesKey("category_$it") }
    private val credentialRevisionKey = stringPreferencesKey("credential_revision")

    private fun store(profileId: Int = profileManager.activeProfileId.value) = factory.get(profileId, FEATURE)

    val config: Flow<SkipProviderConfig> = profileManager.activeProfileId.flatMapLatest { profileId ->
        store(profileId).data.map { prefs -> if (featureEnabled) prefs.toConfig(profileId) else SkipProviderConfig.NONE }
    }

    /** Snapshot for one lookup; nothing active on any read failure (official providers only). */
    suspend fun configNow(): SkipProviderConfig = runCatching { config.first() }.getOrDefault(SkipProviderConfig.NONE)

    suspend fun setEnabled(provider: ForkSkipProvider, value: Boolean) {
        store().edit { it[enabledKeys.getValue(provider)] = value }
    }

    suspend fun setCategoryEnabled(category: String, value: Boolean) {
        val key = categoryKeys[category] ?: return
        store().edit { it[key] = value }
    }

    suspend fun setApiKey(provider: ForkSkipProvider, value: String) {
        val normalized = value.trim()
        store().edit { prefs ->
            val key = apiKeys.getValue(provider)
            if (normalized.isEmpty()) prefs.remove(key) else prefs[key] = cipher.encrypt(normalized)
            prefs[credentialRevisionKey] = java.util.UUID.randomUUID().toString()
        }
    }

    private fun Preferences.toConfig(profileId: Int) = SkipProviderConfig(
        profileId = profileId,
        credentialRevision = this[credentialRevisionKey].orEmpty(),
        enabled = ForkSkipProvider.entries.filterTo(LinkedHashSet()) { this[enabledKeys.getValue(it)] == true },
        keys = ForkSkipProvider.entries.mapNotNull { provider ->
            cipher.decryptOrEmpty(this[apiKeys.getValue(provider)]).takeIf { it.isNotEmpty() }?.let { provider to it }
        }.toMap(),
        categories = SkipCategories.OPTIONAL.filterTo(LinkedHashSet()) { this[categoryKeys.getValue(it)] == true },
    )

    private companion object {
        const val FEATURE = "fork_skip_providers"
        const val KEY_ALIAS = "com.nuvio.tv.skip.provider.credentials.v1"
    }
}
