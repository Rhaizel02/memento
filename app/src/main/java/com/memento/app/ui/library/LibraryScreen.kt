package com.memento.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import com.memento.app.ui.components.MementoPrimaryButton as Button
import com.memento.app.ui.components.MementoFilterChip as FilterChip
import com.memento.app.ui.components.MementoBottomSheet as ModalBottomSheet
import com.memento.app.ui.components.MementoSecondaryButton as OutlinedButton
import com.memento.app.ui.components.MementoTextField as OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.LibrarySort
import com.memento.app.domain.model.LibraryMediaSummary
import com.memento.app.domain.model.MediaType
import com.memento.app.ui.components.EmptyState
import com.memento.app.ui.components.MementoScreenHeader
import com.memento.app.ui.components.MementoSearchField
import com.memento.app.ui.theme.MementoSpacing

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LibraryScreen(
    state: LibraryUiState,
    onQueryChanged: (String) -> Unit,
    onTypeSelected: (MediaType?) -> Unit,
    onStatusSelected: (ConsumptionStatus?) -> Unit,
    onMinRatingSelected: (Int?) -> Unit,
    onFavoritesOnlyChanged: (Boolean) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onSortSelected: (LibrarySort) -> Unit,
    onTagToggled: (String) -> Unit = {},
    onClearFilters: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onAdd: () -> Unit,
    onOpenGlobalSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {

    var showFilters by rememberSaveable { mutableStateOf(false) }
    var grid by rememberSaveable { mutableStateOf(false) }
    val collectionState = rememberLazyGridState()
    val gridWidth = if (LocalDensity.current.fontScale > 1.3f) 188.dp else 144.dp
    Column(Modifier.fillMaxSize().imePadding()) {
        MementoScreenHeader(
            title = stringResource(R.string.library),
            subtitle = stringResource(R.string.library_subtitle_compact),
            compact = true, onSettings = onOpenSettings,
            actions = {
                FilledTonalIconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, stringResource(R.string.add_work)) }
                IconButton(onClick = onOpenGlobalSearch) { Icon(Icons.Outlined.Search, stringResource(R.string.global_search_title)) }
            },
        )
        // One scrolling collection: controls never consume the entire viewport at large font sizes.
        LazyVerticalGrid(
            columns = if (grid) GridCells.Adaptive(gridWidth) else GridCells.Fixed(1),
            state = collectionState, modifier = Modifier.weight(1f).testTag("library_collection"),
            contentPadding = PaddingValues(start = MementoSpacing.normal, end = MementoSpacing.normal,
                top = 8.dp, bottom = MementoSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("search", span = { GridItemSpan(maxLineSpan) }) {
                MementoSearchField(state.query, onQueryChanged, stringResource(R.string.search_library),
                    Modifier.fillMaxWidth(), compact = true)
            }
            item("media-types", span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf<MediaType?>(null) + MediaType.entries) { type ->
                        FilterChip(state.type == type, { onTypeSelected(type) },
                            label = { Text(typeFilterLabel(type), maxLines = 1) })
                    }
                }
            }
            item("statuses", span = { GridItemSpan(maxLineSpan) }) {
                LibraryStatusTabs(state.filters.status, onStatusSelected)
            }
            item("toolbar", span = { GridItemSpan(maxLineSpan) }) {
                LibraryCollectionToolbar(
                    state = state, grid = grid, onFilters = { showFilters = true },
                    onSort = onSortSelected, onToggleView = { grid = !grid },
                )
            }
            val constrained = state.query.isNotBlank() || state.type != null ||
                state.filters.status != null || advancedFilterCount(state) > 0
            when {
                state.isLoading -> item("loading", span = { GridItemSpan(maxLineSpan) }) {
                    com.memento.app.ui.components.ArchiveLoading()
                }
                state.items.isEmpty() -> item("empty", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = stringResource(if (constrained) R.string.no_search_results else R.string.library_empty_title),
                        body = stringResource(if (constrained) R.string.no_library_filter_results_body else R.string.library_empty_body),
                    ) {
                        if (!constrained) Button(onClick = onAdd) { Text(stringResource(R.string.add_first_work)) }
                        else OutlinedButton(onClick = { onQueryChanged(""); onTypeSelected(null); onClearFilters() }) {
                            Text(stringResource(R.string.clear_filters))
                        }
                    }
                }
                else -> items(state.items, key = { it.id }, contentType = { if (grid) "grid-media" else "list-media" }) { media ->
                    LibraryMediaItem(state.summaries[media.id] ?: LibraryMediaSummary(media), grid) { onOpenMedia(media.id) }
                }
            }
        }
    }
    if (showFilters) LibraryFilterSheet(
        state = state, onDismiss = { showFilters = false },
        onStatusSelected = onStatusSelected, onMinRatingSelected = onMinRatingSelected,
        onFavoritesOnlyChanged = onFavoritesOnlyChanged, onYearSelected = onYearSelected,
        onSortSelected = onSortSelected, onTagToggled = onTagToggled, onClear = onClearFilters,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LibraryFilterSheet(
    state: LibraryUiState,
    onDismiss: () -> Unit,
    onStatusSelected: (ConsumptionStatus?) -> Unit,
    onMinRatingSelected: (Int?) -> Unit,
    onFavoritesOnlyChanged: (Boolean) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onSortSelected: (LibrarySort) -> Unit,
    onTagToggled: (String) -> Unit,
    onClear: () -> Unit,
) {
    var yearText by remember(state.filters.year) { mutableStateOf(state.filters.year?.toString().orEmpty()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(bottom = MementoSpacing.huge),
            verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
        ) {
            Text(
                stringResource(R.string.filter_library),
                modifier = Modifier.padding(horizontal = MementoSpacing.normal),
                style = MaterialTheme.typography.headlineSmall,
            )
            FilterChoiceRow(
                title = stringResource(R.string.status),
                values = listOf(null, ConsumptionStatus.PLANNED, ConsumptionStatus.IN_PROGRESS, ConsumptionStatus.COMPLETED, ConsumptionStatus.DROPPED),
                selected = state.filters.status,
                label = { statusFilterLabel(it) },
                onSelected = onStatusSelected,
            )
            FilterChoiceRow(
                title = stringResource(R.string.rating),
                values = listOf<Int?>(null, 6, 8, 9, 10),
                selected = state.filters.minRatingHalfStars,
                label = { it?.let { value -> stringResource(R.string.minimum_rating, value / 2f) } ?: stringResource(R.string.all) },
                onSelected = onMinRatingSelected,
            )
            FilterChip(
                selected = state.filters.favoritesOnly,
                onClick = { onFavoritesOnlyChanged(!state.filters.favoritesOnly) },
                label = { Text(stringResource(R.string.only_favorites)) },
                modifier = Modifier.padding(horizontal = MementoSpacing.normal),
            )
            OutlinedTextField(
                value = yearText,
                onValueChange = { value ->
                    yearText = value.filter(Char::isDigit).take(4)
                    onYearSelected(yearText.toIntOrNull())
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal),
                labelText = stringResource(R.string.year),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            FilterChoiceRow(
                title = stringResource(R.string.sort_by),
                values = LibrarySort.entries,
                selected = state.filters.sort,
                label = { librarySortLabel(it) },
                onSelected = onSortSelected,
            )
            if (state.availableTags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                    Text(
                        stringResource(R.string.personal_tags),
                        modifier = Modifier.padding(horizontal = MementoSpacing.normal),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = MementoSpacing.normal),
                        horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small),
                    ) {
                        items(state.availableTags, key = { it.id }) { tag ->
                            FilterChip(
                                selected = tag.id in state.filters.tagIds,
                                onClick = { onTagToggled(tag.id) },
                                label = { Text(tag.name) },
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { onClear(); yearText = "" }) { Text(stringResource(R.string.clear_filters)) }
                Button(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            }
        }
    }
}

@Composable
private fun <T> FilterChoiceRow(
    title: String,
    values: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
        Text(title, modifier = Modifier.padding(horizontal = MementoSpacing.normal), style = MaterialTheme.typography.labelLarge)
        LazyRow(
            contentPadding = PaddingValues(horizontal = MementoSpacing.normal),
            horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small),
        ) {
            items(values) { value ->
                FilterChip(selected = selected == value, onClick = { onSelected(value) }, label = { Text(label(value)) })
            }
        }
    }
}

fun advancedFilterCount(state: LibraryUiState): Int = listOf(
    state.filters.minRatingHalfStars != null,
    state.filters.favoritesOnly,
    state.filters.year != null,
    state.filters.tagIds.isNotEmpty(),
).count { it }

@Composable
internal fun statusFilterLabel(status: ConsumptionStatus?): String = stringResource(
    when (status) {
        null -> R.string.all
        ConsumptionStatus.PLANNED -> R.string.planned
        ConsumptionStatus.IN_PROGRESS -> R.string.status_in_progress
        ConsumptionStatus.COMPLETED -> R.string.completed
        ConsumptionStatus.DROPPED -> R.string.dropped
    },
)

@Composable
internal fun librarySortLabel(sort: LibrarySort): String = stringResource(
    when (sort) {
        LibrarySort.RECENT -> R.string.sort_recent
        LibrarySort.TITLE -> R.string.sort_title
        LibrarySort.RATING -> R.string.sort_rating
        LibrarySort.COMPLETED_DATE -> R.string.sort_completed_date
        LibrarySort.ADDED_DATE -> R.string.sort_added_date
    },
)

@Composable
internal fun typeFilterLabel(type: MediaType?): String = stringResource(
    when (type) {
        null -> R.string.all
        MediaType.BOOK -> R.string.books
        MediaType.MOVIE -> R.string.movies
        MediaType.SERIES -> R.string.series
        MediaType.GAME -> R.string.games
    },
)
