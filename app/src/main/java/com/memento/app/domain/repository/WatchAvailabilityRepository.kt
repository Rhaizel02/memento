package com.memento.app.domain.repository

import com.memento.app.domain.model.MediaType
import com.memento.app.domain.watch.WatchAvailabilityResult

interface WatchAvailabilityRepository {
    suspend fun get(mediaType: MediaType, tmdbId: String): WatchAvailabilityResult
}
