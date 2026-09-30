package com.nuvio.tv.fork.skip

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.fork.foundation.FeatureRegistry
import com.nuvio.tv.fork.foundation.KeystoreCipher
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SkipProviderSettingsTest {
    private class MemoryPreferences : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it }
    }

    @Test
    fun `correcting credentials invalidates cached identity and survives store recreation`() = runTest {
        mockkConstructor(KeystoreCipher::class)
        try {
            every { anyConstructed<KeystoreCipher>().encrypt(any()) } answers { "cipher:" + firstArg<String>() }
            every { anyConstructed<KeystoreCipher>().decryptOrEmpty(any()) } answers {
                firstArg<String?>()?.removePrefix("cipher:").orEmpty()
            }
            val active = MutableStateFlow(1)
            val stores = mutableMapOf<Int, MemoryPreferences>()
            val factory = mockk<ProfileDataStoreFactory> {
                every { get(any(), any()) } answers { stores.getOrPut(firstArg(), ::MemoryPreferences) }
            }
            val profiles = mockk<ProfileManager> { every { activeProfileId } returns active }
            fun newSettings() = SkipProviderSettings(factory, profiles, FeatureRegistry())
            val settings = newSettings()
            val provider = ForkSkipProvider.PUBLIC_META_DB
            settings.setEnabled(provider, true)
            settings.setApiKey(provider, "bad-fixture-key")
            val before = settings.configNow()
            settings.setApiKey(provider, "corrected-fixture-key")
            val corrected = newSettings().configNow()
            assertEquals(before.active, corrected.active)
            assertNotEquals(before.cacheKey(), corrected.cacheKey())
            assertEquals("corrected-fixture-key", corrected.keys[provider])
            assertFalse(corrected.cacheKey().contains("fixture-key"))
            active.value = 2
            settings.setEnabled(provider, true)
            settings.setApiKey(provider, "corrected-fixture-key")
            assertNotEquals(corrected.cacheKey(), settings.configNow().cacheKey())
            active.value = 1
            assertEquals(corrected.cacheKey(), settings.configNow().cacheKey())
            settings.setApiKey(provider, "")
            assertTrue(settings.configNow().active.isEmpty())
        } finally {
            unmockkConstructor(KeystoreCipher::class)
        }
    }
}
