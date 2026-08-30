package com.memento.app.data.repository

import androidx.room.withTransaction
import com.memento.app.data.local.dao.ConsumptionDao
import com.memento.app.data.local.dao.HomeMediaRow
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
import com.memento.app.data.local.entity.MediaTagCrossRef
import com.memento.app.data.local.entity.TagEntity
import com.memento.app.data.mapper.toDomain
import com.memento.app.data.mapper.toEntity
import com.memento.app.domain.model.AddMediaInput
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.CompletedMediaInput
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.HomeMediaFeed
import com.memento.app.domain.model.HomeMediaSummary
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.LibraryFilters
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.ProgressEntry
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.TimelineEvent
import com.memento.app.domain.model.SaveExternalResult
import com.memento.app.domain.model.EditMediaInput
import com.memento.app.domain.model.Tag
import com.memento.app.domain.model.MediaExternalReference
import com.memento.app.domain.model.defaultCreatorRole
import com.memento.app.domain.repository.MediaRepository
import com.memento.app.domain.usecase.TimelineBuilder
import com.memento.app.domain.usecase.ProgressValidator
import com.memento.app.domain.usecase.TagNameNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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
            tagIds = filters.tagIds.toList(),
            tagCount = filters.tagIds.size,
        ).map { rows -> rows.map { it.toDomain() } }

    override fun observeMediaDetail(mediaId: String): Flow<MediaDetail?> = combine(
        mediaDao.observeById(mediaId),
        mediaDao.observeCreatorNames(mediaId),
        mediaDao.observeGenreNames(mediaId),
        consumptionDao.observeForMedia(mediaId),
        consumptionDao.observeProgressForMedia(mediaId),
        consumptionDao.observeReflectionsForMedia(mediaId),
        mediaDao.observeTagsForMedia(mediaId),
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
            tags = (values[6] as List<TagEntity>).map { it.toDomain() },
        )
    }

    override fun observeTags(): Flow<List<Tag>> =
        mediaDao.observeTags().map { rows -> rows.map { it.toDomain() } }

    override fun observeTimeline(mediaId: String): Flow<List<TimelineEvent>> =
        observeMediaDetail(mediaId).map { detail ->
            if (detail == null) return@map emptyList()
            TimelineBuilder.build(detail.media, detail.consumptions, detail.progress, detail.reflections)
        }

    override fun observeInProgress(): Flow<List<MediaItem>> =
        mediaDao.observeInProgress().map { rows -> rows.map { it.toDomain() } }

    override fun observeRecentlyCompleted(limit: Int): Flow<List<MediaItem>> =
        mediaDao.observeRecentlyCompleted(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeHomeMedia(inProgressLimit: Int, recentLimit: Int): Flow<HomeMediaFeed> = combine(
        mediaDao.observeMediaCount(),
        mediaDao.observeHomeInProgress(inProgressLimit),
        mediaDao.observeHomeRecentlyCompleted(recentLimit),
    ) { mediaCount, inProgress, recent ->
        HomeMediaFeed(
            mediaCount = mediaCount,
            inProgress = inProgress.map(HomeMediaRow::toHomeSummary),
            recentlyCompleted = recent.map(HomeMediaRow::toHomeSummary),
        )
    }

    override fun observeCompletedCounts(year: Int): Flow<Map<MediaType, Int>> =
        mediaDao.observeCompletedCounts(LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1))
            .map { rows -> MediaType.entries.associateWith { type -> rows.firstOrNull { it.type == type }?.count ?: 0 } }

    @Suppress("UNCHECKED_CAST")
    override fun observeAllDetails(): Flow<List<MediaDetail>> = combine(
        mediaDao.observeAll(),
        mediaDao.observeAllCreatorNames(),
        mediaDao.observeAllGenreNames(),
        consumptionDao.observeAll(),
        consumptionDao.observeAllProgress(),
        consumptionDao.observeAllReflections(),
        mediaDao.observeAllTags(),
        mediaDao.observeAllExternalRefs(),
    ) { values ->
        val items = values[0] as List<MediaItemEntity>
        val creatorsByMedia = (values[1] as List<com.memento.app.data.local.dao.MediaNameRow>).groupBy({ it.mediaItemId }, { it.name })
        val genresByMedia = (values[2] as List<com.memento.app.data.local.dao.MediaNameRow>).groupBy({ it.mediaItemId }, { it.name })
        val consumptionsByMedia = (values[3] as List<ConsumptionEntity>).groupBy { it.mediaItemId }
        val progressByConsumption = (values[4] as List<ProgressEntryEntity>).groupBy { it.consumptionId }
        val reflectionsByConsumption = (values[5] as List<ReflectionEntity>).groupBy { it.consumptionId }
        val tagsByMedia = (values[6] as List<com.memento.app.data.local.dao.MediaTagRow>).groupBy { it.mediaItemId }
        val externalRefsByMedia = (values[7] as List<ExternalMediaRefEntity>).groupBy { it.mediaItemId }
        items.map { media ->
            val consumptions = consumptionsByMedia[media.id].orEmpty()
            MediaDetail(
                media = media.toDomain(),
                creators = creatorsByMedia[media.id].orEmpty(),
                genres = genresByMedia[media.id].orEmpty(),
                consumptions = consumptions.map { it.toDomain() },
                progress = consumptions.flatMap { progressByConsumption[it.id].orEmpty() }.map { it.toDomain() },
                reflections = consumptions.flatMap { reflectionsByConsumption[it.id].orEmpty() }.map { it.toDomain() },
                tags = tagsByMedia[media.id].orEmpty().map { Tag(it.tagId, it.name, it.normalizedName, it.createdAt) },
                externalRefs = externalRefsByMedia[media.id].orEmpty().map {
                    MediaExternalReference(it.provider, it.externalId, it.mediaType, it.externalUrl)
                },
            )
        }
    }

    override suspend fun addManual(
        input: AddMediaInput,
        initialStatus: ConsumptionStatus,
        completion: CompletedMediaInput?,
    ): String {
        require(input.title.isNotBlank()) { "El título es obligatorio" }
        validateInitialCompletion(initialStatus, completion)
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
                    isFavorite = completion?.favorite == true,
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
            insertInitialConsumption(mediaId, initialStatus, completion, now)
        }
        return mediaId
    }

    override suspend fun addExternal(
        input: MetadataSearchResult,
        initialStatus: ConsumptionStatus,
        completion: CompletedMediaInput?,
    ): SaveExternalResult = database.withTransaction {
        validateInitialCompletion(initialStatus, completion)
        val now = Instant.now()
        mediaDao.findByExternalRef(input.provider, input.externalId, input.type)?.let { existingId ->
            if (completion != null) {
                if (completion.favorite) {
                    mediaDao.getById(existingId)?.takeUnless { it.isFavorite }?.let { existing ->
                        mediaDao.update(existing.copy(isFavorite = true, updatedAt = now))
                    }
                }
                insertInitialConsumption(existingId, initialStatus, completion, now)
            }
            return@withTransaction SaveExternalResult(existingId, wasDuplicate = true)
        }

        require(input.title.isNotBlank()) { "El título es obligatorio" }
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
                isFavorite = completion?.favorite == true,
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
        insertInitialConsumption(mediaId, initialStatus, completion, now)
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

    override suspend fun deleteMedia(mediaId: String) = database.withTransaction {
        mediaDao.deleteById(mediaId)
        mediaDao.deleteOrphanCreators()
        mediaDao.deleteOrphanGenres()
    }

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
            newConsumption(
                mediaId,
                ConsumptionStatus.COMPLETED,
                CompletedMediaInput(completedDate = date, ratingHalfStars = ratingHalfStars),
                now,
            )
                .also { consumptionDao.insert(it) }
        } else {
            active.copy(status = ConsumptionStatus.COMPLETED, completedDate = date, ratingHalfStars = ratingHalfStars, updatedAt = now)
                .also { consumptionDao.update(it) }
        }
        insertFinalReflection(completed.id, finalReflection, now)
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
    ) = database.withTransaction {
        ProgressValidator.validate(type, currentValue, totalValue, season, episode)
        requireActiveConsumption(consumptionId)
        consumptionDao.insertProgress(
            ProgressEntryEntity(UUID.randomUUID().toString(), consumptionId, type, currentValue, totalValue, season, episode, Instant.now()),
        )
    }

    override suspend fun saveReflection(consumptionId: String, type: ReflectionType, content: String): String =
        database.withTransaction {
            val cleanContent = content.trim()
            require(cleanContent.isNotEmpty())
            if (type == ReflectionType.NOTE) requireActiveConsumption(consumptionId)
            if (type == ReflectionType.QUOTE) require(consumptionDao.getById(consumptionId) != null) {
                "El consumo ya no existe"
            }
            val now = Instant.now()
            val existing = if (type == ReflectionType.FINAL_REFLECTION) consumptionDao.getReflection(consumptionId, type) else null
            if (existing != null) {
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

    override suspend fun createAndAttachTag(mediaId: String, name: String): Tag = database.withTransaction {
        require(mediaDao.getById(mediaId) != null) { "La obra ya no existe" }
        val displayName = TagNameNormalizer.displayName(name)
        val normalizedName = TagNameNormalizer.normalize(name)
        require(displayName.isNotEmpty()) { "El nombre de la etiqueta es obligatorio" }
        val tag = mediaDao.findTag(normalizedName) ?: TagEntity(
            id = UUID.randomUUID().toString(),
            name = displayName,
            normalizedName = normalizedName,
            createdAt = Instant.now(),
        ).also { mediaDao.insertTag(it) }
        mediaDao.insertMediaTag(MediaTagCrossRef(mediaId, tag.id))
        tag.toDomain()
    }

    override suspend fun attachTag(mediaId: String, tagId: String) = database.withTransaction {
        require(mediaDao.getById(mediaId) != null) { "La obra ya no existe" }
        require(mediaDao.getTagById(tagId) != null) { "La etiqueta ya no existe" }
        mediaDao.insertMediaTag(MediaTagCrossRef(mediaId, tagId))
        Unit
    }

    override suspend fun removeTag(mediaId: String, tagId: String) {
        mediaDao.deleteMediaTag(mediaId, tagId)
    }

    private suspend fun insertInitialConsumption(
        mediaId: String,
        status: ConsumptionStatus,
        completion: CompletedMediaInput?,
        now: Instant,
    ) {
        val consumption = newConsumption(mediaId, status, completion, now)
        consumptionDao.insert(consumption)
        insertFinalReflection(consumption.id, completion?.finalReflection, now)
    }

    private suspend fun insertFinalReflection(consumptionId: String, content: String?, now: Instant) {
        content?.trim()?.takeIf(String::isNotEmpty)?.let { cleanContent ->
            consumptionDao.insertReflection(
                ReflectionEntity(
                    UUID.randomUUID().toString(),
                    consumptionId,
                    ReflectionType.FINAL_REFLECTION,
                    cleanContent,
                    now,
                    now,
                ),
            )
        }
    }

    private fun validateInitialCompletion(status: ConsumptionStatus, completion: CompletedMediaInput?) {
        require((status == ConsumptionStatus.COMPLETED) == (completion != null)) {
            "Los datos de finalización son obligatorios únicamente para una obra terminada"
        }
        completion?.let { validateRating(it.ratingHalfStars) }
    }

    private fun newConsumption(
        mediaId: String,
        status: ConsumptionStatus,
        completion: CompletedMediaInput?,
        now: Instant,
    ): ConsumptionEntity =
        ConsumptionEntity(
            id = UUID.randomUUID().toString(),
            mediaItemId = mediaId,
            status = status,
            startedDate = if (status == ConsumptionStatus.IN_PROGRESS) LocalDate.now() else null,
            completedDate = completion?.completedDate,
            ratingHalfStars = completion?.ratingHalfStars,
            createdAt = now,
            updatedAt = now,
        )

    private fun validateRating(rating: Int?) {
        require(rating == null || rating in 1..10) { "La valoración debe estar entre 0,5 y 5 estrellas" }
    }

    private suspend fun requireActiveConsumption(consumptionId: String) {
        val consumption = consumptionDao.getById(consumptionId)
        require(
            consumption?.status == ConsumptionStatus.PLANNED ||
                consumption?.status == ConsumptionStatus.IN_PROGRESS,
        ) { "Este consumo ya no está activo" }
    }
}

private fun String.normalized(): String = Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
    .replace("\\p{Mn}+".toRegex(), "")
    .trim()

private fun TagEntity.toDomain() = Tag(id, name, normalizedName, createdAt)

private fun HomeMediaRow.toHomeSummary() = HomeMediaSummary(
    media = media.toDomain(),
    consumptionId = consumptionId,
    ratingHalfStars = ratingHalfStars,
    completedDate = completedDate,
    creator = creatorName,
    genres = listOfNotNull(genreOne, genreTwo),
    additionalGenreCount = (genreCount - 2).coerceAtLeast(0),
    latestProgress = if (progressId != null && progressType != null && progressRecordedAt != null) {
        ProgressEntry(
            id = progressId,
            consumptionId = consumptionId,
            progressType = progressType,
            currentValue = progressCurrentValue,
            totalValue = progressTotalValue,
            season = progressSeason,
            episode = progressEpisode,
            recordedAt = progressRecordedAt,
        )
    } else {
        null
    },
)
