package com.memento.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.repository.GlobalSearchRepository
import com.memento.app.domain.search.FacetSearchResult
import com.memento.app.domain.search.GlobalSearchSnapshot
import com.memento.app.domain.search.MediaSearchResult
import com.memento.app.domain.search.mediaFor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class GlobalSearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val snapshot: GlobalSearchSnapshot? = null,
    val selectedFacet: FacetSearchResult? = null,
    val facetMedia: List<MediaSearchResult> = emptyList(),
    val hasError: Boolean = false,
)

private sealed interface SearchLoadState {
    val query: String

    data class Idle(override val query: String) : SearchLoadState
    data class Loading(override val query: String) : SearchLoadState
    data class Success(val snapshot: GlobalSearchSnapshot) : SearchLoadState {
        override val query: String = snapshot.query
    }
    data class Failed(override val query: String) : SearchLoadState
}

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class GlobalSearchViewModel @Inject constructor(
    repository: GlobalSearchRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedFacet = MutableStateFlow<FacetSearchResult?>(null)
    private val loadState = query
        .map(String::trim)
        .debounce(SEARCH_DEBOUNCE_MILLIS)
        .distinctUntilChanged()
        .flatMapLatest { cleanQuery ->
            if (cleanQuery.length < MIN_QUERY_LENGTH) {
                flowOf<SearchLoadState>(SearchLoadState.Idle(cleanQuery))
            } else {
                flow<SearchLoadState> {
                    emit(SearchLoadState.Loading(cleanQuery))
                    emit(SearchLoadState.Success(repository.search(cleanQuery)))
                }.catch { emit(SearchLoadState.Failed(cleanQuery)) }
            }
        }

    val state = combine(query, loadState, selectedFacet) { rawQuery, load, facet ->
        val cleanQuery = rawQuery.trim()
        val snapshot = (load as? SearchLoadState.Success)
            ?.snapshot
            ?.takeIf { it.query == cleanQuery }
        val activeFacet = facet?.takeIf { selected -> snapshot?.contains(selected) == true }
        GlobalSearchUiState(
            query = rawQuery,
            isSearching = cleanQuery.length >= MIN_QUERY_LENGTH &&
                (load.query != cleanQuery || load is SearchLoadState.Loading),
            snapshot = snapshot,
            selectedFacet = activeFacet,
            facetMedia = activeFacet?.let { selected -> snapshot?.mediaFor(selected) }.orEmpty(),
            hasError = load is SearchLoadState.Failed && load.query == cleanQuery,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GlobalSearchUiState())

    fun setQuery(value: String) {
        query.value = value
        selectedFacet.value = null
    }

    fun selectFacet(facet: FacetSearchResult) {
        selectedFacet.value = facet
    }

    fun clearFacet() {
        selectedFacet.value = null
    }

    private fun GlobalSearchSnapshot.contains(facet: FacetSearchResult): Boolean = when (facet.type) {
        com.memento.app.domain.search.SearchFacetType.TAG -> tags
        com.memento.app.domain.search.SearchFacetType.CREATOR -> creators
        com.memento.app.domain.search.SearchFacetType.GENRE -> genres
    }.any { it.id == facet.id }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
