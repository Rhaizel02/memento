package com.memento.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.memento.app.data.preferences.ThemePalette

@Immutable
data class PalettePreviewColors(val primary: Color, val secondary: Color, val surface: Color)

fun ThemePalette.previewColors(dark: Boolean): PalettePreviewColors {
    val scheme = colorSchemeFor(this, dark)
    return PalettePreviewColors(scheme.primary, scheme.onSurfaceVariant, scheme.surface)
}

// Persisted palette names remain valid, but all now express the same archive.
@Suppress("UNUSED_PARAMETER")
fun colorSchemeFor(palette: ThemePalette, dark: Boolean): ColorScheme =
    if (dark) WarmDarkColors else WarmLightColors

val WarmLightColors = lightColorScheme(
    primary = Color(0xFF9F3C16), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF7E7DD), onPrimaryContainer = Color(0xFF6A260C),
    secondary = Color(0xFF6E6259), onSecondary = Color(0xFFFFFDFB),
    secondaryContainer = Color(0xFFF7EFE6), onSecondaryContainer = Color(0xFF241E1A),
    tertiary = Color(0xFF6E6259), onTertiary = Color(0xFFFFFDFB),
    tertiaryContainer = Color(0xFFF2E8DC), onTertiaryContainer = Color(0xFF241E1A),
    background = Color(0xFFFFF8F3), onBackground = Color(0xFF241E1A),
    surface = Color(0xFFFFFDFB), onSurface = Color(0xFF241E1A),
    surfaceVariant = Color(0xFFF7EFE6), onSurfaceVariant = Color(0xFF6E6259),
    surfaceDim = Color(0xFFF2E8DC), surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFFDFB),
    surfaceContainer = Color(0xFFFFF8F3), surfaceContainerHigh = Color(0xFFF7EFE6),
    surfaceContainerHighest = Color(0xFFF2E8DC),
    outline = Color(0xFFD6C8B8), outlineVariant = Color(0xFFE8DED2),
    inverseSurface = Color(0xFF201B18), inverseOnSurface = Color(0xFFEDE4DC),
    inversePrimary = Color(0xFFE07752),
    error = Color(0xFFA33A31), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7E7DD), onErrorContainer = Color(0xFFA33A31),
    scrim = Color(0xFF181412),
)

val WarmDarkColors = darkColorScheme(
    primary = Color(0xFFE07752), onPrimary = Color(0xFF181412),
    primaryContainer = Color(0xFF471F14), onPrimaryContainer = Color(0xFFF7C9B7),
    secondary = Color(0xFFB5A69B), onSecondary = Color(0xFF181412),
    secondaryContainer = Color(0xFF2E2722), onSecondaryContainer = Color(0xFFEDE4DC),
    tertiary = Color(0xFFB5A69B), onTertiary = Color(0xFF181412),
    tertiaryContainer = Color(0xFF332C27), onTertiaryContainer = Color(0xFFEDE4DC),
    background = Color(0xFF181412), onBackground = Color(0xFFEDE4DC),
    surface = Color(0xFF201B18), onSurface = Color(0xFFEDE4DC),
    surfaceVariant = Color(0xFF2E2722), onSurfaceVariant = Color(0xFFB5A69B),
    surfaceDim = Color(0xFF181412), surfaceBright = Color(0xFF332C27),
    surfaceContainerLowest = Color(0xFF181412), surfaceContainerLow = Color(0xFF201B18),
    surfaceContainer = Color(0xFF28221E), surfaceContainerHigh = Color(0xFF2E2722),
    surfaceContainerHighest = Color(0xFF332C27),
    outline = Color(0xFF4D4139), outlineVariant = Color(0xFF382F29),
    inverseSurface = Color(0xFFFFF8F3), inverseOnSurface = Color(0xFF241E1A),
    inversePrimary = Color(0xFF9F3C16),
    error = Color(0xFFF49A89), onError = Color(0xFF181412),
    errorContainer = Color(0xFF471F14), onErrorContainer = Color(0xFFF49A89),
    scrim = Color(0xFF181412),
)
