package com.memento.app

import com.memento.app.domain.model.AddMediaInput
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.LibraryFilters
import com.memento.app.domain.model.EditMediaInput
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.TimelineEvent
import com.memento.app.domain.model.SaveExternalResult
import com.memento.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

class FakeMediaRepository : MediaRepository {
    val library = MutableStateFlow<List<MediaItem>>(emptyList())
    val inProgress = MutableStateFlow<List<MediaItem>>(emptyList())
    val completed = MutableStateFlow<List<MediaItem>>(emptyList())
    val completedCounts = MutableStateFlow<Map<MediaType, Int>>(emptyMap())
    val detail = MutableStateFlow<MediaDetail?>(null)
    val allDetails = MutableStateFlow<List<MediaDetail>>(emptyList())
    val timeline = MutableStateFlow<List<TimelineEvent>>(emptyList())
    var lastQuery: String = ""
    var lastType: MediaType? = null
    var lastFilters: LibraryFilters = LibraryFilters()
    var addedInput: AddMediaInput? = null
    var addedStatus: ConsumptionStatus? = null
    var externalResult: MetadataSearchResult? = null
    var startedMediaId: String? = null
    var editedMediaId: String? = null
    var editedInput: EditMediaInput? = null
    var deletedConsumptionId: String? = null
    var editedReflectionId: String? = null
    var editedReflectionContent: String? = null

    override fun observeLibrary(query: String, type: MediaType?, filters: LibraryFilters): Flow<List<MediaItem>> {
        lastQuery = query
        lastType = type
        lastFilters = filters
        return library
    }
    override fun observeMediaDetail(mediaId: String): Flow<MediaDetail?> = detail
    override fun observeTimeline(mediaId: String): Flow<List<TimelineEvent>> = timeline
    override fun observeInProgress(): Flow<List<MediaItem>> = inProgress
    override fun observeRecentlyCompleted(limit: Int): Flow<List<MediaItem>> = completed
    override fun observeCompletedCounts(year: Int): Flow<Map<MediaType, Int>> = completedCounts
    override fun observeAllDetails(): Flow<List<MediaDetail>> = allDetails
    override suspend fun addManual(input: AddMediaInput, initialStatus: ConsumptionStatus): String {
        addedInput = input
        addedStatus = initialStatus
        return "new-media"
    }
    override suspend fun addExternal(input: MetadataSearchResult, initialStatus: ConsumptionStatus): SaveExternalResult {
        externalResult = input
        addedStatus = initialStatus
        return SaveExternalResult("new-external-media", wasDuplicate = false)
    }
    override suspend fun updateMedia(mediaId: String, input: EditMediaInput) {
        editedMediaId = mediaId
        editedInput = input
    }
    override suspend fun deleteMedia(mediaId: String) = Unit
    override suspend fun toggleFavorite(mediaId: String) = Unit
    override suspend fun startConsumption(mediaId: String, date: LocalDate): String {
        startedMediaId = mediaId
        return "consumption"
    }
    override suspend fun completeConsumption(mediaId: String, date: LocalDate, ratingHalfStars: Int?, finalReflection: String?) = Unit
    override suspend fun dropConsumption(mediaId: String) = Unit
    override suspend fun setRating(consumptionId: String, ratingHalfStars: Int?) = Unit
    override suspend fun addProgress(
        consumptionId: String,
        type: ProgressType,
        currentValue: Double?,
        totalValue: Double?,
        season: Int?,
        episode: Int?,
    ) = Unit
    override suspend fun saveReflection(consumptionId: String, type: ReflectionType, content: String): String = "reflection"
    override suspend fun updateReflection(reflectionId: String, content: String) {
        editedReflectionId = reflectionId
        editedReflectionContent = content
    }
    override suspend fun deleteConsumption(consumptionId: String) { deletedConsumptionId = consumptionId }
}
