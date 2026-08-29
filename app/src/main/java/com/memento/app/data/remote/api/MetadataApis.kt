package com.memento.app.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {
    @GET("3/search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "es-ES",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1,
    ): TmdbMovieSearchResponse

    @GET("3/search/tv")
    suspend fun searchSeries(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "es-ES",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1,
    ): TmdbSeriesSearchResponse

    @GET("3/discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("with_genres") genreId: String,
        @Query("language") language: String = "es-ES",
        @Query("sort_by") sortBy: String = "vote_count.desc",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1,
    ): TmdbMovieSearchResponse

    @GET("3/discover/tv")
    suspend fun discoverSeries(
        @Query("api_key") apiKey: String,
        @Query("with_genres") genreId: String,
        @Query("language") language: String = "es-ES",
        @Query("sort_by") sortBy: String = "vote_count.desc",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1,
    ): TmdbSeriesSearchResponse
}

interface OpenLibraryApi {
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("fields") fields: String = "key,title,author_name,first_publish_year,cover_i,number_of_pages_median,subject",
        @Query("limit") limit: Int = 20,
        @Query("lang") language: String = "es",
    ): OpenLibrarySearchResponse
}

interface RawgApi {
    @GET("api/games")
    suspend fun searchGames(
        @Query("key") apiKey: String,
        @Query("search") query: String,
        @Query("search_precise") precise: Boolean = true,
        @Query("page_size") pageSize: Int = 20,
    ): RawgSearchResponse

    @GET("api/games")
    suspend fun discoverGames(
        @Query("key") apiKey: String,
        @Query("genres") genreSlug: String,
        @Query("ordering") ordering: String = "-metacritic",
        @Query("page_size") pageSize: Int = 20,
    ): RawgSearchResponse
}

@Serializable
data class TmdbMovieSearchResponse(val results: List<TmdbMovieDto> = emptyList())

@Serializable
data class TmdbMovieDto(
    val id: Long,
    val title: String,
    @SerialName("original_title") val originalTitle: String? = null,
    val overview: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
)

@Serializable
data class TmdbSeriesSearchResponse(val results: List<TmdbSeriesDto> = emptyList())

@Serializable
data class TmdbSeriesDto(
    val id: Long,
    val name: String,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
)

@Serializable
data class OpenLibrarySearchResponse(val docs: List<OpenLibraryBookDto> = emptyList())

@Serializable
data class OpenLibraryBookDto(
    val key: String,
    val title: String,
    @SerialName("author_name") val authors: List<String> = emptyList(),
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    @SerialName("cover_i") val coverId: Long? = null,
    @SerialName("number_of_pages_median") val pageCount: Int? = null,
    val subject: List<String> = emptyList(),
)

@Serializable
data class RawgSearchResponse(val results: List<RawgGameDto> = emptyList())

@Serializable
data class RawgGameDto(
    val id: Long,
    val slug: String? = null,
    val name: String,
    val released: String? = null,
    @SerialName("background_image") val backgroundImage: String? = null,
    val genres: List<RawgGenreDto> = emptyList(),
)

@Serializable
data class RawgGenreDto(val name: String)
