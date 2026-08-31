package com.memento.app.data.repository

import com.memento.app.BuildConfig
import com.memento.app.data.remote.api.TmdbApi
import com.memento.app.data.remote.api.TmdbWatchProvidersResponse
import com.memento.app.data.remote.mapper.toWatchAvailability
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.repository.WatchAvailabilityRepository
import com.memento.app.domain.watch.WatchAvailabilityResult
import com.memento.app.domain.watch.WatchRegionResolver
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class TmdbWatchAvailabilityRepository constructor(
    private val tmdbApi: TmdbApi,
    private val regionResolver: WatchRegionResolver,
    private val apiKey: String,
) : WatchAvailabilityRepository {
    @Inject
    constructor(
        tmdbApi: TmdbApi,
        regionResolver: WatchRegionResolver,
    ) : this(tmdbApi, regionResolver, BuildConfig.TMDB_API_KEY)

    private val cache = mutableMapOf<CacheKey, WatchAvailabilityResult>()
    private val cacheMutex = Mutex()

    override suspend fun get(mediaType: MediaType, tmdbId: String): WatchAvailabilityResult {
        if (mediaType != MediaType.MOVIE && mediaType != MediaType.SERIES) return WatchAvailabilityResult.Unsupported
        val cleanId = tmdbId.trim().takeIf { it.toLongOrNull()?.let { value -> value > 0 } == true }
            ?: return WatchAvailabilityResult.Unsupported
        check(apiKey.isNotBlank()) { "TMDB is not configured" }
        val region = regionResolver.resolve()
        val key = CacheKey(mediaType, cleanId, region)
        cacheMutex.withLock { cache[key] }?.let { return it }

        val response = fetchWatchProviders(tmdbApi, mediaType, cleanId, apiKey)
        val result = response.toWatchAvailability(region)?.let { WatchAvailabilityResult.Available(it) }
            ?: WatchAvailabilityResult.Empty(region)
        cacheMutex.withLock { cache[key] = result }
        return result
    }

    private data class CacheKey(val mediaType: MediaType, val tmdbId: String, val region: String)
}

suspend fun fetchWatchProviders(
    tmdbApi: TmdbApi,
    mediaType: MediaType,
    tmdbId: String,
    apiKey: String,
): TmdbWatchProvidersResponse = when (mediaType) {
    MediaType.MOVIE -> tmdbApi.movieWatchProviders(tmdbId, apiKey)
    MediaType.SERIES -> tmdbApi.seriesWatchProviders(tmdbId, apiKey)
    MediaType.BOOK, MediaType.GAME -> error("Watch availability is not supported for $mediaType")
}
