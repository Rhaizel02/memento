package com.memento.app.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.model.AddMediaInput
import com.memento.app.domain.model.CompletedMediaInput
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class AddMediaMode { SEARCH, MANUAL, CONFIRM_EXTERNAL, COMPLETE_DETAILS }
enum class SearchIssue { NOT_CONFIGURED, UNAVAILABLE, NO_RESULTS }

data class AddMediaDraft(
    val type: MediaType = MediaType.BOOK,
    val title: String = "",
    val year: String = "",
    val creator: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val pageCount: String = "",
)

data class CompletedDraft(
    val completedDateText: String = LocalDate.now().toString(),
    val ratingHalfStars: Int? = null,
    val favorite: Boolean = false,
    val finalReflection: String = "",
) {
    val completedDate: LocalDate? get() = runCatching { LocalDate.parse(completedDateText) }.getOrNull()
}

data class AddMediaUiState(
    val mode: AddMediaMode = AddMediaMode.SEARCH,
    val searchType: MediaType = MediaType.BOOK,
    val query: String = "",
    val searchResults: List<MetadataSearchResult> = emptyList(),
    val searchProvider: MetadataProvider? = null,
    val searchIssue: SearchIssue? = null,
    val isSearching: Boolean = false,
    val selectedExternal: MetadataSearchResult? = null,
    val manualDraft: AddMediaDraft = AddMediaDraft(),
    val externalDraft: AddMediaDraft? = null,
    val completionReturnMode: AddMediaMode? = null,
    val completedDraft: CompletedDraft? = null,
    val isLoadingDetails: Boolean = false,
    val metadataIsPartial: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedMediaId: String? = null,
    val savedWasDuplicate: Boolean = false,
) {
    val activeDraft: AddMediaDraft? get() = when (mode) {
        AddMediaMode.MANUAL -> manualDraft
        AddMediaMode.CONFIRM_EXTERNAL -> externalDraft
        AddMediaMode.COMPLETE_DETAILS -> when (completionReturnMode) {
            AddMediaMode.MANUAL -> manualDraft
            AddMediaMode.CONFIRM_EXTERNAL -> externalDraft
            else -> null
        }
        AddMediaMode.SEARCH -> null
    }
    val type: MediaType get() = activeDraft?.type ?: searchType
    val title: String get() = activeDraft?.title.orEmpty()
    val year: String get() = activeDraft?.year.orEmpty()
    val creator: String get() = activeDraft?.creator.orEmpty()
    val description: String get() = activeDraft?.description.orEmpty()
    val imageUrl: String get() = activeDraft?.imageUrl.orEmpty()
    val pageCount: String get() = activeDraft?.pageCount.orEmpty()
    val canSave: Boolean get() =
        activeDraft?.title?.isNotBlank() == true && savedMediaId == null && !isSaving && !isLoadingDetails
    val canSaveCompletion: Boolean get() = canSave && completedDraft?.completedDate != null
}

private data class SearchRequest(val query: String, val type: MediaType)
private data class SearchResponse(val request: SearchRequest, val outcome: MetadataSearchOutcome?)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddMediaViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val metadataRepository: MetadataRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AddMediaUiState())
    private val searchRequest = MutableStateFlow(SearchRequest("", MediaType.BOOK))
    private var detailsJob: Job? = null
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            searchRequest
                .transformLatest { request ->
                    if (request.query.trim().length < 2) {
                        emit(SearchResponse(request, null))
                    } else {
                        delay(350)
                        emit(SearchResponse(request, metadataRepository.search(request.type, request.query)))
                    }
                }
                .collect(::applySearchOutcome)
        }
    }

    fun setType(value: MediaType) {
        cancelDetailsLoad()
        mutableState.update {
            when (it.mode) {
                AddMediaMode.MANUAL -> it.copy(
                    manualDraft = it.manualDraft.copy(
                        type = value,
                        pageCount = if (value == MediaType.BOOK) it.manualDraft.pageCount else "",
                    ),
                )
                AddMediaMode.SEARCH, AddMediaMode.CONFIRM_EXTERNAL -> it.copy(
                    searchType = value,
                    searchResults = emptyList(),
                    searchIssue = null,
                    selectedExternal = null,
                    externalDraft = null,
                    mode = AddMediaMode.SEARCH,
                )
                AddMediaMode.COMPLETE_DETAILS -> it
            }
        }
        if (mutableState.value.mode == AddMediaMode.SEARCH) {
            searchRequest.value = SearchRequest(mutableState.value.query, mutableState.value.searchType)
        }
    }

    fun setQuery(value: String) {
        mutableState.update {
            it.copy(
                query = value,
                isSearching = value.trim().length >= 2,
                searchResults = emptyList(),
                searchProvider = null,
                searchIssue = null,
            )
        }
        searchRequest.value = SearchRequest(value, mutableState.value.searchType)
    }

    fun showManual() {
        cancelDetailsLoad()
        mutableState.update {
            it.copy(
                mode = AddMediaMode.MANUAL,
                selectedExternal = null,
                error = null,
            )
        }
    }

    fun returnToSearch() {
        cancelDetailsLoad()
        mutableState.update { it.copy(mode = AddMediaMode.SEARCH, selectedExternal = null, error = null) }
    }

    fun selectResult(result: MetadataSearchResult) {
        detailsJob?.cancel()
        mutableState.update {
            it.copy(
                mode = AddMediaMode.CONFIRM_EXTERNAL,
                selectedExternal = result,
                searchType = result.type,
                externalDraft = result.toDraft(),
                isLoadingDetails = true,
                metadataIsPartial = false,
                error = null,
            )
        }
        detailsJob = viewModelScope.launch {
            val outcome = metadataRepository.fetchDetails(result)
            val detailed = outcome.result
            mutableState.update {
                if (it.mode != AddMediaMode.CONFIRM_EXTERNAL || it.selectedExternal?.externalId != result.externalId ||
                    it.selectedExternal.provider != result.provider || it.selectedExternal.type != result.type
                ) it else {
                    it.copy(
                        selectedExternal = detailed,
                        externalDraft = detailed.toDraft(),
                        isLoadingDetails = false,
                        metadataIsPartial = outcome is MetadataDetailsOutcome.Partial,
                    )
                }
            }
        }
    }

    fun setTitle(value: String) = updateActiveDraft { copy(title = value) }
    fun setYear(value: String) = updateActiveDraft { copy(year = value.filter(Char::isDigit).take(4)) }
    fun setCreator(value: String) = updateActiveDraft { copy(creator = value) }
    fun setDescription(value: String) = updateActiveDraft { copy(description = value) }
    fun setImageUrl(value: String) = updateActiveDraft { copy(imageUrl = value) }
    fun setPageCount(value: String) = updateActiveDraft { copy(pageCount = value.filter(Char::isDigit)) }

    fun setCompletedDate(value: String) = mutableState.update { state ->
        state.copy(
            completedDraft = state.completedDraft?.copy(
                completedDateText = value.filter { it.isDigit() || it == '-' }.take(10),
            ),
            error = null,
        )
    }

    fun setCompletedRating(value: Int?) = mutableState.update { state ->
        state.copy(
            completedDraft = state.completedDraft?.copy(ratingHalfStars = value?.takeIf { it in 1..10 }),
            error = null,
        )
    }

    fun setCompletedFavorite(value: Boolean) = mutableState.update { state ->
        state.copy(completedDraft = state.completedDraft?.copy(favorite = value), error = null)
    }

    fun setCompletedReflection(value: String) = mutableState.update { state ->
        state.copy(completedDraft = state.completedDraft?.copy(finalReflection = value), error = null)
    }

    fun cancelCompletion() = mutableState.update { state ->
        if (state.isSaving) return@update state
        val returnMode = state.completionReturnMode ?: return@update state
        state.copy(
            mode = returnMode,
            completionReturnMode = null,
            completedDraft = null,
            error = null,
        )
    }

    fun save(status: ConsumptionStatus) {
        if (status == ConsumptionStatus.COMPLETED) {
            beginCompletion()
            return
        }
        persist(status, completion = null)
    }

    fun saveCompleted() {
        val snapshot = mutableState.value
        val completedDraft = snapshot.completedDraft ?: return
        val completedDate = completedDraft.completedDate ?: return
        if (!snapshot.canSaveCompletion) return
        persist(
            ConsumptionStatus.COMPLETED,
            CompletedMediaInput(
                completedDate = completedDate,
                ratingHalfStars = completedDraft.ratingHalfStars,
                favorite = completedDraft.favorite,
                finalReflection = completedDraft.finalReflection.trim().takeIf(String::isNotEmpty),
            ),
        )
    }

    private fun beginCompletion() {
        val snapshot = mutableState.value
        if (!snapshot.canSave || snapshot.mode !in setOf(AddMediaMode.MANUAL, AddMediaMode.CONFIRM_EXTERNAL)) return
        mutableState.value = snapshot.copy(
            mode = AddMediaMode.COMPLETE_DETAILS,
            completionReturnMode = snapshot.mode,
            completedDraft = snapshot.completedDraft ?: CompletedDraft(),
            error = null,
        )
    }

    private fun persist(status: ConsumptionStatus, completion: CompletedMediaInput?) {
        val snapshot = mutableState.value
        val draft = snapshot.activeDraft ?: return
        if (!snapshot.canSave) return
        mutableState.value = snapshot.copy(isSaving = true, error = null)
        viewModelScope.launch {
            runCatching {
                val external = snapshot.selectedExternal
                val sourceMode = snapshot.completionReturnMode ?: snapshot.mode
                if (sourceMode == AddMediaMode.CONFIRM_EXTERNAL && external != null) {
                    repository.addExternal(
                        external.copy(
                            title = draft.title.trim(),
                            releaseYear = draft.year.toIntOrNull(),
                            creators = draft.creator.split(',').map(String::trim).filter(String::isNotEmpty),
                            description = draft.description.trim().takeIf(String::isNotEmpty),
                            posterUrl = draft.imageUrl.trim().takeIf(String::isNotEmpty),
                            pageCount = draft.pageCount.toIntOrNull(),
                        ),
                        status,
                        completion,
                    )
                } else {
                    SaveExternalResult(
                        repository.addManual(
                            AddMediaInput(
                                type = draft.type,
                                title = draft.title,
                                year = draft.year.toIntOrNull(),
                                creator = draft.creator,
                                description = draft.description,
                                imageUrl = draft.imageUrl,
                                pageCount = draft.pageCount.toIntOrNull(),
                            ),
                            status,
                            completion,
                        ),
                        wasDuplicate = false,
                    )
                }
            }.onSuccess { result ->
                mutableState.update {
                    it.copy(isSaving = false, savedMediaId = result.mediaId, savedWasDuplicate = result.wasDuplicate)
                }
            }.onFailure {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        error = "No se pudo guardar la obra. Revisa los datos e inténtalo de nuevo.",
                    )
                }
            }
        }
    }

    fun consumeNavigation() = mutableState.update { it.copy(savedMediaId = null, savedWasDuplicate = false) }

    private fun cancelDetailsLoad() {
        detailsJob?.cancel()
        detailsJob = null
        mutableState.update { it.copy(isLoadingDetails = false) }
    }

    private fun updateActiveDraft(transform: AddMediaDraft.() -> AddMediaDraft) {
        mutableState.update { state ->
            when (state.mode) {
                AddMediaMode.MANUAL -> state.copy(manualDraft = state.manualDraft.transform(), error = null)
                AddMediaMode.CONFIRM_EXTERNAL -> state.copy(
                    externalDraft = state.externalDraft?.transform(),
                    error = null,
                )
                else -> state
            }
        }
    }

    private fun applySearchOutcome(response: SearchResponse) {
        mutableState.update { current ->
            if (current.query != response.request.query || current.searchType != response.request.type) return@update current
            val outcome = response.outcome
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

private fun MetadataSearchResult.toDraft() = AddMediaDraft(
    type = type,
    title = title,
    year = releaseYear?.toString().orEmpty(),
    creator = creators.joinToString(),
    description = description.orEmpty(),
    imageUrl = posterUrl.orEmpty(),
    pageCount = pageCount?.toString().orEmpty(),
)
