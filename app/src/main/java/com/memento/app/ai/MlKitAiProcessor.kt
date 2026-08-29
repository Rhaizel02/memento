package com.memento.app.ai

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MlKitAiProcessor @Inject constructor() : AiProcessor {
    private val model by lazy { Generation.getClient() }

    override suspend fun availability(): AiAvailability = runCatching {
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> AiAvailability.AVAILABLE
            FeatureStatus.DOWNLOADABLE -> AiAvailability.MODEL_DOWNLOAD_REQUIRED
            FeatureStatus.DOWNLOADING -> AiAvailability.DOWNLOADING
            else -> AiAvailability.DEVICE_NOT_SUPPORTED
        }
    }.getOrDefault(AiAvailability.ERROR)

    override suspend fun downloadModel(onProgress: (Long) -> Unit): AiAvailability = runCatching {
        model.download().collect { status ->
            if (status is DownloadStatus.DownloadProgress) onProgress(status.totalBytesDownloaded)
        }
        availability()
    }.getOrDefault(AiAvailability.ERROR)

    override suspend fun process(capability: AiCapability, reflection: String, comparison: String?): String {
        require(reflection.isNotBlank())
        check(availability() == AiAvailability.AVAILABLE) { "La IA local no está disponible" }
        val prompt = promptFor(capability, reflection.take(8_000), comparison?.take(8_000))
        return model.generateContent(prompt).candidates.firstOrNull()?.text?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: error("La IA local no devolvió contenido")
    }

    private fun promptFor(capability: AiCapability, reflection: String, comparison: String?): String = when (capability) {
        AiCapability.REWRITE -> """
            Reescribe la siguiente reflexión en español corrigiendo errores y mejorando claridad. Conserva exactamente la opinión, el tono personal y la primera persona. No añadas hechos ni ideas. Devuelve solo la propuesta.

            REFLEXIÓN:
            $reflection
        """.trimIndent()
        AiCapability.SUMMARIZE -> """
            Resume esta reflexión personal en 1 a 3 ideas breves en español. No inventes información ni uses conocimiento externo. Devuelve solo las ideas.

            REFLEXIÓN:
            $reflection
        """.trimIndent()
        AiCapability.EXTRACT_THEMES -> """
            Extrae entre 1 y 5 temas conceptuales presentes de verdad en esta reflexión. Responde en español con una lista separada por comas, sin explicación y sin inventar temas.

            REFLEXIÓN:
            $reflection
        """.trimIndent()
        AiCapability.REFLECTION_QUESTION -> """
            Formula una sola pregunta concreta y estimulante a partir de lo que la persona escribió. Usa únicamente su texto, no conocimiento externo. No preguntes de forma genérica qué le pareció la obra. Devuelve solo la pregunta.

            REFLEXIÓN:
            $reflection
        """.trimIndent()
        AiCapability.CONNECT_REFLECTIONS -> """
            Explica en dos frases una conexión real entre estas dos reflexiones personales. Cita solo ideas presentes en ellas y no inventes contexto de las obras.

            PRIMERA:
            $reflection

            SEGUNDA:
            ${comparison.orEmpty()}
        """.trimIndent()
        AiCapability.COMPARE_REFLECTIONS -> """
            Compara en español cómo cambió la opinión entre estas dos reflexiones. Señala coincidencias y cambios reales, sin inventar causas ni hechos externos.

            ANTES:
            $reflection

            AHORA:
            ${comparison.orEmpty()}
        """.trimIndent()
    }
}
