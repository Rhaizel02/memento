package com.memento.app.domain.model

import java.time.Instant
import java.time.LocalDate

enum class MediaType { BOOK, MOVIE, SERIES, GAME }

enum class ConsumptionStatus { PLANNED, IN_PROGRESS, COMPLETED, DROPPED }

enum class ProgressType { PAGES, EPISODE, HOURS, PERCENT, MINUTES }

enum class ReflectionType { NOTE, QUOTE, FINAL_REFLECTION, LATER_REFLECTION }

enum class CreatorRole { AUTHOR, DIRECTOR, DEVELOPER, CREATOR, OTHER }

enum class MetadataProvider { TMDB, OPEN_LIBRARY, RAWG }

enum class RecommendationFeedbackType { INTERESTED, NOT_INTERESTED, ALREADY_KNOWN }

enum class LibrarySort { RECENT, TITLE, RATING, COMPLETED_DATE, ADDED_DATE }

data class LibraryFilters(
    val status: ConsumptionStatus? = null,
    val minRatingHalfStars: Int? = null,
    val favoritesOnly: Boolean = false,
    val year: Int? = null,
    val sort: LibrarySort = LibrarySort.RECENT,
    val tagIds: Set<String> = emptySet(),
)

data class Tag(
    val id: String,
    val name: String,
    val normalizedName: String,
    val createdAt: Instant,
)

data class MediaItem(
    val id: String,
    val type: MediaType,
    val title: String,
    val originalTitle: String? = null,
    val description: String? = null,
    val releaseDate: LocalDate? = null,
    val releaseYear: Int? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val isFavorite: Boolean = false,
    val isManual: Boolean = true,
    val runtimeMinutes: Int? = null,
    val pageCount: Int? = null,
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class Consumption(
    val id: String,
    val mediaItemId: String,
    val status: ConsumptionStatus,
    val startedDate: LocalDate? = null,
    val completedDate: LocalDate? = null,
    val ratingHalfStars: Int? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ProgressEntry(
    val id: String,
    val consumptionId: String,
    val progressType: ProgressType,
    val currentValue: Double? = null,
    val totalValue: Double? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val recordedAt: Instant,
)

data class Reflection(
    val id: String,
    val consumptionId: String,
    val type: ReflectionType,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class MediaDetail(
    val media: MediaItem,
    val creators: List<String>,
    val genres: List<String>,
    val consumptions: List<Consumption>,
    val progress: List<ProgressEntry>,
    val reflections: List<Reflection>,
    val tags: List<Tag> = emptyList(),
    val externalRefs: List<MediaExternalReference> = emptyList(),
) {
    val activeConsumption: Consumption?
        get() = consumptions.firstOrNull {
            it.status == ConsumptionStatus.PLANNED || it.status == ConsumptionStatus.IN_PROGRESS
        }

    val latestConsumption: Consumption?
        get() = activeConsumption ?: consumptions.maxByOrNull { it.updatedAt }
}

data class MediaExternalReference(
    val provider: MetadataProvider,
    val externalId: String,
    val mediaType: MediaType,
    val externalUrl: String? = null,
)

data class HomeMediaSummary(
    val media: MediaItem,
    val consumptionId: String,
    val ratingHalfStars: Int? = null,
    val completedDate: LocalDate? = null,
    val creator: String? = null,
    val genres: List<String> = emptyList(),
    val additionalGenreCount: Int = 0,
    val latestProgress: ProgressEntry? = null,
)

data class HomeMediaFeed(
    val mediaCount: Int = 0,
    val inProgress: List<HomeMediaSummary> = emptyList(),
    val recentlyCompleted: List<HomeMediaSummary> = emptyList(),
)

data class AddMediaInput(
    val type: MediaType,
    val title: String,
    val year: Int? = null,
    val creator: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val pageCount: Int? = null,
)

data class CompletedMediaInput(
    val completedDate: LocalDate,
    val ratingHalfStars: Int? = null,
    val favorite: Boolean = false,
    val finalReflection: String? = null,
)

data class EditMediaInput(
    val title: String,
    val year: Int? = null,
    val description: String? = null,
    val creators: List<String> = emptyList(),
    val imageUrl: String? = null,
)

data class MetadataSearchResult(
    val provider: MetadataProvider,
    val externalId: String,
    val externalUrl: String?,
    val type: MediaType,
    val title: String,
    val originalTitle: String? = null,
    val description: String? = null,
    val releaseDate: LocalDate? = null,
    val releaseYear: Int? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val creators: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val runtimeMinutes: Int? = null,
    val pageCount: Int? = null,
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val externalRating: Double? = null,
    val externalVoteCount: Int? = null,
    val popularity: Double? = null,
    val externalTags: List<String> = emptyList(),
    val sourceAnchorMediaIds: List<String> = emptyList(),
    val sourceAnchorTitles: List<String> = emptyList(),
)

data class SaveExternalResult(val mediaId: String, val wasDuplicate: Boolean)

sealed interface MetadataSearchOutcome {
    val provider: MetadataProvider

    data class Success(
        override val provider: MetadataProvider,
        val results: List<MetadataSearchResult>,
    ) : MetadataSearchOutcome

    data class NotConfigured(override val provider: MetadataProvider) : MetadataSearchOutcome
    data class Unavailable(override val provider: MetadataProvider) : MetadataSearchOutcome
}

sealed interface TimelineEvent {
    val sortInstant: Instant

    data class ConsumptionStarted(
        val date: LocalDate,
        override val sortInstant: Instant,
    ) : TimelineEvent

    data class ConsumptionCompleted(
        val date: LocalDate,
        val ratingHalfStars: Int?,
        override val sortInstant: Instant,
    ) : TimelineEvent

    data class ConsumptionDropped(
        val date: LocalDate?,
        override val sortInstant: Instant,
    ) : TimelineEvent

    data class ProgressUpdated(
        val entry: ProgressEntry,
        override val sortInstant: Instant = entry.recordedAt,
    ) : TimelineEvent

    data class ReflectionWritten(
        val reflection: Reflection,
        override val sortInstant: Instant = reflection.createdAt,
    ) : TimelineEvent
}
