package com.memento.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.memento.app.data.local.entity.RememberExposureEntity
import com.memento.app.domain.model.ReflectionType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

data class RememberCandidateRow(
    val consumptionId: String,
    val mediaId: String,
    val title: String,
    val completedDate: LocalDate,
    val ratingHalfStars: Int?,
    val isFavorite: Boolean,
    val posterUrl: String?,
    val backdropUrl: String?,
    val reflectionId: String,
    val reflectionType: ReflectionType,
    val reflectionContent: String,
    val reflectionCount: Int,
    val lastShownAt: Instant?,
)

@Dao
interface RememberDao {
    @Query(REMEMBER_SELECT)
    fun observeCandidates(exposuresBefore: Instant): Flow<List<RememberCandidateRow>>

    @Query(REMEMBER_SELECT + " AND c.id = :consumptionId")
    fun observeCandidate(consumptionId: String, exposuresBefore: Instant): Flow<RememberCandidateRow?>

    @Query(
        """
        SELECT r.id FROM reflections r WHERE r.consumptionId = :consumptionId
        ORDER BY CASE r.type WHEN 'FINAL_REFLECTION' THEN 0 WHEN 'LATER_REFLECTION' THEN 1 ELSE 2 END, r.createdAt DESC LIMIT 1
        """,
    )
    suspend fun getFeaturedReflectionId(consumptionId: String): String?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExposure(exposure: RememberExposureEntity)

    @Query("SELECT COUNT(*) FROM remember_exposures WHERE consumptionId = :consumptionId AND shownAt >= :since")
    suspend fun exposureCountSince(consumptionId: String, since: Instant): Int

    @Transaction
    suspend fun insertExposureOncePerDay(exposure: RememberExposureEntity, dayStart: Instant) {
        if (exposureCountSince(exposure.consumptionId, dayStart) == 0) insertExposure(exposure)
    }

    companion object {
        const val REMEMBER_SELECT = """
            SELECT
                c.id AS consumptionId,
                m.id AS mediaId,
                m.title AS title,
                c.completedDate AS completedDate,
                c.ratingHalfStars AS ratingHalfStars,
                m.isFavorite AS isFavorite,
                m.posterUrl AS posterUrl,
                m.backdropUrl AS backdropUrl,
                (SELECT r.id FROM reflections r WHERE r.consumptionId = c.id
                    ORDER BY CASE r.type WHEN 'FINAL_REFLECTION' THEN 0 WHEN 'LATER_REFLECTION' THEN 1 ELSE 2 END, r.createdAt DESC LIMIT 1) AS reflectionId,
                (SELECT r.type FROM reflections r WHERE r.consumptionId = c.id
                    ORDER BY CASE r.type WHEN 'FINAL_REFLECTION' THEN 0 WHEN 'LATER_REFLECTION' THEN 1 ELSE 2 END, r.createdAt DESC LIMIT 1) AS reflectionType,
                (SELECT r.content FROM reflections r WHERE r.consumptionId = c.id
                    ORDER BY CASE r.type WHEN 'FINAL_REFLECTION' THEN 0 WHEN 'LATER_REFLECTION' THEN 1 ELSE 2 END, r.createdAt DESC LIMIT 1) AS reflectionContent,
                (SELECT COUNT(*) FROM reflections r WHERE r.consumptionId = c.id) AS reflectionCount,
                (SELECT MAX(e.shownAt) FROM remember_exposures e
                    WHERE e.consumptionId = c.id AND e.shownAt < :exposuresBefore) AS lastShownAt
            FROM consumptions c
            INNER JOIN media_items m ON m.id = c.mediaItemId
            WHERE c.status = 'COMPLETED'
              AND c.completedDate IS NOT NULL
              AND EXISTS (SELECT 1 FROM reflections r WHERE r.consumptionId = c.id)
        """
    }
}
