package com.memento.app.data.repository

import com.memento.app.BuildConfig
import com.memento.app.data.remote.api.OpenLibraryApi
import com.memento.app.data.remote.api.RawgApi
import com.memento.app.data.remote.api.TmdbApi
import com.memento.app.data.remote.mapper.toMetadataResult
import com.memento.app.data.remote.mapper.tmdbGenreId
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchOutcome
import com.memento.app.domain.repository.MetadataRepository
import com.memento.app.domain.repository.MetadataDetailsOutcome
import javax.inject.Inject
import javax.inject.Singleton
import java.text.Normalizer

@Singleton
class RemoteMetadataRepository @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val openLibraryApi: OpenLibraryApi,
    private val rawgApi: RawgApi,
) : MetadataRepository {
    override suspend fun fetchDetails(result: com.memento.app.domain.model.MetadataSearchResult): MetadataDetailsOutcome =
        runCatching {
            when (result.provider) {
                MetadataProvider.TMDB -> when (result.type) {
                    MediaType.MOVIE -> tmdbApi.movieDetails(result.externalId, BuildConfig.TMDB_API_KEY).toMetadataResult()
                    MediaType.SERIES -> tmdbApi.seriesDetails(result.externalId, BuildConfig.TMDB_API_KEY).toMetadataResult()
                    else -> error("Tipo incompatible con TMDB")
                }
                MetadataProvider.OPEN_LIBRARY -> {
                    val work = openLibraryApi.workDetails(result.externalId)
                    val edition = runCatching { openLibraryApi.workEditions(result.externalId).entries.firstOrNull() }.getOrNull()
                    work.toMetadataResult(result, edition)
                }
                MetadataProvider.RAWG -> rawgApi.gameDetails(result.externalId, BuildConfig.RAWG_API_KEY).toMetadataResult()
            }
        }.fold(
            onSuccess = { MetadataDetailsOutcome.Complete(it) },
            onFailure = { MetadataDetailsOutcome.Partial(result) },
        )

    override suspend fun search(type: MediaType, query: String): MetadataSearchOutcome {
        val cleanQuery = query.trim()
        require(cleanQuery.length >= 2)
        return when (type) {
            MediaType.BOOK -> runProvider(MetadataProvider.OPEN_LIBRARY) {
                openLibraryApi.searchBooks(cleanQuery).docs.map { it.toMetadataResult() }
            }
            MediaType.MOVIE -> if (BuildConfig.TMDB_API_KEY.isBlank()) {
                MetadataSearchOutcome.NotConfigured(MetadataProvider.TMDB)
            } else runProvider(MetadataProvider.TMDB) {
                tmdbApi.searchMovies(BuildConfig.TMDB_API_KEY, cleanQuery).results.map { it.toMetadataResult() }
            }
            MediaType.SERIES -> if (BuildConfig.TMDB_API_KEY.isBlank()) {
                MetadataSearchOutcome.NotConfigured(MetadataProvider.TMDB)
            } else runProvider(MetadataProvider.TMDB) {
                tmdbApi.searchSeries(BuildConfig.TMDB_API_KEY, cleanQuery).results.map { it.toMetadataResult() }
            }
            MediaType.GAME -> if (BuildConfig.RAWG_API_KEY.isBlank()) {
                MetadataSearchOutcome.NotConfigured(MetadataProvider.RAWG)
            } else runProvider(MetadataProvider.RAWG) {
                rawgApi.searchGames(BuildConfig.RAWG_API_KEY, cleanQuery).results.map { it.toMetadataResult() }
            }
        }
    }

    override suspend fun recommendationCandidates(
        type: MediaType,
        preferredGenres: List<String>,
        preferredCreators: List<String>,
    ) = runCatching {
        when (type) {
            MediaType.BOOK -> {
                val query = preferredGenres.firstOrNull()?.let { "subject:\"$it\"" }
                    ?: preferredCreators.firstOrNull()?.let { "author:\"$it\"" }
                    ?: return@runCatching emptyList()
                openLibraryApi.searchBooks(query).docs.map { it.toMetadataResult() }
            }
            MediaType.MOVIE -> {
                if (BuildConfig.TMDB_API_KEY.isBlank()) return@runCatching emptyList()
                val genreId = preferredGenres.firstNotNullOfOrNull { tmdbGenreId(it, type) }
                    ?: return@runCatching emptyList()
                tmdbApi.discoverMovies(BuildConfig.TMDB_API_KEY, genreId.toString()).results.map { it.toMetadataResult() }
            }
            MediaType.SERIES -> {
                if (BuildConfig.TMDB_API_KEY.isBlank()) return@runCatching emptyList()
                val genreId = preferredGenres.firstNotNullOfOrNull { tmdbGenreId(it, type) }
                    ?: return@runCatching emptyList()
                tmdbApi.discoverSeries(BuildConfig.TMDB_API_KEY, genreId.toString()).results.map { it.toMetadataResult() }
            }
            MediaType.GAME -> {
                if (BuildConfig.RAWG_API_KEY.isBlank()) return@runCatching emptyList()
                val genre = preferredGenres.firstOrNull() ?: return@runCatching emptyList()
                rawgApi.discoverGames(BuildConfig.RAWG_API_KEY, genre.toSlug()).results.map { it.toMetadataResult() }
            }
        }
    }.getOrDefault(emptyList())

    private suspend fun runProvider(
        provider: MetadataProvider,
        request: suspend () -> List<com.memento.app.domain.model.MetadataSearchResult>,
    ): MetadataSearchOutcome = runCatching { request() }
        .fold(
            onSuccess = { MetadataSearchOutcome.Success(provider, it) },
            onFailure = { MetadataSearchOutcome.Unavailable(provider) },
        )
}

private fun String.toSlug(): String = Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
    .replace("\\p{Mn}+".toRegex(), "")
    .replace("[^a-z0-9]+".toRegex(), "-")
    .trim('-')
