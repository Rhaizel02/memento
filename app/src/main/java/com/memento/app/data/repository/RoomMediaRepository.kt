package com.memento.app.data.repository

import androidx.room.withTransaction
import com.memento.app.data.local.dao.ConsumptionDao
import com.memento.app.data.local.dao.MediaDao
import com.memento.app.data.local.database.MementoDatabase
import com.memento.app.data.local.entity.ConsumptionEntity
import com.memento.app.data.local.entity.CreatorEntity
import com.memento.app.data.local.entity.ExternalMediaRefEntity
import com.memento.app.data.local.entity.GenreEntity
import com.memento.app.data.local.entity.MediaCreatorCrossRef
import com.memento.app.data.local.entity.MediaGenreCrossRef
import com.memento.app.data.local.entity.MediaItemEntity
import com.memento.app.data.local.entity.ProgressEntryEntity
import com.memento.app.data.local.entity.ReflectionEntity
import com.memento.app.data.mapper.toDomain
import com.memento.app.data.mapper.toEntity
import com.memento.app.domain.model.AddMediaInput
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.CreatorRole
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
import com.memento.app.domain.repository.MediaRepository
import com.memento.app.domain.usecase.TimelineBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class RoomMediaRepository @Inject constructor(
    private val database: MementoDatabase,
    private val mediaDao: MediaDao,
    private val consumptionDao: ConsumptionDao,
) : MediaRepository {
    override fun observeLibrary(query: String, type: MediaType?, filters: LibraryFilters): Flow<List<MediaItem>> =
        mediaDao.observeLibrary(
            query = query.trim(),
            type = type,
            status = filters.status,
            favoritesOnly = filters.favoritesOnly,
            minRating = filters.minRatingHalfStars,
            year = filters.year,
            sort = filters.sort.name,
        ).map { rows -> rows.map { it.toDomain() } }

    override fun observeMediaDetail(mediaId: String): Flow<MediaDetail?> = combine(
        mediaDao.observeById(mediaId),
        mediaDao.observeCreatorNames(mediaId),
        mediaDao.observeGenreNames(mediaId),
        consumptionDao.observeForMedia(mediaId),
        consumptionDao.observeProgressForMedia(mediaId),
        consumptionDao.observeReflectionsForMedia(mediaId),
    ) { values ->
        val media = (values[0] as MediaItemEntity?)?.toDomain() ?: return@combine null
        @Suppress("UNCHECKED_CAST")
        MediaDetail(
            media = media,
            creators = values[1] as List<String>,
            genres = values[2] as List<String>,
            consumptions = (values[3] as List<ConsumptionEntity>).map { it.toDomain() },
            progress = (values[4] as List<ProgressEntryEntity>).map { it.toDomain() },
            reflections = (values[5] as List<ReflectionEntity>).map { it.toDomain() },
        )
    }

    override fun observeTimeline(mediaId: String): Flow<List<TimelineEvent>> =
        observeMediaDetail(mediaId).map { detail ->
            if (detail == null) return@map emptyList()
            TimelineBuilder.build(detail.consumptions, detail.progress, detail.reflections)
        }

    override fun observeInProgress(): Flow<List<MediaItem>> =
        mediaDao.observeInProgress().map { rows -> rows.map { it.toDomain() } }

    override fun observeRecentlyCompleted(limit: Int): Flow<List<MediaItem>> =
        mediaDao.observeRecentlyCompleted(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeCompletedCounts(year: Int): Flow<Map<MediaType, Int>> =
        mediaDao.observeCompletedCounts(LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1))
            .map { rows -> MediaType.entries.associateWith { type -> rows.firstOrNull { it.type == type }?.count ?: 0 } }

    override fun observeAllDetails(): Flow<List<MediaDetail>> = mediaDao.observeAll().mapLatest { items ->
        items.map { media ->
            MediaDetail(
                media = media.toDomain(),
                creators = mediaDao.getCreatorNames(media.id),
                genres = mediaDao.getGenreNames(media.id),
                consumptions = consumptionDao.getForMedia(media.id).map { it.toDomain() },
                progress = consumptionDao.getProgressForMedia(media.id).map { it.toDomain() },
                reflections = consumptionDao.getReflectionsForMedia(media.id).map { it.toDomain() },
            )
        }
    }

    override suspend fun addManual(input: AddMediaInput, initialStatus: ConsumptionStatus): String {
        require(input.title.isNotBlank()) { "El título es obligatorio" }
        val now = Instant.now()
        val mediaId = UUID.randomUUID().toString()
        database.withTransaction {
            mediaDao.insert(
                MediaItemEntity(
                    id = mediaId,
                    type = input.type,
                    title = input.title.trim(),
                    originalTitle = null,
                    description = input.description?.trim()?.takeIf(String::isNotEmpty),
                    releaseDate = null,
                    releaseYear = input.year,
                    posterUrl = input.imageUrl?.trim()?.takeIf(String::isNotEmpty),
                    backdropUrl = null,
                    isFavorite = false,
                    isManual = true,
                    runtimeMinutes = null,
                    pageCount = input.pageCount,
                    seasonCount = null,
                    episodeCount = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            input.creator?.trim()?.takeIf(String::isNotEmpty)?.let { creatorName ->
                val normalized = creatorName.normalized()
                val existing = mediaDao.findCreator(normalized)
                val creatorId = existing?.id ?: UUID.randomUUID().toString().also { id ->
                    mediaDao.insertCreator(CreatorEntity(id, creatorName, normalized))
                }
                mediaDao.insertMediaCreator(MediaCreatorCrossRef(mediaId, creatorId, input.type.defaultCreatorRole()))
            }
            consumptionDao.insert(newConsumption(mediaId, initialStatus, now))
        }
        return mediaId
    }

    override suspend fun addExternal(
        input: MetadataSearchResult,
        initialStatus: ConsumptionStatus,
    ): SaveExternalResult = database.withTransaction {
        mediaDao.findByExternalRef(input.provider, input.externalId, input.type)?.let { existingId ->
            return@withTransaction SaveExternalResult(existingId, wasDuplicate = true)
        }

        require(input.title.isNotBlank()) { "El título es obligatorio" }
        val now = Instant.now()
        val mediaId = UUID.randomUUID().toString()
        mediaDao.insert(
            MediaItemEntity(
                id = mediaId,
                type = input.type,
                title = input.title.trim(),
                originalTitle = input.originalTitle?.trim()?.takeIf(String::isNotEmpty),
                description = input.description?.trim()?.takeIf(String::isNotEmpty),
                releaseDate = input.releaseDate,
                releaseYear = input.releaseYear,
                posterUrl = input.posterUrl,
                backdropUrl = input.backdropUrl,
                isFavorite = false,
                isManual = false,
                runtimeMinutes = input.runtimeMinutes,
                pageCount = input.pageCount,
                seasonCount = input.seasonCount,
                episodeCount = input.episodeCount,
                createdAt = now,
                updatedAt = now,
            ),
        )
        mediaDao.insertExternalRef(
            ExternalMediaRefEntity(mediaId, input.provider, input.externalId, input.type, input.externalUrl),
        )
        input.creators.distinctBy { it.normalized() }.forEach { creatorName ->
            val normalized = creatorName.normalized()
            val creatorId = mediaDao.findCreator(normalized)?.id ?: UUID.randomUUID().toString().also { id ->
                mediaDao.insertCreator(CreatorEntity(id, creatorName.trim(), normalized))
            }
            mediaDao.insertMediaCreator(MediaCreatorCrossRef(mediaId, creatorId, input.type.defaultCreatorRole()))
        }
        input.genres.distinctBy { it.normalized() }.forEach { genreName ->
            val normalized = genreName.normalized()
            val genreId = mediaDao.findGenre(normalized)?.id ?: UUID.randomUUID().toString().also { id ->
                mediaDao.insertGenre(GenreEntity(id, genreName.trim(), normalized))
            }
            mediaDao.insertMediaGenre(MediaGenreCrossRef(mediaId, genreId))
        }
        consumptionDao.insert(newConsumption(mediaId, initialStatus, now))
        SaveExternalResult(mediaId, wasDuplicate = false)
    }

    override suspend fun updateMedia(mediaId: String, input: EditMediaInput) = database.withTransaction {
        require(input.title.isNotBlank()) { "El título es obligatorio" }
        require(input.year == null || input.year in 1..9999) { "El año no es válido" }
        val current = mediaDao.getById(mediaId) ?: return@withTransaction
        mediaDao.update(
            current.copy(
                title = input.title.trim(),
                description = input.description?.trim()?.takeIf(String::isNotEmpty),
                releaseYear = input.year,
                posterUrl = input.imageUrl?.trim()?.takeIf(String::isNotEmpty),
                updatedAt = Instant.now(),
            ),
        )
        mediaDao.deleteMediaCreators(mediaId)
        input.creators.map(String::trim).filter(String::isNotEmpty).distinctBy(String::normalized).forEach { creatorName ->
            val normalized = creatorName.normalized()
            val creatorId = mediaDao.findCreator(normalized)?.id ?: UUID.randomUUID().toString().also { id ->
                mediaDao.insertCreator(CreatorEntity(id, creatorName, normalized))
            }
            mediaDao.insertMediaCreator(MediaCreatorCrossRef(mediaId, creatorId, current.type.defaultCreatorRole()))
        }
        mediaDao.deleteOrphanCreators()
    }

    override suspend fun deleteMedia(mediaId: String) = mediaDao.deleteById(mediaId)

    override suspend fun toggleFavorite(mediaId: String) = mediaDao.toggleFavorite(mediaId, Instant.now())

    override suspend fun startConsumption(mediaId: String, date: LocalDate): String = database.withTransaction {
        val now = Instant.now()
        val active = consumptionDao.getActive(mediaId)
        if (active != null) {
            consumptionDao.update(active.copy(status = ConsumptionStatus.IN_PROGRESS, startedDate = active.startedDate ?: date, updatedAt = now))
            active.id
        } else {
            UUID.randomUUID().toString().also { id ->
                consumptionDao.insert(
                    ConsumptionEntity(id, mediaId, ConsumptionStatus.IN_PROGRESS, date, null, null, now, now),
                )
            }
        }
    }

    override suspend fun completeConsumption(
        mediaId: String,
        date: LocalDate,
        ratingHalfStars: Int?,
        finalReflection: String?,
    ) = database.withTransaction {
        validateRating(ratingHalfStars)
        val now = Instant.now()
        val active = consumptionDao.getActive(mediaId)
        val completed = if (active == null) {
            newConsumption(mediaId, ConsumptionStatus.COMPLETED, now).copy(completedDate = date, ratingHalfStars = ratingHalfStars)
                .also { consumptionDao.insert(it) }
        } else {
            active.copy(status = ConsumptionStatus.COMPLETED, completedDate = date, ratingHalfStars = ratingHalfStars, updatedAt = now)
                .also { consumptionDao.update(it) }
        }
        finalReflection?.trim()?.takeIf(String::isNotEmpty)?.let { content ->
            consumptionDao.insertReflection(
                ReflectionEntity(UUID.randomUUID().toString(), completed.id, ReflectionType.FINAL_REFLECTION, content, now, now),
            )
        }
        Unit
    }

    override suspend fun dropConsumption(mediaId: String) {
        val active = consumptionDao.getActive(mediaId) ?: return
        consumptionDao.update(active.copy(status = ConsumptionStatus.DROPPED, updatedAt = Instant.now()))
    }

    override suspend fun setRating(consumptionId: String, ratingHalfStars: Int?) {
        validateRating(ratingHalfStars)
        val current = consumptionDao.getById(consumptionId) ?: return
        consumptionDao.update(current.copy(ratingHalfStars = ratingHalfStars, updatedAt = Instant.now()))
    }

    override suspend fun addProgress(
        consumptionId: String,
        type: ProgressType,
        currentValue: Double?,
        totalValue: Double?,
        season: Int?,
        episode: Int?,
    ) {
        require(currentValue == null || currentValue >= 0)
        require(totalValue == null || totalValue >= 0)
        require(season == null || season >= 0)
        require(episode == null || episode >= 0)
        consumptionDao.insertProgress(
            ProgressEntryEntity(UUID.randomUUID().toString(), consumptionId, type, currentValue, totalValue, season, episode, Instant.now()),
        )
    }

    override suspend fun saveReflection(consumptionId: String, type: ReflectionType, content: String): String {
        val cleanContent = content.trim()
        require(cleanContent.isNotEmpty())
        val now = Instant.now()
        val existing = if (type == ReflectionType.FINAL_REFLECTION) consumptionDao.getReflection(consumptionId, type) else null
        return if (existing != null) {
            consumptionDao.updateReflection(existing.copy(content = cleanContent, updatedAt = now))
            existing.id
        } else {
            UUID.randomUUID().toString().also { id ->
                consumptionDao.insertReflection(ReflectionEntity(id, consumptionId, type, cleanContent, now, now))
            }
        }
    }

    override suspend fun updateReflection(reflectionId: String, content: String) {
        val cleanContent = content.trim()
        require(cleanContent.isNotEmpty())
        val current = consumptionDao.getReflectionById(reflectionId) ?: return
        consumptionDao.updateReflection(current.copy(content = cleanContent, updatedAt = Instant.now()))
    }

    override suspend fun deleteConsumption(consumptionId: String) = consumptionDao.deleteById(consumptionId)

    private fun newConsumption(mediaId: String, status: ConsumptionStatus, now: Instant): ConsumptionEntity =
        ConsumptionEntity(
            id = UUID.randomUUID().toString(),
            mediaItemId = mediaId,
            status = status,
            startedDate = if (status == ConsumptionStatus.IN_PROGRESS) LocalDate.now() else null,
            completedDate = if (status == ConsumptionStatus.COMPLETED) LocalDate.now() else null,
            ratingHalfStars = null,
            createdAt = now,
            updatedAt = now,
        )

    private fun validateRating(rating: Int?) {
        require(rating == null || rating in 1..10) { "La valoración debe estar entre 0,5 y 5 estrellas" }
    }
}

private fun String.normalized(): String = Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
    .replace("\\p{Mn}+".toRegex(), "")
    .trim()

private fun MediaType.defaultCreatorRole(): CreatorRole = when (this) {
    MediaType.BOOK -> CreatorRole.AUTHOR
    MediaType.MOVIE -> CreatorRole.DIRECTOR
    MediaType.SERIES -> CreatorRole.CREATOR
    MediaType.GAME -> CreatorRole.DEVELOPER
}
