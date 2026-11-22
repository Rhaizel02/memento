package com.memento.app.domain.repository

import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataSearchOutcome
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.recommendation.RecommendationAnchor

interface MetadataRepository {
    suspend fun search(type: MediaType, query: String): MetadataSearchOutcome
    suspend fun fetchDetails(result: MetadataSearchResult): MetadataDetailsOutcome
    suspend fun recommendationCandidates(
        type: MediaType,
        preferredGenres: List<String>,
        anchors: List<RecommendationAnchor>,
    ): CandidateBatch

    suspend fun popularCandidates(type: MediaType): CandidateBatch
}

enum class CandidateIssue { NOT_CONFIGURED, AUTHENTICATION, RATE_LIMITED, CONNECTION, UNAVAILABLE }

data class CandidateBatch(
    val items: List<MetadataSearchResult> = emptyList(),
    val issue: CandidateIssue? = null,
    val loadMore: (suspend () -> CandidateBatch)? = null,
)

sealed interface MetadataDetailsOutcome {
    val result: MetadataSearchResult
    data class Complete(override val result: MetadataSearchResult) : MetadataDetailsOutcome
    data class Partial(override val result: MetadataSearchResult) : MetadataDetailsOutcome
}
