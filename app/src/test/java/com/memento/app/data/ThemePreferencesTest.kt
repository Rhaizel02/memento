package com.memento.app.data

import com.memento.app.data.preferences.ThemeMode
import com.memento.app.data.preferences.ThemePalette
import com.memento.app.data.preferences.parseThemeMode
import com.memento.app.data.preferences.parseThemePalette
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferencesTest {
    @Test
    fun missingPreferencesUseCompatibleDefaults() {
        assertEquals(ThemeMode.SYSTEM, parseThemeMode(null))
        assertEquals(ThemePalette.MEMENTO, parseThemePalette(null))
    }

    @Test
    fun invalidPreferencesUseCompatibleDefaults() {
        assertEquals(ThemeMode.SYSTEM, parseThemeMode("AUTOMATIC"))
        assertEquals(ThemePalette.MEMENTO, parseThemePalette("NEON"))
    }

    @Test
    fun legacyThemeValuesRemainValid() {
        ThemeMode.entries.forEach { mode -> assertEquals(mode, parseThemeMode(mode.name)) }
    }
}
