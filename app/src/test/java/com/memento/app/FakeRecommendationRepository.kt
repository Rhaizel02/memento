package com.memento.app

import com.memento.app.domain.model.RecommendationFeedbackType
import com.memento.app.domain.recommendation.RecommendationKey
import com.memento.app.domain.recommendation.TasteProfile
import com.memento.app.domain.repository.RecommendationFeed
import com.memento.app.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRecommendationRepository : RecommendationRepository {
    val feed = MutableStateFlow(RecommendationFeed(TasteProfile(0, emptyMap(), emptyMap(), emptyMap()), emptyList()))
    override fun observeFeed(): Flow<RecommendationFeed> = feed
    override suspend fun refreshCandidates(force: Boolean) = Unit
    override suspend fun setFeedback(key: RecommendationKey, feedback: RecommendationFeedbackType) = Unit
}
