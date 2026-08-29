package com.memento.app.data.repository

import androidx.room.withTransaction
import com.memento.app.BuildConfig
import com.memento.app.backup.*
import com.memento.app.data.local.dao.BackupDao
import com.memento.app.data.local.database.MementoDatabase
import com.memento.app.data.local.entity.*
import com.memento.app.domain.model.*
import com.memento.app.domain.repository.BackupRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomBackupRepository @Inject constructor(
    private val database: MementoDatabase,
    private val dao: BackupDao,
) : BackupRepository {
    override suspend fun exportJson(): String {
        val data = database.withTransaction {
            BackupData(
                mediaItems = dao.mediaItems().map { it.toBackup() },
                externalRefs = dao.externalRefs().map { it.toBackup() },
                creators = dao.creators().map { it.toBackup() },
                mediaCreators = dao.mediaCreators().map { it.toBackup() },
                genres = dao.genres().map { it.toBackup() },
                mediaGenres = dao.mediaGenres().map { it.toBackup() },
                consumptions = dao.consumptions().map { it.toBackup() },
                progressEntries = dao.progressEntries().map { it.toBackup() },
                reflections = dao.reflections().map { it.toBackup() },
                rememberExposures = dao.rememberExposures().map { it.toBackup() },
                recommendationFeedback = dao.recommendationFeedback().map { it.toBackup() },
                aiInsights = dao.aiInsights().map { it.toBackup() },
            )
        }
        return BackupCodec.encode(
            BackupEnvelope(BackupCodec.SCHEMA_VERSION, Instant.now().toString(), BuildConfig.VERSION_NAME, data),
        )
    }

    override fun preview(json: String) = BackupCodec.preview(BackupCodec.decodeAndValidate(json))

    override suspend fun restoreReplaceAll(json: String): BackupPreview {
        val envelope = BackupCodec.decodeAndValidate(json)
        val data = envelope.data
        database.withTransaction {
            dao.clearRememberExposures()
            dao.clearAiInsights()
            dao.clearReflections()
            dao.clearProgressEntries()
            dao.clearConsumptions()
            dao.clearMediaCreators()
            dao.clearMediaGenres()
            dao.clearExternalRefs()
            dao.clearRecommendationFeedback()
            dao.clearRecommendationCandidates()
            dao.clearMediaItems()
            dao.clearCreators()
            dao.clearGenres()

            dao.insertMediaItems(data.mediaItems.map { it.toEntity() })
            dao.insertCreators(data.creators.map { it.toEntity() })
            dao.insertGenres(data.genres.map { it.toEntity() })
            dao.insertExternalRefs(data.externalRefs.map { it.toEntity() })
            dao.insertMediaCreators(data.mediaCreators.map { it.toEntity() })
            dao.insertMediaGenres(data.mediaGenres.map { it.toEntity() })
            dao.insertConsumptions(data.consumptions.map { it.toEntity() })
            dao.insertProgressEntries(data.progressEntries.map { it.toEntity() })
            dao.insertReflections(data.reflections.map { it.toEntity() })
            dao.insertRememberExposures(data.rememberExposures.map { it.toEntity() })
            dao.insertRecommendationFeedback(data.recommendationFeedback.map { it.toEntity() })
            dao.insertAiInsights(data.aiInsights.map { it.toEntity() })
        }
        return BackupCodec.preview(envelope)
    }
}

private fun MediaItemEntity.toBackup() = BackupMediaItem(
    id, type.name, title, originalTitle, description, releaseDate?.toString(), releaseYear, posterUrl, backdropUrl,
    isFavorite, isManual, runtimeMinutes, pageCount, seasonCount, episodeCount, createdAt.toString(), updatedAt.toString(),
)
private fun ExternalMediaRefEntity.toBackup() = BackupExternalRef(mediaItemId, provider.name, externalId, mediaType.name, externalUrl)
private fun CreatorEntity.toBackup() = BackupCreator(id, name, normalizedName)
private fun MediaCreatorCrossRef.toBackup() = BackupMediaCreator(mediaItemId, creatorId, role.name)
private fun GenreEntity.toBackup() = BackupGenre(id, name, normalizedName)
private fun MediaGenreCrossRef.toBackup() = BackupMediaGenre(mediaItemId, genreId)
private fun ConsumptionEntity.toBackup() = BackupConsumption(
    id, mediaItemId, status.name, startedDate?.toString(), completedDate?.toString(), ratingHalfStars, createdAt.toString(), updatedAt.toString(),
)
private fun ProgressEntryEntity.toBackup() = BackupProgress(
    id, consumptionId, progressType.name, currentValue, totalValue, season, episode, recordedAt.toString(),
)
private fun ReflectionEntity.toBackup() = BackupReflection(id, consumptionId, type.name, content, createdAt.toString(), updatedAt.toString())
private fun RememberExposureEntity.toBackup() = BackupRememberExposure(id, consumptionId, reflectionId, shownAt.toString())
private fun RecommendationFeedbackEntity.toBackup() = BackupRecommendationFeedback(
    id, provider.name, externalId, mediaType.name, feedbackType.name, createdAt.toString(),
)
private fun AiInsightEntity.toBackup() = BackupAiInsight(id, reflectionId, capability, content, createdAt.toString())

private fun BackupMediaItem.toEntity() = MediaItemEntity(
    id, MediaType.valueOf(type), title, originalTitle, description, releaseDate?.let(LocalDate::parse), releaseYear,
    posterUrl, backdropUrl, isFavorite, isManual, runtimeMinutes, pageCount, seasonCount, episodeCount,
    Instant.parse(createdAt), Instant.parse(updatedAt),
)
private fun BackupExternalRef.toEntity() = ExternalMediaRefEntity(
    mediaItemId, MetadataProvider.valueOf(provider), externalId, MediaType.valueOf(mediaType), externalUrl,
)
private fun BackupCreator.toEntity() = CreatorEntity(id, name, normalizedName)
private fun BackupMediaCreator.toEntity() = MediaCreatorCrossRef(mediaItemId, creatorId, CreatorRole.valueOf(role))
private fun BackupGenre.toEntity() = GenreEntity(id, name, normalizedName)
private fun BackupMediaGenre.toEntity() = MediaGenreCrossRef(mediaItemId, genreId)
private fun BackupConsumption.toEntity() = ConsumptionEntity(
    id, mediaItemId, ConsumptionStatus.valueOf(status), startedDate?.let(LocalDate::parse), completedDate?.let(LocalDate::parse),
    ratingHalfStars, Instant.parse(createdAt), Instant.parse(updatedAt),
)
private fun BackupProgress.toEntity() = ProgressEntryEntity(
    id, consumptionId, ProgressType.valueOf(progressType), currentValue, totalValue, season, episode, Instant.parse(recordedAt),
)
private fun BackupReflection.toEntity() = ReflectionEntity(
    id, consumptionId, ReflectionType.valueOf(type), content, Instant.parse(createdAt), Instant.parse(updatedAt),
)
private fun BackupRememberExposure.toEntity() = RememberExposureEntity(id, consumptionId, reflectionId, Instant.parse(shownAt))
private fun BackupRecommendationFeedback.toEntity() = RecommendationFeedbackEntity(
    id, MetadataProvider.valueOf(provider), externalId, MediaType.valueOf(mediaType),
    RecommendationFeedbackType.valueOf(feedbackType), Instant.parse(createdAt),
)
private fun BackupAiInsight.toEntity() = AiInsightEntity(id, reflectionId, capability, content, Instant.parse(createdAt))
