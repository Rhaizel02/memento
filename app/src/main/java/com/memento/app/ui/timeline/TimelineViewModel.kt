package com.memento.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.repository.CulturalTimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TimelineUiState(
    val events: List<CulturalTimelineEvent> = emptyList(),
    val selectedMediaType: MediaType? = null,
    val hasMore: Boolean = false,
    val isLoading: Boolean = true,
    val isError: Boolean = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel @Inject constructor(
    repository: CulturalTimelineRepository,
) : ViewModel() {
    private val selectedMediaType = MutableStateFlow<MediaType?>(null)
    private val requestedLimit = MutableStateFlow(INITIAL_WINDOW_SIZE)
    private val retrySignal = MutableStateFlow(0)

    val state = combine(selectedMediaType, requestedLimit, retrySignal) { type, limit, _ -> type to limit }
        .flatMapLatest { (type, limit) ->
            repository.observeWindow(type, limit)
                .map { window ->
                    TimelineUiState(
                        events = window.events,
                        selectedMediaType = type,
                        hasMore = window.hasMore,
                        isLoading = false,
                    )
                }
                .catch {
                    emit(TimelineUiState(selectedMediaType = type, isLoading = false, isError = true))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState())

    fun selectMediaType(type: MediaType?) {
        if (selectedMediaType.value == type) return
        requestedLimit.value = INITIAL_WINDOW_SIZE
        selectedMediaType.value = type
    }

    fun loadMore() {
        if (!state.value.hasMore) return
        requestedLimit.value += WINDOW_INCREMENT
    }

    fun retry() {
        retrySignal.value += 1
    }

    private companion object {
        const val INITIAL_WINDOW_SIZE = 60
        const val WINDOW_INCREMENT = 60
    }
}
