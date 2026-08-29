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
import com.memento.app.domain.insight.ReflectionConnectionSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RememberUiState(
    val memory: RememberCandidate? = null,
    val detail: MediaDetail? = null,
    val isLoading: Boolean = true,
    val saved: Boolean = false,
    val isSavingThought: Boolean = false,
    val thoughtError: String? = null,
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
    val sourceReflectionIds: List<String> = emptyList(),
)

private data class ThoughtState(
    val saved: Boolean = false,
    val isWorking: Boolean = false,
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
    private val thoughtState = MutableStateFlow(ThoughtState())
    private val aiPanel = MutableStateFlow(AiPanelState())
    private val memory = consumptionId.flatMapLatest { id -> id?.let(rememberRepository::observeRemember) ?: flowOf(null) }
    private val detail = memory.flatMapLatest { candidate ->
        candidate?.mediaId?.let(mediaRepository::observeMediaDetail) ?: flowOf(null)
    }
    private val insights = memory.flatMapLatest { candidate ->
        candidate?.reflectionId?.let(aiInsightRepository::observe) ?: flowOf(emptyList())
    }
    val state = combine(memory, detail, thoughtState, aiPanel, insights) { candidate, mediaDetail, thought, ai, storedInsights ->
        RememberUiState(
            memory = candidate,
            detail = mediaDetail,
            isLoading = false,
            saved = thought.saved,
            isSavingThought = thought.isWorking,
            thoughtError = thought.error,
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
        if (content.isBlank() || thoughtState.value.isWorking || thoughtState.value.saved) return
        thoughtState.value = ThoughtState(isWorking = true)
        viewModelScope.launch {
            runCatching { mediaRepository.saveReflection(id, ReflectionType.LATER_REFLECTION, content) }
                .onSuccess { thoughtState.value = ThoughtState(saved = true) }
                .onFailure {
                    thoughtState.value = ThoughtState(error = "No se pudo guardar la reflexión. Tu texto sigue aquí.")
                }
        }
    }

    fun runAi(capability: AiCapability) {
        if (aiPanel.value.isWorking) return
        val candidate = state.value.memory ?: return
        val laterReflection = state.value.detail?.reflections
            ?.filter { it.type == ReflectionType.LATER_REFLECTION }
            ?.maxByOrNull { it.createdAt }
        if (capability == AiCapability.COMPARE_REFLECTIONS && laterReflection == null) return
        aiPanel.value = aiPanel.value.copy(isWorking = true, capability = capability, output = null, error = null)
        viewModelScope.launch {
            val connection = if (capability == AiCapability.CONNECT_REFLECTIONS) {
                ReflectionConnectionSelector.select(
                    candidate.mediaId,
                    candidate.reflectionContent,
                    mediaRepository.observeAllDetails().first(),
                )
            } else null
            if (capability == AiCapability.CONNECT_REFLECTIONS && connection == null) {
                aiPanel.value = aiPanel.value.copy(isWorking = false, error = "No hay otra obra con una reflexión relacionada todavía")
                return@launch
            }
            val comparison = when (capability) {
                AiCapability.COMPARE_REFLECTIONS -> laterReflection?.content
                AiCapability.CONNECT_REFLECTIONS -> connection?.reflection?.content
                else -> null
            }
            val sources = when (capability) {
                AiCapability.COMPARE_REFLECTIONS -> listOf(candidate.reflectionId, requireNotNull(laterReflection).id)
                AiCapability.CONNECT_REFLECTIONS -> listOf(candidate.reflectionId, requireNotNull(connection).reflection.id)
                else -> listOf(candidate.reflectionId)
            }
            runCatching { aiProcessor.process(capability, candidate.reflectionContent, comparison) }
                .onSuccess { aiPanel.value = aiPanel.value.copy(isWorking = false, output = it, sourceReflectionIds = sources) }
                .onFailure {
                    aiPanel.value = aiPanel.value.copy(
                        isWorking = false,
                        error = "No se pudo completar el procesamiento local. Inténtalo de nuevo.",
                    )
                }
        }
    }

    fun saveAiInsight() {
        if (aiPanel.value.isWorking) return
        val memory = state.value.memory ?: return
        val capability = aiPanel.value.capability ?: return
        val output = aiPanel.value.output ?: return
        aiPanel.value = aiPanel.value.copy(isWorking = true, error = null)
        viewModelScope.launch {
            runCatching {
                aiInsightRepository.save(aiPanel.value.sourceReflectionIds.ifEmpty { listOf(memory.reflectionId) }, capability, output)
            }.onSuccess {
                aiPanel.value = aiPanel.value.copy(isWorking = false, output = null, capability = null)
            }.onFailure {
                aiPanel.value = aiPanel.value.copy(isWorking = false, error = "No se pudo guardar el insight.")
            }
        }
    }

    fun discardAiResult() {
        aiPanel.value = aiPanel.value.copy(output = null, capability = null, error = null, sourceReflectionIds = emptyList())
    }
}
