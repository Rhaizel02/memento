package com.memento.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Update
import com.memento.app.ui.components.MementoPrimaryButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.memento.app.R
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.TimelineEventType
import com.memento.app.domain.recommendation.Recommendation
import com.memento.app.domain.recommendation.RecommendationReason
import com.memento.app.domain.recommendation.RecommendationCategory
import com.memento.app.domain.remember.RememberCandidate
import com.memento.app.ui.components.PosterArtwork
import com.memento.app.ui.components.icon
import com.memento.app.ui.components.mediaTypeLabel
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.theme.mediaTypeColor
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
internal fun HomeSectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge)
}

@Composable
internal fun HomeEmptyState(onAdd: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MementoSpacing.large),
            verticalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
            horizontalAlignment = Alignment.Start,
        ) {
            Icon(
                Icons.Outlined.AutoStories,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.home_empty_title_v2), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.home_empty_body_v2),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAdd) { Text(stringResource(R.string.add_first_work)) }
            Text(
                stringResource(R.string.home_empty_remember_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun OnThisDayCard(memory: OnThisDayMemory, onClick: () -> Unit) {
    val event = memory.event
    val accent = MaterialTheme.mediaTypeColor(event.mediaType)
    val age = pluralStringResource(R.plurals.on_this_day_years_ago, memory.yearsAgo, memory.yearsAgo)
    val accessibilityLabel = stringResource(R.string.on_this_day_accessibility, event.title, event.date.year, age)
    Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
        HomeSectionHeader(stringResource(R.string.on_this_day_title))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = accessibilityLabel }
                .clickable(role = Role.Button, onClick = onClick),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(MementoSpacing.normal),
                horizontalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
            ) {
                Box(Modifier.width(78.dp).aspectRatio(2f / 3f).clearAndSetSemantics { }) {
                    PosterArtwork(event.mediaType, event.title, event.posterUrl, Modifier.fillMaxSize())
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MementoSpacing.small),
                ) {
                    Text(
                        stringResource(R.string.on_this_day_year_and_age, event.date.year, age),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        event.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(event.mediaType.icon(), contentDescription = null, modifier = Modifier.size(16.dp), tint = accent)
                        Text(onThisDayEventLabel(event), style = MaterialTheme.typography.labelLarge, color = accent)
                    }
                    event.ratingHalfStars?.let { CompactRating(it) }
                    event.reflectionContent?.takeIf(String::isNotBlank)?.let { reflection ->
                        Text(
                            stringResource(R.string.quoted_reflection, reflection),
                            style = com.memento.app.ui.theme.EditorialQuoteStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun onThisDayEventLabel(event: CulturalTimelineEvent): String = when (event.eventType) {
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
    TimelineEventType.COMPLETED -> stringResource(
        if (event.isReconsumption) R.string.timeline_event_recompleted else R.string.timeline_event_completed,
    )
    TimelineEventType.PROGRESS -> stringResource(R.string.timeline_event_progress)
    TimelineEventType.NOTE -> stringResource(R.string.timeline_event_note)
    TimelineEventType.QUOTE -> stringResource(R.string.timeline_event_quote)
    TimelineEventType.FINAL_REFLECTION -> stringResource(R.string.timeline_event_final_reflection)
    TimelineEventType.LATER_REFLECTION -> stringResource(R.string.timeline_event_later_reflection)
}

@Composable
internal fun RememberEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MementoSpacing.normal),
            horizontalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.xSmall)) {
                Text(stringResource(R.string.remember_empty_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.remember_empty_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun RememberHero(memory: RememberCandidate, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoStories, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.remember), Modifier.weight(1f).padding(start = 8.dp),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(rememberAge(memory.completedDate), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PosterArtwork(memory.mediaType, memory.title, memory.posterUrl, Modifier.width(48.dp).height(70.dp))
                Column(Modifier.weight(1f)) {
                    Text(memory.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(memory.completedDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.forLanguageTag("es"))),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small) {
                Text("“" + memory.reflectionContent + "”", Modifier.fillMaxWidth().padding(16.dp),
                    style = com.memento.app.ui.theme.EditorialQuoteStyle, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.remember_open), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.padding(start = 8.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
internal fun InProgressMediaCard(
    item: HomeMediaItem,
    onClick: () -> Unit,
    onUpdateProgress: () -> Unit = {},
    onAddNote: () -> Unit = {},
) {
    val updateLabel = stringResource(R.string.quick_update_progress_accessibility, item.title)
    val noteLabel = stringResource(R.string.quick_add_note_accessibility, item.title)
    Surface(
        modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HomePoster(item, Modifier.width(68.dp).height(102.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MediaTypeBadge(item.type)
                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    metadataLine(item)?.let { Text(it, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    if (item.genres.isNotEmpty()) Text(item.genres.take(2).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    item.ratingHalfStars?.let { CompactRating(it) }
                }
            }
            item.progress?.let { progress ->
                Text(progressLabel(progress), style = MaterialTheme.typography.labelLarge)
                progress.fraction?.let { fraction ->
                    LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                }
            }
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onUpdateProgress, modifier = Modifier.semantics { contentDescription = updateLabel }) {
                    Icon(Icons.Outlined.Update, null, Modifier.size(18.dp))
                    Text(stringResource(R.string.quick_update_progress), Modifier.padding(start = 6.dp))
                }
                TextButton(onClick = onAddNote, modifier = Modifier.semantics { contentDescription = noteLabel }) {
                    Icon(Icons.Outlined.EditNote, null, Modifier.size(18.dp))
                    Text(stringResource(R.string.quick_add_note), Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
internal fun RecentMediaCard(item: HomeMediaItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.small),
    ) {
        HomePoster(item, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
        Text(
            item.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        item.ratingHalfStars?.let { CompactRating(it) }
        if (item.ratingHalfStars == null) {
            item.releaseYear?.let { year ->
                Text(year.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HomePoster(item: HomeMediaItem, modifier: Modifier) {
    Box(modifier.clearAndSetSemantics { }) {
        PosterArtwork(
            type = item.type,
            title = item.title,
            imageUrl = item.posterUrl,
            modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium),
        )
        if (item.isFavorite) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(MementoSpacing.small),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp).size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun MediaTypeBadge(type: MediaType) {
    val accent = MaterialTheme.mediaTypeColor(type)
    Surface(color = accent.copy(alpha = 0.14f), shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = MementoSpacing.small, vertical = MementoSpacing.xSmall),
            horizontalArrangement = Arrangement.spacedBy(MementoSpacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(type.icon(), contentDescription = null, modifier = Modifier.size(14.dp), tint = accent)
            Text(mediaTypeLabel(type), style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}

@Composable
private fun CompactRating(ratingHalfStars: Int) {
    val rating = formatRating(ratingHalfStars)
    val description = stringResource(R.string.rating_accessibility, rating)
    Text(
        stringResource(R.string.home_rating_compact, rating),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
internal fun HomeRecommendationCard(recommendation: Recommendation, onClick: () -> Unit, modifier: Modifier = Modifier) {
    com.memento.app.ui.components.RecommendationPreview(recommendation, onClick, modifier)
}

@Composable
internal fun RecommendationLearningState(
    profileReady: Boolean,
    onOpenDiscover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(if (profileReady) R.string.recommendation_none_now else R.string.recommendation_learning),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onOpenDiscover) { Text(stringResource(R.string.discover)) }
    }
}

@Composable
internal fun YearSummaryCard(
    year: Int,
    completedByType: Map<MediaType, Int>,
    onOpenStats: () -> Unit,
    onOpenWrapped: () -> Unit,
) {
    val visibleTypes = MediaType.entries.filter { (completedByType[it] ?: 0) > 0 }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(MementoSpacing.large)) {
            Text(stringResource(R.string.year_summary_title_v2, year), style = MaterialTheme.typography.titleLarge)
            visibleTypes.chunked(2).forEachIndexed { index, rowTypes ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = if (index == 0) MementoSpacing.normal else MementoSpacing.small),
                    horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
                ) {
                    rowTypes.forEach { type ->
                        YearMetric(type, completedByType.getValue(type), Modifier.weight(1f))
                    }
                    if (rowTypes.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = MementoSpacing.normal),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onOpenStats) { Text(stringResource(R.string.open_statistics)) }
                Button(onClick = onOpenWrapped) {
                    Text(stringResource(R.string.open_year_wrapped, year))
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(start = MementoSpacing.small).size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun YearMetric(type: MediaType, count: Int, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.mediaTypeColor(type)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(type.icon(), contentDescription = null, modifier = Modifier.size(22.dp), tint = accent)
        Column {
            Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(mediaTypePluralLabel(type), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun metadataLine(item: HomeMediaItem): String? = listOfNotNull(
    item.creator?.takeIf(String::isNotBlank),
    item.releaseYear?.toString(),
).takeIf(List<String>::isNotEmpty)?.joinToString(" · ")

@Composable
private fun progressLabel(progress: HomeProgress): String = when (progress) {
    is HomeProgress.Pages -> progress.total?.let {
        stringResource(R.string.home_pages_progress, number(progress.current), number(it))
    } ?: stringResource(R.string.home_page_progress, number(progress.current))
    is HomeProgress.Episode -> stringResource(R.string.episode_format, progress.season, progress.episode)
    is HomeProgress.Game -> progress.percent?.let {
        stringResource(R.string.home_game_progress, number(progress.hours), number(it))
    } ?: stringResource(R.string.hours_format, number(progress.hours))
    is HomeProgress.Minutes -> stringResource(R.string.home_minutes_progress, number(progress.minutes))
    is HomeProgress.Percent -> stringResource(R.string.percent_format, number(progress.percent))
}

@Composable
private fun rememberAge(completedDate: LocalDate): String {
    val today = LocalDate.now()
    val years = ChronoUnit.YEARS.between(completedDate, today).coerceAtLeast(0)
    if (years > 0) return pluralStringResource(R.plurals.remember_years_ago, years.toInt(), years)
    val months = ChronoUnit.MONTHS.between(completedDate, today).coerceAtLeast(0)
    if (months > 0) return pluralStringResource(R.plurals.remember_months_ago, months.toInt(), months)
    val days = ChronoUnit.DAYS.between(completedDate, today).coerceAtLeast(0)
    return if (days == 0L) stringResource(R.string.remember_completed_today)
    else pluralStringResource(R.plurals.remember_days_ago, days.toInt(), days)
}

@Composable
private fun mediaTypePluralLabel(type: MediaType): String = stringResource(
    when (type) {
        MediaType.BOOK -> R.string.books
        MediaType.MOVIE -> R.string.movies
        MediaType.SERIES -> R.string.series
        MediaType.GAME -> R.string.games
    },
)

private fun number(value: Double): String = DecimalFormat("0.#").format(value)
private fun formatRating(halfStars: Int): String = if (halfStars % 2 == 0) "${halfStars / 2}" else "${halfStars / 2}.5"
