package com.memento.app.domain.repository

import com.memento.app.domain.model.RecommendationFeedbackType
import com.memento.app.domain.recommendation.Recommendation
import com.memento.app.domain.recommendation.RecommendationKey
import com.memento.app.domain.recommendation.TasteProfile
import kotlinx.coroutines.flow.Flow

data class RecommendationFeed(
    val profile: TasteProfile,
    val recommendations: List<Recommendation>,
)

interface RecommendationRepository {
    fun observeFeed(): Flow<RecommendationFeed>
    suspend fun refreshCandidates(force: Boolean = false)
    suspend fun setFeedback(key: RecommendationKey, feedback: RecommendationFeedbackType)
}
