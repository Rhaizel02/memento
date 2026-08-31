package com.memento.app

import com.memento.app.domain.model.MediaType
import com.memento.app.domain.repository.WatchAvailabilityRepository
import com.memento.app.domain.watch.WatchAvailabilityResult

class FakeWatchAvailabilityRepository : WatchAvailabilityRepository {
    val requests = mutableListOf<Pair<MediaType, String>>()
    var result: WatchAvailabilityResult = WatchAvailabilityResult.Unsupported
    var handler: (suspend (MediaType, String) -> WatchAvailabilityResult)? = null

    override suspend fun get(mediaType: MediaType, tmdbId: String): WatchAvailabilityResult {
        requests += mediaType to tmdbId
        return handler?.invoke(mediaType, tmdbId) ?: result
    }
}
