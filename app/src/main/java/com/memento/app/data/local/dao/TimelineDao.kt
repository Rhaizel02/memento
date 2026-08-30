package com.memento.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.ReflectionType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

data class TimelineConsumptionRow(
    val consumptionId: String,
    val mediaItemId: String,
    val status: ConsumptionStatus,
    val startedDate: LocalDate?,
    val completedDate: LocalDate?,
    val ratingHalfStars: Int?,
    val consumptionCreatedAt: Instant,
    val consumptionUpdatedAt: Instant,
    val mediaType: MediaType,
    val title: String,
    val posterUrl: String?,
    val isFavorite: Boolean,
    val isReconsumption: Boolean,
)

data class TimelineProgressRow(
    val progressId: String,
    val consumptionId: String,
    val progressType: ProgressType,
    val currentValue: Double?,
    val totalValue: Double?,
    val season: Int?,
    val episode: Int?,
    val recordedAt: Instant,
    val mediaItemId: String,
    val mediaType: MediaType,
    val title: String,
    val posterUrl: String?,
    val isFavorite: Boolean,
)

data class TimelineReflectionRow(
    val reflectionId: String,
    val consumptionId: String,
    val reflectionType: ReflectionType,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val mediaItemId: String,
    val mediaType: MediaType,
    val title: String,
    val posterUrl: String?,
    val isFavorite: Boolean,
)

@Dao
interface TimelineDao {
    @Query(
        """
        SELECT c.id AS consumptionId, c.mediaItemId AS mediaItemId, c.status AS status,
            c.startedDate AS startedDate, c.completedDate AS completedDate,
            c.ratingHalfStars AS ratingHalfStars, c.createdAt AS consumptionCreatedAt,
            c.updatedAt AS consumptionUpdatedAt, m.type AS mediaType, m.title AS title,
            m.posterUrl AS posterUrl, m.isFavorite AS isFavorite,
            CASE WHEN EXISTS (
                SELECT 1 FROM consumptions previous
                WHERE previous.mediaItemId = c.mediaItemId
                  AND (previous.createdAt < c.createdAt OR (previous.createdAt = c.createdAt AND previous.id < c.id))
            ) THEN 1 ELSE 0 END AS isReconsumption
        FROM consumptions c
        INNER JOIN media_items m ON m.id = c.mediaItemId
        WHERE c.startedDate IS NOT NULL AND (:mediaType IS NULL OR m.type = :mediaType)
        ORDER BY c.startedDate DESC, c.createdAt DESC, c.id DESC
        LIMIT :limit
        """,
    )
    fun observeStarted(mediaType: MediaType?, limit: Int): Flow<List<TimelineConsumptionRow>>

    @Query(
        """
        SELECT c.id AS consumptionId, c.mediaItemId AS mediaItemId, c.status AS status,
            c.startedDate AS startedDate, c.completedDate AS completedDate,
            c.ratingHalfStars AS ratingHalfStars, c.createdAt AS consumptionCreatedAt,
            c.updatedAt AS consumptionUpdatedAt, m.type AS mediaType, m.title AS title,
            m.posterUrl AS posterUrl, m.isFavorite AS isFavorite,
            CASE WHEN EXISTS (
                SELECT 1 FROM consumptions previous
                WHERE previous.mediaItemId = c.mediaItemId
                  AND (previous.createdAt < c.createdAt OR (previous.createdAt = c.createdAt AND previous.id < c.id))
            ) THEN 1 ELSE 0 END AS isReconsumption
        FROM consumptions c
        INNER JOIN media_items m ON m.id = c.mediaItemId
        WHERE c.status = 'COMPLETED' AND c.completedDate IS NOT NULL
          AND (:mediaType IS NULL OR m.type = :mediaType)
        ORDER BY c.completedDate DESC, c.createdAt DESC, c.id DESC
        LIMIT :limit
        """,
    )
    fun observeCompleted(mediaType: MediaType?, limit: Int): Flow<List<TimelineConsumptionRow>>

    @Query(
        """
        SELECT p.id AS progressId, p.consumptionId AS consumptionId,
            p.progressType AS progressType, p.currentValue AS currentValue,
            p.totalValue AS totalValue, p.season AS season, p.episode AS episode,
            p.recordedAt AS recordedAt, m.id AS mediaItemId, m.type AS mediaType,
            m.title AS title, m.posterUrl AS posterUrl, m.isFavorite AS isFavorite
        FROM progress_entries p
        INNER JOIN consumptions c ON c.id = p.consumptionId
        INNER JOIN media_items m ON m.id = c.mediaItemId
        WHERE (:mediaType IS NULL OR m.type = :mediaType)
        ORDER BY p.recordedAt DESC, p.id DESC
        LIMIT :limit
        """,
    )
    fun observeProgress(mediaType: MediaType?, limit: Int): Flow<List<TimelineProgressRow>>

    @Query(
        """
        SELECT r.id AS reflectionId, r.consumptionId AS consumptionId,
            r.type AS reflectionType, r.content AS content, r.createdAt AS createdAt,
            r.updatedAt AS updatedAt, m.id AS mediaItemId, m.type AS mediaType,
            m.title AS title, m.posterUrl AS posterUrl, m.isFavorite AS isFavorite
        FROM reflections r
        INNER JOIN consumptions c ON c.id = r.consumptionId
        INNER JOIN media_items m ON m.id = c.mediaItemId
        WHERE (:mediaType IS NULL OR m.type = :mediaType)
        ORDER BY r.createdAt DESC, r.id DESC
        LIMIT :limit
        """,
    )
    fun observeReflections(mediaType: MediaType?, limit: Int): Flow<List<TimelineReflectionRow>>
}
