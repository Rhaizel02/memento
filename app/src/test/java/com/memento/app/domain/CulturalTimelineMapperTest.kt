package com.memento.app.domain

import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ProgressEntry
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.Reflection
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.TimelineEventType
import com.memento.app.domain.model.TimelineMediaContext
import com.memento.app.domain.timeline.CulturalTimelineMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CulturalTimelineMapperTest {
    private val media = TimelineMediaContext("m1", MediaType.BOOK, "Dune", null, true)

    @Test
    fun `started date creates a started event without inventing a timestamp`() {
        val event = CulturalTimelineMapper.started(media, consumption("c1", started = "2026-08-24"), false)

        requireNotNull(event)
        assertEquals(TimelineEventType.STARTED, event.eventType)
        assertEquals(LocalDate.parse("2026-08-24"), event.date)
        assertEquals(null, event.occurredAt)
        assertFalse(event.isReconsumption)
    }

    @Test
    fun `completed date and rating create a completed event`() {
        val event = CulturalTimelineMapper.completed(
            media,
            consumption("c1", completed = "2026-08-29", rating = 9),
        )

        requireNotNull(event)
        assertEquals(TimelineEventType.COMPLETED, event.eventType)
        assertEquals(9, event.ratingHalfStars)
        assertTrue(event.isFavorite)
    }

    @Test
    fun `reflection variants preserve their cultural meaning`() {
        val types = ReflectionType.entries.associateWith { type ->
            CulturalTimelineMapper.reflection(media, reflection(type), ZoneOffset.UTC).eventType
        }

        assertEquals(TimelineEventType.NOTE, types[ReflectionType.NOTE])
        assertEquals(TimelineEventType.QUOTE, types[ReflectionType.QUOTE])
        assertEquals(TimelineEventType.FINAL_REFLECTION, types[ReflectionType.FINAL_REFLECTION])
        assertEquals(TimelineEventType.LATER_REFLECTION, types[ReflectionType.LATER_REFLECTION])
    }

    @Test
    fun `two consumptions remain separate and the later one is a reconsumption`() {
        val first = consumption("c1", started = "2024-01-01", createdAt = "2024-01-01T10:00:00Z")
        val second = consumption("c2", started = "2026-08-24", createdAt = "2026-08-24T10:00:00Z")

        val events = CulturalTimelineMapper.build(media, listOf(second, first), emptyList(), emptyList(), ZoneOffset.UTC)

        assertEquals(listOf("started:c2", "started:c1"), events.map { it.id })
        assertTrue(events.first().isReconsumption)
        assertFalse(events.last().isReconsumption)
    }

    @Test
    fun `retrospective consumption becomes historical first even when inserted later`() {
        val insertedFirst = consumption(
            "c-2025",
            started = "2025-08-30",
            completed = "2025-09-10",
            createdAt = "2025-09-10T10:00:00Z",
        )
        val insertedLater = consumption(
            "c-2021",
            started = "2021-08-30",
            completed = "2021-09-10",
            createdAt = "2026-08-30T10:00:00Z",
        )

        val events = CulturalTimelineMapper.build(
            media,
            listOf(insertedFirst, insertedLater),
            emptyList(),
            emptyList(),
            ZoneOffset.UTC,
        )

        val events2021 = events.filter { it.consumptionId == "c-2021" }
        val events2025 = events.filter { it.consumptionId == "c-2025" }
        assertTrue(events2021.all { !it.isReconsumption })
        assertTrue(events2025.all { it.isReconsumption })
    }

    @Test
    fun `different sources use date real timestamp semantic priority and stable id`() {
        val consumption = consumption(
            id = "c1",
            started = "2026-08-29",
            completed = "2026-08-29",
            rating = 10,
        )
        val progress = ProgressEntry(
            id = "p1",
            consumptionId = "c1",
            progressType = ProgressType.PAGES,
            currentValue = 350.0,
            totalValue = 1200.0,
            recordedAt = Instant.parse("2026-08-29T08:00:00Z"),
        )
        val note = reflection(ReflectionType.NOTE, "2026-08-29T20:00:00Z")

        val events = CulturalTimelineMapper.build(
            media,
            listOf(consumption),
            listOf(progress),
            listOf(note),
            ZoneOffset.UTC,
        )

        assertEquals(
            listOf(TimelineEventType.NOTE, TimelineEventType.PROGRESS, TimelineEventType.COMPLETED, TimelineEventType.STARTED),
            events.map { it.eventType },
        )
    }

    private fun consumption(
        id: String,
        started: String? = null,
        completed: String? = null,
        rating: Int? = null,
        createdAt: String = "2026-01-01T10:00:00Z",
    ) = Consumption(
        id = id,
        mediaItemId = media.id,
        status = if (completed == null) ConsumptionStatus.IN_PROGRESS else ConsumptionStatus.COMPLETED,
        startedDate = started?.let(LocalDate::parse),
        completedDate = completed?.let(LocalDate::parse),
        ratingHalfStars = rating,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(createdAt),
    )

    private fun reflection(
        type: ReflectionType,
        createdAt: String = "2026-08-29T12:00:00Z",
    ) = Reflection(
        id = "r-${type.name}",
        consumptionId = "c1",
        type = type,
        content = "Una idea que sigue conmigo",
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(createdAt),
    )
}
