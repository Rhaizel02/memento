package com.memento.app.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.LibraryMediaSummary
import com.memento.app.domain.model.LibrarySort
import com.memento.app.ui.components.PosterArtwork
import com.memento.app.ui.components.RatingText
import com.memento.app.ui.components.mediaTypeLabel

/** Secondary text tabs deliberately differ from the filled media-type chips. */
@Composable
internal fun LibraryStatusTabs(selected: ConsumptionStatus?, onSelected: (ConsumptionStatus?) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    LazyRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items(listOf<ConsumptionStatus?>(null) + ConsumptionStatus.entries) { status ->
            Box(
                Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
                    .selectable(selected == status, role = Role.RadioButton, onClick = { onSelected(status) })
                    .drawBehind {
                        if (selected == status) drawLine(accent, Offset(0f, size.height - 1.dp.toPx()),
                            Offset(size.width, size.height - 1.dp.toPx()), strokeWidth = 2.dp.toPx())
                    }.padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(statusFilterLabel(status), style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected == status) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected == status) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1)
            }
        }
    }
}

@Composable
internal fun LibraryCollectionToolbar(
    state: LibraryUiState, grid: Boolean, onFilters: () -> Unit,
    onSort: (LibrarySort) -> Unit, onToggleView: () -> Unit,
) {
    var showSort by rememberSaveable { mutableStateOf(false) }
    val count = advancedFilterCount(state)
    val filtersDescription = stringResource(R.string.library_advanced_filters_count, count)
    val sortDescription = stringResource(R.string.library_sort_description, librarySortLabel(state.filters.sort))
    val filtersAction: @Composable () -> Unit = {
        TextButton(onClick = onFilters, modifier = Modifier.heightIn(min = 48.dp)
            .semantics { contentDescription = filtersDescription }) {
            Icon(Icons.Outlined.FilterList, null, Modifier.size(18.dp))
            Text(if (count == 0) stringResource(R.string.filters) else stringResource(R.string.filters_count, count),
                Modifier.padding(start = 6.dp), maxLines = 1)
        }
    }
    val sortAction: @Composable () -> Unit = {
        Box(Modifier.fillMaxWidth()) {
            TextButton(onClick = { showSort = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("library_sort").semantics { contentDescription = sortDescription }) {
                Text(librarySortLabel(state.filters.sort), Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
            }
            DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                LibrarySort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(librarySortLabel(sort)) },
                        onClick = { onSort(sort); showSort = false },
                        modifier = Modifier.semantics { selected = state.filters.sort == sort },
                        leadingIcon = { if (state.filters.sort == sort) Icon(Icons.Outlined.Check, null) else Spacer(Modifier.size(24.dp)) },
                    )
                }
            }
        }
    }
    val viewAction: @Composable () -> Unit = {
        IconButton(onClick = onToggleView, modifier = Modifier.testTag("library_view_toggle")) {
            Icon(if (grid) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                stringResource(if (grid) R.string.archive_list_view else R.string.archive_grid_view))
        }
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f) {
                // Keep sorting readable and every action tappable on narrow / large-text layouts.
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) { filtersAction(); viewAction() }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    sortAction()
                }
            } else {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    filtersAction()
                    VerticalDivider(Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Box(Modifier.weight(1f)) { sortAction() }
                    viewAction()
                }
            }
        }
    }
}

@Composable
internal fun LibraryMediaItem(summary: LibraryMediaSummary, grid: Boolean, onClick: () -> Unit) {
    val media = summary.media
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (grid) Column {
            PosterArtwork(media.type, media.title, media.posterUrl,
                Modifier.fillMaxWidth().aspectRatio(2f / 3f).clearAndSetSemantics { })
            LibraryItemMetadata(summary, Modifier.padding(12.dp))
        } else Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PosterArtwork(media.type, media.title, media.posterUrl, Modifier.size(68.dp, 102.dp).clearAndSetSemantics { })
            LibraryItemMetadata(summary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LibraryItemMetadata(summary: LibraryMediaSummary, modifier: Modifier) {
    val media = summary.media
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(media.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        summary.creator?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(listOfNotNull(mediaTypeLabel(media.type), media.releaseYear?.toString()).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            summary.status?.let { Text(statusFilterLabel(it), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
            summary.ratingHalfStars?.let { RatingText(it) }
            if (media.isFavorite) Icon(Icons.Outlined.Favorite, stringResource(R.string.library_favorite_label),
                Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        if (summary.tags.isNotEmpty()) Text(summary.tags.take(2).joinToString(" · ") { it.name },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
