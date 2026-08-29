package com.memento.app

import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchOutcome
import com.memento.app.domain.repository.MetadataRepository
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.repository.MetadataDetailsOutcome

class FakeMetadataRepository : MetadataRepository {
    var outcome: MetadataSearchOutcome = MetadataSearchOutcome.Success(MetadataProvider.OPEN_LIBRARY, emptyList())
    var detailOutcome: MetadataDetailsOutcome? = null
    var detailRequests = 0
    var recommendationResult: List<MetadataSearchResult> = emptyList()
    var recommendationRequests = 0
    override suspend fun search(type: MediaType, query: String): MetadataSearchOutcome = outcome
    override suspend fun fetchDetails(result: MetadataSearchResult): MetadataDetailsOutcome {
        detailRequests++
        return detailOutcome ?: MetadataDetailsOutcome.Complete(result)
    }
    override suspend fun recommendationCandidates(
        type: MediaType,
        preferredGenres: List<String>,
        preferredCreators: List<String>,
    ): List<MetadataSearchResult> {
        recommendationRequests++
        return recommendationResult
    }
}
