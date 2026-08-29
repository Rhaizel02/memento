package com.memento.app.domain.recommendation

import java.time.Duration
import java.time.Instant

object RecommendationCachePolicy {
    val refreshAfter: Duration = Duration.ofHours(24)
    val retentionWindow: Duration = Duration.ofDays(7)

    fun shouldRefresh(latestFetch: Instant?, now: Instant): Boolean =
        latestFetch == null || !latestFetch.plus(refreshAfter).isAfter(now)
}
