package com.memento.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.ui.add.AddMediaMode
import com.memento.app.ui.add.AddMediaDraft
import com.memento.app.ui.add.AddMediaScreen
import com.memento.app.ui.add.AddMediaUiState
import com.memento.app.ui.add.CompletedDraft
import com.memento.app.ui.detail.MediaDetailScreen
import com.memento.app.ui.detail.MediaDetailUiState
import com.memento.app.ui.components.ExpandableText
import com.memento.app.ui.theme.MementoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CriticalFlowsComposeTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun manualAddSendsPlannedWorkAfterEnteringTitle() {
        lateinit var state: MutableState<AddMediaUiState>
        var savedStatus: ConsumptionStatus? = null
        composeRule.setContent {
            state = remember {
                mutableStateOf(
                    AddMediaUiState(mode = AddMediaMode.MANUAL, manualDraft = AddMediaDraft(type = MediaType.BOOK)),
                )
            }
            MementoTheme(ThemeMode.LIGHT) {
                AddMediaScreen(
                    state = state.value,
                    onBack = {},
                    onTypeChanged = {
                        state.value = state.value.copy(manualDraft = state.value.manualDraft.copy(type = it))
                    },
                    onQueryChanged = {},
                    onResultSelected = {},
                    onShowManual = {},
                    onReturnToSearch = {},
                    onTitleChanged = {
                        state.value = state.value.copy(manualDraft = state.value.manualDraft.copy(title = it))
                    },
                    onYearChanged = {},
                    onCreatorChanged = {},
                    onDescriptionChanged = {},
                    onImageChanged = {},
                    onPageCountChanged = {},
                    onSave = { savedStatus = it },
                    onCompletedDateChanged = {},
                    onCompletedRatingChanged = {},
                    onCompletedFavoriteChanged = {},
                    onCompletedReflectionChanged = {},
                    onCancelCompletion = {},
                    onSaveCompleted = {},
                )
            }
        }

        composeRule.onNodeWithText("Título *").performTextInput("Dune")
        composeRule.onNodeWithText("Añadir a pendientes").performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(ConsumptionStatus.PLANNED, savedStatus) }
    }

    @Test
    fun addSearchClearRemovesQueryAndKeepsKeyboardFocus() {
        lateinit var state: MutableState<AddMediaUiState>
        composeRule.setContent {
            state = remember { mutableStateOf(AddMediaUiState(query = "Dune")) }
            MementoTheme(ThemeMode.LIGHT) {
                AddMediaScreen(
                    state = state.value,
                    onBack = {},
                    onTypeChanged = {},
                    onQueryChanged = { state.value = state.value.copy(query = it) },
                    onResultSelected = {},
                    onShowManual = {},
                    onReturnToSearch = {},
                    onTitleChanged = {},
                    onYearChanged = {},
                    onCreatorChanged = {},
                    onDescriptionChanged = {},
                    onImageChanged = {},
                    onPageCountChanged = {},
                    onSave = {},
                    onCompletedDateChanged = {},
                    onCompletedRatingChanged = {},
                    onCompletedFavoriteChanged = {},
                    onCompletedReflectionChanged = {},
                    onCancelCompletion = {},
                    onSaveCompleted = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Borrar búsqueda").performClick()

        composeRule.runOnIdle { assertEquals("", state.value.query) }
        composeRule.onNodeWithContentDescription("Borrar búsqueda").assertDoesNotExist()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun completedAddStepCollectsOptionalHistoryBeforeSaving() {
        lateinit var state: MutableState<AddMediaUiState>
        var saved = false
        var cancelled = false
        composeRule.setContent {
            state = remember {
                mutableStateOf(
                    AddMediaUiState(
                        mode = AddMediaMode.COMPLETE_DETAILS,
                        manualDraft = AddMediaDraft(MediaType.MOVIE, title = "Película histórica"),
                        completionReturnMode = AddMediaMode.MANUAL,
                        completedDraft = CompletedDraft(completedDateText = "2022-04-12"),
                    ),
                )
            }
            MementoTheme(ThemeMode.LIGHT) {
                AddMediaScreen(
                    state = state.value,
                    onBack = {},
                    onTypeChanged = {},
                    onQueryChanged = {},
                    onResultSelected = {},
                    onShowManual = {},
                    onReturnToSearch = {},
                    onTitleChanged = {},
                    onYearChanged = {},
                    onCreatorChanged = {},
                    onDescriptionChanged = {},
                    onImageChanged = {},
                    onPageCountChanged = {},
                    onSave = {},
                    onCompletedDateChanged = { value ->
                        state.value = state.value.copy(
                            completedDraft = state.value.completedDraft?.copy(completedDateText = value),
                        )
                    },
                    onCompletedRatingChanged = { value ->
                        state.value = state.value.copy(
                            completedDraft = state.value.completedDraft?.copy(ratingHalfStars = value),
                        )
                    },
                    onCompletedFavoriteChanged = { value ->
                        state.value = state.value.copy(
                            completedDraft = state.value.completedDraft?.copy(favorite = value),
                        )
                    },
                    onCompletedReflectionChanged = {},
                    onCancelCompletion = { cancelled = true },
                    onSaveCompleted = { saved = true },
                )
            }
        }

        composeRule.onNodeWithText("4.5").performClick()
        composeRule.onNodeWithText("Marcar como favorito").performClick()
        composeRule.onNodeWithText("Guardar").performScrollTo().performClick()
        composeRule.onNodeWithText("Cancelar").performClick()

        composeRule.runOnIdle {
            assertEquals(9, state.value.completedDraft?.ratingHalfStars)
            assertEquals(true, state.value.completedDraft?.favorite)
            assertEquals(true, saved)
            assertEquals(true, cancelled)
        }
    }

    @Test
    fun expandableTextOnlyOffersExpansionWhenContentOverflows() {
        composeRule.setContent {
            MementoTheme(ThemeMode.LIGHT) {
                Column {
                    ExpandableText(
                        text = "Breve",
                        modifier = Modifier.width(120.dp),
                        collapsedMaxLines = 2,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    ExpandableText(
                        text = "Una descripción deliberadamente larga que necesita muchas líneas para mostrarse completa en un espacio estrecho.",
                        modifier = Modifier.width(120.dp),
                        collapsedMaxLines = 2,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        composeRule.onAllNodesWithText("Ver más").assertCountEquals(1)
        composeRule.onNodeWithText("Ver más").assertExists().performClick()
        composeRule.onNodeWithText("Ver menos").assertExists()
    }

    @Test
    fun deletingOneConsumptionRequiresConfirmationAndReturnsItsId() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val media = MediaItem("m1", MediaType.BOOK, "Dune", createdAt = now, updatedAt = now)
        val consumption = Consumption(
            "c1",
            "m1",
            ConsumptionStatus.COMPLETED,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 10),
            9,
            now,
            now,
        )
        var deletedId: String? = null
        composeRule.setContent {
            MementoTheme(ThemeMode.LIGHT) {
                MediaDetailScreen(
                    state = MediaDetailUiState(detail = MediaDetail(media, emptyList(), emptyList(), listOf(consumption), emptyList(), emptyList())),
                    onBack = {},
                    onToggleFavorite = {},
                    onStart = {},
                    onComplete = { _, _, _ -> },
                    onDrop = {},
                    onAddNote = {},
                    onAddProgress = { _, _, _, _, _ -> },
                    onUpdateMetadata = {},
                    onDeleteConsumption = { deletedId = it },
                    onUpdateReflection = { _, _ -> },
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Eliminar este consumo").performScrollTo().performClick()
        composeRule.onNodeWithText("Eliminar consumo").assertExists()
        composeRule.onNodeWithText("Eliminar").performClick()

        composeRule.runOnIdle { assertEquals("c1", deletedId) }
    }
}
