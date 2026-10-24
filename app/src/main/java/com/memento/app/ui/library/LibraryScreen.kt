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
import com.memento.app.ui.components.MementoPrimaryButton as Button
import com.memento.app.ui.components.MementoFilterChip as FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.memento.app.ui.components.MementoBottomSheet as ModalBottomSheet
import com.memento.app.ui.components.MementoSecondaryButton as OutlinedButton
import com.memento.app.ui.components.MementoTextField as OutlinedTextField
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
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
    onOpenSettings: () -> Unit = {},
) {
    var showFilters by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var grid by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        com.memento.app.ui.components.MementoScreenHeader(
            stringResource(R.string.library), onSettings = onOpenSettings,
            subtitle = stringResource(R.string.archive_library_subtitle),
            actions = { IconButton(onClick = onOpenGlobalSearch) {
                Icon(Icons.Outlined.Search, stringResource(R.string.global_search_title))
            } },
        )
        MementoSearchField(
            value = state.query, onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.medium),
            label = stringResource(R.string.search_library),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = MementoSpacing.normal),
            horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
            items(listOf<MediaType?>(null) + MediaType.entries) { type ->
                FilterChip(selected = state.type == type, onClick = { onTypeSelected(type) },
                    label = { Text(typeFilterLabel(type)) })
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = MementoSpacing.normal),
            horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
            items(listOf<ConsumptionStatus?>(null) + ConsumptionStatus.entries) { status ->
                FilterChip(selected = state.filters.status == status, onClick = { onStatusSelected(status) },
                    label = { Text(statusFilterLabel(status)) })
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = { showFilters = true }) {
                Icon(Icons.Outlined.FilterList, null, Modifier.size(18.dp))
                Text(if (activeFilterCount(state) == 0) stringResource(R.string.filters)
                    else stringResource(R.string.filters_count, activeFilterCount(state)),
                    Modifier.padding(start = 6.dp))
            }
            Text(librarySortLabel(state.filters.sort), Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            IconButton(onClick = { grid = !grid }) {
                Icon(if (grid) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                    stringResource(if (grid) R.string.archive_list_view else R.string.archive_grid_view))
            }
        }
        val constrained = state.query.isNotBlank() || state.type != null || activeFilterCount(state) > 0
        when {
            state.isLoading -> com.memento.app.ui.components.ArchiveLoading()
            state.items.isEmpty() -> EmptyState(
                title = stringResource(if (constrained) R.string.no_search_results else R.string.library_empty_title),
                body = stringResource(if (constrained) R.string.no_library_filter_results_body else R.string.library_empty_body),
                modifier = Modifier.padding(MementoSpacing.normal),
            ) {
                if (!constrained) Button(onClick = onAdd) { Text(stringResource(R.string.add_first_work)) }
                else OutlinedButton(onClick = { onQueryChanged(""); onTypeSelected(null); onClearFilters() }) {
                    Text(stringResource(R.string.clear_filters))
                }
            }
            grid -> LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp), modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = MementoSpacing.normal, end = MementoSpacing.normal, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
            ) {
                items(state.items, key = { it.id }) { media ->
                    MediaCard(media, onClick = { onOpenMedia(media.id) },
                        supportingText = state.summaries[media.id]?.creator ?: media.releaseYear?.toString())
                }
            }
            else -> androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f), contentPadding = PaddingValues(
                    start = MementoSpacing.normal, end = MementoSpacing.normal, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { media ->
                    LibraryRow(state.summaries[media.id] ?: com.memento.app.domain.model.LibraryMediaSummary(media)) { onOpenMedia(media.id) }
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

@Composable
private fun LibraryRow(summary: com.memento.app.domain.model.LibraryMediaSummary, onClick: () -> Unit) {
    val media = summary.media
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            com.memento.app.ui.components.PosterArtwork(media.type, media.title, media.posterUrl, Modifier.size(70.dp, 105.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(listOfNotNull(summary.status?.let { statusFilterLabel(it) }, media.releaseYear?.toString()).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(media.title, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                summary.creator?.let { Text(it, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(com.memento.app.ui.components.mediaTypeLabel(media.type), style = MaterialTheme.typography.bodyMedium)
                    summary.ratingHalfStars?.let { com.memento.app.ui.components.RatingText(it) }
                }
                if (summary.tags.isNotEmpty()) Text(summary.tags.take(2).joinToString(" · ") { it.name },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
}
