package com.memento.app.domain

import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.Reflection
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.wrapped.WrappedEngine
import com.memento.app.domain.wrapped.WrappedSlide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class WrappedEngineTest {
    @Test
    fun `2026 data creates coherent recalculated story`() {
        val consumption = Consumption(
            id = "c1",
            mediaItemId = "m1",
            status = ConsumptionStatus.COMPLETED,
            completedDate = LocalDate.of(2026, 5, 1),
            ratingHalfStars = 10,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        val history = listOf(
            MediaDetail(
                media = MediaItem("m1", MediaType.BOOK, "La obra", isFavorite = true, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH),
                creators = listOf("La autora"),
                genres = listOf("Fantasía"),
                consumptions = listOf(consumption),
                progress = emptyList(),
                reflections = listOf(
                    Reflection(
                        id = "r1",
                        consumptionId = "c1",
                        type = ReflectionType.FINAL_REFLECTION,
                        content = "Una idea importante que quiero conservar.",
                        createdAt = Instant.parse("2026-05-02T10:00:00Z"),
                        updatedAt = Instant.parse("2026-05-02T10:00:00Z"),
                    ),
                ),
            ),
        )

        val story = WrappedEngine.create(history, 2026)

        assertEquals(WrappedSlide.Cover(2026), story.slides.first())
        assertTrue(story.slides.any { it is WrappedSlide.BestRated && it.work.title == "La obra" })
        assertTrue(story.slides.any { it is WrappedSlide.ReflectionSpotlight && it.workTitle == "La obra" })
        assertTrue(story.slides.last() is WrappedSlide.Finale)
    }
}
