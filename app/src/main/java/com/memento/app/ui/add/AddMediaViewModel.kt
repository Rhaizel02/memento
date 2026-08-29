package com.memento.app.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.model.AddMediaInput
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchOutcome
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.SaveExternalResult
import com.memento.app.domain.repository.MediaRepository
import com.memento.app.domain.repository.MetadataRepository
import com.memento.app.domain.repository.MetadataDetailsOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AddMediaMode { SEARCH, MANUAL, CONFIRM_EXTERNAL }
enum class SearchIssue { NOT_CONFIGURED, UNAVAILABLE, NO_RESULTS }

data class AddMediaUiState(
    val mode: AddMediaMode = AddMediaMode.SEARCH,
    val type: MediaType = MediaType.BOOK,
    val query: String = "",
    val searchResults: List<MetadataSearchResult> = emptyList(),
    val searchProvider: MetadataProvider? = null,
    val searchIssue: SearchIssue? = null,
    val isSearching: Boolean = false,
    val selectedExternal: MetadataSearchResult? = null,
    val title: String = "",
    val year: String = "",
    val creator: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val pageCount: String = "",
    val isLoadingDetails: Boolean = false,
    val metadataIsPartial: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedMediaId: String? = null,
    val savedWasDuplicate: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank() && !isSaving && !isLoadingDetails
}

private data class SearchRequest(val query: String, val type: MediaType)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddMediaViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val metadataRepository: MetadataRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AddMediaUiState())
    private val searchRequest = MutableStateFlow(SearchRequest("", MediaType.BOOK))
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            searchRequest
                .debounce(350)
                .mapLatest { request ->
                    if (request.query.trim().length < 2) null
                    else metadataRepository.search(request.type, request.query)
                }
                .collect(::applySearchOutcome)
        }
    }

    fun setType(value: MediaType) {
        mutableState.update {
            it.copy(
                type = value,
                searchResults = emptyList(),
                searchIssue = null,
                selectedExternal = null,
                mode = if (it.mode == AddMediaMode.MANUAL) AddMediaMode.MANUAL else AddMediaMode.SEARCH,
                pageCount = if (value == MediaType.BOOK) it.pageCount else "",
            )
        }
        searchRequest.value = SearchRequest(mutableState.value.query, value)
    }

    fun setQuery(value: String) {
        mutableState.update {
            it.copy(query = value, isSearching = value.trim().length >= 2, searchIssue = null)
        }
        searchRequest.value = SearchRequest(value, mutableState.value.type)
    }

    fun showManual() = mutableState.update {
        it.copy(
            mode = AddMediaMode.MANUAL,
            selectedExternal = null,
            title = "",
            year = "",
            creator = "",
            description = "",
            imageUrl = "",
            pageCount = "",
            error = null,
        )
    }

    fun returnToSearch() = mutableState.update { it.copy(mode = AddMediaMode.SEARCH, selectedExternal = null, error = null) }

    fun selectResult(result: MetadataSearchResult) {
        mutableState.update {
            it.copy(
            mode = AddMediaMode.CONFIRM_EXTERNAL,
            selectedExternal = result,
            type = result.type,
            title = result.title,
            year = result.releaseYear?.toString().orEmpty(),
            creator = result.creators.joinToString(),
            description = result.description.orEmpty(),
            imageUrl = result.posterUrl.orEmpty(),
            pageCount = result.pageCount?.toString().orEmpty(),
            isLoadingDetails = true,
            metadataIsPartial = false,
            error = null,
        )
        }
        viewModelScope.launch {
            val outcome = metadataRepository.fetchDetails(result)
            val detailed = outcome.result
            mutableState.update {
                it.copy(
                    selectedExternal = detailed,
                    type = detailed.type,
                    title = detailed.title,
                    year = detailed.releaseYear?.toString().orEmpty(),
                    creator = detailed.creators.joinToString(),
                    description = detailed.description.orEmpty(),
                    imageUrl = detailed.posterUrl.orEmpty(),
                    pageCount = detailed.pageCount?.toString().orEmpty(),
                    isLoadingDetails = false,
                    metadataIsPartial = outcome is MetadataDetailsOutcome.Partial,
                )
            }
        }
    }

    fun setTitle(value: String) = mutableState.update { it.copy(title = value, error = null) }
    fun setYear(value: String) = mutableState.update { it.copy(year = value.filter(Char::isDigit).take(4)) }
    fun setCreator(value: String) = mutableState.update { it.copy(creator = value) }
    fun setDescription(value: String) = mutableState.update { it.copy(description = value) }
    fun setImageUrl(value: String) = mutableState.update { it.copy(imageUrl = value) }
    fun setPageCount(value: String) = mutableState.update { it.copy(pageCount = value.filter(Char::isDigit)) }

    fun save(status: ConsumptionStatus) {
        val snapshot = mutableState.value
        if (!snapshot.canSave) return
        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true, error = null) }
            runCatching {
                val external = snapshot.selectedExternal
                if (snapshot.mode == AddMediaMode.CONFIRM_EXTERNAL && external != null) {
                    repository.addExternal(
                        external.copy(
                            title = snapshot.title.trim(),
                            releaseYear = snapshot.year.toIntOrNull(),
                            creators = snapshot.creator.split(',').map(String::trim).filter(String::isNotEmpty),
                            description = snapshot.description.trim().takeIf(String::isNotEmpty),
                            posterUrl = snapshot.imageUrl.trim().takeIf(String::isNotEmpty),
                            pageCount = snapshot.pageCount.toIntOrNull(),
                        ),
                        status,
                    )
                } else {
                    SaveExternalResult(
                        repository.addManual(
                            AddMediaInput(
                                type = snapshot.type,
                                title = snapshot.title,
                                year = snapshot.year.toIntOrNull(),
                                creator = snapshot.creator,
                                description = snapshot.description,
                                imageUrl = snapshot.imageUrl,
                                pageCount = snapshot.pageCount.toIntOrNull(),
                            ),
                            status,
                        ),
                        wasDuplicate = false,
                    )
                }
            }.onSuccess { result ->
                mutableState.update {
                    it.copy(isSaving = false, savedMediaId = result.mediaId, savedWasDuplicate = result.wasDuplicate)
                }
            }.onFailure { error -> mutableState.update { it.copy(isSaving = false, error = error.message) } }
        }
    }

    fun consumeNavigation() = mutableState.update { it.copy(savedMediaId = null, savedWasDuplicate = false) }

    private fun applySearchOutcome(outcome: MetadataSearchOutcome?) {
        mutableState.update { current ->
            when (outcome) {
                null -> current.copy(isSearching = false, searchResults = emptyList(), searchIssue = null, searchProvider = null)
                is MetadataSearchOutcome.Success -> current.copy(
                    isSearching = false,
                    searchProvider = outcome.provider,
                    searchResults = outcome.results,
                    searchIssue = if (outcome.results.isEmpty()) SearchIssue.NO_RESULTS else null,
                )
                is MetadataSearchOutcome.NotConfigured -> current.copy(
                    isSearching = false,
                    searchProvider = outcome.provider,
                    searchResults = emptyList(),
                    searchIssue = SearchIssue.NOT_CONFIGURED,
                )
                is MetadataSearchOutcome.Unavailable -> current.copy(
                    isSearching = false,
                    searchProvider = outcome.provider,
                    searchResults = emptyList(),
                    searchIssue = SearchIssue.UNAVAILABLE,
                )
            }
        }
    }
}
