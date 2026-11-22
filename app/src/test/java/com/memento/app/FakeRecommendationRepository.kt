package com.memento.app

import com.memento.app.domain.model.RecommendationFeedbackType
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.recommendation.recommendationKey
import com.memento.app.domain.recommendation.RecommendationKey
import com.memento.app.domain.recommendation.TasteProfile
import com.memento.app.domain.repository.RecommendationFeed
import com.memento.app.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRecommendationRepository : RecommendationRepository {
    val feed = MutableStateFlow(RecommendationFeed(TasteProfile(0, emptyMap(), emptyMap(), emptyMap()), emptyList()))
    val feedbackCalls = mutableListOf<Pair<RecommendationKey, RecommendationFeedbackType>>()
    override fun observeFeed(): Flow<RecommendationFeed> = feed
    override suspend fun refreshCandidates(force: Boolean, type: MediaType?, includePopular: Boolean) = Unit
    override suspend fun setFeedback(key: RecommendationKey, feedback: RecommendationFeedbackType) {
        feedbackCalls += key to feedback
        if (feedback == RecommendationFeedbackType.NOT_INTERESTED || feedback == RecommendationFeedbackType.ALREADY_KNOWN) {
            feed.value = feed.value.copy(
                recommendations = feed.value.recommendations.filterNot { it.candidate.recommendationKey() == key },
                byType = feed.value.byType.mapValues { (_, items) ->
                    items.filterNot { it.candidate.recommendationKey() == key }
                },
                popularByType = feed.value.popularByType.mapValues { (_, items) ->
                    items.filterNot { it.recommendationKey() == key }
                },
            )
        }
    }
}
