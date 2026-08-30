package com.memento.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.TimelineEvent
import com.memento.app.domain.model.EditMediaInput
import com.memento.app.domain.model.Tag
import com.memento.app.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate

data class MediaDetailUiState(
    val detail: MediaDetail? = null,
    val timeline: List<TimelineEvent> = emptyList(),
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val message: String? = null,
    val wasDeleted: Boolean = false,
    val availableTags: List<Tag> = emptyList(),
)

private data class DetailOperationState(
    val isWorking: Boolean = false,
    val message: String? = null,
    val wasDeleted: Boolean = false,
)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MediaDetailViewModel @Inject constructor(private val repository: MediaRepository) : ViewModel() {
    private val mediaId = MutableStateFlow<String?>(null)
    private val detail = mediaId.flatMapLatest { id -> id?.let(repository::observeMediaDetail) ?: flowOf(null) }
    private val timeline = mediaId.flatMapLatest { id -> id?.let(repository::observeTimeline) ?: flowOf(emptyList()) }
    private val operation = MutableStateFlow(DetailOperationState())
    private val tags = repository.observeTags()

    val state = combine(detail, timeline, operation, tags) { item, events, currentOperation, availableTags ->
        MediaDetailUiState(
            detail = item,
            timeline = events,
            isLoading = false,
            isWorking = currentOperation.isWorking,
            message = currentOperation.message,
            wasDeleted = currentOperation.wasDeleted,
            availableTags = availableTags,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaDetailUiState())

    fun load(id: String) { mediaId.value = id }
    fun toggleFavorite() = mediaAction { repository.toggleFavorite(it) }
    fun start() = mediaAction { repository.startConsumption(it) }
    fun drop() = mediaAction { repository.dropConsumption(it) }
    fun delete() = mediaAction(onSuccess = { operation.update { it.copy(wasDeleted = true) } }) {
        repository.deleteMedia(it)
    }
    fun updateMetadata(input: EditMediaInput) = mediaAction { repository.updateMedia(it, input) }
    fun deleteConsumption(consumptionId: String) {
        runAction { repository.deleteConsumption(consumptionId) }
    }
    fun updateReflection(reflectionId: String, content: String) {
        runAction { repository.updateReflection(reflectionId, content) }
    }

    fun complete(date: LocalDate, ratingHalfStars: Int?, reflection: String?) = mediaAction {
        repository.completeConsumption(it, date = date, ratingHalfStars = ratingHalfStars, finalReflection = reflection)
    }

    fun addNote(content: String) {
        val consumptionId = state.value.detail?.activeConsumption?.id ?: return
        runAction { repository.saveReflection(consumptionId, ReflectionType.NOTE, content) }
    }

    fun addQuote(content: String) {
        val consumptionId = state.value.detail?.activeConsumption?.id ?: return
        runAction { repository.saveReflection(consumptionId, ReflectionType.QUOTE, content) }
    }

    fun createTag(name: String) = mediaAction { repository.createAndAttachTag(it, name) }

    fun attachTag(tagId: String) = mediaAction { repository.attachTag(it, tagId) }

    fun removeTag(tagId: String) = mediaAction { repository.removeTag(it, tagId) }

    fun addProgress(
        type: ProgressType,
        current: Double? = null,
        total: Double? = null,
        season: Int? = null,
        episode: Int? = null,
    ) {
        val consumptionId = state.value.detail?.activeConsumption?.id ?: return
        runAction { repository.addProgress(consumptionId, type, current, total, season, episode) }
    }

    private fun mediaAction(onSuccess: () -> Unit = {}, block: suspend (String) -> Unit) {
        val id = mediaId.value ?: return
        runAction(onSuccess) { block(id) }
    }

    private fun runAction(onSuccess: () -> Unit = {}, block: suspend () -> Unit) {
        if (operation.value.isWorking) return
        operation.update { it.copy(isWorking = true, message = null) }
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess {
                    operation.update { it.copy(isWorking = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    operation.update {
                        it.copy(
                            isWorking = false,
                            message = if (error is IllegalArgumentException) error.message
                                else "No se pudo completar la acción. Inténtalo de nuevo.",
                        )
                    }
                }
        }
    }
}
