package com.memento.app.data

import com.memento.app.data.remote.api.OpenLibraryBookDto
import com.memento.app.data.remote.api.RawgGameDto
import com.memento.app.data.remote.api.RawgGenreDto
import com.memento.app.data.remote.api.TmdbMovieDto
import com.memento.app.data.remote.mapper.toMetadataResult
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetadataMappersTest {
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
