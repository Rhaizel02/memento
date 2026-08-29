package com.memento.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.memento.app.data.local.entity.AiInsightEntity
import com.memento.app.data.local.entity.AiInsightSourceCrossRef
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface AiInsightDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(insight: AiInsightEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSources(sources: List<AiInsightSourceCrossRef>)

    @Query(
        """
        SELECT i.* FROM ai_insights i
        INNER JOIN ai_insight_sources s ON s.aiInsightId = i.id
        WHERE s.reflectionId = :reflectionId
        ORDER BY i.createdAt DESC
        """,
    )
    fun observeForReflection(reflectionId: String): Flow<List<AiInsightEntity>>

    @Query(
        """
        SELECT s.aiInsightId AS aiInsightId, s.reflectionId AS reflectionId,
               m.title AS mediaTitle, r.createdAt AS reflectionCreatedAt
        FROM ai_insight_sources s
        INNER JOIN reflections r ON r.id = s.reflectionId
        INNER JOIN consumptions c ON c.id = r.consumptionId
        INNER JOIN media_items m ON m.id = c.mediaItemId
        WHERE s.aiInsightId IN (
            SELECT owner.aiInsightId FROM ai_insight_sources owner WHERE owner.reflectionId = :reflectionId
        )
        ORDER BY s.aiInsightId, r.createdAt
        """,
    )
    fun observeSourcesForReflection(reflectionId: String): Flow<List<AiInsightSourceRow>>
}

data class AiInsightSourceRow(
    val aiInsightId: String,
    val reflectionId: String,
    val mediaTitle: String,
    val reflectionCreatedAt: Instant,
)
