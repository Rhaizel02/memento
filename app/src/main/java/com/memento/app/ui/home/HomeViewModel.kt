package com.memento.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.model.HomeMediaSummary
import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.repository.MediaRepository
import com.memento.app.domain.repository.CulturalTimelineRepository
import com.memento.app.domain.remember.RememberCandidate
import com.memento.app.domain.repository.RememberRepository
import com.memento.app.domain.repository.RecommendationRepository
import com.memento.app.domain.recommendation.Recommendation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Clock

data class HomeUiState(
    val mediaCount: Int = 0,
    val inProgress: List<HomeMediaItem> = emptyList(),
    val recentlyCompleted: List<HomeMediaItem> = emptyList(),
    val remember: RememberCandidate? = null,
    val onThisDay: OnThisDayMemory? = null,
    val recommendation: Recommendation? = null,
    val recommendationProfileReady: Boolean = false,
    val summaryYear: Int = 0,
    val completedByType: Map<MediaType, Int> = emptyMap(),
    val isLoading: Boolean = true,
)

data class OnThisDayMemory(
    val event: CulturalTimelineEvent,
    val yearsAgo: Int,
)

data class HomeMediaItem(
    val mediaId: String,
    val consumptionId: String,
    val type: MediaType,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val isFavorite: Boolean,
    val creator: String?,
    val releaseYear: Int?,
    val genres: List<String>,
    val additionalGenreCount: Int,
    val ratingHalfStars: Int?,
    val progress: HomeProgress? = null,
    val completedDate: LocalDate? = null,
)

sealed interface HomeProgress {
    val fraction: Float?

    data class Pages(val current: Double, val total: Double?, override val fraction: Float?) : HomeProgress
    data class Episode(val season: Int, val episode: Int) : HomeProgress { override val fraction: Float? = null }
    data class Game(val hours: Double, val percent: Double?) : HomeProgress {
        override val fraction: Float? = percent?.div(100.0)?.toFloat()
    }
    data class Minutes(val minutes: Double, override val fraction: Float?) : HomeProgress
    data class Percent(val percent: Double) : HomeProgress { override val fraction: Float = (percent / 100.0).toFloat() }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: MediaRepository,
    rememberRepository: RememberRepository,
    private val recommendationRepository: RecommendationRepository,
    culturalTimelineRepository: CulturalTimelineRepository,
    clock: Clock,
) : ViewModel() {
    private val currentDate = LocalDate.now(clock)
    private val currentYear = currentDate.year
    private val remember = rememberRepository.observeRemember().onEach { candidate ->
        candidate?.let { rememberRepository.recordExposure(it.consumptionId) }
    }
    val state = combine(
        repository.observeHomeMedia(),
        remember,
        recommendationRepository.observeFeed(),
        repository.observeCompletedCounts(currentYear),
        culturalTimelineRepository.observeOnThisDay(currentDate),
    ) { homeMedia, remember, recommendationFeed, completedCounts, onThisDayEvents ->
        HomeUiState(
            mediaCount = homeMedia.mediaCount,
            inProgress = homeMedia.inProgress.map(HomeMediaSummary::toHomeItem),
            recentlyCompleted = homeMedia.recentlyCompleted.map(HomeMediaSummary::toHomeItem),
            remember = remember,
            onThisDay = onThisDayEvents.firstOrNull()?.let { event ->
                OnThisDayMemory(event = event, yearsAgo = currentYear - event.date.year)
            },
            recommendation = recommendationFeed.recommendations.firstOrNull(),
            recommendationProfileReady = recommendationFeed.profile.isReady,
            summaryYear = currentYear,
            completedByType = completedCounts,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { runCatching { recommendationRepository.refreshCandidates() } }
    }
}

private fun HomeMediaSummary.toHomeItem() = HomeMediaItem(
    mediaId = media.id,
    consumptionId = consumptionId,
    type = media.type,
    title = media.title,
    posterUrl = media.posterUrl,
    backdropUrl = media.backdropUrl,
    isFavorite = media.isFavorite,
    creator = creator,
    releaseYear = media.releaseYear,
    genres = genres,
    additionalGenreCount = additionalGenreCount,
    ratingHalfStars = ratingHalfStars,
    progress = latestProgress?.let { entry ->
        when (entry.progressType) {
            ProgressType.PAGES -> entry.currentValue?.let { current ->
                val total = entry.totalValue ?: media.pageCount?.toDouble()
                HomeProgress.Pages(current, total, fraction(current, total))
            }
            ProgressType.EPISODE -> entry.season?.let { season ->
                entry.episode?.let { episode -> HomeProgress.Episode(season, episode) }
            }
            ProgressType.HOURS -> entry.currentValue?.let { hours -> HomeProgress.Game(hours, entry.totalValue) }
            ProgressType.PERCENT -> entry.currentValue?.let { percent -> HomeProgress.Percent(percent) }
            ProgressType.MINUTES -> entry.currentValue?.let { minutes ->
                HomeProgress.Minutes(minutes, fraction(minutes, media.runtimeMinutes?.toDouble()))
            }
        }
    },
    completedDate = completedDate,
)

private fun fraction(current: Double, total: Double?): Float? =
    total?.takeIf { it > 0 }?.let { (current / it).coerceIn(0.0, 1.0).toFloat() }
