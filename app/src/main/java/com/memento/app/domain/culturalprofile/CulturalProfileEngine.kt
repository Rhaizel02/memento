package com.memento.app.domain.culturalprofile

import com.memento.app.domain.model.MediaType
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

/** Deterministic thresholds that prevent one-off ratings from becoming cultural conclusions. */
object CulturalProfileThresholds {
    const val MIN_RATED_SAMPLE = 3
    const val MIN_TEMPORAL_SAMPLE = 3
    const val MIN_ACTIVE_MONTH_SAMPLE = 3
    const val MIN_PRESENCE_COUNT = 2
    const val MIN_PERIOD_WORKS = 4
    const val MIN_PERIOD_MATCHES = 3
    const val MIN_PERIOD_SHARE = 0.60
}

object CulturalProfileEngine {
    fun build(
        source: CulturalProfileSource,
        today: LocalDate,
        comparisonYear: Int = today.year,
    ): CulturalProfile {
        val completionsByMedia = source.completions.groupBy(CulturalCompletion::mediaId)
        val works = source.works.distinctBy(CulturalProfileWork::mediaId).map { work ->
            WorkSignal(
                work = work,
                isCompleted = completionsByMedia[work.mediaId].orEmpty().isNotEmpty(),
                rating = completionsByMedia[work.mediaId].orEmpty()
                    .filter { it.ratingHalfStars != null }
                    .maxWithOrNull(compareBy<CulturalCompletion> { it.completedDate }.thenBy { it.updatedAt })
                    ?.ratingHalfStars?.div(2.0),
            )
        }
        val completedWorks = works.filter(WorkSignal::isCompleted)
        val ratedWorks = completedWorks.mapNotNull(WorkSignal::rating)
        val genreMetrics = stringMetrics(completedWorks) { it.work.genres }
        val creatorMetrics = stringMetrics(completedWorks) { it.work.creators }
        val tagMetrics = stringMetrics(works) { it.work.tags }
        val mediaTypeMetrics = MediaType.entries.mapNotNull { type ->
            val matching = completedWorks.filter { it.work.mediaType == type }
            matching.takeIf(List<WorkSignal>::isNotEmpty)?.let {
                CulturalMediaTypeMetric(
                    mediaType = type,
                    count = it.size,
                    averageRating = it.mapNotNull(WorkSignal::rating).takeIf(List<Double>::isNotEmpty)?.average(),
                    ratedCount = it.count { signal -> signal.rating != null },
                )
            }
        }.sortedWith(compareByDescending<CulturalMediaTypeMetric> { it.count }.thenBy { it.mediaType.name })

        val insights = buildList {
            genreMetrics.bestRated()?.let { add(CulturalInsight.BestRatedGenre(it)) }
            genreMetrics.filter { it.count >= CulturalProfileThresholds.MIN_PRESENCE_COUNT }
                .take(5).takeIf(List<CulturalMetric>::isNotEmpty)?.let { add(CulturalInsight.PresentGenres(it)) }
            creatorMetrics.firstOrNull()?.takeIf { it.count >= CulturalProfileThresholds.MIN_PRESENCE_COUNT }
                ?.let { add(CulturalInsight.MostPresentCreator(it)) }
            creatorMetrics.bestRated()?.let { add(CulturalInsight.BestRatedCreator(it)) }
            if (completedWorks.size >= CulturalProfileThresholds.MIN_TEMPORAL_SAMPLE) {
                add(
                    CulturalInsight.MediaTypes(
                        items = mediaTypeMetrics,
                        bestRated = mediaTypeMetrics.filter { it.ratedCount >= CulturalProfileThresholds.MIN_RATED_SAMPLE }
                            .maxWithOrNull(compareBy<CulturalMediaTypeMetric> { it.averageRating ?: 0.0 }.thenBy { it.count }),
                    ),
                )
            }
            tagMetrics.firstOrNull()?.takeIf { it.count >= CulturalProfileThresholds.MIN_PRESENCE_COUNT }?.let { mostUsed ->
                add(CulturalInsight.PersonalTags(mostUsed, tagMetrics.bestRated()))
            }
            favoriteGenre(works)?.let(::add)
            temporalInsights(source.completions).forEach(::add)
            annualComparisons(source, comparisonYear, today).forEach(::add)
        }

        return CulturalProfile(
            summary = CulturalProfileSummary(
                workCount = works.size,
                averageRating = ratedWorks.takeIf(List<Double>::isNotEmpty)?.average(),
                favoriteCount = works.count { it.work.isFavorite },
            ),
            insights = insights,
            periods = detectPeriods(source),
        )
    }

    fun detectPeriods(source: CulturalProfileSource): List<CulturalPeriod> {
        if (source.completions.isEmpty()) return emptyList()
        val workById = source.works.associateBy(CulturalProfileWork::mediaId)
        val firstMonth = source.completions.minOfOrNull { YearMonth.from(it.completedDate) } ?: return emptyList()
        val lastMonth = source.completions.maxOfOrNull { YearMonth.from(it.completedDate) } ?: return emptyList()
        val windowStarts = generateSequence(firstMonth.minusMonths(2)) { it.plusMonths(1) }
            .takeWhile { it.plusMonths(2) <= lastMonth }
            .toList()
        val candidates = windowStarts.mapNotNull { from ->
            val until = from.plusMonths(2)
            val events = source.completions.filter {
                val month = YearMonth.from(it.completedDate)
                month >= from && month <= until
            }.groupBy(CulturalCompletion::mediaId).values.mapNotNull { rows ->
                rows.maxWithOrNull(compareBy<CulturalCompletion> { it.completedDate }.thenBy { it.updatedAt })
            }
            if (events.size < CulturalProfileThresholds.MIN_PERIOD_WORKS) return@mapNotNull null
            val periodWorks = events.mapNotNull { workById[it.mediaId] }
            if (periodWorks.size < CulturalProfileThresholds.MIN_PERIOD_WORKS) return@mapNotNull null
            val signalCandidates = buildList {
                signalCounts(periodWorks.flatMap { work -> work.genres.distinctNormalized().map { it to work.mediaId } })
                    .forEach { add(PeriodSignal(CulturalPeriodKind.GENRE, it.label, it.count, null)) }
                signalCounts(periodWorks.flatMap { work -> work.tags.distinctNormalized().map { it to work.mediaId } })
                    .forEach { add(PeriodSignal(CulturalPeriodKind.TAG, it.label, it.count, null)) }
                periodWorks.groupingBy(CulturalProfileWork::mediaType).eachCount().forEach { (type, count) ->
                    add(PeriodSignal(CulturalPeriodKind.MEDIA_TYPE, type.name, count, type))
                }
            }.filter { signal ->
                signal.count >= CulturalProfileThresholds.MIN_PERIOD_MATCHES &&
                    signal.count.toDouble() / periodWorks.size >= CulturalProfileThresholds.MIN_PERIOD_SHARE
            }
            val strongest = signalCandidates.maxWithOrNull(
                compareBy<PeriodSignal> { it.count }
                    .thenBy { kindPriority(it.kind) },
            ) ?: return@mapNotNull null
            val matchingIds = when (strongest.kind) {
                CulturalPeriodKind.GENRE -> periodWorks.filter { work -> work.genres.any { it.equals(strongest.label, true) } }
                CulturalPeriodKind.TAG -> periodWorks.filter { work -> work.tags.any { it.equals(strongest.label, true) } }
                CulturalPeriodKind.MEDIA_TYPE -> periodWorks.filter { it.mediaType == strongest.mediaType }
            }.mapTo(mutableSetOf(), CulturalProfileWork::mediaId)
            val ratings = events.filter { it.mediaId in matchingIds }.mapNotNull { it.ratingHalfStars?.div(2.0) }
            CulturalPeriod(
                kind = strongest.kind,
                label = strongest.label,
                from = from,
                until = until,
                matchingWorks = strongest.count,
                totalWorks = periodWorks.size,
                averageRating = ratings.takeIf(List<Double>::isNotEmpty)?.average(),
                mediaType = strongest.mediaType,
            )
        }
        return candidates
            .groupBy { it.kind to it.label.normalized() }
            .values.mapNotNull { sameSignal ->
                sameSignal.maxWithOrNull(compareBy<CulturalPeriod> { it.matchingWorks }.thenBy { it.from })
            }
            .sortedWith(compareByDescending<CulturalPeriod> { it.matchingWorks }.thenByDescending { it.from })
            .take(2)
    }

    private fun favoriteGenre(works: List<WorkSignal>): CulturalInsight.FavoriteGenre? {
        val favorites = works.filter { it.work.isFavorite }
        if (favorites.size < CulturalProfileThresholds.MIN_RATED_SAMPLE) return null
        val top = signalCounts(favorites.flatMap { signal ->
            signal.work.genres.distinctNormalized().map { it to signal.work.mediaId }
        }).firstOrNull()?.takeIf { it.count >= 2 } ?: return null
        return CulturalInsight.FavoriteGenre(top.label, top.count, favorites.size)
    }

    private fun temporalInsights(completions: List<CulturalCompletion>): List<CulturalInsight> {
        val byMonth = completions.groupBy { YearMonth.from(it.completedDate) }
            .mapValues { (_, rows) -> rows.distinctBy(CulturalCompletion::mediaId).size }
        val activeMonth = byMonth.entries.maxWithOrNull(compareBy<Map.Entry<YearMonth, Int>> { it.value }.thenBy { it.key })
            ?.takeIf { it.value >= CulturalProfileThresholds.MIN_ACTIVE_MONTH_SAMPLE }
        val byYear = completions.groupBy { it.completedDate.year }
            .mapValues { (_, rows) -> rows.distinctBy(CulturalCompletion::mediaId).size }
        val activeYear = byYear.takeIf { it.size >= 2 }?.entries
            ?.maxWithOrNull(compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            ?.takeIf { it.value >= CulturalProfileThresholds.MIN_TEMPORAL_SAMPLE }
        return buildList {
            activeMonth?.let { add(CulturalInsight.MostActiveMonth(it.key, it.value)) }
            activeYear?.let { add(CulturalInsight.MostActiveYear(it.key, it.value)) }
        }
    }

    private fun annualComparisons(
        source: CulturalProfileSource,
        currentYear: Int,
        today: LocalDate,
    ): List<CulturalInsight> {
        val previousYear = currentYear - 1
        val isCurrentYear = currentYear == today.year
        val currentCutoff = if (isCurrentYear) today else LocalDate.of(currentYear, 12, 31)
        val previousCutoff = if (isCurrentYear) equivalentDate(today, previousYear) else LocalDate.of(previousYear, 12, 31)
        val current = uniqueYearCompletions(source.completions, currentYear, currentCutoff)
        val previous = uniqueYearCompletions(source.completions, previousYear, previousCutoff)
        if (current.size < CulturalProfileThresholds.MIN_TEMPORAL_SAMPLE ||
            previous.size < CulturalProfileThresholds.MIN_TEMPORAL_SAMPLE
        ) return emptyList()
        val workById = source.works.associateBy(CulturalProfileWork::mediaId)
        val typeChange = MediaType.entries.mapNotNull { type ->
            val currentCount = current.count { workById[it.mediaId]?.mediaType == type }
            val previousCount = previous.count { workById[it.mediaId]?.mediaType == type }
            if (currentCount < 2 || previousCount < 2 || currentCount == previousCount) return@mapNotNull null
            CulturalInsight.MediaTypeYearChange(
                mediaType = type,
                previousYear = previousYear,
                currentYear = currentYear,
                previousCount = previousCount,
                currentCount = currentCount,
                percentChange = percentageChange(previousCount, currentCount),
            )
        }.maxByOrNull { abs(it.percentChange) }
        val previousRatings = previous.mapNotNull { it.ratingHalfStars?.div(2.0) }
        val currentRatings = current.mapNotNull { it.ratingHalfStars?.div(2.0) }
        val ratingChange = if (
            previousRatings.size >= CulturalProfileThresholds.MIN_RATED_SAMPLE &&
            currentRatings.size >= CulturalProfileThresholds.MIN_RATED_SAMPLE &&
            abs(currentRatings.average() - previousRatings.average()) >= 0.2
        ) {
            CulturalInsight.AverageRatingYearChange(
                previousYear,
                currentYear,
                previousRatings.average(),
                currentRatings.average(),
            )
        } else null
        val totalChange = if (current.size != previous.size) {
            CulturalInsight.TotalYearChange(
                previousYear,
                currentYear,
                previous.size,
                current.size,
                percentageChange(previous.size, current.size),
            )
        } else null
        return if (typeChange != null) listOfNotNull(typeChange, ratingChange)
        else listOfNotNull(totalChange, ratingChange)
    }

    private fun uniqueYearCompletions(
        completions: List<CulturalCompletion>,
        year: Int,
        cutoff: LocalDate,
    ): List<CulturalCompletion> =
        completions.filter { it.completedDate.year == year && it.completedDate <= cutoff }
            .groupBy(CulturalCompletion::mediaId)
            .values.mapNotNull { rows ->
                rows.maxWithOrNull(compareBy<CulturalCompletion> { it.completedDate }.thenBy { it.updatedAt })
            }

    private fun equivalentDate(date: LocalDate, year: Int): LocalDate {
        val month = java.time.YearMonth.of(year, date.month)
        return month.atDay(date.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
    }

    private fun stringMetrics(
        works: List<WorkSignal>,
        values: (WorkSignal) -> List<String>,
    ): List<CulturalMetric> {
        val grouped = linkedMapOf<String, MutableList<Pair<String, WorkSignal>>>()
        works.forEach { work ->
            values(work).distinctNormalized().forEach { label ->
                grouped.getOrPut(label.normalized()) { mutableListOf() }.add(label to work)
            }
        }
        return grouped.values.map { rows ->
            val ratings = rows.mapNotNull { it.second.rating }
            CulturalMetric(
                label = rows.first().first,
                count = rows.map { it.second.work.mediaId }.distinct().size,
                averageRating = ratings.takeIf(List<Double>::isNotEmpty)?.average(),
                ratedCount = ratings.size,
            )
        }.sortedWith(compareByDescending<CulturalMetric> { it.count }.thenBy { it.label.lowercase() })
    }

    private fun List<CulturalMetric>.bestRated(): CulturalMetric? =
        filter { it.ratedCount >= CulturalProfileThresholds.MIN_RATED_SAMPLE }
            .maxWithOrNull(compareBy<CulturalMetric> { it.averageRating ?: 0.0 }.thenBy { it.count })

    private fun signalCounts(values: List<Pair<String, String>>): List<SignalCount> = values
        .groupBy { it.first.normalized() }
        .values.map { rows -> SignalCount(rows.first().first, rows.map { it.second }.distinct().size) }
        .sortedWith(compareByDescending<SignalCount> { it.count }.thenBy { it.label.lowercase() })

    private fun percentageChange(previous: Int, current: Int): Int =
        ((current - previous) * 100.0 / previous).roundToInt()

    private fun kindPriority(kind: CulturalPeriodKind): Int = when (kind) {
        CulturalPeriodKind.GENRE -> 3
        CulturalPeriodKind.TAG -> 2
        CulturalPeriodKind.MEDIA_TYPE -> 1
    }

    private fun List<String>.distinctNormalized(): List<String> =
        map(String::trim).filter(String::isNotEmpty).distinctBy { it.normalized() }

    private fun String.normalized(): String = trim().lowercase()

    private data class WorkSignal(
        val work: CulturalProfileWork,
        val isCompleted: Boolean,
        val rating: Double?,
    )

    private data class SignalCount(val label: String, val count: Int)
    private data class PeriodSignal(
        val kind: CulturalPeriodKind,
        val label: String,
        val count: Int,
        val mediaType: MediaType?,
    )
}
