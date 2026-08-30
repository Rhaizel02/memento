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
import com.memento.app.domain.repository.RecommendationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecommendationDetailUiState(
    val recommendation: Recommendation? = null,
    val candidate: MetadataSearchResult? = null,
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val error: String? = null,
    val savedMediaId: String? = null,
    val wasDismissed: Boolean = false,
)

private data class RecommendationOperationState(
    val isWorking: Boolean = false,
    val error: String? = null,
    val savedMediaId: String? = null,
    val wasDismissed: Boolean = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationDetailViewModel @Inject constructor(
    private val recommendationRepository: RecommendationRepository,
    private val metadataRepository: MetadataRepository,
    private val mediaRepository: MediaRepository,
) : ViewModel() {
    private val key = MutableStateFlow<RecommendationKey?>(null)
    private val detailedCandidate = MutableStateFlow<MetadataSearchResult?>(null)
    private val operation = MutableStateFlow(RecommendationOperationState())
    private val recommendation = key.flatMapLatest { currentKey ->
        currentKey?.let { wanted ->
            recommendationRepository.observeFeed().map { feed ->
                feed.recommendations.firstOrNull { item ->
                    val candidate = item.candidate
                    candidate.provider == wanted.provider && candidate.externalId == wanted.externalId && candidate.type == wanted.mediaType
                }
            }
        } ?: flowOf(null)
    }

    val state = combine(recommendation, detailedCandidate, operation) { item, details, currentOperation ->
        RecommendationDetailUiState(
            recommendation = item,
            candidate = details ?: item?.candidate,
            isLoading = key.value != null && details == null && item == null,
            isWorking = currentOperation.isWorking,
            error = currentOperation.error,
            savedMediaId = currentOperation.savedMediaId,
            wasDismissed = currentOperation.wasDismissed,
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
        detailedCandidate.value = null
        operation.value = RecommendationOperationState()
        viewModelScope.launch {
            val item = recommendationRepository.observeFeed().map { feed ->
                feed.recommendations.firstOrNull { recommendation ->
                    val candidate = recommendation.candidate
                    candidate.provider == loadedKey.provider && candidate.externalId == loadedKey.externalId && candidate.type == loadedKey.mediaType
                }
            }.firstNonNull() ?: return@launch
            detailedCandidate.value = item.candidate
            val details = metadataRepository.fetchDetails(item.candidate).result
            if (key.value == loadedKey) detailedCandidate.value = details.copy(
                externalRating = details.externalRating ?: item.candidate.externalRating,
                externalVoteCount = details.externalVoteCount ?: item.candidate.externalVoteCount,
                popularity = details.popularity ?: item.candidate.popularity,
                externalTags = details.externalTags.ifEmpty { item.candidate.externalTags },
                sourceAnchorMediaIds = item.candidate.sourceAnchorMediaIds,
                sourceAnchorTitles = item.candidate.sourceAnchorTitles,
            )
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

private suspend fun <T : Any> kotlinx.coroutines.flow.Flow<T?>.firstNonNull(): T? =
    first { it != null }
