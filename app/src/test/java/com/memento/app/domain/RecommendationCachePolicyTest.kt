package com.memento.app.domain

import com.memento.app.domain.recommendation.RecommendationCachePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RecommendationCachePolicyTest {
    private val now = Instant.parse("2029-06-15T10:00:00Z")

    @Test fun `fresh cache avoids remote refresh`() {
        assertFalse(RecommendationCachePolicy.shouldRefresh(now.minusSeconds(23 * 60 * 60), now))
    }

    @Test fun `expired or missing cache requests refresh`() {
        assertTrue(RecommendationCachePolicy.shouldRefresh(now.minusSeconds(24 * 60 * 60), now))
        assertTrue(RecommendationCachePolicy.shouldRefresh(null, now))
    }
}
