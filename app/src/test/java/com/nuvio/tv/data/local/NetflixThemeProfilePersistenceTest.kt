package com.nuvio.tv.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.domain.model.AppTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NetflixThemeProfilePersistenceTest {
    private val activeProfileIdFlow = MutableStateFlow(1)
    private val stores = mutableMapOf<Int, MemoryPreferences>()
    private val profileManager = mockk<ProfileManager> {
        every { activeProfileId } returns activeProfileIdFlow
    }
    private val factory = mockk<ProfileDataStoreFactory> {
        every { get(any(), "theme_settings") } answers {
            stores.getOrPut(firstArg()) { MemoryPreferences() }
        }
    }

    @Test
    fun switchingProfilesRestoresNetflixWithoutChangingOtherProfile() = runTest {
        val settings = ThemeDataStore(factory, profileManager)
        settings.setTheme(AppTheme.NETFLIX)
        activeProfileIdFlow.value = 2
        settings.setTheme(AppTheme.OCEAN)
        assertEquals(AppTheme.OCEAN, settings.themeSelection.first().theme)
        activeProfileIdFlow.value = 1
        assertEquals(AppTheme.NETFLIX, settings.themeSelection.first().theme)
        assertEquals(AppTheme.OCEAN, settings.getThemeForProfile(2))
    }

    @Test
    fun netflixSelectionSurvivesSettingsRecreationAndProfileObservation() = runTest {
        activeProfileIdFlow.value = 3
        ThemeDataStore(factory, profileManager).setTheme(AppTheme.NETFLIX)
        val restored = ThemeDataStore(factory, profileManager)
        assertEquals(AppTheme.NETFLIX, restored.getThemeForProfile(3))
        assertEquals(AppTheme.NETFLIX, restored.observeThemeForProfile(3).first())
        assertEquals(null, restored.getThemeForProfile(1))
    }

    private class MemoryPreferences : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it }
    }
}
