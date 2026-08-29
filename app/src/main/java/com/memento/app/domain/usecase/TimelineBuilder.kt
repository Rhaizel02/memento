package com.memento.app.domain.usecase

import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.ProgressEntry
import com.memento.app.domain.model.Reflection
import com.memento.app.domain.model.TimelineEvent
import java.time.ZoneOffset

object TimelineBuilder {
    fun build(
        consumptions: List<Consumption>,
        progress: List<ProgressEntry>,
        reflections: List<Reflection>,
    ): List<TimelineEvent> = buildList {
        consumptions.forEach { consumption ->
            consumption.startedDate?.let { date ->
                add(TimelineEvent.ConsumptionStarted(date, date.atStartOfDay().toInstant(ZoneOffset.UTC)))
            }
            if (consumption.status == ConsumptionStatus.COMPLETED) {
                consumption.completedDate?.let { date ->
                    add(TimelineEvent.ConsumptionCompleted(date, consumption.ratingHalfStars, consumption.updatedAt))
                }
            }
            if (consumption.status == ConsumptionStatus.DROPPED) {
                add(TimelineEvent.ConsumptionDropped(consumption.completedDate, consumption.updatedAt))
            }
        }
        progress.forEach { add(TimelineEvent.ProgressUpdated(it)) }
        reflections.forEach { add(TimelineEvent.ReflectionWritten(it)) }
    }.sortedByDescending { it.sortInstant }
}

