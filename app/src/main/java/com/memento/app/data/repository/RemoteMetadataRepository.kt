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
import com.memento.app.domain.recommendation.RecommendationAnchor
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
        anchors: List<RecommendationAnchor>,
    ): List<com.memento.app.domain.model.MetadataSearchResult> {
        val collected = linkedMapOf<String, com.memento.app.domain.model.MetadataSearchResult>()
        fun add(
            candidates: List<com.memento.app.domain.model.MetadataSearchResult>,
            anchor: RecommendationAnchor? = null,
        ) {
            candidates.forEach { candidate ->
                val key = "${candidate.provider}:${candidate.externalId}:${candidate.type}"
                val previous = collected[key]
                collected[key] = (previous ?: candidate).copy(
                    sourceAnchorMediaIds = (previous?.sourceAnchorMediaIds.orEmpty() + candidate.sourceAnchorMediaIds + listOfNotNull(anchor?.mediaId)).distinct(),
                    sourceAnchorTitles = (previous?.sourceAnchorTitles.orEmpty() + candidate.sourceAnchorTitles + listOfNotNull(anchor?.title)).distinct(),
                )
            }
        }

        when (type) {
            MediaType.BOOK -> {
                anchors.filter { it.type == MediaType.BOOK && it.strength >= 0.65 }.take(4).forEach { anchor ->
                    anchor.creators.take(1).forEach { author ->
                        val rows = runCatching { openLibraryApi.searchBooks("author:\"$author\"").docs }.getOrDefault(emptyList())
                        add(rows.map { it.toMetadataResult() }, anchor)
                    }
                }
                val subjectQuery = preferredGenres.take(2).joinToString(" AND ") { "subject:\"$it\"" }
                if (subjectQuery.isNotBlank()) {
                    val rows = runCatching { openLibraryApi.searchBooks(subjectQuery).docs }.getOrDefault(emptyList())
                    add(rows.map { it.toMetadataResult() })
                }
            }
            MediaType.MOVIE, MediaType.SERIES -> {
                if (BuildConfig.TMDB_API_KEY.isBlank()) return emptyList()
                anchors.filter { it.type == type && it.strength >= 0.65 }.take(4).forEach { anchor ->
                    anchor.externalKeys.filter { it.provider == MetadataProvider.TMDB && it.mediaType == type }.take(1).forEach { key ->
                        val candidates = if (type == MediaType.MOVIE) {
                            runCatching { tmdbApi.movieRecommendations(key.externalId, BuildConfig.TMDB_API_KEY).results.map { it.toMetadataResult() } }
                                .getOrDefault(emptyList())
                        } else {
                            runCatching { tmdbApi.seriesRecommendations(key.externalId, BuildConfig.TMDB_API_KEY).results.map { it.toMetadataResult() } }
                                .getOrDefault(emptyList())
                        }
                        add(candidates, anchor)
                    }
                }
                val genreIds = preferredGenres.mapNotNull { tmdbGenreId(it, type) }.distinct().take(3)
                if (genreIds.isNotEmpty()) {
                    val candidates = if (type == MediaType.MOVIE) {
                        runCatching { tmdbApi.discoverMovies(BuildConfig.TMDB_API_KEY, genreIds.joinToString(",")).results.map { it.toMetadataResult() } }
                            .getOrDefault(emptyList())
                    } else {
                        runCatching { tmdbApi.discoverSeries(BuildConfig.TMDB_API_KEY, genreIds.joinToString(",")).results.map { it.toMetadataResult() } }
                            .getOrDefault(emptyList())
                    }
                    add(candidates)
                }
            }
            MediaType.GAME -> {
                if (BuildConfig.RAWG_API_KEY.isBlank()) return emptyList()
                anchors.filter { it.type == MediaType.GAME && it.strength >= 0.65 }.take(4).forEach { anchor ->
                    val externalId = anchor.externalKeys.firstOrNull {
                        it.provider == MetadataProvider.RAWG && it.mediaType == MediaType.GAME
                    }?.externalId
                    val details = externalId?.let { id ->
                        runCatching { rawgApi.gameDetails(id, BuildConfig.RAWG_API_KEY) }.getOrNull()
                    }
                    val rows = runCatching {
                        rawgApi.discoverGames(
                            apiKey = BuildConfig.RAWG_API_KEY,
                            genreSlugs = details?.genres?.take(2)?.joinToString(",") { it.name.toSlug() }?.takeIf(String::isNotBlank)
                                ?: anchor.genres.take(2).joinToString(",") { it.toSlug() }.takeIf(String::isNotBlank),
                            developerSlugs = details?.developers?.firstOrNull()?.name?.toSlug()
                                ?: anchor.creators.firstOrNull()?.toSlug(),
                            tagSlugs = details?.tags?.take(2)?.joinToString(",") { it.name.toSlug() }?.takeIf(String::isNotBlank),
                        ).results
                    }.getOrDefault(emptyList())
                    add(rows.map { it.toMetadataResult() }, anchor)
                }
                val rows = runCatching {
                    rawgApi.discoverGames(
                        apiKey = BuildConfig.RAWG_API_KEY,
                        genreSlugs = preferredGenres.take(2).joinToString(",") { it.toSlug() }.takeIf(String::isNotBlank),
                        developerSlugs = preferredCreators.firstOrNull()?.toSlug(),
                    ).results
                }.getOrDefault(emptyList())
                add(rows.map { it.toMetadataResult() })
            }
        }
        return collected.values.toList()
    }

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
