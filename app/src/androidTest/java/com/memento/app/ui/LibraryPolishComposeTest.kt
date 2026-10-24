package com.memento.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.domain.model.*
import com.memento.app.ui.library.LibraryScreen
import com.memento.app.ui.library.LibraryUiState
import com.memento.app.ui.theme.MementoTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LibraryPolishComposeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun collectionControlsKeepTheirCallbacksInListAndGrid() {
        lateinit var state: MutableState<LibraryUiState>
        var opened: String? = null
        var added = false
        var searchedGlobally = false
        compose.setContent {
            state = remember { mutableStateOf(LibraryUiState(isLoading = false, items = listOf(
                MediaItem("dune", MediaType.BOOK, "Dune", createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH),
            ))) }
            MementoTheme(ThemeMode.DARK) {
                LibraryScreen(
                    state.value,
                    onQueryChanged = { state.value = state.value.copy(query = it) },
                    onTypeSelected = { state.value = state.value.copy(type = it) },
                    onStatusSelected = { state.value = state.value.copy(filters = state.value.filters.copy(status = it)) },
                    onMinRatingSelected = {}, onFavoritesOnlyChanged = {}, onYearSelected = {},
                    onSortSelected = { state.value = state.value.copy(filters = state.value.filters.copy(sort = it)) },
                    onClearFilters = {}, onOpenMedia = { opened = it }, onAdd = { added = true },
                    onOpenGlobalSearch = { searchedGlobally = true },
                )
            }
        }
        compose.onNodeWithContentDescription("Buscar por título o creador").performTextInput("Dune")
        compose.onNodeWithContentDescription("Borrar búsqueda").performClick()
        compose.onNodeWithContentDescription("Buscar por título o creador").assertIsFocused()
        compose.onNodeWithText("Libros").performClick()
        compose.onNodeWithText("En progreso").performClick()
        compose.onNodeWithTag("library_sort").performScrollTo().performClick()
        compose.onNodeWithText("Valoración").performClick()
        compose.onNodeWithTag("library_view_toggle").performClick()
        compose.onNodeWithContentDescription("Vista de lista").assertExists()
        compose.onNodeWithText("Dune").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Añadir obra").performClick()
        compose.onNodeWithContentDescription("Búsqueda global").performClick()
        compose.runOnIdle {
            assertEquals("", state.value.query)
            assertEquals(MediaType.BOOK, state.value.type)
            assertEquals(ConsumptionStatus.IN_PROGRESS, state.value.filters.status)
            assertEquals(LibrarySort.RATING, state.value.filters.sort)
            assertEquals("dune", opened)
            assertEquals(true, added)
            assertEquals(true, searchedGlobally)
        }
    }

    @Test fun filteredEmptyCollectionCanClearFiltersWithLargeTextOnNarrowScreen() {
        lateinit var state: MutableState<LibraryUiState>
        compose.setContent {
            state = remember { mutableStateOf(LibraryUiState(
                query = "sin coincidencias", type = MediaType.BOOK, isLoading = false,
                filters = LibraryFilters(status = ConsumptionStatus.COMPLETED, favoritesOnly = true),
            )) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                MementoTheme(ThemeMode.LIGHT) {
                    Box(Modifier.width(320.dp)) {
                        LibraryScreen(state.value,
                            onQueryChanged = { state.value = state.value.copy(query = it) },
                            onTypeSelected = { state.value = state.value.copy(type = it) },
                            onStatusSelected = {}, onMinRatingSelected = {}, onFavoritesOnlyChanged = {},
                            onYearSelected = {}, onSortSelected = {},
                            onClearFilters = { state.value = state.value.copy(filters = LibraryFilters()) },
                            onOpenMedia = {}, onAdd = {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Limpiar").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("", state.value.query)
            assertEquals(null, state.value.type)
            assertEquals(LibraryFilters(), state.value.filters)
        }
    }
}
