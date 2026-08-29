package com.memento.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.repository.MediaRepository
import com.memento.app.domain.remember.RememberCandidate
import com.memento.app.domain.repository.RememberRepository
import com.memento.app.domain.repository.RecommendationRepository
import com.memento.app.domain.recommendation.Recommendation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val inProgress: List<MediaItem> = emptyList(),
    val recentlyCompleted: List<MediaItem> = emptyList(),
    val remember: RememberCandidate? = null,
    val recommendation: Recommendation? = null,
    val summaryYear: Int = LocalDate.now().year,
    val completedByType: Map<MediaType, Int> = emptyMap(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: MediaRepository,
    rememberRepository: RememberRepository,
    private val recommendationRepository: RecommendationRepository,
) : ViewModel() {
    private val currentYear = LocalDate.now().year
    val state = combine(
        repository.observeInProgress(),
        repository.observeRecentlyCompleted(),
        rememberRepository.observeRemember(),
        recommendationRepository.observeFeed(),
        repository.observeCompletedCounts(currentYear),
    ) { inProgress, recent, remember, recommendationFeed, completedCounts ->
        HomeUiState(
            inProgress = inProgress,
            recentlyCompleted = recent,
            remember = remember,
            recommendation = recommendationFeed.recommendations.firstOrNull(),
            summaryYear = currentYear,
            completedByType = completedCounts,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { runCatching { recommendationRepository.refreshCandidates() } }
    }
}
