package com.memento.app.data.remote.mapper

import com.memento.app.data.remote.api.OpenLibraryBookDto
import com.memento.app.data.remote.api.RawgGameDto
import com.memento.app.data.remote.api.TmdbMovieDto
import com.memento.app.data.remote.api.TmdbSeriesDto
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import java.time.LocalDate

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/"

fun TmdbMovieDto.toMetadataResult() = MetadataSearchResult(
    provider = MetadataProvider.TMDB,
    externalId = id.toString(),
    externalUrl = "https://www.themoviedb.org/movie/$id",
    type = MediaType.MOVIE,
    title = title,
    originalTitle = originalTitle?.takeUnless { it == title },
    description = overview?.takeIf(String::isNotBlank),
    releaseDate = releaseDate.toLocalDateOrNull(),
    releaseYear = releaseDate.toLocalDateOrNull()?.year,
    posterUrl = posterPath?.let { "${TMDB_IMAGE_BASE}w500$it" },
    backdropUrl = backdropPath?.let { "${TMDB_IMAGE_BASE}w780$it" },
    genres = genreIds.mapNotNull(movieGenres::get),
)

fun TmdbSeriesDto.toMetadataResult() = MetadataSearchResult(
    provider = MetadataProvider.TMDB,
    externalId = id.toString(),
    externalUrl = "https://www.themoviedb.org/tv/$id",
    type = MediaType.SERIES,
    title = name,
    originalTitle = originalName?.takeUnless { it == name },
    description = overview?.takeIf(String::isNotBlank),
    releaseDate = firstAirDate.toLocalDateOrNull(),
    releaseYear = firstAirDate.toLocalDateOrNull()?.year,
    posterUrl = posterPath?.let { "${TMDB_IMAGE_BASE}w500$it" },
    backdropUrl = backdropPath?.let { "${TMDB_IMAGE_BASE}w780$it" },
    genres = genreIds.mapNotNull(tvGenres::get),
)

fun OpenLibraryBookDto.toMetadataResult() = MetadataSearchResult(
    provider = MetadataProvider.OPEN_LIBRARY,
    externalId = key.removePrefix("/works/"),
    externalUrl = "https://openlibrary.org$key",
    type = MediaType.BOOK,
    title = title,
    releaseYear = firstPublishYear,
    posterUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" },
    creators = authors,
    genres = subject.take(5),
    pageCount = pageCount,
)

fun RawgGameDto.toMetadataResult() = MetadataSearchResult(
    provider = MetadataProvider.RAWG,
    externalId = id.toString(),
    externalUrl = slug?.let { "https://rawg.io/games/$it" },
    type = MediaType.GAME,
    title = name,
    releaseDate = released.toLocalDateOrNull(),
    releaseYear = released.toLocalDateOrNull()?.year,
    posterUrl = backgroundImage,
    backdropUrl = backgroundImage,
    genres = genres.map { it.name },
)

private fun String?.toLocalDateOrNull(): LocalDate? = this?.takeIf(String::isNotBlank)?.let {
    runCatching { LocalDate.parse(it) }.getOrNull()
}

private val movieGenres = mapOf(
    12 to "Aventura", 14 to "Fantasía", 16 to "Animación", 18 to "Drama", 27 to "Terror",
    28 to "Acción", 35 to "Comedia", 36 to "Historia", 37 to "Western", 53 to "Suspense",
    80 to "Crimen", 99 to "Documental", 878 to "Ciencia ficción", 9648 to "Misterio",
    10402 to "Música", 10749 to "Romance", 10751 to "Familia", 10752 to "Bélica",
)

private val tvGenres = movieGenres + mapOf(
    10759 to "Acción y aventura", 10762 to "Infantil", 10763 to "Noticias",
    10764 to "Reality", 10765 to "Ciencia ficción y fantasía", 10766 to "Telenovela",
    10767 to "Talk show", 10768 to "Guerra y política",
)

fun tmdbGenreId(name: String, type: MediaType): Int? {
    val source = if (type == MediaType.SERIES) tvGenres else movieGenres
    return source.entries.firstOrNull { it.value.equals(name.trim(), ignoreCase = true) }?.key
}
