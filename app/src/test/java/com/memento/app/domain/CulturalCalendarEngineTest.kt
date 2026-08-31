package com.memento.app.domain

import com.memento.app.domain.calendar.CulturalCalendarEngine
import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.TimelineEventType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CulturalCalendarEngineTest {
    @Test
    fun `monthly snapshot returns only events inside requested range`() {
        val events = listOf(
            event("july", "2026-07-31"),
            event("august-start", "2026-08-01"),
            event("august-end", "2026-08-31"),
            event("september", "2026-09-01"),
        )

        val month = CulturalCalendarEngine.buildMonth(events, YearMonth.of(2026, 8))

        assertEquals(listOf("august-end", "august-start"), month.days.values.flatMap { it.events }.map { it.id })
    }

    @Test
    fun `events on the same day are grouped together`() {
        val date = LocalDate.of(2026, 8, 17)
        val month = CulturalCalendarEngine.buildMonth(
            listOf(event("one", date.toString()), event("two", date.toString())),
            YearMonth.from(date),
        )

        assertEquals(1, month.activeDayCount)
        assertEquals(2, month.days.getValue(date).eventCount)
    }

    @Test
    fun `different media types create bounded semantic indicators`() {
        val date = "2026-08-17"
        val month = CulturalCalendarEngine.buildMonth(
            listOf(
                event("book", date, MediaType.BOOK),
                event("movie", date, MediaType.MOVIE),
                event("series", date, MediaType.SERIES),
                event("game", date, MediaType.GAME),
            ),
            YearMonth.of(2026, 8),
        )
        val day = month.days.getValue(LocalDate.parse(date))

        assertEquals(listOf(MediaType.BOOK, MediaType.MOVIE, MediaType.SERIES), day.indicatorTypes)
        assertEquals(1, day.hiddenEventCount)
        assertEquals(1, day.mediaCounts.getValue(MediaType.GAME))
    }

    @Test
    fun `empty month keeps an empty calendar snapshot`() {
        val month = CulturalCalendarEngine.buildMonth(emptyList(), YearMonth.of(2026, 8))

        assertEquals(0, month.eventCount)
        assertEquals(0, month.activeDayCount)
        assertTrue(month.days.isEmpty())
        assertTrue(CulturalCalendarEngine.calendarCells(month.month).any { it == LocalDate.of(2026, 8, 1) })
    }

    @Test
    fun `month navigation crosses december and january`() {
        assertEquals(YearMonth.of(2026, 1), CulturalCalendarEngine.next(YearMonth.of(2025, 12)))
        assertEquals(YearMonth.of(2025, 12), CulturalCalendarEngine.previous(YearMonth.of(2026, 1)))
    }

    @Test
    fun `reconsumptions remain independent events`() {
        val date = LocalDate.of(2026, 8, 20)
        val month = CulturalCalendarEngine.buildMonth(
            listOf(
                event("restart-one", date.toString(), reconsumption = true),
                event("restart-two", date.toString(), reconsumption = true),
            ),
            YearMonth.from(date),
        )

        val day = month.days.getValue(date)
        assertEquals(2, day.eventCount)
        assertEquals(listOf("restart-one", "restart-two"), day.events.map(CulturalTimelineEvent::id))
        assertTrue(day.events.all(CulturalTimelineEvent::isReconsumption))
    }

    @Test
    fun `year intensity counts events and active days without leaking another year`() {
        val year = CulturalCalendarEngine.buildYear(
            listOf(
                event("august-one", "2026-08-01"),
                event("august-two", "2026-08-01"),
                event("august-three", "2026-08-17"),
                event("other-year", "2025-08-17"),
            ),
            2026,
        )

        val august = year.months.single { it.month == YearMonth.of(2026, 8) }
        assertEquals(3, august.eventCount)
        assertEquals(2, august.activeDayCount)
        assertEquals(12, year.months.size)
    }

    private fun event(
        id: String,
        date: String,
        mediaType: MediaType = MediaType.BOOK,
        reconsumption: Boolean = false,
    ) = CulturalTimelineEvent(
        id = id,
        date = LocalDate.parse(date),
        occurredAt = null,
        mediaItemId = "media-$id",
        consumptionId = "consumption-$id",
        mediaType = mediaType,
        title = "Historia $id",
        posterUrl = null,
        eventType = TimelineEventType.STARTED,
        isReconsumption = reconsumption,
    )
}
