package com.memento.app.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.memento.app.ui.components.MementoPrimaryButton as Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.memento.app.ui.components.MementoFilterChip as FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.recommendation.Recommendation
import com.memento.app.ui.components.EmptyState
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.components.StaticTag

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    onRefresh: () -> Unit,
    onTypeSelected: (MediaType?) -> Unit,
    onOpenRecommendation: (Recommendation) -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
    com.memento.app.ui.components.MementoScreenHeader(stringResource(R.string.discover), onSettings = onOpenSettings,
        subtitle = stringResource(R.string.archive_discover_subtitle), compact = true)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = MementoSpacing.normal, end = MementoSpacing.normal, top = 8.dp, bottom = MementoSpacing.large),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.large),
    ) {
        if (state.isRefreshing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                items(listOf<MediaType?>(null, MediaType.BOOK, MediaType.MOVIE, MediaType.SERIES, MediaType.GAME)) { type ->
                    FilterChip(
                        selected = state.selectedType == type,
                        onClick = { onTypeSelected(type) },
                        label = { Text(discoverTypeLabel(type)) },
                    )
                }
            }
        }

        if (!state.profile.isReady) {
            item {
                EmptyState(
                    title = stringResource(R.string.discover_empty_title),
                    body = stringResource(R.string.discover_evidence_body, state.profile.evidenceCount),
                )
            }
        } else if (state.recommendations.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.discover_no_candidates_title),
                    body = stringResource(R.string.discover_no_candidates_body),
                    action = { Button(onClick = onRefresh) { Text(stringResource(R.string.refresh_recommendations)) } },
                )
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.for_you), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onRefresh) { Text(stringResource(R.string.refresh)) }
                }
            }
            items(
                items = state.recommendations,
                key = { it.candidate.provider.name + it.candidate.externalId + it.candidate.type.name },
            ) { recommendation ->
                com.memento.app.ui.components.RecommendationPreview(recommendation, { onOpenRecommendation(recommendation) })
            }
            val genres = state.profile.genreWeights.filterValues { it > 0 }.entries
                .sortedByDescending { it.value }.take(5).map { it.key }
            if (genres.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                        Text(stringResource(R.string.explore_by_genre), style = MaterialTheme.typography.titleLarge)
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                            genres.take(3).forEach { StaticTag(it) }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun discoverTypeLabel(type: MediaType?): String = stringResource(
    when (type) {
        null -> R.string.all
        MediaType.BOOK -> R.string.books
        MediaType.MOVIE -> R.string.movies
        MediaType.SERIES -> R.string.series
        MediaType.GAME -> R.string.games
    },
)
