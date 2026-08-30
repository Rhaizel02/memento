package com.memento.app.domain

import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.TimelineEventType
import com.memento.app.domain.timeline.OnThisDaySelector
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class OnThisDaySelectorTest {
    private val today = LocalDate.of(2026, 8, 30)

    @Test
    fun `same month and day from an earlier year appears`() {
        val selected = OnThisDaySelector.select(listOf(event("past", "2022-08-30")), today)

        assertEquals(listOf("past"), selected.map { it.id })
    }

    @Test
    fun `current year and a different date are excluded`() {
        val selected = OnThisDaySelector.select(
            listOf(
                event("current", "2026-08-30"),
                event("different-day", "2022-08-29"),
                event("different-month", "2022-07-30"),
            ),
            today,
        )

        assertEquals(emptyList<CulturalTimelineEvent>(), selected)
    }

    @Test
    fun `years are newest first when events have the same priority`() {
        val selected = OnThisDaySelector.select(
            listOf(event("old", "2021-08-30"), event("new", "2025-08-30"), event("middle", "2023-08-30")),
            today,
        )

        assertEquals(listOf("new", "middle", "old"), selected.map { it.id })
    }

    @Test
    fun `reflections and completions are selected before progress`() {
        val selected = OnThisDaySelector.select(
            listOf(
                event("progress", "2025-08-30", TimelineEventType.PROGRESS),
                event("completed", "2022-08-30", TimelineEventType.COMPLETED),
                event("reflection", "2021-08-30", TimelineEventType.FINAL_REFLECTION),
            ),
            today,
            limit = 2,
        )

        assertEquals(listOf("reflection", "completed"), selected.map { it.id })
    }

    @Test
    fun `quote priority sits between later reflection and completion`() {
        val selected = OnThisDaySelector.select(
            listOf(
                event("completion", "2025-08-30", TimelineEventType.COMPLETED),
                event("quote", "2025-08-30", TimelineEventType.QUOTE),
                event("later", "2025-08-30", TimelineEventType.LATER_REFLECTION),
            ),
            today,
        )

        assertEquals(listOf("later", "quote", "completion"), selected.map { it.id })
    }

    private fun event(
        id: String,
        date: String,
        type: TimelineEventType = TimelineEventType.COMPLETED,
    ) = CulturalTimelineEvent(
        id = id,
        date = LocalDate.parse(date),
        occurredAt = null,
        mediaItemId = "media-$id",
        consumptionId = "consumption-$id",
        mediaType = MediaType.BOOK,
        title = "Dune",
        posterUrl = null,
        eventType = type,
    )
}
