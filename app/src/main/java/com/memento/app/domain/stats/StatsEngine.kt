package com.memento.app.domain.stats

import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.Reflection
import java.time.ZoneId

data class RankedStat(val label: String, val count: Int, val averageRating: Double? = null)

data class HighlightedWork(val mediaId: String, val title: String, val ratingHalfStars: Int?)

data class StatsSummary(
    val year: Int,
    val completedWorks: Int,
    val completedByType: Map<MediaType, Int>,
    val pagesRead: Int?,
    val gameHours: Double?,
    val episodesWatched: Int?,
    val movieMinutes: Int?,
    val averageRatingHalfStars: Double?,
    val favoriteWorks: Int,
    val droppedWorks: Int,
    val revisits: Int,
    val reflectionCount: Int,
    val topConsumedGenres: List<RankedStat>,
    val topRatedGenres: List<RankedStat>,
    val frequentCreators: List<RankedStat>,
    val monthlyCompletions: Map<Int, Int>,
    val bestRatedWork: HighlightedWork?,
    val mostRevisitedWork: HighlightedWork?,
    val meaningfulReflection: Reflection?,
)

object StatsEngine {
    fun availableYears(history: List<MediaDetail>, currentYear: Int): List<Int> = buildSet {
        add(currentYear)
        history.flatMap { it.consumptions }.mapNotNull { it.completedDate?.year }.forEach(::add)
        history.flatMap { it.reflections }.map { it.createdAt.atZone(ZoneId.systemDefault()).year }.forEach(::add)
    }.sortedDescending()

    fun calculate(history: List<MediaDetail>, year: Int): StatsSummary {
        val completed = history.flatMap { detail ->
            detail.consumptions.filter {
                it.status == ConsumptionStatus.COMPLETED && it.completedDate?.year == year
            }.map { detail to it }
        }
        val completedByType = MediaType.entries.associateWith { type -> completed.count { it.first.media.type == type } }
        val pages = completed.sumOf { (detail, _) -> if (detail.media.type == MediaType.BOOK) detail.media.pageCount ?: 0 else 0 }
        val movieMinutes = completed.sumOf { (detail, _) -> if (detail.media.type == MediaType.MOVIE) detail.media.runtimeMinutes ?: 0 else 0 }
        val gameHours = progressTotals(history, year, ProgressType.HOURS)
        val episodeProgress = progressTotals(history, year, ProgressType.EPISODE).toInt()
        val completedEpisodes = completed.sumOf { (detail, _) -> if (detail.media.type == MediaType.SERIES) detail.media.episodeCount ?: 0 else 0 }
        val ratings = completed.mapNotNull { it.second.ratingHalfStars }
        val dropped = history.sumOf { detail ->
            detail.consumptions.count {
                it.status == ConsumptionStatus.DROPPED && it.updatedAt.atZone(ZoneId.systemDefault()).year == year
            }
        }
        val revisitsByMedia = completed.groupingBy { it.first.media.id }.eachCount()
        val reflections = history.flatMap { it.reflections }.filter {
            it.createdAt.atZone(ZoneId.systemDefault()).year == year
        }

        return StatsSummary(
            year = year,
            completedWorks = completed.size,
            completedByType = completedByType,
            pagesRead = pages.takeIf { it > 0 },
            gameHours = gameHours.takeIf { it > 0.0 },
            episodesWatched = maxOf(episodeProgress, completedEpisodes).takeIf { it > 0 },
            movieMinutes = movieMinutes.takeIf { it > 0 },
            averageRatingHalfStars = ratings.takeIf(List<Int>::isNotEmpty)?.average(),
            favoriteWorks = completed.map { it.first.media }.distinctBy { it.id }.count { it.isFavorite },
            droppedWorks = dropped,
            revisits = revisitsByMedia.values.sumOf { (it - 1).coerceAtLeast(0) },
            reflectionCount = reflections.size,
            topConsumedGenres = frequency(completed.flatMap { it.first.genres }),
            topRatedGenres = ratedGenres(completed),
            frequentCreators = frequency(completed.flatMap { it.first.creators }),
            monthlyCompletions = (1..12).associateWith { month -> completed.count { it.second.completedDate?.monthValue == month } },
            bestRatedWork = completed.maxByOrNull { it.second.ratingHalfStars ?: 0 }?.let { (detail, consumption) ->
                consumption.ratingHalfStars?.let { HighlightedWork(detail.media.id, detail.media.title, it) }
            },
            mostRevisitedWork = revisitsByMedia.maxByOrNull { it.value }?.takeIf { it.value > 1 }?.let { (mediaId, _) ->
                history.firstOrNull { it.media.id == mediaId }?.let { HighlightedWork(it.media.id, it.media.title, null) }
            },
            meaningfulReflection = reflections.maxByOrNull { it.content.length },
        )
    }

    private fun progressTotals(history: List<MediaDetail>, year: Int, type: ProgressType): Double =
        history.flatMap { it.progress }
            .filter { it.progressType == type && it.recordedAt.atZone(ZoneId.systemDefault()).year == year }
            .groupBy { it.consumptionId }
            .values.sumOf { entries -> entries.maxOfOrNull { it.currentValue ?: 0.0 } ?: 0.0 }

    private fun frequency(values: List<String>): List<RankedStat> = values
        .groupingBy { it.trim() }
        .eachCount()
        .filterKeys(String::isNotEmpty)
        .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(5)
        .map { RankedStat(it.key, it.value) }

    private fun ratedGenres(completed: List<Pair<MediaDetail, Consumption>>): List<RankedStat> = completed
        .flatMap { (detail, consumption) -> detail.genres.map { Triple(it, consumption.ratingHalfStars, detail.media.id) } }
        .filter { it.second != null }
        .groupBy { it.first.trim() }
        .filterKeys(String::isNotEmpty)
        .map { (genre, rows) -> RankedStat(genre, rows.mapNotNull { it.second }.size, rows.mapNotNull { it.second }.average() / 2.0) }
        .sortedWith(compareByDescending<RankedStat> { it.averageRating ?: 0.0 }.thenByDescending { it.count })
        .take(5)
}
