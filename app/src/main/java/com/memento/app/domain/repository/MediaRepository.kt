package com.memento.app.domain.repository

import com.memento.app.domain.model.AddMediaInput
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.LibraryFilters
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.TimelineEvent
import com.memento.app.domain.model.SaveExternalResult
import com.memento.app.domain.model.EditMediaInput
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MediaRepository {
    fun observeLibrary(
        query: String = "",
        type: MediaType? = null,
        filters: LibraryFilters = LibraryFilters(),
    ): Flow<List<MediaItem>>
    fun observeMediaDetail(mediaId: String): Flow<MediaDetail?>
    fun observeTimeline(mediaId: String): Flow<List<TimelineEvent>>
    fun observeInProgress(): Flow<List<MediaItem>>
    fun observeRecentlyCompleted(limit: Int = 8): Flow<List<MediaItem>>
    fun observeCompletedCounts(year: Int): Flow<Map<MediaType, Int>>
    fun observeAllDetails(): Flow<List<MediaDetail>>

    suspend fun addManual(input: AddMediaInput, initialStatus: ConsumptionStatus): String
    suspend fun addExternal(input: MetadataSearchResult, initialStatus: ConsumptionStatus): SaveExternalResult
    suspend fun updateMedia(mediaId: String, input: EditMediaInput)
    suspend fun deleteMedia(mediaId: String)
    suspend fun toggleFavorite(mediaId: String)
    suspend fun startConsumption(mediaId: String, date: LocalDate = LocalDate.now()): String
    suspend fun completeConsumption(
        mediaId: String,
        date: LocalDate = LocalDate.now(),
        ratingHalfStars: Int? = null,
        finalReflection: String? = null,
    )
    suspend fun dropConsumption(mediaId: String)
    suspend fun setRating(consumptionId: String, ratingHalfStars: Int?)
    suspend fun addProgress(
        consumptionId: String,
        type: ProgressType,
        currentValue: Double? = null,
        totalValue: Double? = null,
        season: Int? = null,
        episode: Int? = null,
    )
    suspend fun saveReflection(
        consumptionId: String,
        type: ReflectionType,
        content: String,
    ): String
    suspend fun updateReflection(reflectionId: String, content: String)
    suspend fun deleteConsumption(consumptionId: String)
}
