package com.memento.app.domain.repository

import com.memento.app.ai.AiCapability
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class AiInsightSource(val reflectionId: String, val mediaTitle: String, val reflectionCreatedAt: Instant)
data class AiInsight(
    val id: String,
    val sources: List<AiInsightSource>,
    val capability: AiCapability,
    val content: String,
    val createdAt: Instant,
) {
    val sourceReflectionIds: List<String> get() = sources.map { it.reflectionId }
}

interface AiInsightRepository {
    fun observe(reflectionId: String): Flow<List<AiInsight>>
    suspend fun save(sourceReflectionIds: List<String>, capability: AiCapability, content: String): String
}
