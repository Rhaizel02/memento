package com.memento.app.domain.usecase

import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.ProgressEntry
import com.memento.app.domain.model.Reflection
import com.memento.app.domain.model.TimelineEvent
import com.memento.app.domain.model.TimelineEventType
import com.memento.app.domain.model.TimelineMediaContext
import com.memento.app.domain.timeline.CulturalTimelineMapper

object TimelineBuilder {
    fun build(
        media: MediaItem,
        consumptions: List<Consumption>,
        progress: List<ProgressEntry>,
        reflections: List<Reflection>,
    ): List<TimelineEvent> {
        val sharedEvents = CulturalTimelineMapper.build(
            media = TimelineMediaContext(media.id, media.type, media.title, media.posterUrl, media.isFavorite),
            consumptions = consumptions,
            progress = progress,
            reflections = reflections,
        ).map { event ->
            when (event.eventType) {
                TimelineEventType.STARTED -> TimelineEvent.ConsumptionStarted(event.date, event.date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant())
                TimelineEventType.COMPLETED -> TimelineEvent.ConsumptionCompleted(
                    event.date,
                    event.ratingHalfStars,
                    consumptions.first { it.id == event.consumptionId }.updatedAt,
                )
                TimelineEventType.PROGRESS -> TimelineEvent.ProgressUpdated(requireNotNull(event.progress))
                TimelineEventType.NOTE,
                TimelineEventType.QUOTE,
                TimelineEventType.FINAL_REFLECTION,
                TimelineEventType.LATER_REFLECTION,
                -> TimelineEvent.ReflectionWritten(reflections.first { it.id == event.reflectionId })
            }
        }
        val dropped = consumptions.filter { it.status == ConsumptionStatus.DROPPED }.map { consumption ->
            TimelineEvent.ConsumptionDropped(consumption.completedDate, consumption.updatedAt)
        }
        return (sharedEvents + dropped).sortedByDescending { it.sortInstant }
    }
}
