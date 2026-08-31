package com.memento.app.domain.calendar

import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.TimelineEventType
import com.memento.app.domain.timeline.CulturalTimelineMapper
import java.time.LocalDate
import java.time.YearMonth

data class CulturalCalendarDay(
    val date: LocalDate,
    val events: List<CulturalTimelineEvent>,
) {
    val eventCount: Int get() = events.size
    val mediaCounts: Map<MediaType, Int> = MediaType.entries.associateWith { type ->
        events.count { it.mediaType == type }
    }.filterValues { it > 0 }
    val reflectionCount: Int = events.count { it.eventType in REFLECTION_TYPES }
    val indicatorTypes: List<MediaType> = MediaType.entries.filter(mediaCounts::containsKey).take(MAX_INDICATORS)
    val hiddenEventCount: Int = (eventCount - indicatorTypes.size).coerceAtLeast(0)

    private companion object {
        const val MAX_INDICATORS = 3
        val REFLECTION_TYPES = setOf(
            TimelineEventType.NOTE,
            TimelineEventType.QUOTE,
            TimelineEventType.FINAL_REFLECTION,
            TimelineEventType.LATER_REFLECTION,
        )
    }
}

data class CulturalCalendarMonth(
    val month: YearMonth,
    val days: Map<LocalDate, CulturalCalendarDay>,
) {
    val eventCount: Int = days.values.sumOf(CulturalCalendarDay::eventCount)
    val activeDayCount: Int = days.size
}

data class CulturalCalendarMonthIntensity(
    val month: YearMonth,
    val eventCount: Int,
    val activeDayCount: Int,
)

data class CulturalCalendarYear(
    val year: Int,
    val months: List<CulturalCalendarMonthIntensity>,
)

object CulturalCalendarEngine {
    fun buildMonth(
        events: List<CulturalTimelineEvent>,
        month: YearMonth,
    ): CulturalCalendarMonth {
        val from = month.atDay(1)
        val until = month.plusMonths(1).atDay(1)
        val days = CulturalTimelineMapper.sort(events.filter { it.date >= from && it.date < until })
            .groupBy(CulturalTimelineEvent::date)
            .mapValues { (date, dayEvents) -> CulturalCalendarDay(date, dayEvents) }
        return CulturalCalendarMonth(month, days)
    }

    fun buildYear(
        events: List<CulturalTimelineEvent>,
        year: Int,
    ): CulturalCalendarYear {
        val from = LocalDate.of(year, 1, 1)
        val until = LocalDate.of(year + 1, 1, 1)
        val byMonth = events.filter { it.date >= from && it.date < until }
            .groupBy { YearMonth.from(it.date) }
        return CulturalCalendarYear(
            year = year,
            months = (1..12).map { monthNumber ->
                val month = YearMonth.of(year, monthNumber)
                val monthEvents = byMonth[month].orEmpty()
                CulturalCalendarMonthIntensity(
                    month = month,
                    eventCount = monthEvents.size,
                    activeDayCount = monthEvents.map(CulturalTimelineEvent::date).distinct().size,
                )
            },
        )
    }

    fun calendarCells(month: YearMonth): List<LocalDate?> {
        val leadingEmptyCells = month.atDay(1).dayOfWeek.value - 1
        val occupiedCells = leadingEmptyCells + month.lengthOfMonth()
        val totalCells = if (occupiedCells <= 35) 35 else 42
        return List(totalCells) { index ->
            val day = index - leadingEmptyCells + 1
            day.takeIf { it in 1..month.lengthOfMonth() }?.let(month::atDay)
        }
    }

    fun previous(month: YearMonth): YearMonth = month.minusMonths(1)

    fun next(month: YearMonth): YearMonth = month.plusMonths(1)
}
