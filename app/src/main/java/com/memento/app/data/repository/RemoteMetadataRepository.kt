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
import com.memento.app.data.remote.mapper.rawgGenreSlug
import com.memento.app.domain.recommendation.recommendationKey
import com.memento.app.domain.repository.CandidateBatch
import com.memento.app.domain.repository.CandidateIssue
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

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
        anchors: List<RecommendationAnchor>,
    ): CandidateBatch {
        val strongAnchors = anchors.filter { it.type == type && it.strength >= 0.65 }.take(4)
        val batches = mutableListOf<CandidateBatch>()
        when (type) {
            MediaType.BOOK -> {
                for (anchor in strongAnchors) {
                    val author = anchor.creators.firstOrNull() ?: continue
                    batches += requestCandidates {
                        CandidateBatch(openLibraryApi.searchBooks("author:\"$author\"").docs.map { it.toMetadataResult() })
                    }.withAnchor(anchor)
                }
                val subjects = preferredGenres.take(2).joinToString(" AND ") { "subject:\"$it\"" }
                if (subjects.isNotEmpty()) {
                    batches += requestCandidates {
                        CandidateBatch(openLibraryApi.searchBooks(subjects).docs.map { it.toMetadataResult() })
                    }
                }
            }
            MediaType.MOVIE, MediaType.SERIES -> {
                val apiKey = BuildConfig.TMDB_API_KEY
                if (apiKey.isBlank()) return CandidateBatch(issue = CandidateIssue.NOT_CONFIGURED)
                val genreIds = preferredGenres.mapNotNull { tmdbGenreId(it, type) }.distinct().take(3)
                if (genreIds.isNotEmpty()) batches += tmdbPage(type, apiKey, genres = genreIds.joinToString("|"))
                for (anchor in strongAnchors) {
                    val id = anchor.externalKeys.firstOrNull { it.provider == MetadataProvider.TMDB && it.mediaType == type }
                        ?.externalId ?: continue
                    batches += tmdbPage(type, apiKey, anchorId = id).withAnchor(anchor)
                }
            }
            MediaType.GAME -> {
                val apiKey = BuildConfig.RAWG_API_KEY
                if (apiKey.isBlank()) return CandidateBatch(issue = CandidateIssue.NOT_CONFIGURED)
                val genres = preferredGenres.mapNotNull(::rawgGenreSlug).distinct().take(2)
                if (genres.isNotEmpty()) batches += rawgPage(apiKey, genres = genres.joinToString(","))
                for (anchor in strongAnchors) {
                    val id = anchor.externalKeys.firstOrNull { it.provider == MetadataProvider.RAWG && it.mediaType == type }?.externalId
                    val details = try {
                        id?.let { rawgApi.gameDetails(it, apiKey) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                    val genreSlugs = details?.genres.orEmpty().mapNotNull { it.slug ?: rawgGenreSlug(it.name) }
                        .ifEmpty { anchor.genres.mapNotNull(::rawgGenreSlug) }.distinct().take(2)
                        .joinToString(",").takeIf(String::isNotEmpty)
                    val developer = details?.developers?.firstOrNull()?.let { it.slug ?: it.id?.toString() }
                    if (genreSlugs == null && developer == null) continue
                    val batch = rawgPage(apiKey, genres = genreSlugs, developer = developer)
                    batches += if (batch.items.isEmpty() && batch.issue == null && developer != null && genreSlugs != null) {
                        rawgPage(apiKey, genres = genreSlugs).withAnchor(anchor)
                    } else batch.withAnchor(anchor)
                }
            }
        }
        val candidates = batches.flatMap { it.items }.groupBy { it.recommendationKey() }.values.map { matches ->
            matches.first().copy(
                sourceAnchorMediaIds = matches.flatMap { it.sourceAnchorMediaIds }.distinct(),
                sourceAnchorTitles = matches.flatMap { it.sourceAnchorTitles }.distinct(),
            )
        }
        return CandidateBatch(
            candidates,
            issue = batches.firstNotNullOfOrNull { it.issue },
            loadMore = batches.firstNotNullOfOrNull { it.loadMore },
        )
    }

    override suspend fun popularCandidates(type: MediaType): CandidateBatch = when (type) {
        MediaType.BOOK -> CandidateBatch()
        MediaType.MOVIE, MediaType.SERIES -> if (BuildConfig.TMDB_API_KEY.isBlank()) {
            CandidateBatch(issue = CandidateIssue.NOT_CONFIGURED)
        } else tmdbPage(type, BuildConfig.TMDB_API_KEY, popular = true)
        MediaType.GAME -> if (BuildConfig.RAWG_API_KEY.isBlank()) {
            CandidateBatch(issue = CandidateIssue.NOT_CONFIGURED)
        } else rawgPage(BuildConfig.RAWG_API_KEY, popular = true)
    }

    private suspend fun tmdbPage(
        type: MediaType,
        apiKey: String,
        genres: String? = null,
        anchorId: String? = null,
        popular: Boolean = false,
        page: Int = 1,
    ): CandidateBatch = requestCandidates {
        val sorting = if (popular) "popularity.desc" else "vote_count.desc"
        val (items, pageCount) = if (type == MediaType.MOVIE) {
            val response = if (anchorId == null) tmdbApi.discoverMovies(apiKey, genres, sortBy = sorting, page = page)
            else tmdbApi.movieRecommendations(anchorId, apiKey, page = page)
            response.results.map { it.toMetadataResult() } to response.totalPages
        } else {
            val response = if (anchorId == null) tmdbApi.discoverSeries(apiKey, genres, sortBy = sorting, page = page)
            else tmdbApi.seriesRecommendations(anchorId, apiKey, page = page)
            response.results.map { it.toMetadataResult() } to response.totalPages
        }
        CandidateBatch(items, loadMore = if (page < pageCount) {
            { tmdbPage(type, apiKey, genres, anchorId, popular, page + 1) }
        } else null)
    }

    private suspend fun rawgPage(
        apiKey: String,
        genres: String? = null,
        developer: String? = null,
        popular: Boolean = false,
        page: Int = 1,
    ): CandidateBatch = requestCandidates {
        val response = rawgApi.discoverGames(
            apiKey = apiKey, genreSlugs = genres, developerSlugs = developer,
            ordering = if (popular) "-added" else "-metacritic", page = page,
        )
        CandidateBatch(response.results.map { it.toMetadataResult() }, loadMore = if (response.next != null) {
            { rawgPage(apiKey, genres, developer, popular, page + 1) }
        } else null)
    }

    private fun CandidateBatch.withAnchor(anchor: RecommendationAnchor): CandidateBatch = copy(
        items = items.map {
            it.copy(
                sourceAnchorMediaIds = (it.sourceAnchorMediaIds + anchor.mediaId).distinct(),
                sourceAnchorTitles = (it.sourceAnchorTitles + anchor.title).distinct(),
            )
        },
        loadMore = loadMore?.let { load -> { load().withAnchor(anchor) } },
    )

    private suspend fun requestCandidates(request: suspend () -> CandidateBatch): CandidateBatch = try {
        request()
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        CandidateBatch(issue = when (e.code()) {
            401, 403 -> CandidateIssue.AUTHENTICATION
            429 -> CandidateIssue.RATE_LIMITED
            else -> CandidateIssue.UNAVAILABLE
        })
    } catch (_: IOException) {
        CandidateBatch(issue = CandidateIssue.CONNECTION)
    } catch (_: Exception) {
        CandidateBatch(issue = CandidateIssue.UNAVAILABLE)
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
