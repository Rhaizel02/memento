package com.memento.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.LibraryFilters
import com.memento.app.domain.model.LibrarySort
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class LibraryUiState(
    val query: String = "",
    val type: MediaType? = null,
    val filters: LibraryFilters = LibraryFilters(),
    val items: List<MediaItem> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LibraryViewModel @Inject constructor(repository: MediaRepository) : ViewModel() {
    private data class Request(val query: String = "", val type: MediaType? = null, val filters: LibraryFilters = LibraryFilters())
    private val request = MutableStateFlow(Request())
    private val items = request.flatMapLatest { repository.observeLibrary(it.query, it.type, it.filters) }

    val state = combine(request, items) { current, media ->
        LibraryUiState(current.query, current.type, current.filters, media, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setQuery(query: String) = request.update { it.copy(query = query) }
    fun setType(type: MediaType?) = request.update { it.copy(type = type) }
    fun setStatus(status: ConsumptionStatus?) = updateFilters { copy(status = status) }
    fun setMinRating(rating: Int?) = updateFilters { copy(minRatingHalfStars = rating) }
    fun setFavoritesOnly(enabled: Boolean) = updateFilters { copy(favoritesOnly = enabled) }
    fun setYear(year: Int?) = updateFilters { copy(year = year) }
    fun setSort(sort: LibrarySort) = updateFilters { copy(sort = sort) }
    fun clearAdditionalFilters() = request.update { it.copy(filters = LibraryFilters()) }

    private fun updateFilters(transform: LibraryFilters.() -> LibraryFilters) =
        request.update { it.copy(filters = it.filters.transform()) }
}
