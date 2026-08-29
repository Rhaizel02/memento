package com.memento.app.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.memento.app.ai.AiCapability
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.CreatorRole
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.RecommendationFeedbackType
import com.memento.app.domain.model.ReflectionType
import java.time.Instant
import java.time.LocalDate

@Serializable
data class BackupEnvelope(
    val schemaVersion: Int,
    val exportedAt: String,
    val appVersion: String,
    val data: BackupData,
)

@Serializable
data class BackupData(
    val mediaItems: List<BackupMediaItem> = emptyList(),
    val externalRefs: List<BackupExternalRef> = emptyList(),
    val creators: List<BackupCreator> = emptyList(),
    val mediaCreators: List<BackupMediaCreator> = emptyList(),
    val genres: List<BackupGenre> = emptyList(),
    val mediaGenres: List<BackupMediaGenre> = emptyList(),
    val consumptions: List<BackupConsumption> = emptyList(),
    val progressEntries: List<BackupProgress> = emptyList(),
    val reflections: List<BackupReflection> = emptyList(),
    val rememberExposures: List<BackupRememberExposure> = emptyList(),
    val recommendationFeedback: List<BackupRecommendationFeedback> = emptyList(),
    val aiInsights: List<BackupAiInsight> = emptyList(),
)

@Serializable data class BackupMediaItem(
    val id: String, val type: String, val title: String, val originalTitle: String?, val description: String?,
    val releaseDate: String?, val releaseYear: Int?, val posterUrl: String?, val backdropUrl: String?,
    val isFavorite: Boolean, val isManual: Boolean, val runtimeMinutes: Int?, val pageCount: Int?,
    val seasonCount: Int?, val episodeCount: Int?, val createdAt: String, val updatedAt: String,
)
@Serializable data class BackupExternalRef(val mediaItemId: String, val provider: String, val externalId: String, val mediaType: String, val externalUrl: String?)
@Serializable data class BackupCreator(val id: String, val name: String, val normalizedName: String)
@Serializable data class BackupMediaCreator(val mediaItemId: String, val creatorId: String, val role: String)
@Serializable data class BackupGenre(val id: String, val name: String, val normalizedName: String)
@Serializable data class BackupMediaGenre(val mediaItemId: String, val genreId: String)
@Serializable data class BackupConsumption(
    val id: String, val mediaItemId: String, val status: String, val startedDate: String?, val completedDate: String?,
    val ratingHalfStars: Int?, val createdAt: String, val updatedAt: String,
)
@Serializable data class BackupProgress(
    val id: String, val consumptionId: String, val progressType: String, val currentValue: Double?, val totalValue: Double?,
    val season: Int?, val episode: Int?, val recordedAt: String,
)
@Serializable data class BackupReflection(
    val id: String, val consumptionId: String, val type: String, val content: String, val createdAt: String, val updatedAt: String,
)
@Serializable data class BackupRememberExposure(val id: String, val consumptionId: String, val reflectionId: String?, val shownAt: String)
@Serializable data class BackupRecommendationFeedback(
    val id: String, val provider: String, val externalId: String, val mediaType: String, val feedbackType: String, val createdAt: String,
)
@Serializable data class BackupAiInsight(val id: String, val reflectionId: String, val capability: String, val content: String, val createdAt: String)

data class BackupPreview(val mediaItems: Int, val consumptions: Int, val reflections: Int, val exportedAt: Instant)

object BackupCodec {
    const val SCHEMA_VERSION = 1
    const val MAX_IMPORT_BYTES = 10 * 1024 * 1024
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun encode(envelope: BackupEnvelope): String = json.encodeToString(envelope)

    fun decodeAndValidate(content: String): BackupEnvelope {
        require(content.toByteArray(Charsets.UTF_8).size <= MAX_IMPORT_BYTES) { "El backup supera el límite de 10 MB" }
        val envelope = runCatching { json.decodeFromString<BackupEnvelope>(content) }
            .getOrElse { throw IllegalArgumentException("El archivo no es un backup JSON válido", it) }
        require(envelope.schemaVersion == SCHEMA_VERSION) { "Versión de backup no compatible: ${envelope.schemaVersion}" }
        validate(envelope.data)
        runCatching { Instant.parse(envelope.exportedAt) }
            .getOrElse { throw IllegalArgumentException("Fecha de exportación inválida", it) }
        return envelope
    }

    fun preview(envelope: BackupEnvelope) = BackupPreview(
        mediaItems = envelope.data.mediaItems.size,
        consumptions = envelope.data.consumptions.size,
        reflections = envelope.data.reflections.size,
        exportedAt = Instant.parse(envelope.exportedAt),
    )

    private fun validate(data: BackupData) {
        require(data.mediaItems.map { it.id }.allUnique()) { "Hay obras duplicadas en el backup" }
        require(data.creators.map { it.id }.allUnique()) { "Hay creadores duplicados en el backup" }
        require(data.genres.map { it.id }.allUnique()) { "Hay géneros duplicados en el backup" }
        require(data.consumptions.map { it.id }.allUnique()) { "Hay consumos duplicados en el backup" }
        require(data.progressEntries.map { it.id }.allUnique()) { "Hay progresos duplicados en el backup" }
        require(data.reflections.map { it.id }.allUnique()) { "Hay reflexiones duplicadas en el backup" }
        require(data.mediaItems.all { it.id.isNotBlank() && it.title.isNotBlank() }) { "Todas las obras necesitan id y título" }
        require(data.consumptions.all { it.ratingHalfStars == null || it.ratingHalfStars in 1..10 }) { "Hay una valoración fuera de rango" }

        val mediaIds = data.mediaItems.mapTo(mutableSetOf()) { it.id }
        val creatorIds = data.creators.mapTo(mutableSetOf()) { it.id }
        val genreIds = data.genres.mapTo(mutableSetOf()) { it.id }
        val consumptionIds = data.consumptions.mapTo(mutableSetOf()) { it.id }
        val reflectionIds = data.reflections.mapTo(mutableSetOf()) { it.id }
        require(data.externalRefs.all { it.mediaItemId in mediaIds }) { "Una referencia apunta a una obra inexistente" }
        require(data.mediaCreators.all { it.mediaItemId in mediaIds && it.creatorId in creatorIds }) { "Una relación de creador es inválida" }
        require(data.mediaGenres.all { it.mediaItemId in mediaIds && it.genreId in genreIds }) { "Una relación de género es inválida" }
        require(data.consumptions.all { it.mediaItemId in mediaIds }) { "Un consumo apunta a una obra inexistente" }
        require(data.progressEntries.all { it.consumptionId in consumptionIds }) { "Un progreso apunta a un consumo inexistente" }
        require(data.reflections.all { it.consumptionId in consumptionIds }) { "Una reflexión apunta a un consumo inexistente" }
        require(data.rememberExposures.all {
            it.consumptionId in consumptionIds && (it.reflectionId == null || it.reflectionId in reflectionIds)
        }) { "Un recuerdo contiene referencias inválidas" }
        require(data.aiInsights.all { it.reflectionId in reflectionIds }) { "Un insight de IA apunta a una reflexión inexistente" }

        runCatching {
            data.mediaItems.forEach {
                MediaType.valueOf(it.type)
                it.releaseDate?.let(LocalDate::parse)
                Instant.parse(it.createdAt)
                Instant.parse(it.updatedAt)
            }
            data.externalRefs.forEach { MetadataProvider.valueOf(it.provider); MediaType.valueOf(it.mediaType) }
            data.mediaCreators.forEach { CreatorRole.valueOf(it.role) }
            data.consumptions.forEach {
                ConsumptionStatus.valueOf(it.status)
                it.startedDate?.let(LocalDate::parse)
                it.completedDate?.let(LocalDate::parse)
                Instant.parse(it.createdAt)
                Instant.parse(it.updatedAt)
            }
            data.progressEntries.forEach { ProgressType.valueOf(it.progressType); Instant.parse(it.recordedAt) }
            data.reflections.forEach {
                ReflectionType.valueOf(it.type)
                Instant.parse(it.createdAt)
                Instant.parse(it.updatedAt)
            }
            data.rememberExposures.forEach { Instant.parse(it.shownAt) }
            data.recommendationFeedback.forEach {
                MetadataProvider.valueOf(it.provider)
                MediaType.valueOf(it.mediaType)
                RecommendationFeedbackType.valueOf(it.feedbackType)
                Instant.parse(it.createdAt)
            }
            data.aiInsights.forEach { AiCapability.valueOf(it.capability); Instant.parse(it.createdAt) }
        }.getOrElse { throw IllegalArgumentException("El backup contiene tipos o fechas inválidos", it) }
    }
}

private fun <T> List<T>.allUnique(): Boolean = size == toSet().size
