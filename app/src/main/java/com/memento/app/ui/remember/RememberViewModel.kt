package com.memento.app.ui.remember

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.remember.RememberCandidate
import com.memento.app.domain.repository.MediaRepository
import com.memento.app.domain.repository.RememberRepository
import com.memento.app.ai.AiAvailability
import com.memento.app.ai.AiCapability
import com.memento.app.ai.AiProcessor
import com.memento.app.domain.repository.AiInsight
import com.memento.app.domain.repository.AiInsightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RememberUiState(
    val memory: RememberCandidate? = null,
    val detail: MediaDetail? = null,
    val isLoading: Boolean = true,
    val saved: Boolean = false,
    val aiAvailability: AiAvailability? = null,
    val isAiWorking: Boolean = false,
    val aiCapability: AiCapability? = null,
    val aiOutput: String? = null,
    val aiError: String? = null,
    val insights: List<AiInsight> = emptyList(),
)

private data class AiPanelState(
    val availability: AiAvailability? = null,
    val isWorking: Boolean = false,
    val capability: AiCapability? = null,
    val output: String? = null,
    val error: String? = null,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RememberViewModel @Inject constructor(
    private val rememberRepository: RememberRepository,
    private val mediaRepository: MediaRepository,
    private val aiProcessor: AiProcessor,
    private val aiInsightRepository: AiInsightRepository,
) : ViewModel() {
    private val consumptionId = MutableStateFlow<String?>(null)
    private val saved = MutableStateFlow(false)
    private val aiPanel = MutableStateFlow(AiPanelState())
    private val memory = consumptionId.flatMapLatest { id -> id?.let(rememberRepository::observeRemember) ?: flowOf(null) }
    private val detail = memory.flatMapLatest { candidate ->
        candidate?.mediaId?.let(mediaRepository::observeMediaDetail) ?: flowOf(null)
    }
    private val insights = memory.flatMapLatest { candidate ->
        candidate?.reflectionId?.let(aiInsightRepository::observe) ?: flowOf(emptyList())
    }
    val state = combine(memory, detail, saved, aiPanel, insights) { candidate, mediaDetail, wasSaved, ai, storedInsights ->
        RememberUiState(
            memory = candidate,
            detail = mediaDetail,
            isLoading = false,
            saved = wasSaved,
            aiAvailability = ai.availability,
            isAiWorking = ai.isWorking,
            aiCapability = ai.capability,
            aiOutput = ai.output,
            aiError = ai.error,
            insights = storedInsights,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RememberUiState())

    init {
        viewModelScope.launch { aiPanel.value = aiPanel.value.copy(availability = aiProcessor.availability()) }
    }

    fun load(id: String) {
        if (consumptionId.value == id) return
        consumptionId.value = id
        viewModelScope.launch { rememberRepository.recordExposure(id) }
    }

    fun saveCurrentThought(content: String) {
        val id = consumptionId.value ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            mediaRepository.saveReflection(id, ReflectionType.LATER_REFLECTION, content)
            saved.value = true
        }
    }

    fun runAi(capability: AiCapability) {
        val candidate = state.value.memory ?: return
        val comparison = state.value.detail?.reflections
            ?.filter { it.type == ReflectionType.LATER_REFLECTION }
            ?.maxByOrNull { it.createdAt }
            ?.content
        if ((capability == AiCapability.COMPARE_REFLECTIONS || capability == AiCapability.CONNECT_REFLECTIONS) && comparison == null) return
        viewModelScope.launch {
            aiPanel.value = aiPanel.value.copy(isWorking = true, capability = capability, output = null, error = null)
            runCatching { aiProcessor.process(capability, candidate.reflectionContent, comparison) }
                .onSuccess { aiPanel.value = aiPanel.value.copy(isWorking = false, output = it) }
                .onFailure { aiPanel.value = aiPanel.value.copy(isWorking = false, error = it.message) }
        }
    }

    fun saveAiInsight() {
        val memory = state.value.memory ?: return
        val capability = aiPanel.value.capability ?: return
        val output = aiPanel.value.output ?: return
        viewModelScope.launch {
            aiInsightRepository.save(memory.reflectionId, capability, output)
            aiPanel.value = aiPanel.value.copy(output = null, capability = null)
        }
    }

    fun discardAiResult() {
        aiPanel.value = aiPanel.value.copy(output = null, capability = null, error = null)
    }
}
