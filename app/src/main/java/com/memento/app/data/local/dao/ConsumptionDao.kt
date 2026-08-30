package com.memento.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.memento.app.data.local.entity.ConsumptionEntity
import com.memento.app.data.local.entity.ProgressEntryEntity
import com.memento.app.data.local.entity.ReflectionEntity
import com.memento.app.domain.model.ReflectionType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

data class CulturalProfileCompletionRow(
    val mediaItemId: String,
    val completedDate: LocalDate?,
    val ratingHalfStars: Int?,
    val updatedAt: Instant,
)

@Dao
interface ConsumptionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(consumption: ConsumptionEntity)

    @Update
    suspend fun update(consumption: ConsumptionEntity)

    @Query("SELECT * FROM consumptions WHERE id = :consumptionId")
    suspend fun getById(consumptionId: String): ConsumptionEntity?

    @Query(
        """
        SELECT * FROM consumptions
        WHERE mediaItemId = :mediaId AND status IN ('PLANNED', 'IN_PROGRESS')
        ORDER BY updatedAt DESC LIMIT 1
        """,
    )
    suspend fun getActive(mediaId: String): ConsumptionEntity?

    @Query("SELECT * FROM consumptions WHERE mediaItemId = :mediaId ORDER BY createdAt DESC")
    fun observeForMedia(mediaId: String): Flow<List<ConsumptionEntity>>

    @Query("SELECT * FROM consumptions ORDER BY mediaItemId, createdAt DESC")
    fun observeAll(): Flow<List<ConsumptionEntity>>

    @Query(
        """
        SELECT mediaItemId, completedDate, ratingHalfStars, updatedAt
        FROM consumptions
        WHERE status = 'COMPLETED' AND completedDate IS NOT NULL
        ORDER BY completedDate, updatedAt
        """,
    )
    fun observeCulturalProfileCompletions(): Flow<List<CulturalProfileCompletionRow>>

    @Query("DELETE FROM consumptions WHERE id = :consumptionId")
    suspend fun deleteById(consumptionId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProgress(entry: ProgressEntryEntity)

    @Query(
        """
        SELECT p.* FROM progress_entries p
        INNER JOIN consumptions c ON c.id = p.consumptionId
        WHERE c.mediaItemId = :mediaId
        ORDER BY p.recordedAt DESC
        """,
    )
    fun observeProgressForMedia(mediaId: String): Flow<List<ProgressEntryEntity>>

    @Query("SELECT * FROM progress_entries ORDER BY consumptionId, recordedAt DESC")
    fun observeAllProgress(): Flow<List<ProgressEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReflection(reflection: ReflectionEntity)

    @Update
    suspend fun updateReflection(reflection: ReflectionEntity)

    @Query("SELECT * FROM reflections WHERE id = :reflectionId")
    suspend fun getReflectionById(reflectionId: String): ReflectionEntity?

    @Query("SELECT * FROM reflections WHERE consumptionId = :consumptionId AND type = :type ORDER BY createdAt DESC LIMIT 1")
    suspend fun getReflection(consumptionId: String, type: ReflectionType): ReflectionEntity?

    @Query(
        """
        SELECT r.* FROM reflections r
        INNER JOIN consumptions c ON c.id = r.consumptionId
        WHERE c.mediaItemId = :mediaId
        ORDER BY r.createdAt DESC
        """,
    )
    fun observeReflectionsForMedia(mediaId: String): Flow<List<ReflectionEntity>>

    @Query("SELECT * FROM reflections ORDER BY consumptionId, createdAt DESC")
    fun observeAllReflections(): Flow<List<ReflectionEntity>>
}
