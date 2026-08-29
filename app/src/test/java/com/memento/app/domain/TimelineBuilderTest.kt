package com.memento.app.domain

import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.Reflection
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.TimelineEvent
import com.memento.app.domain.usecase.TimelineBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TimelineBuilderTest {
    @Test
    fun `build derives events and orders newest first`() {
        val consumption = Consumption(
            id = "c1",
            mediaItemId = "m1",
            status = ConsumptionStatus.COMPLETED,
            startedDate = LocalDate.parse("2026-01-10"),
            completedDate = LocalDate.parse("2026-02-10"),
            ratingHalfStars = 9,
            createdAt = Instant.parse("2026-01-10T10:00:00Z"),
            updatedAt = Instant.parse("2026-02-10T10:00:00Z"),
        )
        val reflection = Reflection(
            id = "r1",
            consumptionId = "c1",
            type = ReflectionType.FINAL_REFLECTION,
            content = "Una idea que merece volver.",
            createdAt = Instant.parse("2026-02-10T11:00:00Z"),
            updatedAt = Instant.parse("2026-02-10T11:00:00Z"),
        )

        val result = TimelineBuilder.build(listOf(consumption), emptyList(), listOf(reflection))

        assertEquals(3, result.size)
        assertTrue(result.first() is TimelineEvent.ReflectionWritten)
        assertTrue(result[1] is TimelineEvent.ConsumptionCompleted)
        assertTrue(result.last() is TimelineEvent.ConsumptionStarted)
    }
}

