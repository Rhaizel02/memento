package com.memento.app.domain.search

import com.memento.app.domain.model.CreatorRole
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ReflectionType
import java.time.Instant

data class GlobalSearchSnapshot(
    val query: String,
    val media: List<MediaSearchResult> = emptyList(),
    val tags: List<FacetSearchResult> = emptyList(),
    val creators: List<FacetSearchResult> = emptyList(),
    val genres: List<FacetSearchResult> = emptyList(),
    val quotes: List<TextSearchResult> = emptyList(),
    val reflections: List<TextSearchResult> = emptyList(),
    val associatedMedia: Map<SearchFacetKey, List<MediaSearchResult>> = emptyMap(),
) {
    val isEmpty: Boolean
        get() = media.isEmpty() && tags.isEmpty() && creators.isEmpty() && genres.isEmpty() &&
            quotes.isEmpty() && reflections.isEmpty()
}

data class MediaSearchResult(
    val mediaId: String,
    val title: String,
    val mediaType: MediaType,
    val posterUrl: String?,
    val matchReasons: List<SearchMatchReason>,
)

sealed interface SearchMatchReason {
    data object Title : SearchMatchReason
    data class Tag(val id: String, val name: String) : SearchMatchReason
    data class Creator(val id: String, val name: String, val role: CreatorRole) : SearchMatchReason
    data class Genre(val id: String, val name: String) : SearchMatchReason
}

enum class SearchFacetType { TAG, CREATOR, GENRE }

data class SearchFacetKey(val type: SearchFacetType, val id: String)

data class FacetSearchResult(
    val id: String,
    val name: String,
    val type: SearchFacetType,
    val mediaCount: Int,
    val creatorRole: CreatorRole? = null,
) {
    val key: SearchFacetKey get() = SearchFacetKey(type, id)
}

data class TextSearchResult(
    val reflectionId: String,
    val mediaId: String,
    val mediaTitle: String,
    val mediaType: MediaType,
    val reflectionType: ReflectionType,
    val excerpt: String,
    val createdAt: Instant,
)

fun MediaSearchResult.matches(facet: FacetSearchResult): Boolean = matchReasons.any { reason ->
    when (reason) {
        SearchMatchReason.Title -> false
        is SearchMatchReason.Tag -> facet.type == SearchFacetType.TAG && reason.id == facet.id
        is SearchMatchReason.Creator -> facet.type == SearchFacetType.CREATOR && reason.id == facet.id
        is SearchMatchReason.Genre -> facet.type == SearchFacetType.GENRE && reason.id == facet.id
    }
}

fun GlobalSearchSnapshot.mediaFor(facet: FacetSearchResult): List<MediaSearchResult> =
    associatedMedia[facet.key].orEmpty()
