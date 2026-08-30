package com.memento.app.domain

import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaExternalReference
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.RecommendationFeedbackType
import com.memento.app.domain.recommendation.Recommendation
import com.memento.app.domain.recommendation.RecommendationCategory
import com.memento.app.domain.recommendation.RecommendationEngine
import com.memento.app.domain.recommendation.RecommendationKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RecommendationEngineTest {
    @Test
    fun `single generic genre never reaches very affine`() {
        val profile = profile()

        val result = RecommendationEngine.recommend(
            profile,
            listOf(candidate("single", genres = listOf("Ciencia ficción"), rating = 9.3, votes = 50_000)),
        ).single()

        assertTrue(result.category != RecommendationCategory.VERY_AFFINE)
    }

    @Test
    fun `single book genre cannot recommend a movie by itself`() {
        val books = listOf(detail("a", 10), detail("b", 9), detail("c", 8)).map { detail ->
            detail.copy(
                media = detail.media.copy(type = MediaType.BOOK),
                creators = listOf("Autora"),
                externalRefs = listOf(MediaExternalReference(MetadataProvider.OPEN_LIBRARY, detail.media.id, MediaType.BOOK)),
            )
        }
        val profile = RecommendationEngine.buildTasteProfile(books)
        val movie = candidate("cross-media", genres = listOf("Ciencia ficción")).copy(creators = emptyList())

        assertTrue(RecommendationEngine.recommend(profile, listOf(movie)).isEmpty())
    }

    @Test
    fun `candidate recommended from multiple strong anchors scores higher`() {
        val profile = profile()
        val oneSource = candidate("one", sources = listOf("a"))
        val consensus = candidate("many", sources = listOf("a", "b", "c"))

        val ranked = RecommendationEngine.recommend(profile, listOf(oneSource, consensus))

        assertEquals("many", ranked.first().candidate.externalId)
        assertTrue(ranked.first().normalizedScore > ranked.last().normalizedScore)
    }

    @Test
    fun `external rating with few votes has less confidence`() {
        val sparse = RecommendationEngine.externalQualityConfidence(9.8, 7)
        val established = RecommendationEngine.externalQualityConfidence(9.2, 20_000)

        assertTrue(sparse < established)
    }

    @Test
    fun `favorite five star work is stronger anchor than mediocre work`() {
        val favorite = detail("favorite", 10, favorite = true)
        val mediocre = detail("mediocre", 6)

        assertTrue(RecommendationEngine.anchorStrength(favorite) > RecommendationEngine.anchorStrength(mediocre))
    }

    @Test
    fun `existing library work is excluded regardless of status`() {
        val existing = detail("a", 10, externalId = "existing")
        val profile = RecommendationEngine.buildTasteProfile(listOf(existing, detail("b", 9), detail("c", 8)))
        val sameExternal = candidate("existing")
        val sameManualTitle = candidate("different").copy(title = existing.media.title)

        assertTrue(RecommendationEngine.recommend(profile, listOf(sameExternal, sameManualTitle)).isEmpty())
    }

    @Test
    fun `negative and already known feedback remove candidates`() {
        val profile = profile()
        val first = candidate("1")
        val second = candidate("2")
        val feedback = mapOf(
            RecommendationKey(first.provider, first.externalId, first.type) to RecommendationFeedbackType.NOT_INTERESTED,
            RecommendationKey(second.provider, second.externalId, second.type) to RecommendationFeedbackType.ALREADY_KNOWN,
        )

        assertTrue(RecommendationEngine.recommend(profile, listOf(first, second), feedback).isEmpty())
    }

    @Test
    fun `diversity prevents a near identical cluster from monopolizing the top`() {
        val clusterOne = recommendation("cluster-1", 0.90, listOf("Space Opera", "Ciencia ficción"))
        val clusterTwo = recommendation("cluster-2", 0.88, listOf("Space Opera", "Ciencia ficción"))
        val alternative = recommendation("alternative", 0.80, listOf("Misterio", "Drama"))

        val diverse = RecommendationEngine.diversify(listOf(clusterOne, clusterTwo, alternative), 3)

        assertEquals(listOf("cluster-1", "alternative"), diverse.take(2).map { it.candidate.externalId })
    }

    private fun profile() = RecommendationEngine.buildTasteProfile(
        listOf(detail("a", 10), detail("b", 9), detail("c", 8)),
    )

    private fun detail(id: String, rating: Int, favorite: Boolean = false, externalId: String = id) = MediaDetail(
        media = MediaItem(id, MediaType.MOVIE, "Obra $id", isFavorite = favorite, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH),
        creators = listOf("Directora"),
        genres = listOf("Ciencia ficción", "Drama"),
        consumptions = listOf(
            Consumption("c$id", id, ConsumptionStatus.COMPLETED, ratingHalfStars = rating, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH),
        ),
        progress = emptyList(),
        reflections = emptyList(),
        externalRefs = listOf(MediaExternalReference(MetadataProvider.TMDB, externalId, MediaType.MOVIE)),
    )

    private fun candidate(
        id: String,
        genres: List<String> = listOf("Ciencia ficción", "Drama"),
        sources: List<String> = emptyList(),
        rating: Double = 8.2,
        votes: Int = 4_000,
    ) = MetadataSearchResult(
        provider = MetadataProvider.TMDB,
        externalId = id,
        externalUrl = null,
        type = MediaType.MOVIE,
        title = "Candidata $id",
        creators = listOf("Directora"),
        genres = genres,
        externalRating = rating,
        externalVoteCount = votes,
        sourceAnchorMediaIds = sources,
    )

    private fun recommendation(id: String, score: Double, genres: List<String>) = Recommendation(
        candidate = candidate(id, genres = genres).copy(creators = emptyList()),
        affinityScore = (score * 100).toInt(),
        reasons = emptyList(),
        category = RecommendationCategory.GOOD_BET,
        normalizedScore = score,
    )
}
