package com.memento.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.memento.app.data.local.entity.RecommendationCandidateEntity
import com.memento.app.data.local.entity.RecommendationFeedbackEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface RecommendationDao {
    @Query(
        """
        SELECT candidate.* FROM recommendation_candidates candidate
        WHERE NOT EXISTS (
            SELECT 1 FROM external_media_refs reference
            WHERE reference.provider = candidate.provider
                AND reference.externalId = candidate.externalId
                AND reference.mediaType = candidate.mediaType
        )
        ORDER BY candidate.fetchedAt DESC
        """,
    )
    fun observeUnseenCandidates(): Flow<List<RecommendationCandidateEntity>>

    @Query("SELECT * FROM recommendation_feedback ORDER BY createdAt DESC")
    fun observeFeedback(): Flow<List<RecommendationFeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCandidates(candidates: List<RecommendationCandidateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeedback(feedback: RecommendationFeedbackEntity)

    @Query("DELETE FROM recommendation_candidates WHERE fetchedAt < :threshold")
    suspend fun deleteCandidatesOlderThan(threshold: Instant)
}
