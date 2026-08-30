@file:Suppress("ConfigurationScreenWidthHeight")

package com.memento.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.memento.app.R
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.data.preferences.ThemePalette
import com.memento.app.ui.add.AddMediaScreen
import com.memento.app.ui.add.AddMediaViewModel
import com.memento.app.ui.detail.MediaDetailScreen
import com.memento.app.ui.detail.MediaDetailViewModel
import com.memento.app.ui.discover.DiscoverScreen
import com.memento.app.ui.discover.DiscoverViewModel
import com.memento.app.ui.home.HomeScreen
import com.memento.app.ui.home.HomeViewModel
import com.memento.app.ui.library.LibraryScreen
import com.memento.app.ui.library.LibraryViewModel
import com.memento.app.ui.remember.RememberScreen
import com.memento.app.ui.remember.RememberViewModel
import com.memento.app.ui.settings.SettingsScreen
import com.memento.app.ui.settings.SettingsViewModel
import com.memento.app.ui.stats.StatsScreen
import com.memento.app.ui.stats.StatsViewModel
import com.memento.app.ui.timeline.TimelineScreen
import com.memento.app.ui.timeline.TimelineViewModel
import com.memento.app.ui.wrapped.WrappedScreen
import com.memento.app.ui.wrapped.WrappedViewModel
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable sealed interface MementoKey : NavKey
@Serializable data object HomeKey : MementoKey
@Serializable data object LibraryKey : MementoKey
@Serializable data object DiscoverKey : MementoKey
@Serializable data object SettingsKey : MementoKey
@Serializable data class AddMediaKey(val sessionId: String) : MementoKey
@Serializable data class MediaDetailKey(val mediaId: String) : MementoKey
@Serializable data class RememberKey(val consumptionId: String) : MementoKey
@Serializable data object StatsKey : MementoKey
@Serializable data object TimelineKey : MementoKey
@Serializable data class WrappedKey(val year: Int) : MementoKey

private fun newAddMediaKey(): AddMediaKey = AddMediaKey(UUID.randomUUID().toString())

private data class TopDestination(
    val key: MementoKey,
    val label: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
fun MementoApp(
    themeMode: ThemeMode,
    themePalette: ThemePalette,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onThemePaletteChanged: (ThemePalette) -> Unit,
) {
    val backStack = rememberNavBackStack(HomeKey)
    val current = backStack.lastOrNull()
    val destinations = listOf(
        TopDestination(HomeKey, R.string.home, Icons.Outlined.Home),
        TopDestination(LibraryKey, R.string.library, Icons.AutoMirrored.Outlined.LibraryBooks),
        TopDestination(DiscoverKey, R.string.discover, Icons.Outlined.Explore),
        TopDestination(SettingsKey, R.string.settings, Icons.Outlined.Settings),
    )
    val showChrome = current in destinations.map { it.key }

    fun openTopLevel(key: MementoKey) {
        if (backStack.firstOrNull() == key && backStack.size == 1) return
        while (backStack.size > 1) backStack.removeLastOrNull()
        if (backStack.isEmpty()) backStack.add(key) else backStack[0] = key
    }

    Scaffold(
        bottomBar = {
            if (showChrome) {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = current == destination.key,
                            onClick = { openTopLevel(destination.key) },
                            icon = { Icon(destination.icon, contentDescription = stringResource(destination.label)) },
                            label = { Text(stringResource(destination.label)) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (current == HomeKey || current == LibraryKey) {
                FloatingActionButton(onClick = { backStack.add(newAddMediaKey()) }) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_work))
                }
            }
        },
    ) { padding ->
        NavDisplay(
            modifier = Modifier.padding(padding),
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    HomeKey -> NavEntry(key) {
                        val viewModel: HomeViewModel = hiltViewModel()
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        HomeScreen(
                            state = state,
                            onAdd = { backStack.add(newAddMediaKey()) },
                            onOpenMedia = { backStack.add(MediaDetailKey(it)) },
                            onOpenRemember = { backStack.add(RememberKey(it)) },
                            onOpenDiscover = { openTopLevel(DiscoverKey) },
                            onOpenStats = { backStack.add(StatsKey) },
                            onOpenTimeline = { backStack.add(TimelineKey) },
                            onOpenQuickProgress = viewModel::openQuickProgress,
                            onOpenQuickNote = viewModel::openQuickNote,
                            onQuickProgressChanged = viewModel::updateQuickProgress,
                            onQuickNoteChanged = viewModel::updateQuickNote,
                            onSaveQuickProgress = viewModel::saveQuickProgress,
                            onSaveQuickNote = viewModel::saveQuickNote,
                            onDismissQuickCapture = viewModel::dismissQuickCapture,
                        )
                    }
                    LibraryKey -> NavEntry(key) {
                        val viewModel: LibraryViewModel = hiltViewModel()
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        LibraryScreen(
                            state = state,
                            onQueryChanged = viewModel::setQuery,
                            onTypeSelected = viewModel::setType,
                            onStatusSelected = viewModel::setStatus,
                            onMinRatingSelected = viewModel::setMinRating,
                            onFavoritesOnlyChanged = viewModel::setFavoritesOnly,
                            onYearSelected = viewModel::setYear,
                            onSortSelected = viewModel::setSort,
                            onClearFilters = viewModel::clearAdditionalFilters,
                            onOpenMedia = { backStack.add(MediaDetailKey(it)) },
                            onAdd = { backStack.add(newAddMediaKey()) },
                        )
                    }
                    DiscoverKey -> NavEntry(key) {
                        val viewModel: DiscoverViewModel = hiltViewModel()
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        DiscoverScreen(
                            state = state,
                            onRefresh = viewModel::refresh,
                            onFeedback = viewModel::feedback,
                        )
                    }
                    SettingsKey -> NavEntry(key) {
                        val viewModel: SettingsViewModel = hiltViewModel()
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        SettingsScreen(
                            themeMode = themeMode,
                            themePalette = themePalette,
                            state = state,
                            onThemeModeChanged = onThemeModeChanged,
                            onThemePaletteChanged = onThemePaletteChanged,
                            onExport = viewModel::exportTo,
                            onImport = viewModel::prepareImport,
                            onConfirmRestore = viewModel::confirmRestore,
                            onCancelRestore = viewModel::cancelRestore,
                            onDownloadAi = viewModel::downloadAiModel,
                        )
                    }
                    is AddMediaKey -> NavEntry(key) {
                        val viewModel: AddMediaViewModel = hiltViewModel()
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        state.savedMediaId?.let { mediaId ->
                            LaunchedEffect(mediaId) {
                                viewModel.consumeNavigation()
                                backStack.removeLastOrNull()
                                backStack.add(MediaDetailKey(mediaId))
                            }
                        }
                        AddMediaScreen(
                            state = state,
                            onBack = { backStack.removeLastOrNull() },
                            onTypeChanged = viewModel::setType,
                            onQueryChanged = viewModel::setQuery,
                            onResultSelected = viewModel::selectResult,
                            onShowManual = viewModel::showManual,
                            onReturnToSearch = viewModel::returnToSearch,
                            onTitleChanged = viewModel::setTitle,
                            onYearChanged = viewModel::setYear,
                            onCreatorChanged = viewModel::setCreator,
                            onDescriptionChanged = viewModel::setDescription,
                            onImageChanged = viewModel::setImageUrl,
                            onPageCountChanged = viewModel::setPageCount,
                            onSave = viewModel::save,
                            onCompletedDateChanged = viewModel::setCompletedDate,
                            onCompletedRatingChanged = viewModel::setCompletedRating,
                            onCompletedFavoriteChanged = viewModel::setCompletedFavorite,
                            onCompletedReflectionChanged = viewModel::setCompletedReflection,
                            onCancelCompletion = viewModel::cancelCompletion,
                            onSaveCompleted = viewModel::saveCompleted,
                        )
                    }
                    is MediaDetailKey -> NavEntry(key) {
                        val viewModel: MediaDetailViewModel = hiltViewModel()
                        LaunchedEffect(key.mediaId) { viewModel.load(key.mediaId) }
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        LaunchedEffect(state.wasDeleted) {
                            if (state.wasDeleted) backStack.removeLastOrNull()
                        }
                        MediaDetailScreen(
                            state = state,
                            onBack = { backStack.removeLastOrNull() },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onStart = viewModel::start,
                            onComplete = viewModel::complete,
                            onDrop = viewModel::drop,
                            onAddNote = viewModel::addNote,
                            onAddProgress = viewModel::addProgress,
                            onUpdateMetadata = viewModel::updateMetadata,
                            onDeleteConsumption = viewModel::deleteConsumption,
                            onUpdateReflection = viewModel::updateReflection,
                            onDelete = viewModel::delete,
                        )
                    }
                    is RememberKey -> NavEntry(key) {
                        val viewModel: RememberViewModel = hiltViewModel()
                        LaunchedEffect(key.consumptionId) { viewModel.load(key.consumptionId) }
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        RememberScreen(
                            state = state,
                            onBack = { backStack.removeLastOrNull() },
                            onSaveThought = viewModel::saveCurrentThought,
                            onAiAction = viewModel::runAi,
                            onSaveAiInsight = viewModel::saveAiInsight,
                            onDiscardAi = viewModel::discardAiResult,
                        )
                    }
                    StatsKey -> NavEntry(key) {
                        val viewModel: StatsViewModel = hiltViewModel()
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        StatsScreen(
                            state = state,
                            onBack = { backStack.removeLastOrNull() },
                            onSelectYear = viewModel::selectYear,
                            onOpenWrapped = { backStack.add(WrappedKey(it)) },
                        )
                    }
                    TimelineKey -> NavEntry(key) {
                        val viewModel: TimelineViewModel = hiltViewModel()
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        TimelineScreen(
                            state = state,
                            onBack = { backStack.removeLastOrNull() },
                            onMediaTypeSelected = viewModel::selectMediaType,
                            onOpenMedia = { backStack.add(MediaDetailKey(it)) },
                            onLoadMore = viewModel::loadMore,
                            onRetry = viewModel::retry,
                        )
                    }
                    is WrappedKey -> NavEntry(key) {
                        val viewModel: WrappedViewModel = hiltViewModel()
                        LaunchedEffect(key.year) { viewModel.load(key.year) }
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        WrappedScreen(state = state, onBack = { backStack.removeLastOrNull() })
                    }
                    else -> error("Unknown navigation key: $key")
                }
            },
        )
    }
}
