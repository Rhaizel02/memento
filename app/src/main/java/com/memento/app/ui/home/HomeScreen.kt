package com.memento.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.ui.components.EmptyState
import com.memento.app.ui.components.MediaCard
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.domain.model.MediaType
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAdd: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenRemember: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenStats: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MementoSpacing.normal),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.large),
    ) {
        item {
            Column {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenStats) { Text(stringResource(R.string.open_statistics)) }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                Text(stringResource(R.string.remember), style = MaterialTheme.typography.titleLarge)
                val memory = state.remember
                if (memory == null) {
                    EmptyState(
                        title = stringResource(R.string.remember_empty_title),
                        body = stringResource(R.string.remember_empty_body),
                    )
                } else {
                    Card(onClick = { onOpenRemember(memory.consumptionId) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(MementoSpacing.large)) {
                            Text(memory.title, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                stringResource(
                                    R.string.remember_completed_date,
                                    memory.completedDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))),
                                ),
                                modifier = Modifier.padding(top = MementoSpacing.small),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(R.string.quoted_reflection, memory.reflectionContent),
                                modifier = Modifier.padding(vertical = MementoSpacing.normal),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(stringResource(R.string.remember_open), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        if (state.inProgress.isEmpty() && state.recentlyCompleted.isEmpty() && !state.isLoading) {
            item {
                EmptyState(
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                ) {
                    Button(onClick = onAdd) { Text(stringResource(R.string.add_first_work)) }
                }
            }
        }

        if (state.inProgress.isNotEmpty()) {
            item { Text(stringResource(R.string.status_in_progress), style = MaterialTheme.typography.titleLarge) }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
                    contentPadding = PaddingValues(end = MementoSpacing.normal),
                ) {
                    items(state.inProgress, key = { it.id }) { media ->
                        MediaCard(
                            media = media,
                            onClick = { onOpenMedia(media.id) },
                            supportingText = stringResource(R.string.status_in_progress),
                            modifier = Modifier.fillParentMaxWidth(0.44f),
                        )
                    }
                }
            }
        }

        state.recommendation?.let { recommendation ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                    Text(stringResource(R.string.recommendation_for_you), style = MaterialTheme.typography.titleLarge)
                    Card(onClick = onOpenDiscover, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(MementoSpacing.large)) {
                            Text(recommendation.candidate.title, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                stringResource(R.string.affinity_format, recommendation.affinityScore),
                                modifier = Modifier.padding(top = MementoSpacing.small),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                stringResource(R.string.see_why),
                                modifier = Modifier.padding(top = MementoSpacing.normal),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }

        if (state.recentlyCompleted.isNotEmpty()) {
            item { Text(stringResource(R.string.recently_completed), style = MaterialTheme.typography.titleLarge) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.recentlyCompleted, key = { it.id }) { media ->
                        MediaCard(
                            media = media,
                            onClick = { onOpenMedia(media.id) },
                            supportingText = media.releaseYear?.toString(),
                            modifier = Modifier.fillParentMaxWidth(0.38f),
                        )
                    }
                }
            }
        }

        if (state.completedByType.values.sum() > 0) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(MementoSpacing.large),
                        verticalArrangement = Arrangement.spacedBy(MementoSpacing.small),
                    ) {
                        Text(stringResource(R.string.year_summary_title, state.summaryYear), style = MaterialTheme.typography.titleLarge)
                        MediaType.entries.forEach { type ->
                            val count = state.completedByType[type] ?: 0
                            if (count > 0) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(homeMediaTypeLabel(type), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(count.toString(), style = MaterialTheme.typography.labelLarge)
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
private fun homeMediaTypeLabel(type: MediaType): String = stringResource(
    when (type) {
        MediaType.BOOK -> R.string.books
        MediaType.MOVIE -> R.string.movies
        MediaType.SERIES -> R.string.series
        MediaType.GAME -> R.string.games
    },
)
