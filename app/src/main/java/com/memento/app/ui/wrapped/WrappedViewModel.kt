package com.memento.app.ui.wrapped

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.repository.MediaRepository
import com.memento.app.domain.wrapped.WrappedEngine
import com.memento.app.domain.wrapped.WrappedStory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

data class WrappedUiState(val story: WrappedStory? = null)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WrappedViewModel @Inject constructor(private val repository: MediaRepository) : ViewModel() {
    private val year = MutableStateFlow<Int?>(null)
    val state = year.flatMapLatest { selectedYear ->
        repository.observeAllDetails().map { history ->
            WrappedUiState(selectedYear?.let { WrappedEngine.create(history, it) })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WrappedUiState())

    fun load(year: Int) { this.year.value = year }
}
