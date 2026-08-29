package com.memento.app.data.repository

import com.memento.app.ai.AiCapability
import com.memento.app.data.local.dao.AiInsightDao
import com.memento.app.data.local.entity.AiInsightEntity
import com.memento.app.domain.repository.AiInsight
import com.memento.app.domain.repository.AiInsightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomAiInsightRepository @Inject constructor(private val dao: AiInsightDao) : AiInsightRepository {
    override fun observe(reflectionId: String): Flow<List<AiInsight>> = dao.observeForReflection(reflectionId).map { rows ->
        rows.map { AiInsight(it.id, it.reflectionId, AiCapability.valueOf(it.capability), it.content, it.createdAt) }
    }

    override suspend fun save(reflectionId: String, capability: AiCapability, content: String): String {
        require(content.isNotBlank())
        return UUID.randomUUID().toString().also { id ->
            dao.insert(AiInsightEntity(id, reflectionId, capability.name, content.trim(), Instant.now()))
        }
    }
}
