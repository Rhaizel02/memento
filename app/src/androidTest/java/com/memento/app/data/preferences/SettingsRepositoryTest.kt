package com.memento.app.data.preferences

import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    @Test
    fun paletteDefaultsPersistsAndFallsBackDefensivelyWithoutBreakingLegacyMode() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val paletteKey = stringPreferencesKey("theme_palette")
        val legacyThemeKey = stringPreferencesKey("theme")
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val preferencesFile = context.preferencesDataStoreFile("theme-test-${System.nanoTime()}")
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { preferencesFile }
        try {
            val repository = SettingsRepository.createForTest(dataStore)

            assertEquals(ThemePalette.MEMENTO, repository.themePalette.first())

            repository.setThemePalette(ThemePalette.FOREST)
            assertEquals(ThemePalette.FOREST, repository.themePalette.first())

            dataStore.edit {
                it[paletteKey] = "UNKNOWN"
                it[legacyThemeKey] = ThemeMode.DARK.name
            }
            assertEquals(ThemePalette.MEMENTO, repository.themePalette.first())
            assertEquals(ThemeMode.DARK, repository.themeMode.first())
        } finally {
            scope.cancel()
            preferencesFile.delete()
        }
    }
}
