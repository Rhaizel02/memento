package com.memento.app.domain.wrapped

import com.memento.app.domain.culturalprofile.CulturalCompletion
import com.memento.app.domain.culturalprofile.CulturalInsight
import com.memento.app.domain.culturalprofile.CulturalMediaTypeMetric
import com.memento.app.domain.culturalprofile.CulturalMetric
import com.memento.app.domain.culturalprofile.CulturalPeriod
import com.memento.app.domain.culturalprofile.CulturalProfileEngine
import com.memento.app.domain.culturalprofile.CulturalProfileSource
import com.memento.app.domain.culturalprofile.CulturalProfileWork
import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.TimelineEventType
import java.time.LocalDate
import java.time.YearMonth

data class WrappedSource(
    val profileSource: CulturalProfileSource = CulturalProfileSource(),
    val timelineEvents: List<CulturalTimelineEvent> = emptyList(),
)

data class WrappedSnapshot(
    val year: Int,
    val isYearToDate: Boolean,
    val completedWorkCount: Int,
    val cards: List<WrappedCard>,
)

enum class WrappedSharePolicy { NONE, STANDARD, EXPLICIT_REFLECTION }

data class WrappedWork(
    val mediaId: String,
    val title: String,
    val mediaType: MediaType,
    val posterUrl: String?,
    val backdropUrl: String?,
    val ratingHalfStars: Int?,
    val isFavorite: Boolean,
    val tags: List<String>,
)

data class WrappedReflection(
    val reflectionId: String,
    val mediaId: String,
    val workTitle: String,
    val type: TimelineEventType,
    val content: String,
    val posterUrl: String?,
)

sealed interface WrappedCard {
    val sharePolicy: WrappedSharePolicy get() = WrappedSharePolicy.NONE

    data class Cover(
        val year: Int,
        val isYearToDate: Boolean,
        val storyCount: Int,
        val heroImageUrl: String?,
    ) : WrappedCard {
        override val sharePolicy = WrappedSharePolicy.STANDARD
    }

    data class MediaSummary(
        val total: Int,
        val byType: Map<MediaType, Int>,
    ) : WrappedCard {
        override val sharePolicy = WrappedSharePolicy.STANDARD
    }

    data class GenreOfYear(val genre: CulturalMetric) : WrappedCard
    data class BestRatedMedium(val medium: CulturalMediaTypeMetric) : WrappedCard
    data class FeaturedCreator(val creator: CulturalMetric) : WrappedCard
    data class PersonalTags(val mostUsed: CulturalMetric, val bestRated: CulturalMetric?) : WrappedCard
    data class IntenseMonth(val month: YearMonth, val completedWorks: Int) : WrappedCard

    data class CulturalEra(val period: CulturalPeriod) : WrappedCard {
        override val sharePolicy = WrappedSharePolicy.STANDARD
    }

    data class WorkOfYear(val work: WrappedWork) : WrappedCard {
        override val sharePolicy = WrappedSharePolicy.STANDARD
    }

    data class ReflectionSpotlight(val reflection: WrappedReflection) : WrappedCard {
        override val sharePolicy = WrappedSharePolicy.EXPLICIT_REFLECTION
    }

    data class Favorites(val works: List<WrappedWork>) : WrappedCard
    data class Comparisons(val insights: List<CulturalInsight>) : WrappedCard
    data class Finale(val year: Int, val completed: Int, val favoriteCount: Int) : WrappedCard
}

object WrappedEngine {
    private const val MIN_REFLECTION_LENGTH = 24
    private const val MAX_FAVORITES = 5

    fun availableYears(
        source: WrappedSource,
        today: LocalDate,
    ): List<Int> = buildSet {
        source.profileSource.completions
            .filter { it.completedDate <= today }
            .mapTo(this) { it.completedDate.year }
        source.timelineEvents
            .filter { it.isMeaningfulReflection() && it.date <= today }
            .mapTo(this) { it.date.year }
    }.filter { it <= today.year }.sortedDescending()

    fun create(
        source: WrappedSource,
        year: Int,
        today: LocalDate,
    ): WrappedSnapshot {
        val cutoff = if (year == today.year) today else LocalDate.of(year, 12, 31)
        val yearCompletions = source.profileSource.completions.filter {
            it.completedDate.year == year && it.completedDate <= cutoff
        }
        val yearWorkIds = yearCompletions.mapTo(linkedSetOf()) { it.mediaId }
        val yearWorks = source.profileSource.works.filter { it.mediaId in yearWorkIds }
        val yearSource = CulturalProfileSource(yearWorks, yearCompletions)
        val yearProfile = CulturalProfileEngine.build(yearSource, today, year)
        val comparisonInsights = CulturalProfileEngine.build(source.profileSource, today, year).insights
            .filter(::isAnnualComparison)
            .take(2)
        val yearEvents = source.timelineEvents.filter { it.date.year == year && it.date <= cutoff }
        val workOfYear = selectWorkOfYear(yearWorks, yearCompletions, yearEvents)
        val favoriteWorks = yearWorks.filter(CulturalProfileWork::isFavorite)
            .map { it.toWrappedWork(yearCompletions) }
            .sortedWith(compareByDescending<WrappedWork> { it.ratingHalfStars ?: -1 }.thenBy { it.title.lowercase() }.thenBy { it.mediaId })
            .take(MAX_FAVORITES)
        val reflection = selectReflection(yearEvents, yearWorks)
        val mediaTypes = yearProfile.insights.filterIsInstance<CulturalInsight.MediaTypes>().firstOrNull()
        val tags = yearProfile.insights.filterIsInstance<CulturalInsight.PersonalTags>().firstOrNull()
        val cards = buildList {
            add(
                WrappedCard.Cover(
                    year = year,
                    isYearToDate = year == today.year && today != LocalDate.of(year, 12, 31),
                    storyCount = yearWorkIds.size,
                    heroImageUrl = workOfYear?.backdropUrl ?: workOfYear?.posterUrl ?: favoriteWorks.firstOrNull()?.posterUrl,
                ),
            )
            if (yearWorkIds.isNotEmpty()) {
                add(
                    WrappedCard.MediaSummary(
                        total = yearWorkIds.size,
                        byType = MediaType.entries.associateWith { type -> yearWorks.count { it.mediaType == type } },
                    ),
                )
            }
            yearProfile.insights.filterIsInstance<CulturalInsight.PresentGenres>().firstOrNull()
                ?.genres?.firstOrNull()?.let { add(WrappedCard.GenreOfYear(it)) }
            mediaTypes?.bestRated?.let { add(WrappedCard.BestRatedMedium(it)) }
            yearProfile.insights.filterIsInstance<CulturalInsight.MostPresentCreator>().firstOrNull()
                ?.creator?.let { add(WrappedCard.FeaturedCreator(it)) }
            tags?.let { add(WrappedCard.PersonalTags(it.mostUsed, it.bestRated)) }
            yearProfile.insights.filterIsInstance<CulturalInsight.MostActiveMonth>().firstOrNull()
                ?.let { add(WrappedCard.IntenseMonth(it.month, it.completedWorks)) }
            yearProfile.periods.firstOrNull()?.let { add(WrappedCard.CulturalEra(it)) }
            workOfYear?.let { add(WrappedCard.WorkOfYear(it)) }
            if (favoriteWorks.isNotEmpty()) add(WrappedCard.Favorites(favoriteWorks))
            reflection?.let { add(WrappedCard.ReflectionSpotlight(it)) }
            if (comparisonInsights.isNotEmpty()) add(WrappedCard.Comparisons(comparisonInsights))
            add(WrappedCard.Finale(year, yearWorkIds.size, favoriteWorks.size))
        }
        return WrappedSnapshot(
            year = year,
            isYearToDate = year == today.year && today != LocalDate.of(year, 12, 31),
            completedWorkCount = yearWorkIds.size,
            cards = cards,
        )
    }

    fun selectWorkOfYear(
        works: List<CulturalProfileWork>,
        completions: List<CulturalCompletion>,
        events: List<CulturalTimelineEvent>,
    ): WrappedWork? {
        val completionsByMedia = completions.groupBy { it.mediaId }
        val meaningfulByMedia = events.filter { it.isMeaningfulReflection() }.groupBy { it.mediaItemId }
        return works.mapNotNull { work ->
            val rating = completionsByMedia[work.mediaId].orEmpty().mapNotNull { it.ratingHalfStars }.maxOrNull()
            val reflectionWeight = meaningfulByMedia[work.mediaId].orEmpty().maxOfOrNull { reflectionPriority(it.eventType) } ?: 0
            val hasSignal = rating != null || work.isFavorite || reflectionWeight > 0 || work.tags.isNotEmpty()
            if (!hasSignal) return@mapNotNull null
            val score = (rating ?: 0) * 10 +
                (if (work.isFavorite) 25 else 0) +
                reflectionWeight * 5 +
                (work.tags.size * 3).coerceAtMost(12)
            WorkCandidate(work.toWrappedWork(completions), score)
        }.sortedWith(
            compareByDescending<WorkCandidate> { it.score }
                .thenByDescending { it.work.ratingHalfStars ?: -1 }
                .thenBy { it.work.title.lowercase() }
                .thenBy { it.work.mediaId },
        ).firstOrNull()?.work
    }

    private fun selectReflection(
        events: List<CulturalTimelineEvent>,
        works: List<CulturalProfileWork>,
    ): WrappedReflection? {
        val workById = works.associateBy(CulturalProfileWork::mediaId)
        val selected = events.filter { it.isMeaningfulReflection() }.sortedWith(
            compareByDescending<CulturalTimelineEvent> { reflectionPriority(it.eventType) }
                .thenByDescending { it.reflectionContent.orEmpty().trim().length }
                .thenByDescending { it.occurredAt }
                .thenBy { it.id },
        ).firstOrNull() ?: return null
        val work = workById[selected.mediaItemId]
        return WrappedReflection(
            reflectionId = selected.reflectionId ?: selected.id,
            mediaId = selected.mediaItemId,
            workTitle = work?.title?.takeIf(String::isNotBlank) ?: selected.title,
            type = selected.eventType,
            content = selected.reflectionContent.orEmpty().trim(),
            posterUrl = work?.posterUrl ?: selected.posterUrl,
        )
    }

    private fun CulturalProfileWork.toWrappedWork(completions: List<CulturalCompletion>) = WrappedWork(
        mediaId = mediaId,
        title = title.takeIf(String::isNotBlank) ?: mediaId,
        mediaType = mediaType,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        ratingHalfStars = completions.filter { it.mediaId == mediaId }.mapNotNull { it.ratingHalfStars }.maxOrNull(),
        isFavorite = isFavorite,
        tags = tags,
    )

    private fun CulturalTimelineEvent.isMeaningfulReflection(): Boolean {
        if (eventType !in REFLECTION_TYPES) return false
        val clean = reflectionContent.orEmpty().trim()
        return clean.length >= MIN_REFLECTION_LENGTH &&
            clean.split(Regex("\\s+")).size >= 4 &&
            clean.filter(Char::isLetterOrDigit).map(Char::lowercaseChar).distinct().size >= 8
    }

    private fun reflectionPriority(type: TimelineEventType): Int = when (type) {
        TimelineEventType.FINAL_REFLECTION -> 4
        TimelineEventType.LATER_REFLECTION -> 3
        TimelineEventType.QUOTE -> 2
        TimelineEventType.NOTE -> 1
        else -> 0
    }

    private fun isAnnualComparison(insight: CulturalInsight): Boolean = when (insight) {
        is CulturalInsight.MediaTypeYearChange,
        is CulturalInsight.TotalYearChange,
        is CulturalInsight.AverageRatingYearChange,
        -> true
        else -> false
    }

    private data class WorkCandidate(val work: WrappedWork, val score: Int)

    private val REFLECTION_TYPES = setOf(
        TimelineEventType.FINAL_REFLECTION,
        TimelineEventType.LATER_REFLECTION,
        TimelineEventType.QUOTE,
        TimelineEventType.NOTE,
    )
}
