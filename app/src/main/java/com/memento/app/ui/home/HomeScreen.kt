package com.memento.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.memento.app.R
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.domain.recommendation.Recommendation

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAdd: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenRemember: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenRecommendation: (Recommendation) -> Unit = {},
    onOpenStats: () -> Unit,
    onOpenTimeline: () -> Unit = {},
    onOpenQuickProgress: (HomeMediaItem) -> Unit = {},
    onOpenQuickNote: (HomeMediaItem) -> Unit = {},
    onQuickProgressChanged: (QuickProgressField, String) -> Unit = { _, _ -> },
    onQuickNoteChanged: (String) -> Unit = {},
    onSaveQuickProgress: () -> Unit = {},
    onSaveQuickNote: () -> Unit = {},
    onDismissQuickCapture: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MementoSpacing.normal,
            top = MementoSpacing.large,
            end = MementoSpacing.normal,
            bottom = MementoSpacing.xLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.xLarge),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.xSmall)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.isLoading) return@LazyColumn

        if (state.mediaCount == 0) {
            item { HomeEmptyState(onAdd = onAdd) }
            return@LazyColumn
        }

        item {
            state.remember?.let { memory ->
                RememberHero(memory = memory, onClick = { onOpenRemember(memory.consumptionId) })
            } ?: RememberEmptyState()
        }

        state.onThisDay?.let { memory ->
            item {
                OnThisDayCard(memory = memory, onClick = { onOpenMedia(memory.event.mediaItemId) })
            }
        }

        if (state.inProgress.isNotEmpty()) {
            item { HomeSectionHeader(stringResource(R.string.status_in_progress)) }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
                    contentPadding = PaddingValues(end = MementoSpacing.normal),
                ) {
                    items(state.inProgress, key = { it.mediaId }) { item ->
                        InProgressMediaCard(
                            item = item,
                            onClick = { onOpenMedia(item.mediaId) },
                            onUpdateProgress = { onOpenQuickProgress(item) },
                            onAddNote = { onOpenQuickNote(item) },
                        )
                    }
                }
            }
        }

        item { CulturalHistoryCard(onClick = onOpenTimeline) }

        item {
            HomeSectionHeader(stringResource(R.string.recommendation_for_you))
            state.recommendation?.let { recommendation ->
                HomeRecommendationCard(
                    recommendation = recommendation,
                    onClick = { onOpenRecommendation(recommendation) },
                    modifier = Modifier.padding(top = MementoSpacing.medium),
                )
            } ?: RecommendationLearningState(
                profileReady = state.recommendationProfileReady,
                onOpenDiscover = onOpenDiscover,
                modifier = Modifier.padding(top = MementoSpacing.small),
            )
        }

        if (state.recentlyCompleted.isNotEmpty()) {
            item { HomeSectionHeader(stringResource(R.string.recently_completed)) }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
                    contentPadding = PaddingValues(end = MementoSpacing.normal),
                ) {
                    items(state.recentlyCompleted, key = { it.mediaId }) { item ->
                        RecentMediaCard(item = item, onClick = { onOpenMedia(item.mediaId) })
                    }
                }
            }
        }

        if (state.completedByType.values.sum() > 0) {
            item {
                YearSummaryCard(
                    year = state.summaryYear,
                    completedByType = state.completedByType,
                    onOpenStats = onOpenStats,
                )
            }
        }
    }

    state.quickCapture?.let { sheet ->
        HomeQuickCaptureSheet(
            sheet = sheet,
            onDismiss = onDismissQuickCapture,
            onProgressChanged = onQuickProgressChanged,
            onNoteChanged = onQuickNoteChanged,
            onSaveProgress = onSaveQuickProgress,
            onSaveNote = onSaveQuickNote,
        )
    }
}
