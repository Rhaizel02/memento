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
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Button
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
internal fun CulturalHistoryCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MementoSpacing.normal),
            horizontalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    modifier = Modifier.padding(MementoSpacing.medium).size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MementoSpacing.xSmall)) {
                Text(stringResource(R.string.timeline_header), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.timeline_home_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.timeline_home_action),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                        fontWeight = FontWeight.SemiBold,
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
                            style = MaterialTheme.typography.bodyMedium,
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
    val imageUrl = memory.backdropUrl?.takeIf(String::isNotBlank)
        ?: memory.posterUrl?.takeIf(String::isNotBlank)
    val accent = MaterialTheme.mediaTypeColor(memory.mediaType)
    val accessibilityLabel = stringResource(R.string.remember_card_accessibility, memory.title)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .semantics { contentDescription = accessibilityLabel },
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, MaterialTheme.colorScheme.scrim.copy(alpha = 0.86f)),
                                startY = 60f,
                            ),
                        ),
                )
                RememberHeroContent(
                    memory = memory,
                    foreground = Color.White,
                    secondary = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(accent.copy(alpha = 0.34f), MaterialTheme.colorScheme.surfaceVariant),
                            ),
                        ),
                )
                Icon(
                    memory.mediaType.icon(),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).padding(MementoSpacing.large).size(88.dp),
                    tint = accent.copy(alpha = 0.42f),
                )
                RememberHeroContent(
                    memory = memory,
                    foreground = MaterialTheme.colorScheme.onSurface,
                    secondary = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun RememberHeroContent(
    memory: RememberCandidate,
    foreground: Color,
    secondary: Color,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.padding(MementoSpacing.large),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            stringResource(R.string.remember).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = secondary,
        )
        Text(
            memory.title,
            modifier = Modifier.padding(top = MementoSpacing.small),
            style = MaterialTheme.typography.headlineMedium,
            color = foreground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            rememberAge(memory.completedDate),
            modifier = Modifier.padding(top = MementoSpacing.xSmall),
            style = MaterialTheme.typography.bodySmall,
            color = secondary,
        )
        Text(
            stringResource(R.string.quoted_reflection, memory.reflectionContent),
            modifier = Modifier.padding(top = MementoSpacing.normal),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = foreground,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = MementoSpacing.normal),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.remember_open), style = MaterialTheme.typography.labelLarge, color = foreground)
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(start = MementoSpacing.small).size(20.dp),
                tint = foreground,
            )
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
    val accent = MaterialTheme.mediaTypeColor(item.type)
    val updateProgressLabel = stringResource(R.string.quick_update_progress_accessibility, item.title)
    val addNoteLabel = stringResource(R.string.quick_add_note_accessibility, item.title)
    Card(
        onClick = onClick,
        modifier = Modifier.width(308.dp).heightIn(min = 222.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 222.dp).padding(MementoSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MementoSpacing.xSmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
            ) {
                HomePoster(item, Modifier.width(112.dp).aspectRatio(2f / 3f))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MementoSpacing.small),
                ) {
                    MediaTypeBadge(item.type)
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    metadataLine(item)?.let { metadata ->
                        Text(
                            metadata,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (item.genres.isNotEmpty()) GenreRow(item.genres, item.additionalGenreCount)
                    item.progress?.let { progress ->
                        Text(progressLabel(progress), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        progress.fraction?.let { fraction ->
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                color = accent,
                                trackColor = accent.copy(alpha = 0.16f),
                            )
                        }
                    } ?: Text(
                        stringResource(R.string.status_in_progress),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    item.ratingHalfStars?.let { CompactRating(it) }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onUpdateProgress,
                    modifier = Modifier.semantics { contentDescription = updateProgressLabel },
                ) {
                    Icon(
                        Icons.Outlined.Update,
                        contentDescription = null,
                        modifier = Modifier.padding(end = MementoSpacing.xSmall).size(18.dp),
                    )
                    Text(stringResource(R.string.quick_update_progress))
                }
                TextButton(
                    onClick = onAddNote,
                    modifier = Modifier.semantics { contentDescription = addNoteLabel },
                ) {
                    Icon(
                        Icons.Outlined.EditNote,
                        contentDescription = null,
                        modifier = Modifier.padding(end = MementoSpacing.xSmall).size(18.dp),
                    )
                    Text(stringResource(R.string.quick_add_note))
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
            fontWeight = FontWeight.SemiBold,
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
private fun GenreRow(genres: List<String>, additionalCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MementoSpacing.xSmall),
    ) {
        genres.take(2).forEach { genre -> MediaGenreChip(genre, Modifier.weight(1f, fill = false)) }
        if (additionalCount > 0) MediaGenreChip(stringResource(R.string.additional_genres, additionalCount))
    }
}

@Composable
private fun MediaGenreChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(max = 78.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
internal fun HomeRecommendationCard(
    recommendation: Recommendation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val candidate = recommendation.candidate
    val accessibilityLabel = stringResource(R.string.recommendation_card_accessibility, candidate.title)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = accessibilityLabel },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MementoSpacing.normal),
            horizontalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
        ) {
            Box(Modifier.width(92.dp).aspectRatio(2f / 3f).clearAndSetSemantics { }) {
                PosterArtwork(candidate.type, candidate.title, candidate.posterUrl, Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                MediaTypeBadge(candidate.type)
                Text(
                    candidate.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val context = buildList {
                    addAll(candidate.genres.take(2))
                    candidate.releaseYear?.let { add(it.toString()) }
                }.joinToString(" · ")
                if (context.isNotEmpty()) {
                    Text(
                        context,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    compatibilityLabel(recommendation.affinityScore),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                recommendation.reasons.firstOrNull()?.let { reason ->
                    Text(
                        recommendationReason(reason),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
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
            TextButton(
                onClick = onOpenStats,
                modifier = Modifier.align(Alignment.End).padding(top = MementoSpacing.small),
            ) {
                Text(stringResource(R.string.open_statistics))
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(start = MementoSpacing.small).size(18.dp),
                )
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
            Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
private fun compatibilityLabel(affinity: Int): String = stringResource(
    when {
        affinity >= 85 -> R.string.compatibility_high
        affinity >= 70 -> R.string.compatibility_medium
        else -> R.string.compatibility_possible
    },
)

@Composable
private fun recommendationReason(reason: RecommendationReason): String = when (reason) {
    is RecommendationReason.Genre -> stringResource(R.string.reason_genre, reason.name)
    is RecommendationReason.Creator -> stringResource(R.string.reason_creator, reason.name)
    is RecommendationReason.MediaKind -> stringResource(R.string.reason_type, mediaTypeLabel(reason.type))
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
