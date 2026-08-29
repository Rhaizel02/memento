package com.memento.app.data

import com.memento.app.FakeMediaRepository
import com.memento.app.FakeMetadataRepository
import com.memento.app.data.local.dao.RecommendationDao
import com.memento.app.data.local.entity.RecommendationCandidateEntity
import com.memento.app.data.local.entity.RecommendationFeedbackEntity
import com.memento.app.data.repository.RoomRecommendationRepository
import com.memento.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RecommendationCacheRepositoryTest {
    @Test fun `fresh cache performs no remote calls`() = runTest {
        val dao = FakeRecommendationDao(latest = Instant.now())
        val metadata = FakeMetadataRepository()
        val repository = RoomRecommendationRepository(readyHistory(), dao, metadata)

        repository.refreshCandidates()

        assertEquals(0, metadata.recommendationRequests)
    }

    @Test fun `expired cache refreshes while remote failure keeps stale rows`() = runTest {
        val stale = candidateEntity(Instant.now().minusSeconds(3 * 24 * 60 * 60))
        val dao = FakeRecommendationDao(latest = stale.fetchedAt).apply { candidates.value = listOf(stale) }
        val metadata = FakeMetadataRepository()
        val repository = RoomRecommendationRepository(readyHistory(), dao, metadata)

        repository.refreshCandidates()

        assertTrue(metadata.recommendationRequests > 0)
        assertEquals(listOf(stale), dao.candidates.value)
        assertEquals(0, dao.deleteCalls)
    }

    @Test fun `missing cache and remote failure completes without persisting`() = runTest {
        val dao = FakeRecommendationDao(latest = null)
        val metadata = FakeMetadataRepository()
        RoomRecommendationRepository(readyHistory(), dao, metadata).refreshCandidates()

        assertTrue(metadata.recommendationRequests > 0)
        assertTrue(dao.candidates.value.isEmpty())
    }

    private fun readyHistory() = FakeMediaRepository().apply {
        allDetails.value = (1..3).map { index ->
            val media = MediaItem("m$index", MediaType.BOOK, "Libro $index", createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)
            MediaDetail(
                media, listOf("Autora"), listOf("Ficción"),
                listOf(Consumption("c$index", media.id, ConsumptionStatus.COMPLETED, ratingHalfStars = 9, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)),
                emptyList(), emptyList(),
            )
        }
    }

    private fun candidateEntity(fetchedAt: Instant) = RecommendationCandidateEntity(
        MetadataProvider.OPEN_LIBRARY, "OL1W", MediaType.BOOK, null, "Candidato", null, null, null, null,
        null, null, "[]", "[\"Ficción\"]", null, null, null, null, fetchedAt,
    )
}

private class FakeRecommendationDao(var latest: Instant?) : RecommendationDao {
    val candidates = MutableStateFlow<List<RecommendationCandidateEntity>>(emptyList())
    private val feedback = MutableStateFlow<List<RecommendationFeedbackEntity>>(emptyList())
    var deleteCalls = 0
    override fun observeUnseenCandidates(): Flow<List<RecommendationCandidateEntity>> = candidates
    override fun observeFeedback(): Flow<List<RecommendationFeedbackEntity>> = feedback
    override suspend fun upsertCandidates(candidates: List<RecommendationCandidateEntity>) {
        this.candidates.value = candidates
        latest = candidates.maxOfOrNull { it.fetchedAt }
    }
    override suspend fun upsertFeedback(feedback: RecommendationFeedbackEntity) { this.feedback.value = listOf(feedback) }
    override suspend fun latestCandidateFetchAt(): Instant? = latest
    override suspend fun deleteCandidatesOlderThan(threshold: Instant) { deleteCalls++ }
}
