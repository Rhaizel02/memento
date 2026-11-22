package com.memento.app.ui

import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.recommendation.Recommendation
import com.memento.app.domain.recommendation.RecommendationEngine
import com.memento.app.domain.recommendation.TasteProfile
import com.memento.app.domain.repository.RecommendationFeed
import com.memento.app.ui.discover.toDiscoverState
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoverSelectionTest {
    @Test
    fun `popular titles complete sparse film series and game sections without duplicates`() {
        for (type in listOf(MediaType.MOVIE, MediaType.SERIES, MediaType.GAME)) {
            for (personalCount in listOf(0, 1, 5, 6)) {
                val personal = List(personalCount) { index -> Recommendation(candidate(type, "p$index"), 30, emptyList()) }
                val popular = personal.take(1).map { it.candidate.copy(externalId = "same-title") } +
                    List(8) { index -> candidate(type, "popular$index") }
                val state = RecommendationFeed(
                    TasteProfile(3, emptyMap(), emptyMap(), emptyMap()),
                    emptyList(),
                    byType = mapOf(type to personal),
                    popularByType = mapOf(type to popular),
                ).toDiscoverState(type)
                assertEquals(personal, state.recommendations)
                assertEquals(List(6 - personalCount) { "popular$it" }, state.popular.map { it.externalId })
            }
        }
    }

    @Test
    fun `single close match from a book profile remains personal and catalogue fills the list`() {
        val profile = TasteProfile(3, mapOf("Ciencia ficción" to 5.0, "Drama" to 3.0), emptyMap(), mapOf(MediaType.BOOK to 3.0))
        val pool = listOf(candidate(MediaType.MOVIE, "both").copy(genres = listOf("Ciencia ficción", "Drama"))) +
            List(19) { index -> candidate(MediaType.MOVIE, "one$index").copy(genres = listOf("Drama")) }
        val personal = RecommendationEngine.recommend(profile, pool)
        val state = RecommendationFeed(profile, personal, popularByType = mapOf(MediaType.MOVIE to pool))
            .toDiscoverState(MediaType.MOVIE)

        assertEquals(listOf("both"), state.recommendations.map { it.candidate.externalId })
        assertEquals(List(5) { "one$it" }, state.popular.map { it.externalId })
    }

    private fun candidate(type: MediaType, id: String) = MetadataSearchResult(
        if (type == MediaType.GAME) MetadataProvider.RAWG else MetadataProvider.TMDB,
        id, null, type, "Obra $id", externalRating = 8.5, externalVoteCount = 5_000,
    )
}
