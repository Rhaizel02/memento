package com.memento.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.repository.CandidateIssue
import com.memento.app.domain.repository.DISCOVER_TARGET
import com.memento.app.domain.repository.RecommendationFeed
import com.memento.app.domain.recommendation.recommendationKey
import kotlinx.coroutines.flow.update
import com.memento.app.domain.recommendation.Recommendation
import com.memento.app.domain.recommendation.TasteProfile
import com.memento.app.domain.repository.RecommendationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val profile: TasteProfile = TasteProfile(0, emptyMap(), emptyMap(), emptyMap()),
    val recommendations: List<Recommendation> = emptyList(),
    val popular: List<MetadataSearchResult> = emptyList(),
    val issues: Map<MediaType, CandidateIssue> = emptyMap(),
    val isRefreshing: Boolean = true,
    val selectedType: MediaType? = null,
)

fun RecommendationFeed.toDiscoverState(type: MediaType?, requesting: Boolean = false): DiscoverUiState {
    val personal = if (type == null) recommendations else byType[type].orEmpty()
    val missing = (DISCOVER_TARGET - personal.size).coerceAtLeast(0)
    val visibleKeys = personal.mapTo(mutableSetOf()) { it.candidate.recommendationKey() }
    val visibleTitles = personal.mapTo(mutableSetOf()) { it.candidate.discoveryTitleKey() }
    val selections = (type?.let(::listOf) ?: MediaType.entries).map { popularByType[it].orEmpty() }
    // Interleave types in Todo when its personal feed needs additional works.
    val popular = sequence {
        for (index in 0 until (selections.maxOfOrNull { it.size } ?: 0)) {
            for (bucket in selections) bucket.getOrNull(index)?.let { yield(it) }
        }
    }.filter { visibleKeys.add(it.recommendationKey()) && visibleTitles.add(it.discoveryTitleKey()) }
        .take(missing).toList()
    val relevantStatus = statusByType.filterKeys { type == null || it == type }

    return DiscoverUiState(
        profile = profile,
        recommendations = personal,
        popular = popular,
        issues = relevantStatus.mapNotNull { (category, status) -> status.issue?.let { category to it } }.toMap(),
        isRefreshing = requesting || relevantStatus.values.any { it.loading },
        selectedType = type,
    )
}

private fun MetadataSearchResult.discoveryTitleKey() = type to title.trim().lowercase()

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: RecommendationRepository,
) : ViewModel() {
    private val refreshing = MutableStateFlow<Set<MediaType?>>(emptySet())
    private val selectedType = MutableStateFlow<MediaType?>(null)
    val state = combine(repository.observeFeed(), refreshing, selectedType) { feed, requests, type ->
        feed.toDiscoverState(type, requesting = type in requests || (type == null && requests.isNotEmpty()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    fun onShown() = refresh(selectedType.value, force = false)

    fun refresh() = refresh(selectedType.value, force = true)

    private fun refresh(type: MediaType?, force: Boolean) {
        if (type in refreshing.value) return
        refreshing.update { it + type }
        viewModelScope.launch {
            try {
                repository.refreshCandidates(force, type, includePopular = true)
            } finally {
                refreshing.update { it - type }
            }
        }
    }

    fun setType(type: MediaType?) {
        if (type == selectedType.value) return
        selectedType.value = type
        refresh(type, force = false)
    }
}
