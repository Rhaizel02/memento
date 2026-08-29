package com.memento.app.domain

import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.RecommendationFeedbackType
import com.memento.app.domain.recommendation.RecommendationEngine
import com.memento.app.domain.recommendation.RecommendationKey
import com.memento.app.domain.recommendation.RecommendationReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RecommendationEngineTest {
    @Test
    fun `rating weights follow the product scale`() {
        assertEquals(5.0, RecommendationEngine.ratingWeight(10), 0.0)
        assertEquals(0.0, RecommendationEngine.ratingWeight(6), 0.0)
        assertEquals(-5.0, RecommendationEngine.ratingWeight(1), 0.0)
    }

    @Test
    fun `profile needs three meaningful works`() {
        val profile = RecommendationEngine.buildTasteProfile(
            listOf(detail("a", 10), detail("b", 8)),
        )
        val recommendations = RecommendationEngine.recommend(profile, listOf(candidate()))

        assertTrue(recommendations.isEmpty())
    }

    @Test
    fun `matching genre produces ranked explainable recommendation`() {
        val profile = RecommendationEngine.buildTasteProfile(
            listOf(detail("a", 10), detail("b", 9), detail("c", 8)),
        )
        val recommendations = RecommendationEngine.recommend(
            profile,
            listOf(candidate(), candidate(id = "2", genres = listOf("Comedia"))),
        )

        assertEquals(2, recommendations.size)
        assertTrue(recommendations.first().affinityScore > recommendations.last().affinityScore)
        assertEquals(RecommendationReason.Genre("Ciencia ficción"), recommendations.first().reasons.first())
    }

    @Test
    fun `negative and already known feedback remove candidates`() {
        val profile = RecommendationEngine.buildTasteProfile(
            listOf(detail("a", 10), detail("b", 9), detail("c", 8)),
        )
        val first = candidate("1")
        val second = candidate("2")
        val feedback = mapOf(
            RecommendationKey(first.provider, first.externalId, first.type) to RecommendationFeedbackType.NOT_INTERESTED,
            RecommendationKey(second.provider, second.externalId, second.type) to RecommendationFeedbackType.ALREADY_KNOWN,
        )

        assertTrue(RecommendationEngine.recommend(profile, listOf(first, second), feedback).isEmpty())
    }

    private fun detail(id: String, rating: Int) = MediaDetail(
        media = MediaItem(id, MediaType.MOVIE, "Obra $id", createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH),
        creators = listOf("Directora"),
        genres = listOf("Ciencia ficción"),
        consumptions = listOf(
            Consumption("c$id", id, ConsumptionStatus.COMPLETED, ratingHalfStars = rating, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH),
        ),
        progress = emptyList(),
        reflections = emptyList(),
    )

    private fun candidate(id: String = "1", genres: List<String> = listOf("Ciencia ficción")) = MetadataSearchResult(
        provider = MetadataProvider.TMDB,
        externalId = id,
        externalUrl = null,
        type = MediaType.MOVIE,
        title = "Candidata $id",
        genres = genres,
    )
}
