package com.memento.app

import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchOutcome
import com.memento.app.domain.repository.MetadataRepository
import com.memento.app.domain.model.MetadataSearchResult

class FakeMetadataRepository : MetadataRepository {
    var outcome: MetadataSearchOutcome = MetadataSearchOutcome.Success(MetadataProvider.OPEN_LIBRARY, emptyList())
    override suspend fun search(type: MediaType, query: String): MetadataSearchOutcome = outcome
    override suspend fun recommendationCandidates(
        type: MediaType,
        preferredGenres: List<String>,
        preferredCreators: List<String>,
    ): List<MetadataSearchResult> = emptyList()
}
