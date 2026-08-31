package com.memento.app.domain

import com.memento.app.domain.culturalprofile.CulturalCompletion
import com.memento.app.domain.culturalprofile.CulturalInsight
import com.memento.app.domain.culturalprofile.CulturalProfileSource
import com.memento.app.domain.culturalprofile.CulturalProfileWork
import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.TimelineEventType
import com.memento.app.domain.wrapped.WrappedCard
import com.memento.app.domain.wrapped.WrappedEngine
import com.memento.app.domain.wrapped.WrappedSource
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WrappedEngineTest {
    @Test
    fun `work of year selection is deterministic when signals tie`() {
        val alpha = work("alpha", title = "Alpha", favorite = true)
        val beta = work("beta", title = "Beta", favorite = true)
        val completions = listOf(completion("alpha", 2026, 10), completion("beta", 2026, 10))

        val first = WrappedEngine.selectWorkOfYear(listOf(beta, alpha), completions, emptyList())
        val second = WrappedEngine.selectWorkOfYear(listOf(alpha, beta), completions.reversed(), emptyList())

        assertEquals("alpha", first?.mediaId)
        assertEquals(first, second)
    }

    @Test
    fun `small year does not manufacture cultural insights`() {
        val source = WrappedSource(
            CulturalProfileSource(
                works = listOf(work("one", title = "Una historia")),
                completions = listOf(completion("one", 2026, rating = null)),
            ),
        )

        val snapshot = WrappedEngine.create(source, 2026, LocalDate.of(2026, 8, 31))

        assertEquals(1, snapshot.completedWorkCount)
        assertTrue(snapshot.cards.any { it is WrappedCard.Cover })
        assertTrue(snapshot.cards.any { it is WrappedCard.MediaSummary })
        assertTrue(snapshot.cards.any { it is WrappedCard.Finale })
        assertFalse(snapshot.cards.any { it is WrappedCard.GenreOfYear })
        assertFalse(snapshot.cards.any { it is WrappedCard.BestRatedMedium })
        assertFalse(snapshot.cards.any { it is WrappedCard.FeaturedCreator })
        assertFalse(snapshot.cards.any { it is WrappedCard.CulturalEra })
        assertFalse(snapshot.cards.any { it is WrappedCard.WorkOfYear })
    }

    @Test
    fun `previous wrapped year uses its complete calendar year`() {
        val previous = (1..3).map { index -> work("p$index") }
        val selected = (1..6).map { index -> work("c$index") }
        val completions = listOf(
            completion("p1", 2024, month = 1),
            completion("p2", 2024, month = 10),
            completion("p3", 2024, month = 12),
            completion("c1", 2025, month = 1),
            completion("c2", 2025, month = 3),
            completion("c3", 2025, month = 7),
            completion("c4", 2025, month = 10),
            completion("c5", 2025, month = 11),
            completion("c6", 2025, month = 12),
        )
        val source = WrappedSource(CulturalProfileSource(previous + selected, completions))

        val snapshot = WrappedEngine.create(source, 2025, LocalDate.of(2026, 8, 31))
        val comparison = snapshot.cards.filterIsInstance<WrappedCard.Comparisons>().singleOrNull()
            ?.insights?.filterIsInstance<CulturalInsight.MediaTypeYearChange>()?.singleOrNull()

        assertEquals(6, snapshot.completedWorkCount)
        assertNotNull(comparison)
        assertEquals(3, comparison?.previousCount)
        assertEquals(6, comparison?.currentCount)
        assertEquals(100, comparison?.percentChange)
    }

    @Test
    fun `snapshot includes only data from requested year`() {
        val source = WrappedSource(
            profileSource = CulturalProfileSource(
                works = listOf(
                    work("past", title = "Historia de 2025", favorite = true),
                    work("future", title = "Historia de 2026", favorite = true),
                ),
                completions = listOf(
                    completion("past", 2025, 10),
                    completion("future", 2026, 10),
                ),
            ),
            timelineEvents = listOf(
                reflection("past-reflection", "past", "Historia de 2025", 2025),
                reflection("future-reflection", "future", "Historia de 2026", 2026),
            ),
        )

        val snapshot = WrappedEngine.create(source, 2025, LocalDate.of(2026, 8, 31))
        val selectedWork = snapshot.cards.filterIsInstance<WrappedCard.WorkOfYear>().single().work
        val selectedReflection = snapshot.cards.filterIsInstance<WrappedCard.ReflectionSpotlight>().single().reflection

        assertEquals(1, snapshot.completedWorkCount)
        assertEquals("past", selectedWork.mediaId)
        assertEquals("past-reflection", selectedReflection.reflectionId)
        assertFalse(snapshot.cards.toString().contains("Historia de 2026"))
    }

    private fun work(
        id: String,
        title: String = id,
        favorite: Boolean = false,
    ) = CulturalProfileWork(
        mediaId = id,
        mediaType = MediaType.BOOK,
        isFavorite = favorite,
        title = title,
    )

    private fun completion(
        id: String,
        year: Int,
        rating: Int? = null,
        month: Int = 6,
    ) = CulturalCompletion(
        mediaId = id,
        completedDate = LocalDate.of(year, month, 15),
        ratingHalfStars = rating,
        updatedAt = Instant.parse("$year-${month.toString().padStart(2, '0')}-15T12:00:00Z"),
    )

    private fun reflection(id: String, mediaId: String, title: String, year: Int) = CulturalTimelineEvent(
        id = "reflection:$id",
        date = LocalDate.of(year, 7, 1),
        occurredAt = Instant.parse("$year-07-01T12:00:00Z"),
        mediaItemId = mediaId,
        consumptionId = "consumption-$mediaId",
        mediaType = MediaType.BOOK,
        title = title,
        posterUrl = null,
        eventType = TimelineEventType.FINAL_REFLECTION,
        reflectionId = id,
        reflectionContent = "Una reflexión personal suficientemente rica para conservar este recuerdo.",
    )
}
