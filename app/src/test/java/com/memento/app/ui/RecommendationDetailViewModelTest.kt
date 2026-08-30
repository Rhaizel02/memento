package com.memento.app.ui

import com.memento.app.FakeMediaRepository
import com.memento.app.FakeMetadataRepository
import com.memento.app.FakeRecommendationRepository
import com.memento.app.MainDispatcherRule
import com.memento.app.domain.model.CompletedMediaInput
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.RecommendationFeedbackType
import com.memento.app.domain.recommendation.Recommendation
import com.memento.app.domain.recommendation.RecommendationCategory
import com.memento.app.domain.recommendation.TasteProfile
import com.memento.app.domain.repository.RecommendationFeed
import com.memento.app.ui.recommendation.RecommendationDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationDetailViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `planned and in progress actions use normal external persistence`() = runTest {
        val plannedMedia = FakeMediaRepository()
        val planned = viewModel(plannedMedia)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { planned.state.collect() }
        planned.load("TMDB", "42", "MOVIE")
        advanceUntilIdle()
        planned.addToPlanned()
        advanceUntilIdle()

        val startedMedia = FakeMediaRepository()
        val started = viewModel(startedMedia)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { started.state.collect() }
        started.load("TMDB", "42", "MOVIE")
        advanceUntilIdle()
        started.startNow()
        advanceUntilIdle()

        assertEquals(ConsumptionStatus.PLANNED, plannedMedia.addedStatus)
        assertEquals(ConsumptionStatus.IN_PROGRESS, startedMedia.addedStatus)
    }

    @Test
    fun `completed action preserves explicit historical metadata`() = runTest {
        val media = FakeMediaRepository()
        val viewModel = viewModel(media)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        viewModel.load("TMDB", "42", "MOVIE")
        advanceUntilIdle()
        val completion = CompletedMediaInput(LocalDate.of(2019, 3, 7), 9, true, "Sigue conmigo")

        viewModel.complete(completion)
        advanceUntilIdle()

        assertEquals(ConsumptionStatus.COMPLETED, media.addedStatus)
        assertEquals(completion, media.completedInput)
    }

    @Test
    fun `double submit persists recommendation only once`() = runTest {
        val media = FakeMediaRepository()
        val viewModel = viewModel(media)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        viewModel.load("TMDB", "42", "MOVIE")
        advanceUntilIdle()

        viewModel.addToPlanned()
        viewModel.addToPlanned()
        advanceUntilIdle()

        assertEquals(1, media.addExternalInvocations)
    }

    @Test
    fun `not interested records feedback and removes candidate`() = runTest {
        val recommendations = FakeRecommendationRepository().apply { feed.value = readyFeed() }
        val viewModel = RecommendationDetailViewModel(recommendations, FakeMetadataRepository(), FakeMediaRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        viewModel.load("TMDB", "42", "MOVIE")
        advanceUntilIdle()

        viewModel.notInterested()
        advanceUntilIdle()

        assertEquals(RecommendationFeedbackType.NOT_INTERESTED, recommendations.feedbackCalls.single().second)
        assertTrue(recommendations.feed.value.recommendations.isEmpty())
        assertTrue(viewModel.state.value.wasDismissed)
    }

    private fun viewModel(media: FakeMediaRepository): RecommendationDetailViewModel {
        val recommendations = FakeRecommendationRepository().apply { feed.value = readyFeed() }
        return RecommendationDetailViewModel(recommendations, FakeMetadataRepository(), media)
    }

    private fun readyFeed() = RecommendationFeed(
        TasteProfile(3, emptyMap(), emptyMap(), emptyMap()),
        listOf(
            Recommendation(
                candidate = MetadataSearchResult(
                    MetadataProvider.TMDB,
                    "42",
                    "https://www.themoviedb.org/movie/42",
                    MediaType.MOVIE,
                    "Recomendada",
                ),
                affinityScore = 75,
                reasons = emptyList(),
                category = RecommendationCategory.GOOD_BET,
            ),
        ),
    )
}
