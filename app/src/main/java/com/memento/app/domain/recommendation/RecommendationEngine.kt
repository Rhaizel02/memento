package com.memento.app.domain.recommendation

import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.RecommendationFeedbackType
import kotlin.math.roundToInt

data class TasteProfile(
    val evidenceCount: Int,
    val genreWeights: Map<String, Double>,
    val creatorWeights: Map<String, Double>,
    val typeWeights: Map<MediaType, Double>,
) {
    val isReady: Boolean get() = evidenceCount >= MINIMUM_EVIDENCE

    companion object { const val MINIMUM_EVIDENCE = 3 }
}

data class RecommendationKey(
    val provider: MetadataProvider,
    val externalId: String,
    val mediaType: MediaType,
)

sealed interface RecommendationReason {
    data class Genre(val name: String) : RecommendationReason
    data class Creator(val name: String) : RecommendationReason
    data class MediaKind(val type: MediaType) : RecommendationReason
}

data class Recommendation(
    val candidate: MetadataSearchResult,
    val affinityScore: Int,
    val reasons: List<RecommendationReason>,
)

object RecommendationEngine {
    fun buildTasteProfile(history: List<MediaDetail>): TasteProfile {
        val genres = mutableMapOf<String, Double>()
        val creators = mutableMapOf<String, Double>()
        val types = mutableMapOf<MediaType, Double>()
        var evidence = 0

        history.forEach { detail ->
            val meaningful = detail.consumptions.filter {
                it.ratingHalfStars != null || it.status == ConsumptionStatus.COMPLETED || it.status == ConsumptionStatus.DROPPED
            }
            if (meaningful.isEmpty() && !detail.media.isFavorite) return@forEach
            evidence += 1

            val consumptionSignal = meaningful.map { consumption ->
                ratingWeight(consumption.ratingHalfStars) + when (consumption.status) {
                    ConsumptionStatus.COMPLETED -> 0.5
                    ConsumptionStatus.DROPPED -> -2.0
                    else -> 0.0
                }
            }.average().takeUnless(Double::isNaN) ?: 0.0
            val signal = consumptionSignal + if (detail.media.isFavorite) 4.0 else 0.0

            types.add(detail.media.type, signal)
            detail.genres.distinctBy(String::lowercase).forEach { genres.add(it, signal) }
            detail.creators.distinctBy(String::lowercase).forEach { creators.add(it, signal) }
        }

        return TasteProfile(evidence, genres, creators, types)
    }

    fun recommend(
        profile: TasteProfile,
        candidates: List<MetadataSearchResult>,
        feedback: Map<RecommendationKey, RecommendationFeedbackType> = emptyMap(),
        limit: Int = 12,
    ): List<Recommendation> {
        if (!profile.isReady) return emptyList()
        return candidates.mapNotNull { candidate ->
            val key = RecommendationKey(candidate.provider, candidate.externalId, candidate.type)
            when (feedback[key]) {
                RecommendationFeedbackType.NOT_INTERESTED,
                RecommendationFeedbackType.ALREADY_KNOWN,
                -> return@mapNotNull null
                else -> Unit
            }

            val genreMatches = candidate.genres.mapNotNull { genre ->
                profile.genreWeights.entryFor(genre)?.takeIf { it.value > 0 }
            }.sortedByDescending { it.value }
            val creatorMatches = candidate.creators.mapNotNull { creator ->
                profile.creatorWeights.entryFor(creator)?.takeIf { it.value > 0 }
            }.sortedByDescending { it.value }
            val typeWeight = profile.typeWeights[candidate.type].orZero().coerceAtLeast(0.0)
            val interestedBoost = if (feedback[key] == RecommendationFeedbackType.INTERESTED) 2.0 else 0.0
            val rawScore = genreMatches.sumOf { it.value } * 1.6 +
                creatorMatches.sumOf { it.value } * 2.0 + typeWeight * 0.7 + interestedBoost
            if (rawScore <= 0.0) return@mapNotNull null

            val reasons = buildList {
                genreMatches.firstOrNull()?.let { add(RecommendationReason.Genre(it.key)) }
                creatorMatches.firstOrNull()?.let { add(RecommendationReason.Creator(it.key)) }
                if (isEmpty() && typeWeight > 0) add(RecommendationReason.MediaKind(candidate.type))
            }
            Recommendation(
                candidate = candidate,
                affinityScore = (50 + rawScore * 5).roundToInt().coerceIn(1, 99),
                reasons = reasons,
            )
        }.sortedWith(
            compareByDescending<Recommendation> { it.affinityScore }
                .thenBy { it.candidate.provider.name }
                .thenBy { it.candidate.externalId },
        ).take(limit)
    }

    fun ratingWeight(halfStars: Int?): Double = when (halfStars) {
        10 -> 5.0
        9 -> 4.0
        8 -> 3.0
        7 -> 2.0
        6, null -> 0.0
        5 -> -1.0
        4 -> -2.0
        3 -> -3.0
        2 -> -4.0
        1 -> -5.0
        else -> 0.0
    }
}

private fun MutableMap<String, Double>.add(key: String, value: Double) {
    val normalizedKey = key.trim()
    if (normalizedKey.isNotEmpty()) this[normalizedKey] = getOrDefault(normalizedKey, 0.0) + value
}

private fun MutableMap<MediaType, Double>.add(key: MediaType, value: Double) {
    this[key] = getOrDefault(key, 0.0) + value
}

private fun Map<String, Double>.entryFor(name: String): Map.Entry<String, Double>? =
    entries.firstOrNull { it.key.equals(name.trim(), ignoreCase = true) }

private fun Double?.orZero(): Double = this ?: 0.0
