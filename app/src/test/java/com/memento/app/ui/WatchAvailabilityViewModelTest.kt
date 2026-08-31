package com.memento.app.ui

import com.memento.app.FakeMediaRepository
import com.memento.app.FakeMetadataRepository
import com.memento.app.FakeRecommendationRepository
import com.memento.app.FakeWatchAvailabilityRepository
import com.memento.app.MainDispatcherRule
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaExternalReference
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.recommendation.Recommendation
import com.memento.app.domain.recommendation.RecommendationCategory
import com.memento.app.domain.recommendation.TasteProfile
import com.memento.app.domain.repository.RecommendationFeed
import com.memento.app.domain.watch.WatchAvailability
import com.memento.app.domain.watch.WatchAvailabilityResult
import com.memento.app.domain.watch.WatchProvider
import com.memento.app.ui.detail.MediaDetailViewModel
import com.memento.app.ui.recommendation.RecommendationDetailViewModel
import com.memento.app.ui.watch.WatchAvailabilityUiState
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchAvailabilityViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `recommendation detail loads watch availability independently`() = runTest {
        val recommendations = FakeRecommendationRepository().apply { feed.value = feed(candidate("42", MediaType.MOVIE)) }
        val watch = FakeWatchAvailabilityRepository().apply { result = available() }
        val viewModel = RecommendationDetailViewModel(
            recommendations,
            FakeMetadataRepository(),
            FakeMediaRepository(),
            watch,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

        viewModel.load("TMDB", "42", "MOVIE")
        advanceUntilIdle()

        assertEquals(listOf(MediaType.MOVIE to "42"), watch.requests)
        assertTrue(viewModel.state.value.watchAvailability is WatchAvailabilityUiState.Available)
    }

    @Test
    fun `media detail uses its matching TMDB reference`() = runTest {
        val mediaRepository = FakeMediaRepository().apply {
            detail.value = MediaDetail(
                media = MediaItem(
                    id = "local",
                    type = MediaType.SERIES,
                    title = "Serie",
                    createdAt = Instant.EPOCH,
                    updatedAt = Instant.EPOCH,
                ),
                creators = emptyList(),
                genres = emptyList(),
                consumptions = emptyList(),
                progress = emptyList(),
                reflections = emptyList(),
                externalRefs = listOf(MediaExternalReference(MetadataProvider.TMDB, "77", MediaType.SERIES)),
            )
        }
        val watch = FakeWatchAvailabilityRepository().apply { result = WatchAvailabilityResult.Empty("ES") }
        val viewModel = MediaDetailViewModel(mediaRepository, watch)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

        viewModel.load("local")
        advanceUntilIdle()

        assertEquals(listOf(MediaType.SERIES to "77"), watch.requests)
        assertEquals(WatchAvailabilityUiState.Empty("ES"), viewModel.state.value.watchAvailability)
    }

    @Test
    fun `changing recommendation cancels previous availability request`() = runTest {
        val recommendations = FakeRecommendationRepository().apply {
            feed.value = feed(candidate("1", MediaType.MOVIE), candidate("2", MediaType.SERIES))
        }
        var firstWasCancelled = false
        val watch = FakeWatchAvailabilityRepository().apply {
            handler = { _, id ->
                if (id == "1") {
                    try {
                        delay(10_000)
                        available()
                    } finally {
                        firstWasCancelled = true
                    }
                } else {
                    WatchAvailabilityResult.Empty("ES")
                }
            }
        }
        val viewModel = RecommendationDetailViewModel(
            recommendations,
            FakeMetadataRepository(),
            FakeMediaRepository(),
            watch,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

        viewModel.load("TMDB", "1", "MOVIE")
        runCurrent()
        viewModel.load("TMDB", "2", "SERIES")
        advanceUntilIdle()

        assertTrue(firstWasCancelled)
        assertEquals(listOf(MediaType.MOVIE to "1", MediaType.SERIES to "2"), watch.requests)
        assertEquals(WatchAvailabilityUiState.Empty("ES"), viewModel.state.value.watchAvailability)
    }

    private fun candidate(id: String, type: MediaType) = MetadataSearchResult(
        provider = MetadataProvider.TMDB,
        externalId = id,
        externalUrl = null,
        type = type,
        title = "Historia $id",
    )

    private fun feed(vararg candidates: MetadataSearchResult) = RecommendationFeed(
        profile = TasteProfile(3, emptyMap(), emptyMap(), emptyMap()),
        recommendations = candidates.map { candidate ->
            Recommendation(
                candidate = candidate,
                affinityScore = 80,
                reasons = emptyList(),
                category = RecommendationCategory.GOOD_BET,
            )
        },
    )

    private fun available() = WatchAvailabilityResult.Available(
        WatchAvailability(
            region = "ES",
            link = "https://www.themoviedb.org/watch",
            streaming = listOf(WatchProvider(1, "Provider", null, 1)),
            rent = emptyList(),
            buy = emptyList(),
        ),
    )
}
