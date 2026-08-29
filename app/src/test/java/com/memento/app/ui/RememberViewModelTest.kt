package com.memento.app.ui

import com.memento.app.FakeMediaRepository
import com.memento.app.FakeRememberRepository
import com.memento.app.MainDispatcherRule
import com.memento.app.ai.AiAvailability
import com.memento.app.ai.AiCapability
import com.memento.app.ai.AiProcessor
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.Reflection
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.remember.RememberCandidate
import com.memento.app.domain.repository.AiInsight
import com.memento.app.domain.repository.AiInsightRepository
import com.memento.app.domain.repository.AiInsightSource
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
        assertEquals(listOf("r1"), insightRepository.lastSources)
    }

    @Test
    fun `evolution insight persists both reflections from the same work`() = runTest {
        val original = Reflection("r1", "c1", ReflectionType.FINAL_REFLECTION, "Antes", Instant.EPOCH, Instant.EPOCH)
        val later = Reflection("r2", "c1", ReflectionType.LATER_REFLECTION, "Después", Instant.EPOCH.plusSeconds(100), Instant.EPOCH.plusSeconds(100))
        val consumption = Consumption("c1", "m1", ConsumptionStatus.COMPLETED, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)
        val mediaRepository = FakeMediaRepository().apply {
            detail.value = MediaDetail(
                MediaItem("m1", MediaType.BOOK, "Obra", createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH),
                emptyList(), emptyList(), listOf(consumption), emptyList(), listOf(original, later),
            )
        }
        val rememberRepository = FakeRememberRepository().apply {
            memory.value = RememberCandidate(
                "c1", "m1", "Obra", LocalDate.of(2024, 1, 1), 9, false, null, null,
                "r1", ReflectionType.FINAL_REFLECTION, "Antes", 2, null,
            )
        }
        val insights = FakeAiInsightRepository()
        val viewModel = RememberViewModel(rememberRepository, mediaRepository, FakeAiProcessor("Cambio"), insights)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

        viewModel.load("c1")
        advanceUntilIdle()
        viewModel.runAi(AiCapability.COMPARE_REFLECTIONS)
        advanceUntilIdle()
        viewModel.saveAiInsight()
        advanceUntilIdle()

        assertEquals(listOf("r1", "r2"), insights.lastSources)
    }
}

private class FakeAiProcessor(private val response: String) : AiProcessor {
    override suspend fun availability() = AiAvailability.AVAILABLE
    override suspend fun downloadModel(onProgress: (Long) -> Unit) = AiAvailability.AVAILABLE
    override suspend fun process(capability: AiCapability, reflection: String, comparison: String?) = response
}

private class FakeAiInsightRepository : AiInsightRepository {
    val insights = MutableStateFlow<List<AiInsight>>(emptyList())
    var lastSources: List<String> = emptyList()
    override fun observe(reflectionId: String): Flow<List<AiInsight>> = insights
    override suspend fun save(sourceReflectionIds: List<String>, capability: AiCapability, content: String): String = "ai1".also {
        lastSources = sourceReflectionIds
        insights.value = listOf(AiInsight(it, sourceReflectionIds.map { id -> AiInsightSource(id, "Una obra", Instant.EPOCH) }, capability, content, Instant.EPOCH))
    }
}
