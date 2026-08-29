package com.memento.app.domain

import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ProgressEntry
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.stats.StatsEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class StatsEngineTest {
    @Test
    fun `known 2026 dataset produces correct annual aggregates`() {
        val book = detail(
            id = "book",
            type = MediaType.BOOK,
            title = "Libro",
            genres = listOf("Fantasía"),
            creators = listOf("Autora"),
            pageCount = 300,
            consumptions = listOf(completed("b1", "book", 10, 1), completed("b2", "book", 8, 7)),
        )
        val movie = detail(
            id = "movie",
            type = MediaType.MOVIE,
            title = "Película",
            genres = listOf("Drama"),
            creators = listOf("Directora"),
            runtimeMinutes = 120,
            consumptions = listOf(completed("m1", "movie", 6, 2)),
        )
        val game = detail(
            id = "game",
            type = MediaType.GAME,
            title = "Juego",
            genres = listOf("RPG"),
            creators = listOf("Estudio"),
            consumptions = emptyList(),
            progress = listOf(
                ProgressEntry("p1", "g1", ProgressType.HOURS, 10.0, recordedAt = Instant.parse("2026-03-01T12:00:00Z")),
                ProgressEntry("p2", "g1", ProgressType.HOURS, 25.0, recordedAt = Instant.parse("2026-04-01T12:00:00Z")),
            ),
        )

        val summary = StatsEngine.calculate(listOf(book, movie, game), 2026)

        assertEquals(3, summary.completedWorks)
        assertEquals(2, summary.completedByType[MediaType.BOOK])
        assertEquals(600, summary.pagesRead)
        assertEquals(120, summary.movieMinutes)
        assertEquals(25.0, summary.gameHours!!, 0.0)
        assertEquals(1, summary.revisits)
        assertEquals("Fantasía", summary.topConsumedGenres.first().label)
        assertEquals(4.5, summary.topRatedGenres.first().averageRating!!, 0.0)
    }

    private fun completed(id: String, mediaId: String, rating: Int, month: Int) = Consumption(
        id = id,
        mediaItemId = mediaId,
        status = ConsumptionStatus.COMPLETED,
        completedDate = LocalDate.of(2026, month, 1),
        ratingHalfStars = rating,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun detail(
        id: String,
        type: MediaType,
        title: String,
        genres: List<String>,
        creators: List<String>,
        pageCount: Int? = null,
        runtimeMinutes: Int? = null,
        consumptions: List<Consumption>,
        progress: List<ProgressEntry> = emptyList(),
    ) = MediaDetail(
        media = MediaItem(
            id = id,
            type = type,
            title = title,
            pageCount = pageCount,
            runtimeMinutes = runtimeMinutes,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        ),
        creators = creators,
        genres = genres,
        consumptions = consumptions,
        progress = progress,
        reflections = emptyList(),
    )
}
