package com.memento.app.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.data.preferences.ThemePalette
import com.memento.app.ui.settings.SettingsUiState
import com.memento.app.ui.theme.MementoTheme

@Preview(name = "Memento · Light", group = "Onboarding 2.0", widthDp = 412, heightDp = 892)
@Composable
private fun OnboardingMementoLightPreview() = OnboardingPreview(ThemeMode.LIGHT, ThemePalette.MEMENTO)

@Preview(name = "Memento · Dark", group = "Onboarding 2.0", widthDp = 412, heightDp = 892)
@Composable
private fun OnboardingMementoDarkPreview() = OnboardingPreview(ThemeMode.DARK, ThemePalette.MEMENTO)

@Preview(name = "Forest · Light", group = "Onboarding 2.0", widthDp = 412, heightDp = 892)
@Composable
private fun OnboardingForestLightPreview() = OnboardingPreview(ThemeMode.LIGHT, ThemePalette.FOREST)

@Preview(name = "Forest · Dark", group = "Onboarding 2.0", widthDp = 412, heightDp = 892)
@Composable
private fun OnboardingForestDarkPreview() = OnboardingPreview(ThemeMode.DARK, ThemePalette.FOREST)

@Preview(name = "Noble", group = "Onboarding 2.0", widthDp = 412, heightDp = 892)
@Composable
private fun OnboardingNoblePreview() = OnboardingPreview(ThemeMode.LIGHT, ThemePalette.NOBLE)

@Preview(name = "Ink", group = "Onboarding 2.0", widthDp = 412, heightDp = 892)
@Composable
private fun OnboardingInkPreview() = OnboardingPreview(ThemeMode.DARK, ThemePalette.INK)

@Preview(name = "Small screen · Import error", group = "Onboarding states", widthDp = 320, heightDp = 568)
@Composable
private fun OnboardingImportErrorPreview() = OnboardingPreview(
    themeMode = ThemeMode.DARK,
    palette = ThemePalette.MEMENTO,
    state = SettingsUiState(error = "El archivo no parece un backup válido de Memento."),
)

@Composable
private fun OnboardingPreview(
    themeMode: ThemeMode,
    palette: ThemePalette,
    state: SettingsUiState = SettingsUiState(),
) {
    MementoTheme(themeMode = themeMode, palette = palette) {
        OnboardingContent(state = state, onStart = {}, onRestoreRequested = {})
    }
}
