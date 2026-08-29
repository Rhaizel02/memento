package com.memento.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.CreatorRole
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.RecommendationFeedbackType
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "media_items",
    indices = [Index("type"), Index("title"), Index("releaseYear"), Index("updatedAt")],
)
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val type: MediaType,
    val title: String,
    val originalTitle: String?,
    val description: String?,
    val releaseDate: LocalDate?,
    val releaseYear: Int?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val isFavorite: Boolean,
    val isManual: Boolean,
    val runtimeMinutes: Int?,
    val pageCount: Int?,
    val seasonCount: Int?,
    val episodeCount: Int?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "external_media_refs",
    primaryKeys = ["mediaItemId", "provider"],
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mediaItemId"), Index(value = ["provider", "externalId", "mediaType"], unique = true)],
)
data class ExternalMediaRefEntity(
    val mediaItemId: String,
    val provider: MetadataProvider,
    val externalId: String,
    val mediaType: MediaType,
    val externalUrl: String?,
)

@Entity(tableName = "creators", indices = [Index(value = ["normalizedName"], unique = true)])
data class CreatorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
)

@Entity(
    tableName = "media_creator_cross_ref",
    primaryKeys = ["mediaItemId", "creatorId", "role"],
    foreignKeys = [
        ForeignKey(entity = MediaItemEntity::class, parentColumns = ["id"], childColumns = ["mediaItemId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CreatorEntity::class, parentColumns = ["id"], childColumns = ["creatorId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("mediaItemId"), Index("creatorId")],
)
data class MediaCreatorCrossRef(
    val mediaItemId: String,
    val creatorId: String,
    val role: CreatorRole,
)

@Entity(tableName = "genres", indices = [Index(value = ["normalizedName"], unique = true)])
data class GenreEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
)

@Entity(
    tableName = "media_genre_cross_ref",
    primaryKeys = ["mediaItemId", "genreId"],
    foreignKeys = [
        ForeignKey(entity = MediaItemEntity::class, parentColumns = ["id"], childColumns = ["mediaItemId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GenreEntity::class, parentColumns = ["id"], childColumns = ["genreId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("mediaItemId"), Index("genreId")],
)
data class MediaGenreCrossRef(val mediaItemId: String, val genreId: String)

@Entity(
    tableName = "consumptions",
    foreignKeys = [
        ForeignKey(entity = MediaItemEntity::class, parentColumns = ["id"], childColumns = ["mediaItemId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("mediaItemId"), Index("status"), Index("completedDate"), Index("updatedAt")],
)
data class ConsumptionEntity(
    @PrimaryKey val id: String,
    val mediaItemId: String,
    val status: ConsumptionStatus,
    val startedDate: LocalDate?,
    val completedDate: LocalDate?,
    val ratingHalfStars: Int?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "progress_entries",
    foreignKeys = [
        ForeignKey(entity = ConsumptionEntity::class, parentColumns = ["id"], childColumns = ["consumptionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("consumptionId"), Index(value = ["consumptionId", "recordedAt"])],
)
data class ProgressEntryEntity(
    @PrimaryKey val id: String,
    val consumptionId: String,
    val progressType: ProgressType,
    val currentValue: Double?,
    val totalValue: Double?,
    val season: Int?,
    val episode: Int?,
    val recordedAt: Instant,
)

@Entity(
    tableName = "reflections",
    foreignKeys = [
        ForeignKey(entity = ConsumptionEntity::class, parentColumns = ["id"], childColumns = ["consumptionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("consumptionId"), Index(value = ["consumptionId", "type"]), Index("createdAt")],
)
data class ReflectionEntity(
    @PrimaryKey val id: String,
    val consumptionId: String,
    val type: ReflectionType,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "remember_exposures",
    foreignKeys = [
        ForeignKey(entity = ConsumptionEntity::class, parentColumns = ["id"], childColumns = ["consumptionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ReflectionEntity::class, parentColumns = ["id"], childColumns = ["reflectionId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("consumptionId"), Index("reflectionId"), Index("shownAt")],
)
data class RememberExposureEntity(
    @PrimaryKey val id: String,
    val consumptionId: String,
    val reflectionId: String?,
    val shownAt: Instant,
)

@Entity(
    tableName = "recommendation_candidates",
    primaryKeys = ["provider", "externalId", "mediaType"],
    indices = [Index("mediaType"), Index("fetchedAt")],
)
data class RecommendationCandidateEntity(
    val provider: MetadataProvider,
    val externalId: String,
    val mediaType: MediaType,
    val externalUrl: String?,
    val title: String,
    val originalTitle: String?,
    val description: String?,
    val releaseDate: LocalDate?,
    val releaseYear: Int?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val creatorsJson: String,
    val genresJson: String,
    val runtimeMinutes: Int?,
    val pageCount: Int?,
    val seasonCount: Int?,
    val episodeCount: Int?,
    val fetchedAt: Instant,
)

@Entity(
    tableName = "recommendation_feedback",
    indices = [Index(value = ["provider", "externalId", "mediaType"], unique = true), Index("createdAt")],
)
data class RecommendationFeedbackEntity(
    @PrimaryKey val id: String,
    val provider: MetadataProvider,
    val externalId: String,
    val mediaType: MediaType,
    val feedbackType: RecommendationFeedbackType,
    val createdAt: Instant,
)

@Entity(
    tableName = "ai_insights",
    foreignKeys = [
        ForeignKey(entity = ReflectionEntity::class, parentColumns = ["id"], childColumns = ["reflectionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("reflectionId"), Index("createdAt")],
)
data class AiInsightEntity(
    @PrimaryKey val id: String,
    val reflectionId: String,
    val capability: String,
    val content: String,
    val createdAt: Instant,
)
