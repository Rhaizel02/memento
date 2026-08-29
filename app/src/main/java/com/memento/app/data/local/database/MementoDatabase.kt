package com.memento.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.memento.app.data.local.converter.RoomConverters
import com.memento.app.data.local.dao.ConsumptionDao
import com.memento.app.data.local.dao.MediaDao
import com.memento.app.data.local.dao.RememberDao
import com.memento.app.data.local.dao.RecommendationDao
import com.memento.app.data.local.dao.BackupDao
import com.memento.app.data.local.entity.ConsumptionEntity
import com.memento.app.data.local.entity.CreatorEntity
import com.memento.app.data.local.entity.ExternalMediaRefEntity
import com.memento.app.data.local.entity.GenreEntity
import com.memento.app.data.local.entity.MediaCreatorCrossRef
import com.memento.app.data.local.entity.MediaGenreCrossRef
import com.memento.app.data.local.entity.MediaItemEntity
import com.memento.app.data.local.entity.ProgressEntryEntity
import com.memento.app.data.local.entity.ReflectionEntity
import com.memento.app.data.local.entity.RememberExposureEntity
import com.memento.app.data.local.entity.RecommendationCandidateEntity
import com.memento.app.data.local.entity.RecommendationFeedbackEntity
import com.memento.app.data.local.entity.AiInsightEntity
import com.memento.app.data.local.dao.AiInsightDao

@Database(
    entities = [
        MediaItemEntity::class,
        ExternalMediaRefEntity::class,
        CreatorEntity::class,
        MediaCreatorCrossRef::class,
        GenreEntity::class,
        MediaGenreCrossRef::class,
        ConsumptionEntity::class,
        ProgressEntryEntity::class,
        ReflectionEntity::class,
        RememberExposureEntity::class,
        RecommendationCandidateEntity::class,
        RecommendationFeedbackEntity::class,
        AiInsightEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class MementoDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun consumptionDao(): ConsumptionDao
    abstract fun rememberDao(): RememberDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun backupDao(): BackupDao
    abstract fun aiInsightDao(): AiInsightDao
}
