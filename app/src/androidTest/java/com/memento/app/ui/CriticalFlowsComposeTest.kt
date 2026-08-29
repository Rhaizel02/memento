package com.memento.app.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.ui.add.AddMediaMode
import com.memento.app.ui.add.AddMediaScreen
import com.memento.app.ui.add.AddMediaUiState
import com.memento.app.ui.detail.MediaDetailScreen
import com.memento.app.ui.detail.MediaDetailUiState
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
            state = remember { mutableStateOf(AddMediaUiState(mode = AddMediaMode.MANUAL, type = MediaType.BOOK)) }
            MementoTheme(ThemeMode.LIGHT) {
                AddMediaScreen(
                    state = state.value,
                    onBack = {},
                    onTypeChanged = { state.value = state.value.copy(type = it) },
                    onQueryChanged = {},
                    onResultSelected = {},
                    onShowManual = {},
                    onReturnToSearch = {},
                    onTitleChanged = { state.value = state.value.copy(title = it) },
                    onYearChanged = {},
                    onCreatorChanged = {},
                    onDescriptionChanged = {},
                    onImageChanged = {},
                    onPageCountChanged = {},
                    onSave = { savedStatus = it },
                )
            }
        }

        composeRule.onNodeWithText("Título *").performTextInput("Dune")
        composeRule.onNodeWithText("Añadir a pendientes").performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(ConsumptionStatus.PLANNED, savedStatus) }
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
