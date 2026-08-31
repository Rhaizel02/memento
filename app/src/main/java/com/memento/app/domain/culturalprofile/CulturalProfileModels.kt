package com.memento.app.domain.culturalprofile

import com.memento.app.domain.model.MediaType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

data class CulturalProfileSource(
    val works: List<CulturalProfileWork> = emptyList(),
    val completions: List<CulturalCompletion> = emptyList(),
)

data class CulturalProfileWork(
    val mediaId: String,
    val mediaType: MediaType,
    val isFavorite: Boolean,
    val genres: List<String> = emptyList(),
    val creators: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val title: String = "",
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
)

data class CulturalCompletion(
    val mediaId: String,
    val completedDate: LocalDate,
    val ratingHalfStars: Int?,
    val updatedAt: Instant,
)

data class CulturalProfileSummary(
    val workCount: Int = 0,
    val averageRating: Double? = null,
    val favoriteCount: Int = 0,
)

data class CulturalMetric(
    val label: String,
    val count: Int,
    val averageRating: Double? = null,
    val ratedCount: Int = 0,
)

data class CulturalMediaTypeMetric(
    val mediaType: MediaType,
    val count: Int,
    val averageRating: Double? = null,
    val ratedCount: Int = 0,
)

data class CulturalProfile(
    val summary: CulturalProfileSummary = CulturalProfileSummary(),
    val insights: List<CulturalInsight> = emptyList(),
    val periods: List<CulturalPeriod> = emptyList(),
) {
    val isTakingShape: Boolean get() = summary.workCount < 3 || insights.size < 2
}

sealed interface CulturalInsight {
    data class BestRatedGenre(val genre: CulturalMetric) : CulturalInsight
    data class PresentGenres(val genres: List<CulturalMetric>) : CulturalInsight
    data class MostPresentCreator(val creator: CulturalMetric) : CulturalInsight
    data class BestRatedCreator(val creator: CulturalMetric) : CulturalInsight
    data class MediaTypes(
        val items: List<CulturalMediaTypeMetric>,
        val bestRated: CulturalMediaTypeMetric?,
    ) : CulturalInsight
    data class PersonalTags(
        val mostUsed: CulturalMetric,
        val bestRated: CulturalMetric?,
    ) : CulturalInsight
    data class FavoriteGenre(
        val genre: String,
        val genreCount: Int,
        val favoriteCount: Int,
    ) : CulturalInsight
    data class MostActiveMonth(val month: YearMonth, val completedWorks: Int) : CulturalInsight
    data class MostActiveYear(val year: Int, val completedWorks: Int) : CulturalInsight
    data class MediaTypeYearChange(
        val mediaType: MediaType,
        val previousYear: Int,
        val currentYear: Int,
        val previousCount: Int,
        val currentCount: Int,
        val percentChange: Int,
    ) : CulturalInsight
    data class TotalYearChange(
        val previousYear: Int,
        val currentYear: Int,
        val previousCount: Int,
        val currentCount: Int,
        val percentChange: Int,
    ) : CulturalInsight
    data class AverageRatingYearChange(
        val previousYear: Int,
        val currentYear: Int,
        val previousRating: Double,
        val currentRating: Double,
    ) : CulturalInsight
}

enum class CulturalPeriodKind { GENRE, TAG, MEDIA_TYPE }

data class CulturalPeriod(
    val kind: CulturalPeriodKind,
    val label: String,
    val from: YearMonth,
    val until: YearMonth,
    val matchingWorks: Int,
    val totalWorks: Int,
    val averageRating: Double?,
    val mediaType: MediaType? = null,
)
