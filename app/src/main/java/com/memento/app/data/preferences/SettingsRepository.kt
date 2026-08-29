package com.memento.app.data.preferences

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.mementoDataStore by preferencesDataStore(name = "memento_preferences")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ThemePalette { MEMENTO, FOREST, NOBLE, INK }

fun parseThemeMode(stored: String?): ThemeMode =
    stored?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM

fun parseThemePalette(stored: String?): ThemePalette =
    stored?.let { runCatching { ThemePalette.valueOf(it) }.getOrNull() } ?: ThemePalette.MEMENTO

@Singleton
class SettingsRepository private constructor(
    private val dataStore: DataStore<Preferences>,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.mementoDataStore)

    // Keep the legacy key so existing SYSTEM/LIGHT/DARK choices remain intact.
    private val themeKey = stringPreferencesKey("theme")
    private val themePaletteKey = stringPreferencesKey("theme_palette")
    private val onboardingKey = booleanPreferencesKey("onboarding_completed")

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        parseThemeMode(preferences[themeKey])
    }
    val themePalette: Flow<ThemePalette> = dataStore.data.map { preferences ->
        parseThemePalette(preferences[themePaletteKey])
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[onboardingKey] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setThemePalette(palette: ThemePalette) {
        dataStore.edit { it[themePaletteKey] = palette.name }
    }

    suspend fun completeOnboarding() {
        dataStore.edit { it[onboardingKey] = true }
    }

    companion object {
        @VisibleForTesting
        fun createForTest(dataStore: DataStore<Preferences>): SettingsRepository = SettingsRepository(dataStore)
    }
}
