package com.memento.app.domain.timeline

import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.TimelineEventType
import java.time.LocalDate

object OnThisDaySelector {
    fun select(
        events: List<CulturalTimelineEvent>,
        date: LocalDate,
        limit: Int = 5,
    ): List<CulturalTimelineEvent> {
        require(limit > 0) { "On-this-day limit must be positive" }
        return events.asSequence()
            .filter { event ->
                event.date.year < date.year &&
                    event.date.monthValue == date.monthValue &&
                    event.date.dayOfMonth == date.dayOfMonth
            }
            .sortedWith(EVENT_COMPARATOR)
            .take(limit)
            .toList()
    }

    private val EVENT_COMPARATOR = Comparator<CulturalTimelineEvent> { first, second ->
        compareValues(priority(second.eventType), priority(first.eventType))
            .takeIf { it != 0 }
            ?: compareValues(second.date, first.date)
                .takeIf { it != 0 }
            ?: compareValues(second.occurredAt, first.occurredAt)
                .takeIf { it != 0 }
            ?: first.id.compareTo(second.id)
    }

    private fun priority(type: TimelineEventType): Int = when (type) {
        TimelineEventType.FINAL_REFLECTION -> 6
        TimelineEventType.LATER_REFLECTION -> 5
        TimelineEventType.COMPLETED -> 4
        TimelineEventType.NOTE -> 3
        TimelineEventType.STARTED -> 2
        TimelineEventType.PROGRESS -> 1
    }
}
