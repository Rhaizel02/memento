package com.memento.app

import com.memento.app.domain.model.AddMediaInput
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.CompletedMediaInput
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.HomeMediaFeed
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

data class ProgressSaveCall(
    val consumptionId: String,
    val type: ProgressType,
    val currentValue: Double?,
    val totalValue: Double?,
    val season: Int?,
    val episode: Int?,
)

data class ReflectionSaveCall(
    val consumptionId: String,
    val type: ReflectionType,
    val content: String,
)

class FakeMediaRepository : MediaRepository {
    val library = MutableStateFlow<List<MediaItem>>(emptyList())
    val inProgress = MutableStateFlow<List<MediaItem>>(emptyList())
    val completed = MutableStateFlow<List<MediaItem>>(emptyList())
    val completedCounts = MutableStateFlow<Map<MediaType, Int>>(emptyMap())
    val homeMedia = MutableStateFlow(HomeMediaFeed())
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
    var addManualInvocations: Int = 0
    var addExternalInvocations: Int = 0
    var completedInput: CompletedMediaInput? = null
    val progressSaveCalls = mutableListOf<ProgressSaveCall>()
    val reflectionSaveCalls = mutableListOf<ReflectionSaveCall>()
    var progressSaveHandler: suspend (ProgressSaveCall) -> Unit = {}
    var reflectionSaveHandler: suspend (ReflectionSaveCall) -> Unit = {}

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
    override fun observeHomeMedia(inProgressLimit: Int, recentLimit: Int): Flow<HomeMediaFeed> = homeMedia
    override fun observeCompletedCounts(year: Int): Flow<Map<MediaType, Int>> = completedCounts
    override fun observeAllDetails(): Flow<List<MediaDetail>> = allDetails
    override suspend fun addManual(
        input: AddMediaInput,
        initialStatus: ConsumptionStatus,
        completion: CompletedMediaInput?,
    ): String {
        addManualInvocations++
        addedInput = input
        addedStatus = initialStatus
        completedInput = completion
        return "new-media"
    }
    override suspend fun addExternal(
        input: MetadataSearchResult,
        initialStatus: ConsumptionStatus,
        completion: CompletedMediaInput?,
    ): SaveExternalResult {
        addExternalInvocations++
        externalResult = input
        addedStatus = initialStatus
        completedInput = completion
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
    ) {
        val call = ProgressSaveCall(consumptionId, type, currentValue, totalValue, season, episode)
        progressSaveCalls += call
        progressSaveHandler(call)
    }
    override suspend fun saveReflection(consumptionId: String, type: ReflectionType, content: String): String {
        val call = ReflectionSaveCall(consumptionId, type, content)
        reflectionSaveCalls += call
        reflectionSaveHandler(call)
        return "reflection"
    }
    override suspend fun updateReflection(reflectionId: String, content: String) {
        editedReflectionId = reflectionId
        editedReflectionContent = content
    }
    override suspend fun deleteConsumption(consumptionId: String) { deletedConsumptionId = consumptionId }
}
