package com.memento.app.domain.watch

import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider

data class WatchProvider(
    val id: Int,
    val name: String,
    val logoPath: String?,
    val displayPriority: Int?,
)

data class WatchAvailability(
    val region: String,
    val link: String?,
    val streaming: List<WatchProvider>,
    val rent: List<WatchProvider>,
    val buy: List<WatchProvider>,
)

sealed interface WatchAvailabilityResult {
    data class Available(val availability: WatchAvailability) : WatchAvailabilityResult
    data class Empty(val region: String) : WatchAvailabilityResult
    data object Unsupported : WatchAvailabilityResult
}

data class WatchAvailabilityRequest(
    val mediaType: MediaType,
    val tmdbId: String,
) {
    companion object {
        fun create(
            mediaType: MediaType,
            provider: MetadataProvider,
            externalId: String,
        ): WatchAvailabilityRequest? {
            if (provider != MetadataProvider.TMDB || mediaType !in SUPPORTED_TYPES) return null
            val id = externalId.trim().takeIf { it.toLongOrNull()?.let { value -> value > 0 } == true } ?: return null
            return WatchAvailabilityRequest(mediaType, id)
        }

        private val SUPPORTED_TYPES = setOf(MediaType.MOVIE, MediaType.SERIES)
    }
}
