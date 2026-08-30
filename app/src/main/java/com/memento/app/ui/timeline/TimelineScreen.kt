package com.memento.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ProgressEntry
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.TimelineEventType
import com.memento.app.ui.components.EmptyState
import com.memento.app.ui.components.PosterArtwork
import com.memento.app.ui.components.formatHalfStars
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.theme.MementoTheme
import com.memento.app.ui.theme.mediaTypeColor
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TimelineScreen(
    state: TimelineUiState,
    onBack: () -> Unit,
    onMediaTypeSelected: (MediaType?) -> Unit,
    onOpenMedia: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timeline_global_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxHeight().widthIn(max = 700.dp).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = MementoSpacing.huge),
            ) {
                item("header") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.medium),
                        verticalArrangement = Arrangement.spacedBy(MementoSpacing.xSmall),
                    ) {
                        Text(stringResource(R.string.timeline_header), style = MaterialTheme.typography.headlineLarge)
                        Text(
                            stringResource(R.string.timeline_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item("filters") {
                    TimelineTypeFilters(state.selectedMediaType, onMediaTypeSelected)
                }
                when {
                    state.isLoading -> item("loading") {
                        Box(Modifier.fillMaxWidth().padding(MementoSpacing.huge), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.isError -> item("error") {
                        EmptyState(
                            title = stringResource(R.string.timeline_error_title),
                            body = stringResource(R.string.timeline_error_body),
                            modifier = Modifier.padding(MementoSpacing.normal),
                        ) {
                            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                        }
                    }
                    state.events.isEmpty() -> item("empty") {
                        EmptyState(
                            title = stringResource(R.string.timeline_empty_title),
                            body = stringResource(R.string.timeline_empty_body),
                            modifier = Modifier.padding(MementoSpacing.normal),
                        )
                    }
                    else -> {
                        itemsIndexed(state.events, key = { _, event -> event.id }) { index, event ->
                            val previous = state.events.getOrNull(index - 1)
                            TimelineEventGroup(
                                event = event,
                                showYear = previous?.date?.year != event.date.year,
                                showMonth = previous?.date?.let(YearMonth::from) != YearMonth.from(event.date),
                                showDate = previous?.date != event.date,
                                onOpenMedia = onOpenMedia,
                            )
                        }
                        if (state.hasMore) {
                            item("load-${state.selectedMediaType}-${state.events.size}") {
                                LaunchedEffect(state.selectedMediaType, state.events.size) { onLoadMore() }
                                Box(Modifier.fillMaxWidth().padding(MementoSpacing.large), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineTypeFilters(selected: MediaType?, onSelected: (MediaType?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = MementoSpacing.normal, vertical = MementoSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small),
    ) {
        items(listOf<MediaType?>(null) + MediaType.entries) { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                label = { Text(timelineTypeLabel(type)) },
            )
        }
    }
}

@Composable
private fun TimelineEventGroup(
    event: CulturalTimelineEvent,
    showYear: Boolean,
    showMonth: Boolean,
    showDate: Boolean,
    onOpenMedia: (String) -> Unit,
) {
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    Column(Modifier.fillMaxWidth()) {
        if (showYear) {
            Text(
                event.date.year.toString(),
                modifier = Modifier.padding(start = MementoSpacing.normal, top = MementoSpacing.xLarge, end = MementoSpacing.normal),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (showMonth) {
            Text(
                event.date.month.getDisplayName(TextStyle.FULL, locale).uppercase(locale),
                modifier = Modifier.padding(start = MementoSpacing.normal, top = MementoSpacing.large, end = MementoSpacing.normal),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showDate) {
            Text(
                event.date.format(DateTimeFormatter.ofPattern(stringResource(R.string.timeline_date_pattern), locale)),
                modifier = Modifier.padding(start = MementoSpacing.normal, top = MementoSpacing.normal, end = MementoSpacing.normal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        TimelineEventRow(
            event = event,
            onClick = { onOpenMedia(event.mediaItemId) },
            modifier = Modifier.padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.small),
        )
    }
}

@Composable
private fun TimelineEventRow(event: CulturalTimelineEvent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.mediaTypeColor(event.mediaType)
    val importantReflection = event.eventType == TimelineEventType.FINAL_REFLECTION ||
        event.eventType == TimelineEventType.LATER_REFLECTION
    val accessibility = timelineAccessibilityLabel(event)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (importantReflection) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = accessibility }
            .padding(MementoSpacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = accent.copy(alpha = 0.14f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(eventIcon(event.eventType), contentDescription = null, modifier = Modifier.size(19.dp), tint = accent)
            }
        }
        if (event.showsPoster()) {
            PosterArtwork(
                type = event.mediaType,
                title = event.title,
                imageUrl = event.posterUrl,
                modifier = Modifier.size(width = 48.dp, height = 72.dp).clip(MaterialTheme.shapes.small).clearAndSetSemantics { },
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MementoSpacing.xSmall)) {
            Text(eventLabel(event), style = MaterialTheme.typography.labelLarge, color = accent)
            Text(
                event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            event.progress?.let { progress ->
                Text(
                    timelineProgressLabel(progress),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            event.ratingHalfStars?.let { rating ->
                Text(
                    stringResource(R.string.home_rating_compact, formatHalfStars(rating)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            event.reflectionContent?.let { content ->
                Text(
                    stringResource(R.string.quoted_reflection, content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (event.eventType == TimelineEventType.COMPLETED && event.isFavorite) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.timeline_favorite),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun CulturalTimelineEvent.showsPoster() = when (eventType) {
    TimelineEventType.STARTED,
    TimelineEventType.COMPLETED,
    TimelineEventType.FINAL_REFLECTION,
    TimelineEventType.LATER_REFLECTION,
    -> true
    TimelineEventType.PROGRESS,
    TimelineEventType.NOTE,
    -> false
}

private fun eventIcon(type: TimelineEventType): ImageVector = when (type) {
    TimelineEventType.STARTED -> Icons.Outlined.PlayArrow
    TimelineEventType.COMPLETED -> Icons.Outlined.CheckCircle
    TimelineEventType.PROGRESS -> Icons.AutoMirrored.Outlined.TrendingUp
    TimelineEventType.NOTE -> Icons.Outlined.EditNote
    TimelineEventType.FINAL_REFLECTION -> Icons.Outlined.AutoAwesome
    TimelineEventType.LATER_REFLECTION -> Icons.Outlined.History
}

@Composable
private fun eventLabel(event: CulturalTimelineEvent): String = when (event.eventType) {
    TimelineEventType.STARTED -> if (!event.isReconsumption) {
        stringResource(R.string.timeline_event_started)
    } else {
        stringResource(
            when (event.mediaType) {
                MediaType.BOOK -> R.string.timeline_event_restarted_book
                MediaType.MOVIE, MediaType.SERIES -> R.string.timeline_event_restarted_watch
                MediaType.GAME -> R.string.timeline_event_restarted_game
            },
        )
    }
    TimelineEventType.COMPLETED -> stringResource(R.string.timeline_event_completed)
    TimelineEventType.PROGRESS -> stringResource(R.string.timeline_event_progress)
    TimelineEventType.NOTE -> stringResource(R.string.timeline_event_note)
    TimelineEventType.FINAL_REFLECTION -> stringResource(R.string.timeline_event_final_reflection)
    TimelineEventType.LATER_REFLECTION -> stringResource(R.string.timeline_event_later_reflection)
}

@Composable
private fun timelineProgressLabel(progress: ProgressEntry): String = when (progress.progressType) {
    ProgressType.PAGES -> progress.currentValue?.let { current ->
        progress.totalValue?.let { total -> stringResource(R.string.pages_format, number(current), number(total)) }
            ?: stringResource(R.string.home_page_progress, number(current))
    } ?: stringResource(R.string.timeline_progress_update)
    ProgressType.EPISODE -> progress.season?.let { season ->
        progress.episode?.let { episode -> stringResource(R.string.episode_format, season, episode) }
    } ?: stringResource(R.string.timeline_progress_update)
    ProgressType.HOURS -> progress.currentValue?.let { stringResource(R.string.hours_format, number(it)) }
        ?: stringResource(R.string.timeline_progress_update)
    ProgressType.PERCENT -> progress.currentValue?.let { stringResource(R.string.percent_format, number(it)) }
        ?: stringResource(R.string.timeline_progress_update)
    ProgressType.MINUTES -> progress.currentValue?.let { stringResource(R.string.home_minutes_progress, number(it)) }
        ?: stringResource(R.string.timeline_progress_update)
}

@Composable
private fun timelineAccessibilityLabel(event: CulturalTimelineEvent): String {
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val date = event.date.format(
        DateTimeFormatter.ofPattern(stringResource(R.string.timeline_accessibility_date_pattern), locale),
    )
    val rating = event.ratingHalfStars?.let {
        stringResource(R.string.timeline_rating_accessibility_suffix, formatHalfStars(it))
    }.orEmpty()
    return stringResource(R.string.timeline_event_accessibility, date, eventLabel(event), event.title, rating)
}

@Composable
private fun timelineTypeLabel(type: MediaType?): String = stringResource(
    when (type) {
        null -> R.string.all
        MediaType.BOOK -> R.string.books
        MediaType.MOVIE -> R.string.movies
        MediaType.SERIES -> R.string.series
        MediaType.GAME -> R.string.games
    },
)

private fun number(value: Double): String = DecimalFormat("0.#").format(value)

@Preview(name = "Historia · contenido", widthDp = 360, heightDp = 760)
@Composable
private fun TimelineContentPreview() {
    MementoTheme(ThemeMode.LIGHT) {
        TimelineScreen(
            state = TimelineUiState(events = timelinePreviewEvents, isLoading = false),
            onBack = {},
            onMediaTypeSelected = {},
            onOpenMedia = {},
            onLoadMore = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Historia · 320dp · Font 1.5", widthDp = 320, heightDp = 700, fontScale = 1.5f)
@Composable
private fun TimelineLargeTextPreview() {
    MementoTheme(ThemeMode.DARK) {
        TimelineScreen(
            state = TimelineUiState(events = timelinePreviewEvents.takeLast(1), isLoading = false),
            onBack = {},
            onMediaTypeSelected = {},
            onOpenMedia = {},
            onLoadMore = {},
            onRetry = {},
        )
    }
}

private val timelinePreviewEvents = listOf(
    CulturalTimelineEvent(
        id = "completed:c1",
        date = LocalDate.of(2026, 8, 29),
        occurredAt = null,
        mediaItemId = "m1",
        consumptionId = "c1",
        mediaType = MediaType.GAME,
        title = "Clair Obscur: Expedition 33",
        posterUrl = null,
        eventType = TimelineEventType.COMPLETED,
        ratingHalfStars = 10,
        isFavorite = true,
    ),
    CulturalTimelineEvent(
        id = "reflection:r1",
        date = LocalDate.of(2025, 12, 14),
        occurredAt = Instant.parse("2025-12-14T20:00:00Z"),
        mediaItemId = "m2",
        consumptionId = "c2",
        mediaType = MediaType.GAME,
        title = "The Last of Us Part II: una historia con un título deliberadamente largo",
        posterUrl = null,
        eventType = TimelineEventType.LATER_REFLECTION,
        reflectionId = "r1",
        reflectionContent = "Dos años después sigo pensando en cómo cambia nuestra lectura de una historia cuando también hemos cambiado nosotros.",
    ),
)
