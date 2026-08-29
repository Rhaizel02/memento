package com.memento.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "memento_preferences")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Singleton
class SettingsRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val onboardingKey = booleanPreferencesKey("onboarding_completed")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        preferences[themeKey]?.let { stored -> runCatching { ThemeMode.valueOf(stored) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[onboardingKey] ?: false }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { it[onboardingKey] = true }
    }
}
