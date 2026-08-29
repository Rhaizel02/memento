package com.memento.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.memento.app.data.local.entity.ConsumptionEntity
import com.memento.app.data.local.entity.CreatorEntity
import com.memento.app.data.local.entity.ExternalMediaRefEntity
import com.memento.app.data.local.entity.GenreEntity
import com.memento.app.data.local.entity.MediaCreatorCrossRef
import com.memento.app.data.local.entity.MediaGenreCrossRef
import com.memento.app.data.local.entity.MediaItemEntity
import com.memento.app.data.local.entity.ProgressEntryEntity
import com.memento.app.data.local.entity.RecommendationFeedbackEntity
import com.memento.app.data.local.entity.ReflectionEntity
import com.memento.app.data.local.entity.RememberExposureEntity
import com.memento.app.data.local.entity.AiInsightEntity

@Dao
interface BackupDao {
    @Query("SELECT * FROM media_items ORDER BY id") suspend fun mediaItems(): List<MediaItemEntity>
    @Query("SELECT * FROM external_media_refs ORDER BY mediaItemId, provider") suspend fun externalRefs(): List<ExternalMediaRefEntity>
    @Query("SELECT * FROM creators ORDER BY id") suspend fun creators(): List<CreatorEntity>
    @Query("SELECT * FROM media_creator_cross_ref ORDER BY mediaItemId, creatorId, role") suspend fun mediaCreators(): List<MediaCreatorCrossRef>
    @Query("SELECT * FROM genres ORDER BY id") suspend fun genres(): List<GenreEntity>
    @Query("SELECT * FROM media_genre_cross_ref ORDER BY mediaItemId, genreId") suspend fun mediaGenres(): List<MediaGenreCrossRef>
    @Query("SELECT * FROM consumptions ORDER BY id") suspend fun consumptions(): List<ConsumptionEntity>
    @Query("SELECT * FROM progress_entries ORDER BY id") suspend fun progressEntries(): List<ProgressEntryEntity>
    @Query("SELECT * FROM reflections ORDER BY id") suspend fun reflections(): List<ReflectionEntity>
    @Query("SELECT * FROM remember_exposures ORDER BY id") suspend fun rememberExposures(): List<RememberExposureEntity>
    @Query("SELECT * FROM recommendation_feedback ORDER BY id") suspend fun recommendationFeedback(): List<RecommendationFeedbackEntity>
    @Query("SELECT * FROM ai_insights ORDER BY id") suspend fun aiInsights(): List<AiInsightEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertMediaItems(rows: List<MediaItemEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertExternalRefs(rows: List<ExternalMediaRefEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCreators(rows: List<CreatorEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertMediaCreators(rows: List<MediaCreatorCrossRef>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertGenres(rows: List<GenreEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertMediaGenres(rows: List<MediaGenreCrossRef>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertConsumptions(rows: List<ConsumptionEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertProgressEntries(rows: List<ProgressEntryEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertReflections(rows: List<ReflectionEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertRememberExposures(rows: List<RememberExposureEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertRecommendationFeedback(rows: List<RecommendationFeedbackEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAiInsights(rows: List<AiInsightEntity>)

    @Query("DELETE FROM remember_exposures") suspend fun clearRememberExposures()
    @Query("DELETE FROM reflections") suspend fun clearReflections()
    @Query("DELETE FROM progress_entries") suspend fun clearProgressEntries()
    @Query("DELETE FROM consumptions") suspend fun clearConsumptions()
    @Query("DELETE FROM media_creator_cross_ref") suspend fun clearMediaCreators()
    @Query("DELETE FROM media_genre_cross_ref") suspend fun clearMediaGenres()
    @Query("DELETE FROM external_media_refs") suspend fun clearExternalRefs()
    @Query("DELETE FROM media_items") suspend fun clearMediaItems()
    @Query("DELETE FROM creators") suspend fun clearCreators()
    @Query("DELETE FROM genres") suspend fun clearGenres()
    @Query("DELETE FROM recommendation_feedback") suspend fun clearRecommendationFeedback()
    @Query("DELETE FROM recommendation_candidates") suspend fun clearRecommendationCandidates()
    @Query("DELETE FROM ai_insights") suspend fun clearAiInsights()
}
