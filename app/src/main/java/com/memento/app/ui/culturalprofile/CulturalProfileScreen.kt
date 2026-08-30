package com.memento.app.ui.culturalprofile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.culturalprofile.CulturalInsight
import com.memento.app.domain.culturalprofile.CulturalMediaTypeMetric
import com.memento.app.domain.culturalprofile.CulturalMetric
import com.memento.app.domain.culturalprofile.CulturalPeriod
import com.memento.app.domain.culturalprofile.CulturalPeriodKind
import com.memento.app.domain.culturalprofile.CulturalProfile
import com.memento.app.ui.components.StaticTag
import com.memento.app.ui.components.mediaTypeLabel
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.theme.mediaTypeColor
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun CulturalProfileScreen(state: CulturalProfileUiState, onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MementoSpacing.normal,
            top = MementoSpacing.normal,
            end = MementoSpacing.normal,
            bottom = MementoSpacing.xLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.large),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(stringResource(R.string.cultural_profile), style = MaterialTheme.typography.headlineLarge)
            }
        }
        if (state.isLoading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            return@LazyColumn
        }
        item { ProfileSummary(state.profile) }
        if (state.profile.isTakingShape) {
            item {
                Text(
                    stringResource(R.string.cultural_profile_taking_shape),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.profile.insights) { insight -> CulturalInsightCard(insight) }
        if (state.profile.periods.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.cultural_periods_title)) }
            items(state.profile.periods) { period -> CulturalPeriodCard(period) }
        }
    }
}

@Composable
private fun ProfileSummary(profile: CulturalProfile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(MementoSpacing.large),
            verticalArrangement = Arrangement.spacedBy(MementoSpacing.large),
        ) {
            Text(stringResource(R.string.cultural_profile_yours), style = MaterialTheme.typography.headlineMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MementoSpacing.normal)) {
                SummaryValue(
                    pluralStringResource(
                        R.plurals.cultural_works_count,
                        profile.summary.workCount,
                        profile.summary.workCount,
                    ),
                    Modifier.weight(1f),
                )
                SummaryValue(
                    profile.summary.averageRating?.let { stringResource(R.string.cultural_average_rating, it) }
                        ?: stringResource(R.string.cultural_unrated),
                    Modifier.weight(1f),
                )
                SummaryValue(
                    stringResource(R.string.cultural_favorites_summary, profile.summary.favoriteCount),
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryValue(value: String, modifier: Modifier = Modifier) {
    Text(
        value,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun CulturalInsightCard(insight: CulturalInsight) {
    when (insight) {
        is CulturalInsight.BestRatedGenre -> EditorialCard(
            icon = Icons.Outlined.StarOutline,
            eyebrow = stringResource(R.string.cultural_best_genre),
            headline = insight.genre.label,
            body = stringResource(R.string.cultural_rating_sample, insight.genre.averageRating ?: 0.0, insight.genre.ratedCount),
        )
        is CulturalInsight.PresentGenres -> RankingCard(
            title = stringResource(R.string.cultural_present_genres),
            rows = insight.genres,
        )
        is CulturalInsight.MostPresentCreator -> EditorialCard(
            icon = Icons.Outlined.PersonOutline,
            eyebrow = stringResource(R.string.cultural_most_present_creator),
            headline = insight.creator.label,
            body = pluralStringResource(R.plurals.cultural_works_count, insight.creator.count, insight.creator.count),
        )
        is CulturalInsight.BestRatedCreator -> EditorialCard(
            icon = Icons.Outlined.StarOutline,
            eyebrow = stringResource(R.string.cultural_best_rated_creator),
            headline = insight.creator.label,
            body = stringResource(R.string.cultural_rating_sample, insight.creator.averageRating ?: 0.0, insight.creator.ratedCount),
        )
        is CulturalInsight.MediaTypes -> MediaTypesCard(insight.items, insight.bestRated)
        is CulturalInsight.PersonalTags -> TagsCard(insight)
        is CulturalInsight.FavoriteGenre -> EditorialCard(
            icon = Icons.Outlined.FavoriteBorder,
            eyebrow = stringResource(R.string.cultural_favorite_pattern),
            headline = insight.genre,
            body = stringResource(R.string.cultural_favorite_genre_body, insight.genreCount, insight.favoriteCount),
        )
        is CulturalInsight.MostActiveMonth -> EditorialCard(
            icon = Icons.Outlined.CalendarMonth,
            eyebrow = stringResource(R.string.cultural_most_active_month),
            headline = formatMonth(insight.month),
            body = pluralStringResource(R.plurals.cultural_completed_works_count, insight.completedWorks, insight.completedWorks),
        )
        is CulturalInsight.MostActiveYear -> EditorialCard(
            icon = Icons.Outlined.CalendarMonth,
            eyebrow = stringResource(R.string.cultural_most_active_year),
            headline = insight.year.toString(),
            body = pluralStringResource(R.plurals.cultural_completed_works_count, insight.completedWorks, insight.completedWorks),
        )
        is CulturalInsight.MediaTypeYearChange -> ComparisonCard(
            stringResource(
                R.string.cultural_type_year_change,
                insight.currentYear,
                abs(insight.percentChange),
                changeDirection(insight.percentChange),
                mediaTypeLabel(insight.mediaType).lowercase(),
                insight.previousYear,
            ),
        )
        is CulturalInsight.TotalYearChange -> ComparisonCard(
            stringResource(
                R.string.cultural_total_year_change,
                insight.currentYear,
                abs(insight.percentChange),
                changeDirection(insight.percentChange),
                insight.previousYear,
            ),
        )
        is CulturalInsight.AverageRatingYearChange -> ComparisonCard(
            stringResource(
                R.string.cultural_rating_year_change,
                insight.previousRating,
                insight.previousYear,
                insight.currentRating,
                insight.currentYear,
            ),
        )
    }
}

@Composable
private fun EditorialCard(icon: ImageVector, eyebrow: String, headline: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(MementoSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RankingCard(title: String, rows: List<CulturalMetric>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(MementoSpacing.large), verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
            SectionTitle(title)
            val maximum = rows.maxOfOrNull(CulturalMetric::count)?.coerceAtLeast(1) ?: 1
            rows.forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.xSmall)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(row.label)
                        Text(
                            pluralStringResource(R.plurals.cultural_works_count, row.count, row.count),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { row.count.toFloat() / maximum },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaTypesCard(rows: List<CulturalMediaTypeMetric>, bestRated: CulturalMediaTypeMetric?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(MementoSpacing.large), verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
            SectionTitle(stringResource(R.string.cultural_media_types))
            bestRated?.let {
                Text(
                    stringResource(R.string.cultural_best_medium, mediaTypeLabel(it.mediaType), it.averageRating ?: 0.0),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            val maximum = rows.maxOfOrNull(CulturalMediaTypeMetric::count)?.coerceAtLeast(1) ?: 1
            rows.forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.xSmall)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(mediaTypeLabel(row.mediaType))
                        Text(
                            buildString {
                                append(row.count)
                                row.averageRating?.let { append(" · ★%.1f".format(Locale.forLanguageTag("es"), it)) }
                            },
                        )
                    }
                    LinearProgressIndicator(
                        progress = { row.count.toFloat() / maximum },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.mediaTypeColor(row.mediaType),
                    )
                }
            }
        }
    }
}

@Composable
private fun TagsCard(insight: CulturalInsight.PersonalTags) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(MementoSpacing.large), verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
            Row(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                SectionTitle(stringResource(R.string.cultural_personal_tags))
            }
            Text(stringResource(R.string.cultural_most_used_tag), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small), verticalAlignment = Alignment.CenterVertically) {
                StaticTag(insight.mostUsed.label)
                Text(pluralStringResource(R.plurals.cultural_works_count, insight.mostUsed.count, insight.mostUsed.count))
            }
            insight.bestRated?.let { best ->
                Text(
                    stringResource(R.string.cultural_tag_rating, best.label, best.averageRating ?: 0.0, best.ratedCount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ComparisonCard(text: String) {
    EditorialCard(
        icon = Icons.Outlined.AutoAwesome,
        eyebrow = stringResource(R.string.cultural_compared_with_previous),
        headline = text,
        body = stringResource(R.string.cultural_comparison_context),
    )
}

@Composable
private fun CulturalPeriodCard(period: CulturalPeriod) {
    val accent = period.mediaType?.let { MaterialTheme.mediaTypeColor(it) } ?: MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, accent),
    ) {
        Column(Modifier.padding(MementoSpacing.large), verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
            Text(periodTitle(period), style = MaterialTheme.typography.headlineSmall, color = accent)
            Text(
                stringResource(R.string.cultural_period_dates, formatMonth(period.from), formatMonth(period.until)),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                period.averageRating?.let {
                    stringResource(
                        R.string.cultural_period_body_rated,
                        period.matchingWorks,
                        period.totalWorks,
                        it,
                    )
                } ?: stringResource(R.string.cultural_period_body, period.matchingWorks, period.totalWorks),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun periodTitle(period: CulturalPeriod): String = when (period.kind) {
    CulturalPeriodKind.GENRE -> stringResource(R.string.cultural_period_genre, period.label)
    CulturalPeriodKind.TAG -> stringResource(R.string.cultural_period_tag, period.label)
    CulturalPeriodKind.MEDIA_TYPE -> stringResource(
        R.string.cultural_period_type,
        period.mediaType?.let { mediaTypeLabel(it).lowercase() } ?: period.label.lowercase(),
    )
}

@Composable
private fun changeDirection(change: Int): String = stringResource(
    if (change >= 0) R.string.cultural_more else R.string.cultural_less,
)

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

private fun formatMonth(month: YearMonth): String {
    val locale = Locale.forLanguageTag("es")
    return month.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", locale))
        .replaceFirstChar { it.titlecase(locale) }
}
