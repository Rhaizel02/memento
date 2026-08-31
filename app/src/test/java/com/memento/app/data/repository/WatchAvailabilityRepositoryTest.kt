package com.memento.app.data.repository

import com.memento.app.data.remote.api.TmdbApi
import com.memento.app.data.remote.api.TmdbWatchProviderDto
import com.memento.app.data.remote.api.TmdbWatchProvidersResponse
import com.memento.app.data.remote.api.TmdbWatchRegionDto
import com.memento.app.data.remote.mapper.toWatchAvailability
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.watch.LocaleWatchRegionResolver
import com.memento.app.domain.watch.WatchAvailabilityResult
import com.memento.app.domain.watch.WatchRegionResolver
import java.lang.reflect.Proxy
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.GET

class WatchAvailabilityRepositoryTest {
    @Test
    fun `region resolver uses the device country`() {
        assertEquals("US", LocaleWatchRegionResolver.resolve(Locale.forLanguageTag("en-US")))
        assertEquals("MX", LocaleWatchRegionResolver.resolve(Locale.forLanguageTag("es-MX")))
    }

    @Test
    fun `region resolver falls back to ES when country is unavailable`() {
        assertEquals("ES", LocaleWatchRegionResolver.resolve(Locale.forLanguageTag("es")))
        assertEquals("ES", LocaleWatchRegionResolver.resolve(Locale.ROOT))
        assertEquals("ES", LocaleWatchRegionResolver.resolve(null))
    }

    @Test
    fun `mapper separates categories and sorts and deduplicates each provider list`() {
        val response = TmdbWatchProvidersResponse(
            mapOf(
                "ES" to TmdbWatchRegionDto(
                    link = "https://www.themoviedb.org/movie/42/watch",
                    flatrate = listOf(
                        provider(10, "Later", priority = 8),
                        provider(20, "First", priority = 1),
                        provider(20, "Duplicate", priority = 9),
                    ),
                    rent = listOf(provider(30, "Rent", priority = 2)),
                    buy = listOf(provider(40, "Buy", priority = null)),
                ),
            ),
        )

        val availability = response.toWatchAvailability("es")

        assertEquals(listOf(20, 10), availability?.streaming?.map { it.id })
        assertEquals(listOf(30), availability?.rent?.map { it.id })
        assertEquals(listOf(40), availability?.buy?.map { it.id })
        assertEquals("/logo.png", availability?.streaming?.first()?.logoPath)
    }

    @Test
    fun `movie and series route to their exact TMDB watch endpoints`() = runTest {
        val recorder = ApiRecorder(TmdbWatchProvidersResponse())

        fetchWatchProviders(recorder.api, MediaType.MOVIE, "10", "key")
        fetchWatchProviders(recorder.api, MediaType.SERIES, "20", "key")

        assertEquals(listOf("movieWatchProviders", "seriesWatchProviders"), recorder.calls)
        assertEquals(
            "3/movie/{id}/watch/providers",
            TmdbApi::class.java.getMethod("movieWatchProviders", String::class.java, String::class.java, kotlin.coroutines.Continuation::class.java)
                .getAnnotation(GET::class.java)?.value,
        )
        assertEquals(
            "3/tv/{id}/watch/providers",
            TmdbApi::class.java.getMethod("seriesWatchProviders", String::class.java, String::class.java, kotlin.coroutines.Continuation::class.java)
                .getAnnotation(GET::class.java)?.value,
        )
    }

    @Test
    fun `book and game never request TMDB availability`() = runTest {
        val recorder = ApiRecorder(TmdbWatchProvidersResponse())
        val repository = TmdbWatchAvailabilityRepository(recorder.api, WatchRegionResolver { "ES" }, "key")

        assertEquals(WatchAvailabilityResult.Unsupported, repository.get(MediaType.BOOK, "10"))
        assertEquals(WatchAvailabilityResult.Unsupported, repository.get(MediaType.GAME, "20"))
        assertTrue(recorder.calls.isEmpty())
    }

    @Test
    fun `missing regional result produces clean empty state and is cached`() = runTest {
        val recorder = ApiRecorder(
            TmdbWatchProvidersResponse(mapOf("US" to TmdbWatchRegionDto(flatrate = listOf(provider(1, "Provider", 1))))),
        )
        val repository = TmdbWatchAvailabilityRepository(recorder.api, WatchRegionResolver { "ES" }, "key")

        val first = repository.get(MediaType.MOVIE, "42")
        val second = repository.get(MediaType.MOVIE, "42")

        assertTrue(first is WatchAvailabilityResult.Empty)
        assertEquals("ES", (first as WatchAvailabilityResult.Empty).region)
        assertEquals(first, second)
        assertEquals(listOf("movieWatchProviders"), recorder.calls)
        assertNull(recorder.lastFailure)
    }

    private fun provider(id: Int, name: String, priority: Int?) = TmdbWatchProviderDto(
        providerId = id,
        providerName = name,
        logoPath = "/logo.png",
        displayPriority = priority,
    )

    private class ApiRecorder(private val response: TmdbWatchProvidersResponse) {
        val calls = mutableListOf<String>()
        var lastFailure: Throwable? = null
        val api: TmdbApi = Proxy.newProxyInstance(
            TmdbApi::class.java.classLoader,
            arrayOf(TmdbApi::class.java),
        ) { _, method, _ ->
            try {
                calls += method.name
                response
            } catch (error: Throwable) {
                lastFailure = error
                throw error
            }
        } as TmdbApi
    }
}
