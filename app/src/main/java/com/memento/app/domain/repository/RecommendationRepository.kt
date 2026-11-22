package com.memento.app.domain.repository

import com.memento.app.domain.model.RecommendationFeedbackType
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.recommendation.Recommendation
import com.memento.app.domain.recommendation.RecommendationKey
import com.memento.app.domain.recommendation.TasteProfile
import com.memento.app.domain.recommendation.recommendationKey
import kotlinx.coroutines.flow.Flow

data class RecommendationFeed(
    val profile: TasteProfile,
    val recommendations: List<Recommendation>,
    val byType: Map<MediaType, List<Recommendation>> = recommendations.groupBy { it.candidate.type },
    val popularByType: Map<MediaType, List<MetadataSearchResult>> = emptyMap(),
    val statusByType: Map<MediaType, CandidateRefreshStatus> = emptyMap(),
) {
    fun findRecommendation(key: RecommendationKey): Recommendation? =
        byType[key.mediaType]?.firstOrNull { it.candidate.recommendationKey() == key }
            ?: recommendations.firstOrNull { it.candidate.recommendationKey() == key }

    fun findPopular(key: RecommendationKey): MetadataSearchResult? =
        popularByType[key.mediaType]?.firstOrNull { it.recommendationKey() == key }
}

const val DISCOVER_TARGET = 6

data class CandidateRefreshStatus(
    val loading: Boolean = false,
    val issue: CandidateIssue? = null,
)

interface RecommendationRepository {
    fun observeFeed(): Flow<RecommendationFeed>
    suspend fun refreshCandidates(force: Boolean = false, type: MediaType? = null, includePopular: Boolean = false)
    suspend fun setFeedback(key: RecommendationKey, feedback: RecommendationFeedbackType)
}
