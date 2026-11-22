package com.memento.app.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.repository.CandidateIssue
import com.memento.app.ui.components.DiscoveryWorkPreview
import com.memento.app.ui.components.EditorialSectionHeader
import com.memento.app.ui.components.MementoScreenHeader
import com.memento.app.ui.components.RecommendationPreview
import com.memento.app.ui.components.EmptyState
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.components.StaticTag

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    onRefresh: () -> Unit,
    onTypeSelected: (MediaType?) -> Unit,
    onOpenRecommendation: (MetadataSearchResult) -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        MementoScreenHeader(
            stringResource(R.string.discover), onSettings = onOpenSettings,
            subtitle = stringResource(R.string.archive_discover_subtitle), compact = true,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = MementoSpacing.normal, end = MementoSpacing.normal, top = 8.dp, bottom = MementoSpacing.large),
            verticalArrangement = Arrangement.spacedBy(MementoSpacing.large),
        ) {
            item("filters") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                    items(listOf<MediaType?>(null) + MediaType.entries) { type ->
                        FilterChip(
                            selected = state.selectedType == type,
                            onClick = { onTypeSelected(type) },
                            label = { Text(discoverTypeLabel(type)) },
                        )
                    }
                }
            }
            item("heading") {
                val refreshAction: @Composable () -> Unit = {
                    TextButton(onClick = onRefresh, enabled = !state.isRefreshing) { Text(stringResource(R.string.refresh)) }
                }
                if (state.recommendations.isNotEmpty()) {
                    EditorialSectionHeader(stringResource(R.string.for_you), action = refreshAction)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { refreshAction() }
                }
            }
            if (state.isRefreshing) item("loading") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (state.issues.isNotEmpty() && !state.isRefreshing) {
                item("issues") {
                    Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.xSmall)) {
                        state.issues.forEach { (type, issue) ->
                            Text(
                                stringResource(R.string.discover_source_issue, discoverTypeLabel(type), stringResource(issue.messageResource())),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            items(state.recommendations, key = { it.candidate.discoveryKey() }) { recommendation ->
                RecommendationPreview(recommendation, { onOpenRecommendation(recommendation.candidate) })
            }
            if (state.popular.isNotEmpty()) {
                item("popular-heading") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        EditorialSectionHeader(stringResource(R.string.discover_popular_title))
                        Text(stringResource(R.string.discover_popular_body), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(state.popular, key = { it.discoveryKey() }) { work ->
                    DiscoveryWorkPreview(work, { onOpenRecommendation(work) })
                }
            }
            if (state.recommendations.isEmpty() && state.popular.isEmpty() && !state.isRefreshing && state.issues.isEmpty()) {
                item("empty") {
                    if (!state.profile.isReady) {
                        EmptyState(stringResource(R.string.discover_empty_title),
                            stringResource(R.string.discover_evidence_body, state.profile.evidenceCount))
                    } else {
                        EmptyState(stringResource(R.string.discover_no_candidates_title),
                            stringResource(R.string.discover_no_candidates_body))
                    }
                }
            }
            val genres = state.profile.genreWeights.filterValues { it > 0 }.entries
                .sortedByDescending { it.value }.take(3)
            if (state.recommendations.isNotEmpty() && genres.isNotEmpty()) {
                item("genres") {
                    Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                        EditorialSectionHeader(stringResource(R.string.explore_by_genre))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                            genres.forEach { StaticTag(it.key) }
                        }
                    }
                }
            }
        }
    }
}

private fun MetadataSearchResult.discoveryKey() = "${type.name}:${provider.name}:$externalId"

private fun CandidateIssue.messageResource(): Int = when (this) {
    CandidateIssue.NOT_CONFIGURED -> R.string.discover_issue_not_configured
    CandidateIssue.AUTHENTICATION -> R.string.discover_issue_authentication
    CandidateIssue.RATE_LIMITED -> R.string.discover_issue_rate_limit
    CandidateIssue.CONNECTION -> R.string.discover_issue_connection
    CandidateIssue.UNAVAILABLE -> R.string.discover_issue_unavailable
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
