package com.memento.app.data.repository

import com.memento.app.data.local.dao.RememberCandidateRow
import com.memento.app.data.local.dao.RememberDao
import com.memento.app.data.local.entity.RememberExposureEntity
import com.memento.app.domain.remember.RememberCandidate
import com.memento.app.domain.remember.RememberEngine
import com.memento.app.domain.repository.RememberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRememberRepository @Inject constructor(private val dao: RememberDao) : RememberRepository {
    private val engine = RememberEngine()

    override fun observeRemember(): Flow<RememberCandidate?> = dao.observeCandidates().map { rows ->
        engine.select(rows.map(RememberCandidateRow::toDomain))?.candidate
    }

    override fun observeRemember(consumptionId: String): Flow<RememberCandidate?> =
        dao.observeCandidate(consumptionId).map { it?.toDomain() }

    override suspend fun recordExposure(consumptionId: String) {
        dao.insertExposure(
            RememberExposureEntity(
                id = UUID.randomUUID().toString(),
                consumptionId = consumptionId,
                reflectionId = dao.getFeaturedReflectionId(consumptionId),
                shownAt = Instant.now(),
            ),
        )
    }
}

private fun RememberCandidateRow.toDomain() = RememberCandidate(
    consumptionId = consumptionId,
    mediaId = mediaId,
    title = title,
    completedDate = completedDate,
    ratingHalfStars = ratingHalfStars,
    isFavorite = isFavorite,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    reflectionId = reflectionId,
    reflectionType = reflectionType,
    reflectionContent = reflectionContent,
    reflectionCount = reflectionCount,
    lastShownAt = lastShownAt,
)

