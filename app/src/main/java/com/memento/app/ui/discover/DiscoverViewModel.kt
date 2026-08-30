package com.memento.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.model.MediaType
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
    val isRefreshing: Boolean = false,
    val selectedType: MediaType? = null,
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: RecommendationRepository,
) : ViewModel() {
    private val refreshing = MutableStateFlow(false)
    private val selectedType = MutableStateFlow<MediaType?>(null)
    val state = combine(repository.observeFeed(), refreshing, selectedType) { feed, isRefreshing, type ->
        DiscoverUiState(
            profile = feed.profile,
            recommendations = feed.recommendations.filter { type == null || it.candidate.type == type },
            isRefreshing = isRefreshing,
            selectedType = type,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    init { refresh(force = false) }

    fun refresh() = refresh(force = true)

    private fun refresh(force: Boolean) {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            runCatching { repository.refreshCandidates(force) }
            refreshing.value = false
        }
    }

    fun setType(type: MediaType?) { selectedType.value = type }
}
