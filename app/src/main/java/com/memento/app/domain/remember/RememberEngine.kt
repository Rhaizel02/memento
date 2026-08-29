package com.memento.app.domain.remember

import com.memento.app.domain.model.ReflectionType
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.min

data class RememberCandidate(
    val consumptionId: String,
    val mediaId: String,
    val title: String,
    val completedDate: LocalDate,
    val ratingHalfStars: Int?,
    val isFavorite: Boolean,
    val posterUrl: String?,
    val backdropUrl: String?,
    val reflectionId: String,
    val reflectionType: ReflectionType,
    val reflectionContent: String,
    val reflectionCount: Int,
    val lastShownAt: Instant?,
)

data class ScoredRememberCandidate(val candidate: RememberCandidate, val score: Double)

class RememberEngine {
    fun eligible(candidates: List<RememberCandidate>, now: Instant): List<RememberCandidate> {
        val notRecentlyShown = candidates.filter { candidate ->
            candidate.lastShownAt == null || ChronoUnit.DAYS.between(candidate.lastShownAt, now) >= 30
        }
        return notRecentlyShown.ifEmpty { candidates }
    }

    fun scoreCandidates(candidates: List<RememberCandidate>, today: LocalDate, now: Instant): List<ScoredRememberCandidate> =
        candidates.map { ScoredRememberCandidate(it, score(it, today, now).coerceAtLeast(0.1)) }

    fun select(
        candidates: List<RememberCandidate>,
        today: LocalDate = LocalDate.now(),
        now: Instant = Instant.now(),
        randomValue: Double = stableRandom(candidates, today),
    ): ScoredRememberCandidate? {
        if (candidates.isEmpty()) return null
        val scored = scoreCandidates(eligible(candidates, now), today, now).sortedBy { it.candidate.consumptionId }
        val totalWeight = scored.sumOf { it.score }
        val target = randomValue.coerceIn(0.0, 0.999999999).times(totalWeight)
        var cumulative = 0.0
        return scored.firstOrNull {
            cumulative += it.score
            target < cumulative
        } ?: scored.lastOrNull()
    }

    fun score(candidate: RememberCandidate, today: LocalDate, now: Instant): Double {
        var score = 1.0
        if (candidate.reflectionType == ReflectionType.FINAL_REFLECTION) score += 4.0
        if (candidate.reflectionCount > 1) score += 2.0
        if (candidate.isFavorite) score += 3.0

        val ageDays = ChronoUnit.DAYS.between(candidate.completedDate, today).coerceAtLeast(0)
        score += when {
            ageDays > 730 -> 4.0
            ageDays > 365 -> 3.0
            ageDays > 180 -> 2.0
            ageDays > 90 -> 1.0
            else -> 0.0
        }
        if (anniversaryDistance(candidate.completedDate, today) <= 7) score += 3.0
        score += candidate.lastShownAt?.let { shown ->
            when (ChronoUnit.DAYS.between(shown, now)) {
                in Long.MIN_VALUE..29 -> 0.0
                in 30..89 -> 1.0
                in 90..179 -> 3.0
                else -> 5.0
            }
        } ?: 5.0
        return score
    }

    private fun anniversaryDistance(completed: LocalDate, today: LocalDate): Int {
        val anniversary = runCatching { completed.withYear(today.year) }
            .getOrElse { completed.withDayOfMonth(28).withYear(today.year) }
        val direct = abs(ChronoUnit.DAYS.between(anniversary, today).toInt())
        val previous = abs(ChronoUnit.DAYS.between(anniversary.minusYears(1), today).toInt())
        val next = abs(ChronoUnit.DAYS.between(anniversary.plusYears(1), today).toInt())
        return min(direct, min(previous, next))
    }
}

private fun stableRandom(candidates: List<RememberCandidate>, today: LocalDate): Double {
    val key = candidates.map { it.consumptionId }.sorted().joinToString("|")
    val positiveHash = (31 * key.hashCode() + today.toEpochDay().hashCode()).toUInt().toLong()
    return (positiveHash % 10_000L) / 10_000.0
}
