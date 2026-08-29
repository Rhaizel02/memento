package com.memento.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.memento.app.data.preferences.ThemePalette
import com.memento.app.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorPalettesTest {
    @Test
    fun allPaletteAndLightingCombinationsMeetTextContrastTargets() {
        ThemePalette.entries.forEach { palette ->
            listOf(false, true).forEach { dark ->
                val scheme = colorSchemeFor(palette, dark)
                requiredPairs(scheme).forEach { (foreground, background) ->
                    assertTrue(
                        "$palette dark=$dark contrast=${contrast(foreground, background)}",
                        contrast(foreground, background) >= MIN_TEXT_CONTRAST,
                    )
                }
                assertTrue(
                    "$palette dark=$dark outline contrast=${contrast(scheme.outline, scheme.surface)}",
                    contrast(scheme.outline, scheme.surface) >= MIN_NON_TEXT_CONTRAST,
                )
            }
        }
    }

    @Test
    fun paletteIdentitiesHaveDistinctPrimaryColors() {
        listOf(false, true).forEach { dark ->
            val colors = ThemePalette.entries.map { colorSchemeFor(it, dark).primary }
            assertEquals(ThemePalette.entries.size, colors.distinct().size)
        }
    }

    @Test
    fun mediaTypeColorsAreStableAcrossPalettesAndAdaptToLighting() {
        MediaType.entries.forEach { type ->
            val light = semanticColorsFor(dark = false)
            val dark = semanticColorsFor(dark = true)
            assertNotEquals(light.color(type), dark.color(type))
            assertTrue(contrast(light.color(type), light.onColor(type)) >= MIN_TEXT_CONTRAST)
            assertTrue(contrast(dark.color(type), dark.onColor(type)) >= MIN_TEXT_CONTRAST)
        }
    }

    private fun requiredPairs(scheme: ColorScheme) = listOf(
        scheme.onPrimary to scheme.primary,
        scheme.onPrimaryContainer to scheme.primaryContainer,
        scheme.onSecondary to scheme.secondary,
        scheme.onSecondaryContainer to scheme.secondaryContainer,
        scheme.onTertiary to scheme.tertiary,
        scheme.onTertiaryContainer to scheme.tertiaryContainer,
        scheme.onBackground to scheme.background,
        scheme.onSurface to scheme.surface,
        scheme.onSurfaceVariant to scheme.surfaceVariant,
        scheme.onError to scheme.error,
        scheme.onErrorContainer to scheme.errorContainer,
    )

    private fun contrast(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5f
        const val MIN_NON_TEXT_CONTRAST = 3f
    }
}
