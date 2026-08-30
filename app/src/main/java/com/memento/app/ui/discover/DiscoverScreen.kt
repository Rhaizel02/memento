package com.memento.app.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.recommendation.Recommendation
import com.memento.app.domain.recommendation.RecommendationCategory
import com.memento.app.domain.recommendation.RecommendationReason
import com.memento.app.ui.components.EmptyState
import com.memento.app.ui.components.PosterArtwork
import com.memento.app.ui.components.mediaTypeLabel
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.components.StaticTag

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    onRefresh: () -> Unit,
    onTypeSelected: (MediaType?) -> Unit,
    onOpenRecommendation: (Recommendation) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MementoSpacing.normal),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.large),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                Text(stringResource(R.string.discover), style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(R.string.discover_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
                RecommendationCard(recommendation, onOpenRecommendation)
            }
            val genres = state.profile.genreWeights.filterValues { it > 0 }.entries
                .sortedByDescending { it.value }.take(5).map { it.key }
            if (genres.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                        Text(stringResource(R.string.explore_by_genre), style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                            genres.take(3).forEach { StaticTag(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: Recommendation,
    onOpen: (Recommendation) -> Unit,
) {
    val candidate = recommendation.candidate
    Card(onClick = { onOpen(recommendation) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
            Row(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.normal)) {
                PosterArtwork(
                    type = candidate.type,
                    title = candidate.title,
                    imageUrl = candidate.posterUrl,
                    modifier = Modifier.width(88.dp).height(132.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                    Text(candidate.title, maxLines = 2, style = MaterialTheme.typography.titleLarge)
                    Text(
                        recommendationCategoryLabel(recommendation.category),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    candidate.releaseYear?.let { Text(it.toString(), style = MaterialTheme.typography.bodySmall) }
                    if (candidate.genres.isNotEmpty()) {
                        Text(
                            candidate.genres.take(2).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    recommendation.reasons.firstOrNull()?.let { reason ->
                        Text(
                            reasonText(reason),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun reasonText(reason: RecommendationReason): String = when (reason) {
    is RecommendationReason.AnchorWorks -> stringResource(R.string.reason_anchors, reason.titles.joinToString(" y "))
    is RecommendationReason.Genre -> stringResource(R.string.reason_genre, reason.name)
    is RecommendationReason.Creator -> stringResource(R.string.reason_creator, reason.name)
    is RecommendationReason.MediaKind -> stringResource(R.string.reason_type, mediaTypeLabel(reason.type))
    is RecommendationReason.Exploration -> reason.anchorTitle?.let { stringResource(R.string.reason_exploration_anchor, it) }
        ?: stringResource(R.string.reason_exploration)
}

@Composable
private fun recommendationCategoryLabel(category: RecommendationCategory): String = stringResource(
    when (category) {
        RecommendationCategory.VERY_AFFINE -> R.string.recommendation_very_affine
        RecommendationCategory.GOOD_BET -> R.string.recommendation_good_bet
        RecommendationCategory.EXPLORATION -> R.string.recommendation_exploration
    },
)

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
