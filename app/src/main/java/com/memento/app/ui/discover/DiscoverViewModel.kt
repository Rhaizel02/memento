package com.memento.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.model.RecommendationFeedbackType
import com.memento.app.domain.recommendation.Recommendation
import com.memento.app.domain.recommendation.RecommendationKey
import com.memento.app.domain.recommendation.TasteProfile
import com.memento.app.domain.repository.RecommendationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val profile: TasteProfile = TasteProfile(0, emptyMap(), emptyMap(), emptyMap()),
    val recommendations: List<Recommendation> = emptyList(),
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: RecommendationRepository,
) : ViewModel() {
    private val refreshing = MutableStateFlow(false)
    val state = combine(repository.observeFeed(), refreshing) { feed, isRefreshing ->
        DiscoverUiState(feed.profile, feed.recommendations, isRefreshing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    init { refresh() }

    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            runCatching { repository.refreshCandidates() }
            refreshing.value = false
        }
    }

    fun feedback(recommendation: Recommendation, type: RecommendationFeedbackType) {
        viewModelScope.launch {
            val candidate = recommendation.candidate
            repository.setFeedback(RecommendationKey(candidate.provider, candidate.externalId, candidate.type), type)
        }
    }
}
