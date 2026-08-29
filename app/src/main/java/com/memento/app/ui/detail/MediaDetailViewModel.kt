package com.memento.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.TimelineEvent
import com.memento.app.domain.model.EditMediaInput
import com.memento.app.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate

data class MediaDetailUiState(
    val detail: MediaDetail? = null,
    val timeline: List<TimelineEvent> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MediaDetailViewModel @Inject constructor(private val repository: MediaRepository) : ViewModel() {
    private val mediaId = MutableStateFlow<String?>(null)
    private val detail = mediaId.flatMapLatest { id -> id?.let(repository::observeMediaDetail) ?: flowOf(null) }
    private val timeline = mediaId.flatMapLatest { id -> id?.let(repository::observeTimeline) ?: flowOf(emptyList()) }

    val state = combine(detail, timeline) { item, events ->
        MediaDetailUiState(item, events, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaDetailUiState())

    fun load(id: String) { mediaId.value = id }
    fun toggleFavorite() = mediaAction { repository.toggleFavorite(it) }
    fun start() = mediaAction { repository.startConsumption(it) }
    fun drop() = mediaAction { repository.dropConsumption(it) }
    fun delete() = mediaAction { repository.deleteMedia(it) }
    fun updateMetadata(input: EditMediaInput) = mediaAction { repository.updateMedia(it, input) }
    fun deleteConsumption(consumptionId: String) {
        viewModelScope.launch { repository.deleteConsumption(consumptionId) }
    }
    fun updateReflection(reflectionId: String, content: String) {
        viewModelScope.launch { repository.updateReflection(reflectionId, content) }
    }

    fun complete(date: LocalDate, ratingHalfStars: Int?, reflection: String?) = mediaAction {
        repository.completeConsumption(it, date = date, ratingHalfStars = ratingHalfStars, finalReflection = reflection)
    }

    fun addNote(content: String) {
        val consumptionId = state.value.detail?.activeConsumption?.id ?: return
        viewModelScope.launch { repository.saveReflection(consumptionId, ReflectionType.NOTE, content) }
    }

    fun addProgress(
        type: ProgressType,
        current: Double? = null,
        total: Double? = null,
        season: Int? = null,
        episode: Int? = null,
    ) {
        val consumptionId = state.value.detail?.activeConsumption?.id ?: return
        viewModelScope.launch { repository.addProgress(consumptionId, type, current, total, season, episode) }
    }

    private fun mediaAction(block: suspend (String) -> Unit) {
        val id = mediaId.value ?: return
        viewModelScope.launch { block(id) }
    }
}
