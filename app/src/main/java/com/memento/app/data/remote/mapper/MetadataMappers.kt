package com.memento.app.data.remote.mapper

import com.memento.app.data.remote.api.OpenLibraryBookDto
import com.memento.app.data.remote.api.OpenLibraryEditionDto
import com.memento.app.data.remote.api.OpenLibraryWorkDto
import com.memento.app.data.remote.api.RawgGameDto
import com.memento.app.data.remote.api.RawgGameDetailsDto
import com.memento.app.data.remote.api.TmdbMovieDto
import com.memento.app.data.remote.api.TmdbMovieDetailsDto
import com.memento.app.data.remote.api.TmdbSeriesDto
import com.memento.app.data.remote.api.TmdbSeriesDetailsDto
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import java.time.LocalDate
import java.text.Normalizer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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
    externalRating = voteAverage,
    externalVoteCount = voteCount,
    popularity = popularity,
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
    externalRating = voteAverage,
    externalVoteCount = voteCount,
    popularity = popularity,
)

fun OpenLibraryBookDto.toMetadataResult(): MetadataSearchResult {
    val workId = key.substringAfterLast('/')
    return MetadataSearchResult(
    provider = MetadataProvider.OPEN_LIBRARY,
    externalId = workId,
    externalUrl = "https://openlibrary.org/works/$workId",
    type = MediaType.BOOK,
    title = title,
    releaseYear = firstPublishYear,
    posterUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" },
    creators = authors,
    genres = subject.take(5),
    pageCount = pageCount,
    externalRating = ratingsAverage?.times(2.0),
    externalVoteCount = ratingsCount ?: wantToReadCount,
)
}

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
    creators = developers.map { it.name },
    externalTags = tags.map { it.name },
    externalRating = metacritic?.div(10.0) ?: rating?.times(2.0),
    externalVoteCount = ratingsCount,
)

fun TmdbMovieDetailsDto.toMetadataResult() = MetadataSearchResult(
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
    creators = credits?.crew.orEmpty()
        .filter { it.job == "Director" }
        .map { it.name }
        .distinct(),
    genres = genres.map { it.name },
    runtimeMinutes = runtime,
    externalRating = voteAverage,
    externalVoteCount = voteCount,
    popularity = popularity,
)

fun TmdbSeriesDetailsDto.toMetadataResult() = MetadataSearchResult(
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
    creators = createdBy.map { it.name }.distinct(),
    genres = genres.map { it.name },
    runtimeMinutes = episodeRunTime.firstOrNull(),
    seasonCount = seasonCount,
    episodeCount = episodeCount,
    externalRating = voteAverage,
    externalVoteCount = voteCount,
    popularity = popularity,
)

fun OpenLibraryWorkDto.toMetadataResult(
    summary: MetadataSearchResult,
    edition: OpenLibraryEditionDto?,
) = summary.copy(
    title = title,
    description = description.toPlainText() ?: summary.description,
    releaseYear = firstPublishDate?.take(4)?.toIntOrNull() ?: summary.releaseYear,
    posterUrl = (edition?.covers?.firstOrNull() ?: covers.firstOrNull())
        ?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" } ?: summary.posterUrl,
    genres = subjects.take(8).ifEmpty { summary.genres },
    pageCount = edition?.pageCount ?: summary.pageCount,
)

fun RawgGameDetailsDto.toMetadataResult() = MetadataSearchResult(
    provider = MetadataProvider.RAWG,
    externalId = id.toString(),
    externalUrl = slug?.let { "https://rawg.io/games/$it" },
    type = MediaType.GAME,
    title = name,
    description = descriptionRaw?.takeIf(String::isNotBlank) ?: description?.stripHtml()?.takeIf(String::isNotBlank),
    releaseDate = released.toLocalDateOrNull(),
    releaseYear = released.toLocalDateOrNull()?.year,
    posterUrl = backgroundImage,
    backdropUrl = backgroundImage,
    creators = developers.map { it.name },
    genres = genres.map { it.name },
    externalTags = tags.map { it.name },
    externalRating = metacritic?.div(10.0) ?: rating?.times(2.0),
    externalVoteCount = ratingsCount,
)

private fun kotlinx.serialization.json.JsonElement?.toPlainText(): String? = when (this) {
    is JsonPrimitive -> contentOrNull
    is JsonObject -> (get("value") as? JsonPrimitive)?.contentOrNull
    else -> null
}?.takeIf(String::isNotBlank)

private fun String.stripHtml(): String = replace("<[^>]+>".toRegex(), " ").replace("\\s+".toRegex(), " ").trim()

private fun String?.toLocalDateOrNull(): LocalDate? = this?.takeIf(String::isNotBlank)?.let {
    runCatching { LocalDate.parse(it) }.getOrNull()
}

private val movieGenres = mapOf(
    12 to "Aventura", 14 to "Fantasía", 16 to "Animación", 18 to "Drama", 27 to "Terror",
    28 to "Acción", 35 to "Comedia", 36 to "Historia", 37 to "Western", 53 to "Suspense",
    80 to "Crimen", 99 to "Documental", 878 to "Ciencia ficción", 9648 to "Misterio",
    10402 to "Música", 10749 to "Romance", 10751 to "Familia", 10752 to "Bélica",
)

private val tvGenres = movieGenres.filterKeys { it in setOf(16, 18, 35, 37, 80, 99, 9648, 10751) } + mapOf(
    10759 to "Acción y aventura", 10762 to "Infantil", 10763 to "Noticias",
    10764 to "Reality", 10765 to "Ciencia ficción y fantasía", 10766 to "Telenovela",
    10767 to "Talk show", 10768 to "Guerra y política",
)

fun tmdbGenreId(name: String, type: MediaType): Int? {
    val source = if (type == MediaType.SERIES) tvGenres else movieGenres
    val key = normalizedGenre(name)
    val exact = source.entries.firstOrNull { normalizedGenre(it.value) == key }?.key
    return exact ?: if (type == MediaType.SERIES) when (key) {
        "ciencia ficcion", "fantasia", "science fiction", "sci fi", "sci fi fantasy", "sci fi and fantasy",
        "science fiction and fantasy", "fantasy" -> 10765
        "accion", "aventura", "action", "adventure", "action adventure", "action and adventure" -> 10759
        "war politics", "war and politics" -> 10768
        "kids" -> 10762
        "news" -> 10763
        "soap" -> 10766
        "talk" -> 10767
        else -> null
    } else movieGenreAliases[key]
}

private val movieGenreAliases = mapOf(
    "science fiction" to 878, "sci fi" to 878, "thriller" to 53, "action" to 28, "adventure" to 12,
    "fantasy" to 14, "animation" to 16, "horror" to 27, "comedy" to 35, "history" to 36,
    "crime" to 80, "documentary" to 99, "mystery" to 9648, "music" to 10402, "family" to 10751, "war" to 10752,
)

private val rawgGenreSlugs = listOf(
    "action", "indie", "adventure", "role-playing-games-rpg", "strategy", "shooter", "casual",
    "simulation", "puzzle", "arcade", "platformer", "massively-multiplayer", "racing", "sports",
    "fighting", "family", "board-games", "card", "educational",
).associateBy(::normalizedGenre)

private val rawgGenreAliases = mapOf(
    "accion" to "action", "aventura" to "adventure", "rol" to "role-playing-games-rpg",
    "role playing" to "role-playing-games-rpg", "rpg" to "role-playing-games-rpg",
    "estrategia" to "strategy", "simulacion" to "simulation", "plataformas" to "platformer",
    "carreras" to "racing", "deportes" to "sports", "lucha" to "fighting",
    "familia" to "family", "juegos de mesa" to "board-games", "cartas" to "card",
    "educativo" to "educational", "puzle" to "puzzle",
)

fun rawgGenreSlug(name: String): String? {
    val key = normalizedGenre(name)
    return rawgGenreSlugs[key] ?: rawgGenreAliases[key]
}

private fun normalizedGenre(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
    .replace("\\p{Mn}+".toRegex(), "").replace("[^a-z0-9]+".toRegex(), " ").trim()
