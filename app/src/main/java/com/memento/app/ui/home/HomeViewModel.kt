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
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.usecase.ProgressCapturePolicy
import com.memento.app.domain.usecase.ProgressValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    val quickCapture: QuickCaptureSheet? = null,
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

sealed interface QuickCaptureSheet {
    val item: HomeMediaItem
    val isSaving: Boolean
    val error: String?

    data class Progress(
        override val item: HomeMediaItem,
        val currentValue: String = "",
        val totalValue: String = "",
        val isTotalEditable: Boolean = false,
        val season: String = "",
        val episode: String = "",
        override val isSaving: Boolean = false,
        override val error: String? = null,
    ) : QuickCaptureSheet

    data class Note(
        override val item: HomeMediaItem,
        val content: String = "",
        override val isSaving: Boolean = false,
        override val error: String? = null,
    ) : QuickCaptureSheet
}

enum class QuickProgressField { CURRENT, TOTAL, SEASON, EPISODE }

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
    val pageCount: Int? = null,
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
    private val repository: MediaRepository,
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
    private val quickCapture = MutableStateFlow<QuickCaptureSheet?>(null)
    private val content = combine(
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
    }
    val state = combine(content, quickCapture) { home, capture ->
        home.copy(quickCapture = capture)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { runCatching { recommendationRepository.refreshCandidates() } }
    }

    fun openQuickProgress(item: HomeMediaItem) {
        if (quickCapture.value?.isSaving == true) return
        val progress = item.progress
        val bookTotal = (progress as? HomeProgress.Pages)?.total ?: item.pageCount?.toDouble()
        quickCapture.value = QuickCaptureSheet.Progress(
            item = item,
            currentValue = when (progress) {
                is HomeProgress.Pages -> progress.current.draftNumber()
                is HomeProgress.Game -> progress.hours.draftNumber()
                is HomeProgress.Minutes -> progress.minutes.draftNumber()
                else -> ""
            },
            totalValue = when (progress) {
                is HomeProgress.Pages -> progress.total?.draftNumber().orEmpty()
                is HomeProgress.Game -> progress.percent?.draftNumber().orEmpty()
                else -> item.pageCount?.toString().orEmpty()
            },
            isTotalEditable = item.type == MediaType.BOOK && bookTotal == null,
            season = (progress as? HomeProgress.Episode)?.season?.toString().orEmpty(),
            episode = (progress as? HomeProgress.Episode)?.episode?.toString().orEmpty(),
        )
    }

    fun openQuickNote(item: HomeMediaItem) {
        if (quickCapture.value?.isSaving == true) return
        quickCapture.value = QuickCaptureSheet.Note(item)
    }

    fun dismissQuickCapture() {
        if (quickCapture.value?.isSaving != true) quickCapture.value = null
    }

    fun updateQuickProgress(field: QuickProgressField, value: String) {
        val current = quickCapture.value as? QuickCaptureSheet.Progress ?: return
        if (current.isSaving) return
        quickCapture.value = when (field) {
            QuickProgressField.CURRENT -> current.copy(currentValue = value, error = null)
            QuickProgressField.TOTAL -> current.copy(totalValue = value, error = null)
            QuickProgressField.SEASON -> current.copy(season = value, error = null)
            QuickProgressField.EPISODE -> current.copy(episode = value, error = null)
        }
    }

    fun updateQuickNote(content: String) {
        val current = quickCapture.value as? QuickCaptureSheet.Note ?: return
        if (!current.isSaving) quickCapture.value = current.copy(content = content, error = null)
    }

    fun saveQuickProgress() {
        val draft = quickCapture.value as? QuickCaptureSheet.Progress ?: return
        if (draft.isSaving) return
        val values = runCatching { draft.toProgressValues() }.getOrElse { error ->
            quickCapture.value = draft.copy(error = error.message ?: "El progreso no es válido")
            return
        }
        quickCapture.value = draft.copy(isSaving = true, error = null)
        viewModelScope.launch {
            runCatching {
                repository.addProgress(
                    draft.item.consumptionId,
                    values.type,
                    values.currentValue,
                    values.totalValue,
                    values.season,
                    values.episode,
                )
            }.onSuccess {
                quickCapture.value = null
            }.onFailure {
                quickCapture.value = draft.copy(error = "No se pudo guardar el progreso.")
            }
        }
    }

    fun saveQuickNote() {
        val draft = quickCapture.value as? QuickCaptureSheet.Note ?: return
        if (draft.isSaving) return
        val content = draft.content.trim()
        if (content.isEmpty()) {
            quickCapture.value = draft.copy(error = "Escribe una nota antes de guardar.")
            return
        }
        quickCapture.value = draft.copy(isSaving = true, error = null)
        viewModelScope.launch {
            runCatching { repository.saveReflection(draft.item.consumptionId, ReflectionType.NOTE, content) }
                .onSuccess { quickCapture.value = null }
                .onFailure { quickCapture.value = draft.copy(error = "No se pudo guardar la nota.") }
        }
    }
}

private data class QuickProgressValues(
    val type: ProgressType,
    val currentValue: Double?,
    val totalValue: Double?,
    val season: Int?,
    val episode: Int?,
)

private fun QuickCaptureSheet.Progress.toProgressValues(): QuickProgressValues {
    val type = ProgressCapturePolicy.typeFor(item.type)
    val values = when (item.type) {
        MediaType.BOOK -> QuickProgressValues(type, currentValue.toDoubleOrNull(), totalValue.toDoubleOrNull(), null, null)
        MediaType.SERIES -> QuickProgressValues(type, null, null, season.toIntOrNull(), episode.toIntOrNull())
        MediaType.GAME -> QuickProgressValues(type, currentValue.toDoubleOrNull(), totalValue.toDoubleOrNull(), null, null)
        MediaType.MOVIE -> QuickProgressValues(type, currentValue.toDoubleOrNull(), null, null, null)
    }
    ProgressValidator.validate(type, values.currentValue, values.totalValue, values.season, values.episode)
    return values
}

private fun Double.draftNumber(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

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
    pageCount = media.pageCount,
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
