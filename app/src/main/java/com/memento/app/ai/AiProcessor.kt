package com.memento.app.ai

enum class AiCapability { REWRITE, SUMMARIZE, EXTRACT_THEMES, REFLECTION_QUESTION, CONNECT_REFLECTIONS, COMPARE_REFLECTIONS }

enum class AiAvailability { AVAILABLE, MODEL_DOWNLOAD_REQUIRED, DOWNLOADING, DEVICE_NOT_SUPPORTED, ERROR }

interface AiProcessor {
    suspend fun availability(): AiAvailability
    suspend fun downloadModel(onProgress: (Long) -> Unit = {}): AiAvailability
    suspend fun process(capability: AiCapability, reflection: String, comparison: String? = null): String
}
