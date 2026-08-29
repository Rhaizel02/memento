package com.memento.app.ui

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.remember.RememberCandidate
import com.memento.app.ui.home.HomeMediaItem
import com.memento.app.ui.home.HomeProgress
import com.memento.app.ui.home.HomeScreen
import com.memento.app.ui.home.HomeUiState
import com.memento.app.ui.theme.MementoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class HomeScreenComposeTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun emptyHomeExplainsValueAndStartsTheFirstAddFlow() {
        var addClicks = 0
        setHome(HomeUiState(mediaCount = 0, isLoading = false), onAdd = { addClicks++ })

        composeRule.onNodeWithText("Tu memoria empieza aquí").assertIsDisplayed()
        composeRule.onNodeWithText("Añadir primera obra").performClick()

        composeRule.runOnIdle { assertEquals(1, addClicks) }
    }

    @Test
    fun richInProgressCardShowsMetadataGenresProgressAndAccessibleRating() {
        setHome(
            HomeUiState(
                mediaCount = 1,
                inProgress = listOf(
                    homeItem(
                        title = "Dune",
                        creator = "Frank Herbert",
                        releaseYear = 1965,
                        genres = listOf("Ciencia ficción", "Aventura"),
                        additionalGenreCount = 1,
                        ratingHalfStars = 9,
                        progress = HomeProgress.Pages(103.0, 412.0, 0.25f),
                    ),
                ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("En progreso").performScrollTo()
        composeRule.onNodeWithText("Frank Herbert · 1965").assertIsDisplayed()
        composeRule.onNodeWithText("Ciencia ficción").assertIsDisplayed()
        composeRule.onNodeWithText("103 / 412 páginas").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Valoración: 4.5 de 5").assertIsDisplayed()
    }

    @Test
    fun partialInProgressCardRemainsUsableWithoutOptionalMetadata() {
        var openedMediaId: String? = null
        setHome(
            HomeUiState(
                mediaCount = 1,
                inProgress = listOf(
                    homeItem(
                        title = "Obra sin ficha completa",
                        creator = null,
                        releaseYear = null,
                        genres = emptyList(),
                        progress = null,
                    ),
                ),
                isLoading = false,
            ),
            onOpenMedia = { openedMediaId = it },
        )

        composeRule.onNodeWithText("Obra sin ficha completa").performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals("media", openedMediaId) }
    }

    @Test
    fun rememberHeroExposesReflectionAndOpensTheCorrectConsumption() {
        var openedConsumptionId: String? = null
        val memory = RememberCandidate(
            consumptionId = "remember-consumption",
            mediaId = "remember-media",
            title = "La llegada",
            completedDate = LocalDate.now().minusYears(1),
            ratingHalfStars = 10,
            isFavorite = true,
            posterUrl = null,
            backdropUrl = null,
            reflectionId = "reflection",
            reflectionType = ReflectionType.FINAL_REFLECTION,
            reflectionContent = "El lenguaje también modifica la memoria.",
            reflectionCount = 1,
            lastShownAt = null,
            mediaType = MediaType.MOVIE,
        )
        setHome(
            HomeUiState(mediaCount = 1, remember = memory, isLoading = false),
            onOpenRemember = { openedConsumptionId = it },
        )

        composeRule.onNodeWithText("“El lenguaje también modifica la memoria.”").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Recordar La llegada").performClick()

        composeRule.runOnIdle { assertEquals("remember-consumption", openedConsumptionId) }
    }

    @Test
    fun recentCardKeepsRatingSemanticsAndOpensItsMedia() {
        var openedMediaId: String? = null
        setHome(
            HomeUiState(
                mediaCount = 1,
                recentlyCompleted = listOf(homeItem(title = "Perfect Days", ratingHalfStars = 10)),
                isLoading = false,
            ),
            onOpenMedia = { openedMediaId = it },
        )

        composeRule.onNodeWithText("Recientemente terminadas").performScrollTo()
        composeRule.onNodeWithContentDescription("Valoración: 5 de 5").assertIsDisplayed()
        composeRule.onNodeWithText("Perfect Days").performClick()

        composeRule.runOnIdle { assertEquals("media", openedMediaId) }
    }

    private fun setHome(
        state: HomeUiState,
        onAdd: () -> Unit = {},
        onOpenMedia: (String) -> Unit = {},
        onOpenRemember: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            MementoTheme(ThemeMode.LIGHT) {
                Surface {
                    HomeScreen(
                        state = state,
                        onAdd = onAdd,
                        onOpenMedia = onOpenMedia,
                        onOpenRemember = onOpenRemember,
                        onOpenDiscover = {},
                        onOpenStats = {},
                    )
                }
            }
        }
    }

    private fun homeItem(
        title: String,
        creator: String? = null,
        releaseYear: Int? = null,
        genres: List<String> = emptyList(),
        additionalGenreCount: Int = 0,
        ratingHalfStars: Int? = null,
        progress: HomeProgress? = null,
    ) = HomeMediaItem(
        mediaId = "media",
        consumptionId = "consumption",
        type = MediaType.BOOK,
        title = title,
        posterUrl = null,
        backdropUrl = null,
        isFavorite = false,
        creator = creator,
        releaseYear = releaseYear,
        genres = genres,
        additionalGenreCount = additionalGenreCount,
        ratingHalfStars = ratingHalfStars,
        progress = progress,
    )
}
