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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.RecommendationFeedbackType
import com.memento.app.domain.recommendation.Recommendation
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
    onFeedback: (Recommendation, RecommendationFeedbackType) -> Unit,
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
                RecommendationCard(recommendation, onFeedback)
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
    onFeedback: (Recommendation, RecommendationFeedbackType) -> Unit,
) {
    val candidate = recommendation.candidate
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
            Row(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.normal)) {
                PosterArtwork(
                    type = candidate.type,
                    title = candidate.title,
                    imageUrl = candidate.posterUrl,
                    modifier = Modifier.width(88.dp).height(132.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                    Text(candidate.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.affinity_format, recommendation.affinityScore),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    candidate.releaseYear?.let { Text(it.toString(), style = MaterialTheme.typography.bodySmall) }
                    recommendation.reasons.firstOrNull()?.let { reason ->
                        Text(
                            reasonText(reason),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                Button(
                    onClick = { onFeedback(recommendation, RecommendationFeedbackType.INTERESTED) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.interested)) }
                OutlinedButton(
                    onClick = { onFeedback(recommendation, RecommendationFeedbackType.NOT_INTERESTED) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.not_interested)) }
            }
            TextButton(
                onClick = { onFeedback(recommendation, RecommendationFeedbackType.ALREADY_KNOWN) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.already_known)) }
        }
    }
}

@Composable
private fun reasonText(reason: RecommendationReason): String = when (reason) {
    is RecommendationReason.Genre -> stringResource(R.string.reason_genre, reason.name)
    is RecommendationReason.Creator -> stringResource(R.string.reason_creator, reason.name)
    is RecommendationReason.MediaKind -> stringResource(R.string.reason_type, mediaTypeLabel(reason.type))
}
