package com.memento.app.ui

import com.memento.app.FakeMediaRepository
import com.memento.app.FakeRememberRepository
import com.memento.app.MainDispatcherRule
import com.memento.app.ai.AiAvailability
import com.memento.app.ai.AiCapability
import com.memento.app.ai.AiProcessor
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.remember.RememberCandidate
import com.memento.app.domain.repository.AiInsight
import com.memento.app.domain.repository.AiInsightRepository
import com.memento.app.ui.remember.RememberViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RememberViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `local AI result is optional and saved separately from original reflection`() = runTest {
        val rememberRepository = FakeRememberRepository().apply {
            memory.value = RememberCandidate(
                consumptionId = "c1",
                mediaId = "m1",
                title = "Una obra",
                completedDate = LocalDate.of(2024, 1, 1),
                ratingHalfStars = 9,
                isFavorite = true,
                posterUrl = null,
                backdropUrl = null,
                reflectionId = "r1",
                reflectionType = ReflectionType.FINAL_REFLECTION,
                reflectionContent = "Este es mi texto original.",
                reflectionCount = 1,
                lastShownAt = null,
            )
        }
        val insightRepository = FakeAiInsightRepository()
        val viewModel = RememberViewModel(
            rememberRepository,
            FakeMediaRepository(),
            FakeAiProcessor("Una idea breve"),
            insightRepository,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

        viewModel.load("c1")
        advanceUntilIdle()
        viewModel.runAi(AiCapability.SUMMARIZE)
        advanceUntilIdle()

        assertEquals("Una idea breve", viewModel.state.value.aiOutput)
        assertEquals("Este es mi texto original.", viewModel.state.value.memory?.reflectionContent)

        viewModel.saveAiInsight()
        advanceUntilIdle()

        assertNull(viewModel.state.value.aiOutput)
        assertEquals("Una idea breve", insightRepository.insights.value.single().content)
    }
}

private class FakeAiProcessor(private val response: String) : AiProcessor {
    override suspend fun availability() = AiAvailability.AVAILABLE
    override suspend fun downloadModel(onProgress: (Long) -> Unit) = AiAvailability.AVAILABLE
    override suspend fun process(capability: AiCapability, reflection: String, comparison: String?) = response
}

private class FakeAiInsightRepository : AiInsightRepository {
    val insights = MutableStateFlow<List<AiInsight>>(emptyList())
    override fun observe(reflectionId: String): Flow<List<AiInsight>> = insights
    override suspend fun save(reflectionId: String, capability: AiCapability, content: String): String = "ai1".also {
        insights.value = listOf(AiInsight(it, reflectionId, capability, content, Instant.EPOCH))
    }
}
