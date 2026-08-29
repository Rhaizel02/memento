package com.memento.app.data

import com.memento.app.data.remote.api.OpenLibraryBookDto
import com.memento.app.data.remote.api.RawgGameDto
import com.memento.app.data.remote.api.RawgGenreDto
import com.memento.app.data.remote.api.TmdbMovieDto
import com.memento.app.data.remote.api.TmdbMovieDetailsDto
import com.memento.app.data.remote.api.TmdbCreditsDto
import com.memento.app.data.remote.api.TmdbCrewDto
import com.memento.app.data.remote.api.TmdbPersonDto
import com.memento.app.data.remote.api.TmdbGenreDto
import com.memento.app.data.remote.api.TmdbSeriesDetailsDto
import com.memento.app.data.remote.api.OpenLibraryWorkDto
import com.memento.app.data.remote.api.OpenLibraryEditionDto
import com.memento.app.data.remote.api.RawgGameDetailsDto
import com.memento.app.data.remote.api.RawgNamedDto
import com.memento.app.data.remote.mapper.toMetadataResult
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.serialization.json.JsonPrimitive

class MetadataMappersTest {
    @Test
    fun `TMDB movie detail only maps directors as creators`() {
        val result = TmdbMovieDetailsDto(
            id = 42,
            title = "La obra",
            overview = "Completa",
            releaseDate = "2024-05-03",
            genres = listOf(TmdbGenreDto("Drama")),
            runtime = 141,
            credits = TmdbCreditsDto(
                cast = listOf(TmdbPersonDto("Intérprete")),
                crew = listOf(TmdbCrewDto("Directora", "Director")),
            ),
        ).toMetadataResult()

        assertEquals(141, result.runtimeMinutes)
        assertEquals(listOf("Directora"), result.creators)
        assertEquals(listOf("Drama"), result.genres)
    }

    @Test
    fun `TMDB series detail maps structure creators and runtime`() {
        val result = TmdbSeriesDetailsDto(
            id = 84,
            name = "Serie",
            firstAirDate = "2020-01-02",
            genres = listOf(TmdbGenreDto("Drama")),
            createdBy = listOf(TmdbPersonDto("Creadora")),
            credits = TmdbCreditsDto(cast = listOf(TmdbPersonDto("Intérprete"))),
            episodeRunTime = listOf(52),
            seasonCount = 4,
            episodeCount = 32,
        ).toMetadataResult()

        assertEquals(listOf("Creadora"), result.creators)
        assertEquals(52, result.runtimeMinutes)
        assertEquals(4, result.seasonCount)
        assertEquals(32, result.episodeCount)
    }

    @Test
    fun `Open Library detail keeps stable work id and enriches from edition`() {
        val summary = OpenLibraryBookDto("/works/OL123W", "Libro", listOf("Autora")).toMetadataResult()
        val result = OpenLibraryWorkDto(
            title = "Libro completo",
            description = JsonPrimitive("Descripción completa"),
            firstPublishDate = "1984",
            subjects = listOf("Ficción"),
        ).toMetadataResult(summary, OpenLibraryEditionDto("/books/OL9M", pageCount = 320, covers = listOf(99)))

        assertEquals("OL123W", result.externalId)
        assertEquals("Descripción completa", result.description)
        assertEquals(320, result.pageCount)
        assertEquals(listOf("Autora"), result.creators)
    }

    @Test
    fun `RAWG detail adds developer and plain description`() {
        val result = RawgGameDetailsDto(
            id = 7,
            name = "Juego",
            description = "<p>Descripción</p>",
            developers = listOf(RawgNamedDto("Estudio")),
        ).toMetadataResult()

        assertEquals("Descripción", result.description)
        assertEquals(listOf("Estudio"), result.creators)
    }
    @Test
    fun `TMDB movie maps stable id images date and known genres`() {
        val result = TmdbMovieDto(
            id = 42,
            title = "La obra",
            originalTitle = "The Work",
            overview = "Descripción",
            releaseDate = "2024-05-03",
            posterPath = "/poster.jpg",
            backdropPath = "/backdrop.jpg",
            genreIds = listOf(18, 878),
        ).toMetadataResult()

        assertEquals(MetadataProvider.TMDB, result.provider)
        assertEquals(MediaType.MOVIE, result.type)
        assertEquals("42", result.externalId)
        assertEquals(2024, result.releaseYear)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", result.posterUrl)
        assertEquals(listOf("Drama", "Ciencia ficción"), result.genres)
    }

    @Test
    fun `Open Library maps work authors cover and pages`() {
        val result = OpenLibraryBookDto(
            key = "/works/OL123W",
            title = "Libro",
            authors = listOf("Autora"),
            firstPublishYear = 1984,
            coverId = 99,
            pageCount = 320,
        ).toMetadataResult()

        assertEquals("OL123W", result.externalId)
        assertEquals(listOf("Autora"), result.creators)
        assertEquals("https://covers.openlibrary.org/b/id/99-M.jpg", result.posterUrl)
        assertEquals(320, result.pageCount)
    }

    @Test
    fun `RAWG tolerates invalid dates and maps source link`() {
        val result = RawgGameDto(
            id = 7,
            slug = "game-seven",
            name = "Game Seven",
            released = "unknown",
            genres = listOf(RawgGenreDto("RPG")),
        ).toMetadataResult()

        assertNull(result.releaseDate)
        assertEquals("https://rawg.io/games/game-seven", result.externalUrl)
        assertEquals(listOf("RPG"), result.genres)
    }
}
