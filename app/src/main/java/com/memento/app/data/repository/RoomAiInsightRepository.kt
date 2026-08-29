package com.memento.app.data.repository

import com.memento.app.ai.AiCapability
import com.memento.app.data.local.dao.AiInsightDao
import com.memento.app.data.local.entity.AiInsightEntity
import com.memento.app.data.local.entity.AiInsightSourceCrossRef
import com.memento.app.data.local.database.MementoDatabase
import com.memento.app.domain.repository.AiInsight
import com.memento.app.domain.repository.AiInsightSource
import com.memento.app.domain.repository.AiInsightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import androidx.room.withTransaction
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomAiInsightRepository @Inject constructor(
    private val database: MementoDatabase,
    private val dao: AiInsightDao,
) : AiInsightRepository {
    override fun observe(reflectionId: String): Flow<List<AiInsight>> = combine(
        dao.observeForReflection(reflectionId),
        dao.observeSourcesForReflection(reflectionId),
    ) { rows, sourceRows ->
        val sourcesByInsight = sourceRows.groupBy { it.aiInsightId }
        rows.map { row ->
            AiInsight(
                row.id,
                sourcesByInsight[row.id].orEmpty().map { AiInsightSource(it.reflectionId, it.mediaTitle, it.reflectionCreatedAt) },
                AiCapability.valueOf(row.capability),
                row.content,
                row.createdAt,
            )
        }
    }

    override suspend fun save(sourceReflectionIds: List<String>, capability: AiCapability, content: String): String {
        require(content.isNotBlank())
        val sourceIds = sourceReflectionIds.distinct()
        require(sourceIds.isNotEmpty())
        return UUID.randomUUID().toString().also { id ->
            database.withTransaction {
                dao.insert(AiInsightEntity(id, capability.name, content.trim(), Instant.now()))
                dao.insertSources(sourceIds.map { AiInsightSourceCrossRef(id, it) })
            }
        }
    }
}
