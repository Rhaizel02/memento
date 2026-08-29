package com.memento.app.domain

import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.remember.RememberCandidate
import com.memento.app.domain.remember.RememberEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class RememberEngineTest {
    private val today = LocalDate.parse("2029-06-15")
    private val now = Instant.parse("2029-06-15T10:00:00Z")

    @Test fun `final reflections favorites and age increase score`() {
        val plain = candidate("plain", completed = today.minusDays(100), type = ReflectionType.NOTE)
        val meaningful = candidate(
            "meaningful",
            completed = today.minusYears(3),
            type = ReflectionType.FINAL_REFLECTION,
            favorite = true,
            count = 3,
        )
        val engine = RememberEngine()

        assertTrue(engine.score(meaningful, today, now) > engine.score(plain, today, now))
        assertEquals("meaningful", engine.select(listOf(plain, meaningful), today, now) { 0.0 }?.candidate?.consumptionId)
    }

    @Test fun `recent exposure is excluded when an alternative exists`() {
        val recent = candidate("recent", lastShown = now.minus(2, ChronoUnit.DAYS), favorite = true)
        val alternative = candidate("alternative")

        assertEquals("alternative", RememberEngine().select(listOf(recent, alternative), today, now) { 0.0 }?.candidate?.consumptionId)
    }

    @Test fun `small library relaxes repetition restriction`() {
        val only = candidate("only", lastShown = now.minus(2, ChronoUnit.DAYS))
        assertEquals("only", RememberEngine().select(listOf(only), today, now) { 0.0 }?.candidate?.consumptionId)
    }

    @Test fun `anniversary receives a bonus`() {
        val anniversary = candidate("anniversary", completed = LocalDate.parse("2026-06-12"))
        val ordinary = candidate("ordinary", completed = LocalDate.parse("2026-03-01"))
        val engine = RememberEngine()
        assertTrue(engine.score(anniversary, today, now) > engine.score(ordinary, today, now))
    }

    @Test fun `empty candidates return no memory`() {
        assertNull(RememberEngine().select(emptyList(), today, now))
    }

    private fun candidate(
        id: String,
        completed: LocalDate = today.minusYears(1).minusMonths(2),
        type: ReflectionType = ReflectionType.FINAL_REFLECTION,
        favorite: Boolean = false,
        count: Int = 1,
        lastShown: Instant? = null,
    ) = RememberCandidate(
        consumptionId = id,
        mediaId = "media-$id",
        title = id,
        completedDate = completed,
        ratingHalfStars = 9,
        isFavorite = favorite,
        posterUrl = null,
        backdropUrl = null,
        reflectionId = "reflection-$id",
        reflectionType = type,
        reflectionContent = "Una reflexión",
        reflectionCount = count,
        lastShownAt = lastShown,
    )
}

