package com.memento.app.ui.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.model.CompletedMediaInput
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.RecommendationFeedbackType
import com.memento.app.domain.recommendation.Recommendation
import com.memento.app.domain.recommendation.RecommendationKey
import com.memento.app.domain.repository.MediaRepository
import com.memento.app.domain.repository.MetadataRepository
import com.memento.app.domain.repository.MetadataDetailsOutcome
import com.memento.app.domain.repository.RecommendationRepository
import com.memento.app.domain.repository.WatchAvailabilityRepository
import com.memento.app.domain.watch.WatchAvailabilityRequest
import com.memento.app.ui.watch.WatchAvailabilityUiState
import com.memento.app.ui.watch.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

data class RecommendationDetailUiState(
    val recommendation: Recommendation? = null,
    val candidate: MetadataSearchResult? = null,
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val error: String? = null,
    val savedMediaId: String? = null,
    val wasDismissed: Boolean = false,
    val watchAvailability: WatchAvailabilityUiState = WatchAvailabilityUiState.Hidden,
    val isPopular: Boolean = false,
)

private data class RecommendationOperationState(
    val isWorking: Boolean = false,
    val error: String? = null,
    val savedMediaId: String? = null,
    val wasDismissed: Boolean = false,
)

@HiltViewModel
class RecommendationDetailViewModel @Inject constructor(
    private val recommendationRepository: RecommendationRepository,
    private val metadataRepository: MetadataRepository,
    private val mediaRepository: MediaRepository,
    private val watchAvailabilityRepository: WatchAvailabilityRepository,
) : ViewModel() {
    private val key = MutableStateFlow<RecommendationKey?>(null)
    private data class OpenedWork(val candidate: MetadataSearchResult, val recommendation: Recommendation?, val popular: Boolean)

    private val opened = MutableStateFlow<OpenedWork?>(null)
    private val loading = MutableStateFlow(true)
    private val operation = MutableStateFlow(RecommendationOperationState())
    private val watchAvailability = MutableStateFlow<WatchAvailabilityUiState>(WatchAvailabilityUiState.Hidden)
    private var watchAvailabilityJob: Job? = null
    private var detailsJob: Job? = null

    val state = combine(opened, loading, operation, watchAvailability) { work, isLoading, currentOperation, availability ->
        RecommendationDetailUiState(
            recommendation = work?.recommendation,
            candidate = work?.candidate,
            isLoading = isLoading,
            isWorking = currentOperation.isWorking,
            error = currentOperation.error,
            savedMediaId = currentOperation.savedMediaId,
            wasDismissed = currentOperation.wasDismissed,
            watchAvailability = availability,
            isPopular = work?.popular == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecommendationDetailUiState())

    fun load(provider: String, externalId: String, mediaType: String) {
        val loadedKey = RecommendationKey(
            MetadataProvider.valueOf(provider),
            externalId,
            MediaType.valueOf(mediaType),
        )
        if (key.value == loadedKey) return
        key.value = loadedKey
        detailsJob?.cancel()
        opened.value = null
        loading.value = true
        operation.value = RecommendationOperationState()
        watchAvailabilityJob?.cancel()
        val watchRequest = WatchAvailabilityRequest.create(
            mediaType = loadedKey.mediaType,
            provider = loadedKey.provider,
            externalId = loadedKey.externalId,
        )
        watchAvailability.value = if (watchRequest == null) WatchAvailabilityUiState.Hidden else WatchAvailabilityUiState.Loading
        watchAvailabilityJob = watchRequest?.let { request ->
            viewModelScope.launch {
                val availability = runCatching {
                    watchAvailabilityRepository.get(request.mediaType, request.tmdbId).toUiState()
                }.getOrDefault(WatchAvailabilityUiState.Hidden)
                if (key.value == loadedKey) watchAvailability.value = availability
            }
        }
        detailsJob = viewModelScope.launch {
            try {
                val feed = recommendationRepository.observeFeed().first()
                val recommendation = feed.findRecommendation(loadedKey)
                val popular = feed.findPopular(loadedKey)
                val candidate = recommendation?.candidate ?: popular
                if (candidate != null) opened.value = OpenedWork(candidate, recommendation, popular != null && recommendation == null)
                val locator = candidate ?: MetadataSearchResult(
                    provider = loadedKey.provider, externalId = externalId, externalUrl = null,
                    type = loadedKey.mediaType, title = "",
                )
                val outcome = metadataRepository.fetchDetails(locator)
                if (key.value != loadedKey) return@launch
                if (candidate == null && outcome !is MetadataDetailsOutcome.Complete) return@launch
                val details = outcome.result
                if (details.title.isBlank()) return@launch
                opened.value = OpenedWork(
                    candidate = details.copy(
                        externalRating = details.externalRating ?: candidate?.externalRating,
                        externalVoteCount = details.externalVoteCount ?: candidate?.externalVoteCount,
                        popularity = details.popularity ?: candidate?.popularity,
                        externalTags = details.externalTags.ifEmpty { candidate?.externalTags.orEmpty() },
                        sourceAnchorMediaIds = candidate?.sourceAnchorMediaIds.orEmpty(),
                        sourceAnchorTitles = candidate?.sourceAnchorTitles.orEmpty(),
                    ),
                    recommendation = recommendation,
                    popular = popular != null && recommendation == null,
                )
            } finally {
                if (key.value == loadedKey) loading.value = false
            }
        }
    }

    fun addToPlanned() = persist(ConsumptionStatus.PLANNED)

    fun startNow() = persist(ConsumptionStatus.IN_PROGRESS)

    fun complete(input: CompletedMediaInput) = persist(ConsumptionStatus.COMPLETED, input)

    fun notInterested() {
        val currentKey = key.value ?: return
        if (operation.value.isWorking) return
        operation.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            runCatching { recommendationRepository.setFeedback(currentKey, RecommendationFeedbackType.NOT_INTERESTED) }
                .onSuccess { operation.update { it.copy(isWorking = false, wasDismissed = true) } }
                .onFailure { error -> operation.update { it.copy(isWorking = false, error = userMessage(error)) } }
        }
    }

    private fun persist(status: ConsumptionStatus, completion: CompletedMediaInput? = null) {
        val candidate = state.value.candidate ?: return
        if (operation.value.isWorking || operation.value.savedMediaId != null) return
        operation.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            runCatching { mediaRepository.addExternal(candidate, status, completion) }
                .onSuccess { result -> operation.update { it.copy(isWorking = false, savedMediaId = result.mediaId) } }
                .onFailure { error -> operation.update { it.copy(isWorking = false, error = userMessage(error)) } }
        }
    }

    private fun userMessage(error: Throwable): String =
        if (error is IllegalArgumentException) error.message ?: "No se pudo completar la acción."
        else "No se pudo completar la acción. Inténtalo de nuevo."
}
