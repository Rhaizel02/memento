package com.memento.app.domain

import com.memento.app.domain.culturalprofile.CulturalCompletion
import com.memento.app.domain.culturalprofile.CulturalInsight
import com.memento.app.domain.culturalprofile.CulturalPeriodKind
import com.memento.app.domain.culturalprofile.CulturalProfileEngine
import com.memento.app.domain.culturalprofile.CulturalProfileSource
import com.memento.app.domain.culturalprofile.CulturalProfileWork
import com.memento.app.domain.model.MediaType
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CulturalProfileEngineTest {
    @Test
    fun `best rated genre requires the documented minimum sample`() {
        val source = source(
            work("w1", genres = listOf("Western")) to completion("w1", "2026-01-01", 10),
            work("w2", genres = listOf("Western")) to completion("w2", "2026-01-02", 10),
            work("d1", genres = listOf("Drama")) to completion("d1", "2026-02-01", 8),
            work("d2", genres = listOf("Drama")) to completion("d2", "2026-02-02", 8),
            work("d3", genres = listOf("Drama")) to completion("d3", "2026-02-03", 8),
        )

        val insight = CulturalProfileEngine.build(source, LocalDate.of(2026, 8, 30)).insights
            .filterIsInstance<CulturalInsight.BestRatedGenre>().single()

        assertEquals("Drama", insight.genre.label)
        assertEquals(3, insight.genre.ratedCount)
    }

    @Test
    fun `best rated creator requires the documented minimum sample`() {
        val source = source(
            work("a1", creators = listOf("Autora breve")) to completion("a1", "2026-01-01", 10),
            work("a2", creators = listOf("Autora breve")) to completion("a2", "2026-01-02", 10),
            work("b1", creators = listOf("Autora constante")) to completion("b1", "2026-02-01", 9),
            work("b2", creators = listOf("Autora constante")) to completion("b2", "2026-02-02", 9),
            work("b3", creators = listOf("Autora constante")) to completion("b3", "2026-02-03", 9),
        )

        val insight = CulturalProfileEngine.build(source, LocalDate.of(2026, 8, 30)).insights
            .filterIsInstance<CulturalInsight.BestRatedCreator>().single()

        assertEquals("Autora constante", insight.creator.label)
    }

    @Test
    fun `personal tags calculate work count and rating correctly`() {
        val works = listOf(
            work("1", tags = listOf("Comfort")),
            work("2", tags = listOf("Comfort")),
            work("3", tags = listOf("Comfort")),
            work("4", tags = listOf("Comfort")),
            work("5", tags = listOf("Impactante")),
        )
        val source = CulturalProfileSource(
            works,
            listOf(
                completion("1", "2026-01-01", 10),
                completion("2", "2026-01-02", 8),
                completion("3", "2026-01-03", 10),
                completion("5", "2026-01-04", 10),
            ),
        )

        val insight = CulturalProfileEngine.build(source, LocalDate.of(2026, 8, 30)).insights
            .filterIsInstance<CulturalInsight.PersonalTags>().single()

        assertEquals("Comfort", insight.mostUsed.label)
        assertEquals(4, insight.mostUsed.count)
        assertEquals(4.666, insight.bestRated!!.averageRating!!, 0.001)
    }

    @Test
    fun `annual comparison calculates media type growth`() {
        val pairs = buildList {
            repeat(3) { index ->
                val id = "previous-$index"
                add(work(id, type = MediaType.BOOK) to completion(id, "2025-0${index + 1}-01", 8))
            }
            repeat(6) { index ->
                val id = "current-$index"
                add(work(id, type = MediaType.BOOK) to completion(id, "2026-0${index + 1}-01", 8))
            }
        }

        val comparison = CulturalProfileEngine.build(source(*pairs.toTypedArray()), LocalDate.of(2026, 8, 30)).insights
            .filterIsInstance<CulturalInsight.MediaTypeYearChange>().single()

        assertEquals(MediaType.BOOK, comparison.mediaType)
        assertEquals(3, comparison.previousCount)
        assertEquals(6, comparison.currentCount)
        assertEquals(100, comparison.percentChange)
    }

    @Test
    fun `strong concentrated genre creates a cultural period`() {
        val types = listOf(MediaType.BOOK, MediaType.BOOK, MediaType.MOVIE, MediaType.SERIES, MediaType.GAME)
        val pairs = types.mapIndexed { index, type ->
            val id = "sf-$index"
            work(id, type, genres = listOf("Ciencia ficción")) to
                completion(id, "2026-0${6 + index % 3}-0${index + 1}", 9)
        }

        val periods = CulturalProfileEngine.detectPeriods(source(*pairs.toTypedArray()))

        assertTrue(periods.any { it.kind == CulturalPeriodKind.GENRE && it.label == "Ciencia ficción" && it.matchingWorks == 5 })
    }

    @Test
    fun `dataset without a dominant signal does not invent a cultural period`() {
        val types = listOf(MediaType.BOOK, MediaType.BOOK, MediaType.MOVIE, MediaType.MOVIE, MediaType.SERIES, MediaType.GAME)
        val pairs = types.mapIndexed { index, type ->
            val id = "varied-$index"
            work(id, type, genres = listOf("Género $index")) to
                completion(id, "2026-0${6 + index % 3}-0${index + 1}", 8)
        }

        assertTrue(CulturalProfileEngine.detectPeriods(source(*pairs.toTypedArray())).isEmpty())
    }

    private fun source(vararg pairs: Pair<CulturalProfileWork, CulturalCompletion>) = CulturalProfileSource(
        works = pairs.map(Pair<CulturalProfileWork, CulturalCompletion>::first),
        completions = pairs.map(Pair<CulturalProfileWork, CulturalCompletion>::second),
    )

    private fun work(
        id: String,
        type: MediaType = MediaType.BOOK,
        genres: List<String> = emptyList(),
        creators: List<String> = emptyList(),
        tags: List<String> = emptyList(),
    ) = CulturalProfileWork(id, type, false, genres, creators, tags)

    private fun completion(id: String, date: String, rating: Int?) = CulturalCompletion(
        mediaId = id,
        completedDate = LocalDate.parse(date),
        ratingHalfStars = rating,
        updatedAt = Instant.EPOCH,
    )
}
