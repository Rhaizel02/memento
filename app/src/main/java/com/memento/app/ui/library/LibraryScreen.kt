package com.memento.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.LibrarySort
import com.memento.app.domain.model.MediaType
import com.memento.app.ui.components.EmptyState
import com.memento.app.ui.components.MediaCard
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
) {
    var showFilters by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = MementoSpacing.normal, top = MementoSpacing.normal),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.library), style = MaterialTheme.typography.headlineLarge)
            IconButton(onClick = onOpenGlobalSearch) {
                Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.global_search_title))
            }
        }
        MementoSearchField(
            value = state.query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.medium),
            label = stringResource(R.string.search_library),
        )
        val types = listOf<MediaType?>(null, MediaType.BOOK, MediaType.MOVIE, MediaType.SERIES, MediaType.GAME)
        LazyRow(
            contentPadding = PaddingValues(horizontal = MementoSpacing.normal),
            horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small),
        ) {
            items(types) { type ->
                FilterChip(
                    selected = state.type == type,
                    onClick = { onTypeSelected(type) },
                    label = { Text(typeFilterLabel(type)) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(onClick = { showFilters = true }) {
                Icon(Icons.Outlined.FilterList, contentDescription = null)
                Text(
                    if (activeFilterCount(state) == 0) stringResource(R.string.filters)
                    else stringResource(R.string.filters_count, activeFilterCount(state)),
                    modifier = Modifier.padding(start = MementoSpacing.small),
                )
            }
            Text(
                librarySortLabel(state.filters.sort),
                modifier = Modifier.padding(top = MementoSpacing.medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        val hasConstraints = state.query.isNotBlank() || state.type != null || activeFilterCount(state) > 0
        if (!state.isLoading && state.items.isEmpty()) {
            EmptyState(
                title = if (!hasConstraints) stringResource(R.string.library_empty_title)
                    else stringResource(R.string.no_search_results),
                body = if (!hasConstraints) stringResource(R.string.library_empty_body)
                    else stringResource(R.string.no_library_filter_results_body),
                modifier = Modifier.padding(MementoSpacing.normal),
            ) {
                if (!hasConstraints) {
                    Button(onClick = onAdd) { Text(stringResource(R.string.add_first_work)) }
                } else {
                    OutlinedButton(
                        onClick = {
                            onQueryChanged("")
                            onTypeSelected(null)
                            onClearFilters()
                        },
                    ) { Text(stringResource(R.string.clear_filters)) }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.fillMaxSize().padding(top = MementoSpacing.medium),
                contentPadding = PaddingValues(MementoSpacing.normal),
                horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
            ) {
                items(state.items, key = { it.id }) { media ->
                    MediaCard(
                        media = media,
                        onClick = { onOpenMedia(media.id) },
                        supportingText = media.releaseYear?.toString(),
                    )
                }
            }
        }
    }

    if (showFilters) {
        LibraryFilterSheet(
            state = state,
            onDismiss = { showFilters = false },
            onStatusSelected = onStatusSelected,
            onMinRatingSelected = onMinRatingSelected,
            onFavoritesOnlyChanged = onFavoritesOnlyChanged,
            onYearSelected = onYearSelected,
            onSortSelected = onSortSelected,
            onTagToggled = onTagToggled,
            onClear = onClearFilters,
        )
    }
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
            modifier = Modifier.fillMaxWidth().imePadding().padding(bottom = MementoSpacing.huge),
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
                label = { Text(stringResource(R.string.year)) },
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

private fun activeFilterCount(state: LibraryUiState): Int = listOf(
    state.filters.status != null,
    state.filters.minRatingHalfStars != null,
    state.filters.favoritesOnly,
    state.filters.year != null,
    state.filters.sort != LibrarySort.RECENT,
    state.filters.tagIds.isNotEmpty(),
).count { it }

@Composable
private fun statusFilterLabel(status: ConsumptionStatus?): String = stringResource(
    when (status) {
        null -> R.string.all
        ConsumptionStatus.PLANNED -> R.string.planned
        ConsumptionStatus.IN_PROGRESS -> R.string.status_in_progress
        ConsumptionStatus.COMPLETED -> R.string.completed
        ConsumptionStatus.DROPPED -> R.string.dropped
    },
)

@Composable
private fun librarySortLabel(sort: LibrarySort): String = stringResource(
    when (sort) {
        LibrarySort.RECENT -> R.string.sort_recent
        LibrarySort.TITLE -> R.string.sort_title
        LibrarySort.RATING -> R.string.sort_rating
        LibrarySort.COMPLETED_DATE -> R.string.sort_completed_date
        LibrarySort.ADDED_DATE -> R.string.sort_added_date
    },
)

@Composable
private fun typeFilterLabel(type: MediaType?): String = stringResource(
    when (type) {
        null -> R.string.all
        MediaType.BOOK -> R.string.books
        MediaType.MOVIE -> R.string.movies
        MediaType.SERIES -> R.string.series
        MediaType.GAME -> R.string.games
    },
)
