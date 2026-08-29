package com.memento.app.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.data.preferences.ThemePalette
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.recommendation.Recommendation
import com.memento.app.domain.recommendation.RecommendationReason
import com.memento.app.domain.remember.RememberCandidate
import com.memento.app.ui.theme.MementoTheme
import java.time.LocalDate

@Preview(name = "Memento · Light", group = "Home 2.0", widthDp = 412, heightDp = 900)
@Composable
private fun HomeMementoLightPreview() = HomePreview(ThemeMode.LIGHT, ThemePalette.MEMENTO)

@Preview(name = "Memento · Dark", group = "Home 2.0", widthDp = 412, heightDp = 900)
@Composable
private fun HomeMementoDarkPreview() = HomePreview(ThemeMode.DARK, ThemePalette.MEMENTO)

@Preview(name = "Forest", group = "Home 2.0", widthDp = 412, heightDp = 900)
@Composable
private fun HomeForestPreview() = HomePreview(ThemeMode.LIGHT, ThemePalette.FOREST)

@Preview(name = "Noble", group = "Home 2.0", widthDp = 412, heightDp = 900)
@Composable
private fun HomeNoblePreview() = HomePreview(ThemeMode.LIGHT, ThemePalette.NOBLE)

@Preview(name = "Ink", group = "Home 2.0", widthDp = 412, heightDp = 900)
@Composable
private fun HomeInkPreview() = HomePreview(ThemeMode.DARK, ThemePalette.INK)

@Composable
private fun HomePreview(themeMode: ThemeMode, palette: ThemePalette) {
    MementoTheme(themeMode = themeMode, palette = palette) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            HomeScreen(
                state = previewState,
                onAdd = {},
                onOpenMedia = {},
                onOpenRemember = {},
                onOpenDiscover = {},
                onOpenStats = {},
            )
        }
    }
}

private val previewState = HomeUiState(
    mediaCount = 8,
    remember = RememberCandidate(
        consumptionId = "memory-consumption",
        mediaId = "memory-media",
        title = "La llegada",
        completedDate = LocalDate.now().minusYears(2),
        ratingHalfStars = 10,
        isFavorite = true,
        posterUrl = null,
        backdropUrl = null,
        reflectionId = "memory-reflection",
        reflectionType = ReflectionType.FINAL_REFLECTION,
        reflectionContent = "La forma en que hablamos también transforma la forma en que recordamos.",
        reflectionCount = 2,
        lastShownAt = null,
        mediaType = MediaType.MOVIE,
    ),
    inProgress = listOf(
        HomeMediaItem(
            mediaId = "book",
            consumptionId = "book-consumption",
            type = MediaType.BOOK,
            title = "El problema de los tres cuerpos",
            posterUrl = null,
            backdropUrl = null,
            isFavorite = true,
            creator = "Cixin Liu",
            releaseYear = 2008,
            genres = listOf("Ciencia ficción", "Misterio"),
            additionalGenreCount = 1,
            ratingHalfStars = 9,
            progress = HomeProgress.Pages(current = 186.0, total = 408.0, fraction = 0.46f),
        ),
        HomeMediaItem(
            mediaId = "series",
            consumptionId = "series-consumption",
            type = MediaType.SERIES,
            title = "Una serie con un título deliberadamente largo y sin metadatos",
            posterUrl = null,
            backdropUrl = null,
            isFavorite = false,
            creator = null,
            releaseYear = null,
            genres = emptyList(),
            additionalGenreCount = 0,
            ratingHalfStars = null,
            progress = HomeProgress.Episode(season = 2, episode = 4),
        ),
        HomeMediaItem(
            mediaId = "game",
            consumptionId = "game-consumption",
            type = MediaType.GAME,
            title = "Hades",
            posterUrl = null,
            backdropUrl = null,
            isFavorite = false,
            creator = "Supergiant Games",
            releaseYear = 2020,
            genres = listOf("Roguelike"),
            additionalGenreCount = 0,
            ratingHalfStars = 8,
            progress = HomeProgress.Game(hours = 18.5, percent = 62.0),
        ),
    ),
    recommendation = Recommendation(
        candidate = MetadataSearchResult(
            provider = MetadataProvider.OPEN_LIBRARY,
            externalId = "preview-recommendation",
            externalUrl = null,
            type = MediaType.BOOK,
            title = "Piranesi",
            releaseYear = 2020,
            creators = listOf("Susanna Clarke"),
            genres = listOf("Fantasía", "Misterio"),
        ),
        affinityScore = 91,
        reasons = listOf(RecommendationReason.Genre("Fantasía")),
    ),
    recommendationProfileReady = true,
    recentlyCompleted = listOf(
        HomeMediaItem(
            mediaId = "recent-movie",
            consumptionId = "recent-consumption",
            type = MediaType.MOVIE,
            title = "Perfect Days",
            posterUrl = null,
            backdropUrl = null,
            isFavorite = true,
            creator = "Wim Wenders",
            releaseYear = 2023,
            genres = listOf("Drama"),
            additionalGenreCount = 0,
            ratingHalfStars = 9,
            completedDate = LocalDate.now().minusDays(3),
        ),
    ),
    summaryYear = LocalDate.now().year,
    completedByType = mapOf(
        MediaType.BOOK to 9,
        MediaType.MOVIE to 14,
        MediaType.SERIES to 5,
        MediaType.GAME to 3,
    ),
    isLoading = false,
)
