package com.memento.app.domain.model

import java.time.Instant
import java.time.LocalDate

enum class TimelineEventType {
    STARTED,
    COMPLETED,
    PROGRESS,
    NOTE,
    QUOTE,
    FINAL_REFLECTION,
    LATER_REFLECTION,
}

data class TimelineMediaContext(
    val id: String,
    val type: MediaType,
    val title: String,
    val posterUrl: String?,
    val isFavorite: Boolean,
)

data class CulturalTimelineEvent(
    val id: String,
    val date: LocalDate,
    val occurredAt: Instant?,
    val mediaItemId: String,
    val consumptionId: String,
    val mediaType: MediaType,
    val title: String,
    val posterUrl: String?,
    val eventType: TimelineEventType,
    val isReconsumption: Boolean = false,
    val ratingHalfStars: Int? = null,
    val reflectionId: String? = null,
    val reflectionContent: String? = null,
    val progress: ProgressEntry? = null,
    val isFavorite: Boolean = false,
)

data class CulturalTimelineWindow(
    val events: List<CulturalTimelineEvent>,
    val hasMore: Boolean,
)
