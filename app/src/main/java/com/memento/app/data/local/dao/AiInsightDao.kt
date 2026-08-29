package com.memento.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.memento.app.data.local.entity.AiInsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiInsightDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(insight: AiInsightEntity)

    @Query("SELECT * FROM ai_insights WHERE reflectionId = :reflectionId ORDER BY createdAt DESC")
    fun observeForReflection(reflectionId: String): Flow<List<AiInsightEntity>>
}
