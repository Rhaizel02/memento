package com.memento.app.domain.repository

import com.memento.app.ai.AiCapability
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class AiInsight(val id: String, val reflectionId: String, val capability: AiCapability, val content: String, val createdAt: Instant)

interface AiInsightRepository {
    fun observe(reflectionId: String): Flow<List<AiInsight>>
    suspend fun save(reflectionId: String, capability: AiCapability, content: String): String
}
