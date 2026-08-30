package com.memento.app.domain.recommendation

import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.RecommendationFeedbackType
import kotlin.math.ln
import kotlin.math.roundToInt

data class RecommendationAnchor(
    val mediaId: String,
    val title: String,
    val type: MediaType,
    val strength: Double,
    val genres: List<String>,
    val creators: List<String>,
    val personalTags: List<String>,
    val externalKeys: List<RecommendationKey>,
)

data class TasteProfile(
    val evidenceCount: Int,
    val genreWeights: Map<String, Double>,
    val creatorWeights: Map<String, Double>,
    val typeWeights: Map<MediaType, Double>,
    val anchors: List<RecommendationAnchor> = emptyList(),
    val existingExternalKeys: Set<RecommendationKey> = emptySet(),
    val existingTitleKeys: Set<String> = emptySet(),
) {
    val isReady: Boolean get() = evidenceCount >= MINIMUM_EVIDENCE

    companion object { const val MINIMUM_EVIDENCE = 3 }
}

data class RecommendationKey(
    val provider: MetadataProvider,
    val externalId: String,
    val mediaType: MediaType,
)

enum class RecommendationCategory { VERY_AFFINE, GOOD_BET, EXPLORATION }

data class RecommendationEvidence(
    val anchorMediaIds: List<String> = emptyList(),
    val anchorTitles: List<String> = emptyList(),
    val matchedGenres: List<String> = emptyList(),
    val matchedCreators: List<String> = emptyList(),
    val sourceCount: Int = 0,
    val externalRating: Double? = null,
    val externalVoteCount: Int? = null,
    val externalQualityConfidence: Double = 0.0,
)

sealed interface RecommendationReason {
    data class AnchorWorks(val titles: List<String>) : RecommendationReason
    data class Genre(val name: String) : RecommendationReason
    data class Creator(val name: String) : RecommendationReason
    data class MediaKind(val type: MediaType) : RecommendationReason
    data class Exploration(val anchorTitle: String?) : RecommendationReason
}

data class Recommendation(
    val candidate: MetadataSearchResult,
    val affinityScore: Int,
    val reasons: List<RecommendationReason>,
    val category: RecommendationCategory = RecommendationCategory.GOOD_BET,
    val normalizedScore: Double = affinityScore / 100.0,
    val evidence: RecommendationEvidence = RecommendationEvidence(),
)

object RecommendationEngine {
    fun buildTasteProfile(history: List<MediaDetail>): TasteProfile {
        val anchors = history.mapNotNull { detail ->
            val strength = anchorStrength(detail)
            if (strength <= 0.0) return@mapNotNull null
            RecommendationAnchor(
                mediaId = detail.media.id,
                title = detail.media.title,
                type = detail.media.type,
                strength = strength,
                genres = detail.genres.distinctNormalized(),
                creators = detail.creators.distinctNormalized(),
                personalTags = detail.tags.map { it.name }.distinctNormalized(),
                externalKeys = detail.externalRefs.map {
                    RecommendationKey(it.provider, it.externalId, it.mediaType)
                },
            )
        }.sortedByDescending { it.strength }

        val genres = mutableMapOf<String, Double>()
        val creators = mutableMapOf<String, Double>()
        val types = mutableMapOf<MediaType, Double>()
        anchors.forEach { anchor ->
            types[anchor.type] = types.getOrDefault(anchor.type, 0.0) + anchor.strength
            anchor.genres.forEach { genres.addNormalized(it, anchor.strength) }
            anchor.creators.forEach { creators.addNormalized(it, anchor.strength) }
        }
        val existingExternal = history.flatMap { detail ->
            detail.externalRefs.map { RecommendationKey(it.provider, it.externalId, it.mediaType) }
        }.toSet()
        val existingTitles = history.mapTo(mutableSetOf()) { titleKey(it.media.type, it.media.title) }

        return TasteProfile(
            evidenceCount = anchors.size,
            genreWeights = genres,
            creatorWeights = creators,
            typeWeights = types,
            anchors = anchors,
            existingExternalKeys = existingExternal,
            existingTitleKeys = existingTitles,
        )
    }

    fun recommend(
        profile: TasteProfile,
        candidates: List<MetadataSearchResult>,
        feedback: Map<RecommendationKey, RecommendationFeedbackType> = emptyMap(),
        limit: Int = 12,
    ): List<Recommendation> {
        if (!profile.isReady) return emptyList()
        val ranked = candidates.mapNotNull { candidate -> rank(profile, candidate, feedback) }
            .sortedWith(compareByDescending<Recommendation> { it.normalizedScore }.thenBy { it.candidate.title })
        return diversify(ranked, limit)
    }

    private fun rank(
        profile: TasteProfile,
        candidate: MetadataSearchResult,
        feedback: Map<RecommendationKey, RecommendationFeedbackType>,
    ): Recommendation? {
        val key = RecommendationKey(candidate.provider, candidate.externalId, candidate.type)
        if (key in profile.existingExternalKeys || titleKey(candidate.type, candidate.title) in profile.existingTitleKeys) return null
        when (feedback[key]) {
            RecommendationFeedbackType.NOT_INTERESTED,
            RecommendationFeedbackType.ALREADY_KNOWN,
            -> return null
            else -> Unit
        }

        val genreMatches = candidate.genres.mapNotNull { genre -> profile.genreWeights.entryFor(genre) }
            .distinctBy { it.key.normalized() }
            .sortedByDescending { it.value }
        val creatorMatches = candidate.creators.mapNotNull { creator -> profile.creatorWeights.entryFor(creator) }
            .distinctBy { it.key.normalized() }
            .sortedByDescending { it.value }
        val sourceAnchors = candidate.sourceAnchorMediaIds.distinct().mapNotNull { id ->
            profile.anchors.firstOrNull { it.mediaId == id }
        }
        val genreMax = profile.genreWeights.values.maxOrNull()?.coerceAtLeast(0.01) ?: 1.0
        val creatorMax = profile.creatorWeights.values.maxOrNull()?.coerceAtLeast(0.01) ?: 1.0
        val typeMax = profile.typeWeights.values.maxOrNull()?.coerceAtLeast(0.01) ?: 1.0
        val hasSameMediaEvidence = profile.typeWeights[candidate.type].orZero() > 0.0
        if (!hasSameMediaEvidence && sourceAnchors.isEmpty() && creatorMatches.isEmpty() && genreMatches.size < 2) return null
        val normalizedGenres = genreMatches.map { (it.value / genreMax).coerceIn(0.0, 1.0) }
        val genreScore = when (normalizedGenres.size) {
            0 -> 0.0
            1 -> normalizedGenres.first() * 0.12
            else -> 0.12 + normalizedGenres.take(3).average() * 0.13
        }
        val creatorScore = creatorMatches.firstOrNull()?.let { (it.value / creatorMax).coerceIn(0.0, 1.0) * 0.20 } ?: 0.0
        val consensusScore = when (sourceAnchors.size) {
            0 -> 0.0
            1 -> sourceAnchors.first().strength.coerceAtMost(1.0) * 0.18
            2 -> sourceAnchors.map { it.strength.coerceAtMost(1.0) }.average() * 0.32
            else -> sourceAnchors.map { it.strength.coerceAtMost(1.0) }.average() * 0.42
        }
        val typeAffinity = (profile.typeWeights[candidate.type].orZero() / typeMax).coerceIn(0.0, 1.0) * 0.08
        val qualityConfidence = externalQualityConfidence(candidate.externalRating, candidate.externalVoteCount)
        val qualityScore = qualityConfidence * 0.18
        val crossMediaSignal = if (!hasSameMediaEvidence && (creatorMatches.isNotEmpty() || genreMatches.size >= 2)) 0.03 else 0.0
        val score = (genreScore + creatorScore + consensusScore + typeAffinity + qualityScore + crossMediaSignal)
            .coerceIn(0.0, 1.0)
        if (score < 0.13) return null

        val strongEvidence = sourceAnchors.size >= 2 || (creatorMatches.isNotEmpty() && genreMatches.size >= 2)
        val category = when {
            score >= 0.68 && strongEvidence -> RecommendationCategory.VERY_AFFINE
            score >= 0.36 -> RecommendationCategory.GOOD_BET
            else -> RecommendationCategory.EXPLORATION
        }
        val anchorTitles = sourceAnchors.map { it.title }.ifEmpty { candidate.sourceAnchorTitles }.distinct().take(3)
        val evidence = RecommendationEvidence(
            anchorMediaIds = sourceAnchors.map { it.mediaId },
            anchorTitles = anchorTitles,
            matchedGenres = genreMatches.map { it.key }.take(3),
            matchedCreators = creatorMatches.map { it.key }.take(2),
            sourceCount = sourceAnchors.size,
            externalRating = candidate.externalRating,
            externalVoteCount = candidate.externalVoteCount,
            externalQualityConfidence = qualityConfidence,
        )
        val reasons = buildList {
            if (anchorTitles.isNotEmpty()) add(RecommendationReason.AnchorWorks(anchorTitles))
            creatorMatches.firstOrNull()?.let { add(RecommendationReason.Creator(it.key)) }
            genreMatches.firstOrNull()?.let { add(RecommendationReason.Genre(it.key)) }
            if (category == RecommendationCategory.EXPLORATION) add(RecommendationReason.Exploration(anchorTitles.firstOrNull()))
            if (isEmpty()) add(RecommendationReason.MediaKind(candidate.type))
        }
        return Recommendation(candidate, (score * 100).roundToInt(), reasons, category, score, evidence)
    }

    fun diversify(ranked: List<Recommendation>, limit: Int): List<Recommendation> {
        if (ranked.size <= 1 || limit <= 1) return ranked.take(limit)
        val remaining = ranked.toMutableList()
        val selected = mutableListOf<Recommendation>()
        while (remaining.isNotEmpty() && selected.size < limit) {
            val explorationTurn = selected.size % 5 == 4
            val pool = if (explorationTurn) remaining.filter { it.category == RecommendationCategory.EXPLORATION }.ifEmpty { remaining }
                else remaining
            val next = pool.maxByOrNull { candidate ->
                candidate.normalizedScore - selected.maxOfOrNull { similarity(candidate, it) }.orZero() * 0.22
            } ?: break
            selected += next
            remaining.remove(next)
        }
        return selected
    }

    fun externalQualityConfidence(rating: Double?, voteCount: Int?): Double {
        if (rating == null || voteCount == null || voteCount <= 0) return 0.0
        val votes = voteCount.toDouble()
        val priorRating = 6.5
        val priorVotes = 250.0
        val bayesianRating = (votes / (votes + priorVotes)) * rating.coerceIn(0.0, 10.0) +
            (priorVotes / (votes + priorVotes)) * priorRating
        val voteConfidence = (ln(votes + 1.0) / ln(20_001.0)).coerceIn(0.0, 1.0)
        return (((bayesianRating - 5.0) / 5.0).coerceIn(0.0, 1.0) * voteConfidence).coerceIn(0.0, 1.0)
    }

    fun anchorStrength(detail: MediaDetail): Double {
        val ratings = detail.consumptions.mapNotNull { it.ratingHalfStars }
        val ratingSignal = when (ratings.maxOrNull()) {
            10 -> 1.0
            9 -> 0.88
            8 -> 0.68
            7 -> 0.42
            6 -> 0.16
            5 -> 0.05
            else -> 0.0
        }
        val completedSignal = if (detail.consumptions.any { it.status == ConsumptionStatus.COMPLETED }) 0.10 else 0.0
        val favoriteSignal = if (detail.media.isFavorite) 0.25 else 0.0
        val reconsumptionSignal = if (detail.consumptions.count { it.status == ConsumptionStatus.COMPLETED } > 1) 0.12 else 0.0
        val baseSignal = ratingSignal + completedSignal + favoriteSignal + reconsumptionSignal
        val tagSignal = if (baseSignal > 0.0) (detail.tags.size * 0.04).coerceAtMost(0.16) else 0.0
        val droppedPenalty = if (detail.consumptions.isNotEmpty() && detail.consumptions.all { it.status == ConsumptionStatus.DROPPED }) 0.4 else 0.0
        return (ratingSignal + completedSignal + favoriteSignal + tagSignal + reconsumptionSignal - droppedPenalty)
            .coerceIn(0.0, 1.35)
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

    private fun similarity(first: Recommendation, second: Recommendation): Double {
        val firstGenres = first.candidate.genres.mapTo(mutableSetOf()) { it.normalized() }
        val secondGenres = second.candidate.genres.mapTo(mutableSetOf()) { it.normalized() }
        val genreUnion = firstGenres union secondGenres
        val genreSimilarity = if (genreUnion.isEmpty()) 0.0 else (firstGenres intersect secondGenres).size.toDouble() / genreUnion.size
        val firstCreators = first.candidate.creators.mapTo(mutableSetOf()) { it.normalized() }
        val secondCreators = second.candidate.creators.mapTo(mutableSetOf()) { it.normalized() }
        val creatorUnion = firstCreators union secondCreators
        val creatorSimilarity = if (creatorUnion.isEmpty()) 0.0 else (firstCreators intersect secondCreators).size.toDouble() / creatorUnion.size
        return genreSimilarity * 0.75 + creatorSimilarity * 0.25
    }
}

private fun MutableMap<String, Double>.addNormalized(key: String, value: Double) {
    val existing = entries.firstOrNull { it.key.equals(key.trim(), ignoreCase = true) }?.key ?: key.trim()
    if (existing.isNotEmpty()) this[existing] = getOrDefault(existing, 0.0) + value
}

private fun Map<String, Double>.entryFor(name: String): Map.Entry<String, Double>? =
    entries.firstOrNull { it.key.equals(name.trim(), ignoreCase = true) }

private fun List<String>.distinctNormalized(): List<String> =
    map(String::trim).filter(String::isNotEmpty).distinctBy { it.normalized() }

private fun String.normalized(): String = trim().lowercase()
private fun titleKey(type: MediaType, title: String): String = "${type.name}:${title.normalized()}"
private fun Double?.orZero(): Double = this ?: 0.0
