package com.memento.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.memento.app.R
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.domain.recommendation.Recommendation

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAdd: () -> Unit,
    onOpenGlobalSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenMedia: (String) -> Unit,
    onOpenRemember: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenRecommendation: (Recommendation) -> Unit = {},
    onOpenStats: () -> Unit,
    onOpenWrapped: (Int) -> Unit = {},
    onOpenCulturalProfile: () -> Unit = {},
    onOpenTimeline: () -> Unit = {},
    onOpenQuickProgress: (HomeMediaItem) -> Unit = {},
    onOpenQuickNote: (HomeMediaItem) -> Unit = {},
    onQuickProgressChanged: (QuickProgressField, String) -> Unit = { _, _ -> },
    onQuickNoteChanged: (String) -> Unit = {},
    onSaveQuickProgress: () -> Unit = {},
    onSaveQuickNote: () -> Unit = {},
    onDismissQuickCapture: () -> Unit = {},
    onDiscardQuickCapture: () -> Unit = onDismissQuickCapture,
) {
    Column(Modifier.fillMaxSize()) {
    com.memento.app.ui.components.MementoScreenHeader(
        title = stringResource(R.string.app_name), onSettings = onOpenSettings,
        actions = { IconButton(onClick = onOpenGlobalSearch) {
            Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.global_search_title))
        } },
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = MementoSpacing.normal, top = MementoSpacing.medium,
            end = MementoSpacing.normal, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.large),
    ) {
        item {
            Text(stringResource(R.string.archive_home_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (state.isLoading) { item { com.memento.app.ui.components.ArchiveLoading() }; return@LazyColumn }

        if (state.mediaCount == 0) {
            item { HomeEmptyState(onAdd = onAdd) }
            return@LazyColumn
        }

        item {
            state.remember?.let { memory ->
                RememberHero(memory = memory, onClick = { onOpenRemember(memory.consumptionId) })
            } ?: RememberEmptyState()
        }

        if (state.inProgress.isNotEmpty()) {
            item { HomeSectionHeader(stringResource(R.string.status_in_progress)) }
            items(state.inProgress, key = { it.mediaId }) { item ->
                InProgressMediaCard(item, onClick = { onOpenMedia(item.mediaId) },
                    onUpdateProgress = { onOpenQuickProgress(item) }, onAddNote = { onOpenQuickNote(item) })
            }
        }

        state.onThisDay?.let { memory ->
            item { OnThisDayCard(memory, onClick = { onOpenMedia(memory.event.mediaItemId) }) }
        }



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
                    onOpenWrapped = { onOpenWrapped(state.summaryYear) },
                )
            }
        }
        item {
            androidx.compose.material3.HorizontalDivider()
            androidx.compose.foundation.layout.FlowRow {
                androidx.compose.material3.TextButton(onClick = onOpenCulturalProfile) { Text(stringResource(R.string.cultural_profile)) }
                androidx.compose.material3.TextButton(onClick = onOpenStats) { Text(stringResource(R.string.statistics)) }
                androidx.compose.material3.TextButton(onClick = onOpenTimeline) { Text(stringResource(R.string.timeline_header)) }
            }
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
            onDiscard = onDiscardQuickCapture,
        )
    }
}
