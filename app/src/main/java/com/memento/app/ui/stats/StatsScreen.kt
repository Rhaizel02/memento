package com.memento.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.memento.app.R
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.stats.RankedStat
import com.memento.app.ui.components.mediaTypeLabel
import com.memento.app.ui.theme.MementoSpacing
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(
    state: StatsUiState,
    onBack: () -> Unit,
    onSelectYear: (Int) -> Unit,
    onOpenWrapped: (Int) -> Unit,
) {
    val summary = state.summary ?: return
    val yearIndex = state.availableYears.indexOf(state.selectedYear)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MementoSpacing.normal),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.large),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(stringResource(R.string.statistics), style = MaterialTheme.typography.headlineLarge)
                IconButton(onClick = { onOpenWrapped(state.selectedYear) }) {
                    Text(stringResource(R.string.wrapped_short), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(
                    enabled = yearIndex >= 0 && yearIndex < state.availableYears.lastIndex,
                    onClick = { onSelectYear(state.availableYears[yearIndex + 1]) },
                ) { Icon(Icons.Outlined.ChevronLeft, contentDescription = stringResource(R.string.previous_year)) }
                Text(state.selectedYear.toString(), style = MaterialTheme.typography.displaySmall)
                IconButton(
                    enabled = yearIndex > 0,
                    onClick = { onSelectYear(state.availableYears[yearIndex - 1]) },
                ) { Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.next_year)) }
            }
        }
        item {
            StatCard(
                title = stringResource(R.string.completed_works),
                value = summary.completedWorks.toString(),
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
                    Text(stringResource(R.string.by_media_type), style = MaterialTheme.typography.titleLarge)
                    MediaType.entries.forEach { type ->
                        val count = summary.completedByType[type] ?: 0
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(mediaTypeLabel(type))
                            Text(count.toString(), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        item {
            val measures = listOfNotNull(
                summary.pagesRead?.let { stringResource(R.string.pages_read_format, it) },
                summary.gameHours?.let { stringResource(R.string.game_hours_format, it) },
                summary.episodesWatched?.let { stringResource(R.string.episodes_watched_format, it) },
                summary.movieMinutes?.let { stringResource(R.string.movie_hours_format, it / 60.0) },
            )
            if (measures.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                        Text(stringResource(R.string.time_and_pages), style = MaterialTheme.typography.titleLarge)
                        measures.forEach { Text(it) }
                    }
                }
            }
        }
        if (summary.completedWorks > 0) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                        Text(stringResource(R.string.your_signals), style = MaterialTheme.typography.titleLarge)
                        summary.averageRatingHalfStars?.let { Text(stringResource(R.string.average_rating_format, it / 2.0)) }
                        Text(stringResource(R.string.favorites_count, summary.favoriteWorks))
                        Text(stringResource(R.string.dropped_count, summary.droppedWorks))
                        Text(stringResource(R.string.revisits_count, summary.revisits))
                        Text(stringResource(R.string.reflections_count, summary.reflectionCount))
                    }
                }
            }
        }
        if (summary.topConsumedGenres.isNotEmpty()) item {
            RankingCard(stringResource(R.string.most_consumed_genres), summary.topConsumedGenres)
        }
        if (summary.topRatedGenres.isNotEmpty()) item {
            RankingCard(stringResource(R.string.best_rated_genres), summary.topRatedGenres, showRating = true)
        }
        if (summary.frequentCreators.isNotEmpty()) item {
            RankingCard(stringResource(R.string.frequent_creators), summary.frequentCreators)
        }
        if (summary.monthlyCompletions.values.any { it > 0 }) item {
            Card(Modifier.fillMaxWidth()) {
                val max = summary.monthlyCompletions.values.maxOrNull()?.coerceAtLeast(1) ?: 1
                Column(Modifier.padding(MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                    Text(stringResource(R.string.year_timeline), style = MaterialTheme.typography.titleLarge)
                    summary.monthlyCompletions.filterValues { it > 0 }.forEach { (month, count) ->
                        Text(Month.of(month).getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es")))
                        LinearProgressIndicator(progress = { count.toFloat() / max }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item {
            Button(onClick = { onOpenWrapped(state.selectedYear) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.open_year_wrapped, state.selectedYear))
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(MementoSpacing.large)) {
            Text(value, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun RankingCard(title: String, rows: List<RankedStat>, showRating: Boolean = false) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(row.label)
                    Text(
                        if (showRating) stringResource(R.string.short_rating_format, row.averageRating ?: 0.0)
                        else row.count.toString(),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
