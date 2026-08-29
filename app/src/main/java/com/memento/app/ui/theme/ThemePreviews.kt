package com.memento.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.data.preferences.ThemePalette
import com.memento.app.domain.model.MediaType

@Preview(name = "Memento Light", group = "Theme palettes", showBackground = true)
@Composable
private fun MementoLightPreview() = ThemePalettePreview(ThemeMode.LIGHT, ThemePalette.MEMENTO)

@Preview(name = "Memento Dark", group = "Theme palettes", showBackground = true)
@Composable
private fun MementoDarkPreview() = ThemePalettePreview(ThemeMode.DARK, ThemePalette.MEMENTO)

@Preview(name = "Forest Light", group = "Theme palettes", showBackground = true)
@Composable
private fun ForestLightPreview() = ThemePalettePreview(ThemeMode.LIGHT, ThemePalette.FOREST)

@Preview(name = "Forest Dark", group = "Theme palettes", showBackground = true)
@Composable
private fun ForestDarkPreview() = ThemePalettePreview(ThemeMode.DARK, ThemePalette.FOREST)

@Preview(name = "Noble Light", group = "Theme palettes", showBackground = true)
@Composable
private fun NobleLightPreview() = ThemePalettePreview(ThemeMode.LIGHT, ThemePalette.NOBLE)

@Preview(name = "Noble Dark", group = "Theme palettes", showBackground = true)
@Composable
private fun NobleDarkPreview() = ThemePalettePreview(ThemeMode.DARK, ThemePalette.NOBLE)

@Preview(name = "Ink Light", group = "Theme palettes", showBackground = true)
@Composable
private fun InkLightPreview() = ThemePalettePreview(ThemeMode.LIGHT, ThemePalette.INK)

@Preview(name = "Ink Dark", group = "Theme palettes", showBackground = true)
@Composable
private fun InkDarkPreview() = ThemePalettePreview(ThemeMode.DARK, ThemePalette.INK)

@Composable
private fun ThemePalettePreview(themeMode: ThemeMode, palette: ThemePalette) {
    MementoTheme(themeMode = themeMode, palette = palette) {
        ThemePreviewContent()
    }
}

@Composable
private fun ThemePreviewContent() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MementoSpacing.large),
            verticalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
        ) {
            Text("Memento", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Biblioteca, diario y memoria cultural",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(MementoSpacing.normal),
                    verticalArrangement = Arrangement.spacedBy(MementoSpacing.small),
                ) {
                    Text("Una obra para recordar", style = MaterialTheme.typography.titleLarge)
                    FilterChip(selected = true, onClick = {}, label = { Text("En progreso") })
                }
            }
            Button(onClick = {}) { Text("Guardar recuerdo") }
            Row(
                horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaType.entries.forEach { type ->
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.mediaTypeColor(type)),
                    )
                }
            }
        }
    }
}
