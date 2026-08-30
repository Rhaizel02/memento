package com.memento.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.TimelineEventType
import com.memento.app.ui.theme.MementoTheme
import com.memento.app.ui.timeline.TimelineScreen
import com.memento.app.ui.timeline.TimelineUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TimelineScreenComposeTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun emptyTimelineExplainsWhenHistoryWillAppear() {
        setTimeline(TimelineUiState(isLoading = false))

        composeRule.onNodeWithText("Tu historia todavía está por escribir.").assertIsDisplayed()
        composeRule.onNodeWithText("Cuando empieces, termines o escribas sobre una obra, aparecerá aquí.").assertIsDisplayed()
    }

    @Test
    fun completedEventShowsTitleAndRatingAndOpensMedia() {
        var openedMediaId: String? = null
        setTimeline(
            TimelineUiState(events = listOf(completedEvent), isLoading = false),
            onOpenMedia = { openedMediaId = it },
        )

        composeRule.onNodeWithText("Terminaste").assertIsDisplayed()
        composeRule.onNodeWithText("Arrival").assertIsDisplayed()
        composeRule.onNodeWithText("★ 4.5").assertIsDisplayed()
        composeRule.onNodeWithText("Arrival").performClick()

        composeRule.runOnIdle { assertEquals("movie", openedMediaId) }
    }

    @Test
    fun reflectionEventShowsPreview() {
        setTimeline(TimelineUiState(events = listOf(reflectionEvent), isLoading = false))

        composeRule.onNodeWithText("Volviste a pensar en").assertIsDisplayed()
        composeRule.onNodeWithText("“Dos años después sigo pensando en aquel final.”").assertIsDisplayed()
    }

    @Test
    fun longReflectionAndTitleRemainUsableAtFontScaleOnePointFive() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                MementoTheme(ThemeMode.DARK) {
                    Surface {
                        Box(Modifier.width(320.dp).height(700.dp)) {
                            TimelineScreen(
                                state = TimelineUiState(events = listOf(reflectionEvent), isLoading = false),
                                onBack = {},
                                onMediaTypeSelected = {},
                                onOpenMedia = {},
                                onLoadMore = {},
                                onRetry = {},
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText(reflectionEvent.title).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("“Dos años después sigo pensando en aquel final.”").assertIsDisplayed()
    }

    private fun setTimeline(state: TimelineUiState, onOpenMedia: (String) -> Unit = {}) {
        composeRule.setContent {
            MementoTheme(ThemeMode.LIGHT) {
                TimelineScreen(
                    state = state,
                    onBack = {},
                    onMediaTypeSelected = {},
                    onOpenMedia = onOpenMedia,
                    onLoadMore = {},
                    onRetry = {},
                )
            }
        }
    }

    private companion object {
        val completedEvent = CulturalTimelineEvent(
            id = "completed:c1",
            date = LocalDate.of(2026, 8, 29),
            occurredAt = null,
            mediaItemId = "movie",
            consumptionId = "c1",
            mediaType = MediaType.MOVIE,
            title = "Arrival",
            posterUrl = null,
            eventType = TimelineEventType.COMPLETED,
            ratingHalfStars = 9,
        )
        val reflectionEvent = CulturalTimelineEvent(
            id = "reflection:r1",
            date = LocalDate.of(2024, 8, 14),
            occurredAt = Instant.parse("2024-08-14T20:00:00Z"),
            mediaItemId = "game",
            consumptionId = "c2",
            mediaType = MediaType.GAME,
            title = "The Last of Us Part II y el peso de recordar historias que cambian con nosotros",
            posterUrl = null,
            eventType = TimelineEventType.LATER_REFLECTION,
            reflectionId = "r1",
            reflectionContent = "Dos años después sigo pensando en aquel final.",
        )
    }
}
