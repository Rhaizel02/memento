package com.memento.app.domain.timeline

import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.ProgressEntry
import com.memento.app.domain.model.Reflection
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.TimelineEventType
import com.memento.app.domain.model.TimelineMediaContext
import java.time.ZoneId

object CulturalTimelineMapper {
    fun build(
        media: TimelineMediaContext,
        consumptions: List<Consumption>,
        progress: List<ProgressEntry>,
        reflections: List<Reflection>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<CulturalTimelineEvent> {
        val orderedConsumptions = consumptions.sortedWith(compareBy<Consumption>({ it.createdAt }, { it.id }))
        val reconsumptionIds = orderedConsumptions.drop(1).mapTo(mutableSetOf()) { it.id }
        return sort(
            buildList {
                consumptions.forEach { consumption ->
                    started(media, consumption, consumption.id in reconsumptionIds)?.let(::add)
                    completed(media, consumption)?.let(::add)
                }
                progress.forEach { entry -> add(progress(media, entry, zoneId)) }
                reflections.forEach { reflection -> add(reflection(media, reflection, zoneId)) }
            },
        )
    }

    fun started(
        media: TimelineMediaContext,
        consumption: Consumption,
        isReconsumption: Boolean,
    ): CulturalTimelineEvent? = consumption.startedDate?.let { date ->
        CulturalTimelineEvent(
            id = "started:${consumption.id}",
            date = date,
            occurredAt = null,
            mediaItemId = media.id,
            consumptionId = consumption.id,
            mediaType = media.type,
            title = media.title,
            posterUrl = media.posterUrl,
            eventType = TimelineEventType.STARTED,
            isReconsumption = isReconsumption,
            isFavorite = media.isFavorite,
        )
    }

    fun completed(
        media: TimelineMediaContext,
        consumption: Consumption,
    ): CulturalTimelineEvent? {
        if (consumption.status != ConsumptionStatus.COMPLETED) return null
        return consumption.completedDate?.let { date ->
            CulturalTimelineEvent(
                id = "completed:${consumption.id}",
                date = date,
                occurredAt = null,
                mediaItemId = media.id,
                consumptionId = consumption.id,
                mediaType = media.type,
                title = media.title,
                posterUrl = media.posterUrl,
                eventType = TimelineEventType.COMPLETED,
                ratingHalfStars = consumption.ratingHalfStars,
                isFavorite = media.isFavorite,
            )
        }
    }

    fun progress(
        media: TimelineMediaContext,
        entry: ProgressEntry,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) = CulturalTimelineEvent(
        id = "progress:${entry.id}",
        date = entry.recordedAt.atZone(zoneId).toLocalDate(),
        occurredAt = entry.recordedAt,
        mediaItemId = media.id,
        consumptionId = entry.consumptionId,
        mediaType = media.type,
        title = media.title,
        posterUrl = media.posterUrl,
        eventType = TimelineEventType.PROGRESS,
        progress = entry,
        isFavorite = media.isFavorite,
    )

    fun reflection(
        media: TimelineMediaContext,
        reflection: Reflection,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) = CulturalTimelineEvent(
        id = "reflection:${reflection.id}",
        date = reflection.createdAt.atZone(zoneId).toLocalDate(),
        occurredAt = reflection.createdAt,
        mediaItemId = media.id,
        consumptionId = reflection.consumptionId,
        mediaType = media.type,
        title = media.title,
        posterUrl = media.posterUrl,
        eventType = reflection.type.toTimelineEventType(),
        reflectionId = reflection.id,
        reflectionContent = reflection.content,
        isFavorite = media.isFavorite,
    )

    fun sort(events: List<CulturalTimelineEvent>): List<CulturalTimelineEvent> = events.sortedWith(EVENT_COMPARATOR)

    private fun ReflectionType.toTimelineEventType() = when (this) {
        ReflectionType.NOTE -> TimelineEventType.NOTE
        ReflectionType.FINAL_REFLECTION -> TimelineEventType.FINAL_REFLECTION
        ReflectionType.LATER_REFLECTION -> TimelineEventType.LATER_REFLECTION
    }

    /*
     * LocalDate events never receive invented times. On the same date, real timestamps
     * come first (newest first), then date-only events use semantic priority and stable id.
     */
    private val EVENT_COMPARATOR = Comparator<CulturalTimelineEvent> { first, second ->
        compareValuesBy(second, first, CulturalTimelineEvent::date)
            .takeIf { it != 0 }
            ?: compareValuesBy(second, first) { it.occurredAt != null }
                .takeIf { it != 0 }
            ?: compareValues(second.occurredAt, first.occurredAt)
                .takeIf { it != 0 }
            ?: compareValues(priority(second.eventType), priority(first.eventType))
                .takeIf { it != 0 }
            ?: first.id.compareTo(second.id)
    }

    private fun priority(type: TimelineEventType): Int = when (type) {
        TimelineEventType.LATER_REFLECTION -> 6
        TimelineEventType.FINAL_REFLECTION -> 5
        TimelineEventType.NOTE -> 4
        TimelineEventType.COMPLETED -> 3
        TimelineEventType.PROGRESS -> 2
        TimelineEventType.STARTED -> 1
    }
}
