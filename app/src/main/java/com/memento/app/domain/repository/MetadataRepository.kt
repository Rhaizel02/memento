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
        preferredCreators: List<String>,
        anchors: List<RecommendationAnchor>,
    ): List<MetadataSearchResult>
}

sealed interface MetadataDetailsOutcome {
    val result: MetadataSearchResult
    data class Complete(override val result: MetadataSearchResult) : MetadataDetailsOutcome
    data class Partial(override val result: MetadataSearchResult) : MetadataDetailsOutcome
}
