package com.memento.app.data

import com.memento.app.data.preferences.ThemeMode
import com.memento.app.data.preferences.ThemePalette
import com.memento.app.data.preferences.parseThemeMode
import com.memento.app.data.preferences.parseThemePalette
import com.memento.app.ui.theme.resolveDarkTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun systemModeFollowsTheCurrentAndroidAppearance() {
        assertTrue(resolveDarkTheme(ThemeMode.SYSTEM, systemDark = true))
        assertFalse(resolveDarkTheme(ThemeMode.SYSTEM, systemDark = false))
    }

    @Test
    fun explicitModesRemainIndependentFromAndroidAppearance() {
        assertTrue(resolveDarkTheme(ThemeMode.DARK, systemDark = false))
        assertFalse(resolveDarkTheme(ThemeMode.LIGHT, systemDark = true))
    }
}
