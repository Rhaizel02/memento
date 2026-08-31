package com.memento.app.data.repository

import com.memento.app.data.local.dao.GlobalSearchDao
import com.memento.app.data.local.dao.GlobalSearchFacetRow
import com.memento.app.data.local.dao.GlobalSearchMediaRow
import com.memento.app.data.local.dao.GlobalSearchTextRow
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.repository.GlobalSearchRepository
import com.memento.app.domain.search.FacetSearchResult
import com.memento.app.domain.search.GlobalSearchSnapshot
import com.memento.app.domain.search.MediaSearchResult
import com.memento.app.domain.search.SearchFacetType
import com.memento.app.domain.search.SearchMatchReason
import com.memento.app.domain.search.matches
import com.memento.app.domain.search.TextSearchResult
import com.memento.app.domain.usecase.TagNameNormalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class PreparedGlobalSearchQuery(
    val exact: String,
    val prefixPattern: String,
    val containsPattern: String,
)

fun prepareGlobalSearchQuery(query: String): PreparedGlobalSearchQuery {
    val normalized = TagNameNormalizer.normalize(query)
    val escaped = buildString(normalized.length) {
        normalized.forEach { character ->
            if (character == '\\' || character == '%' || character == '_') append('\\')
            append(character)
        }
    }
    return PreparedGlobalSearchQuery(
        exact = normalized,
        prefixPattern = "$escaped%",
        containsPattern = "%$escaped%",
    )
}

@Singleton
class RoomGlobalSearchRepository @Inject constructor(
    private val dao: GlobalSearchDao,
) : GlobalSearchRepository {
    override suspend fun search(query: String): GlobalSearchSnapshot {
        val prepared = prepareGlobalSearchQuery(query)
        if (prepared.exact.length < MIN_QUERY_LENGTH) return GlobalSearchSnapshot(query.trim())

        val titleRows = dao.searchMediaTitles(prepared.exact, prepared.prefixPattern, prepared.containsPattern, MEDIA_CANDIDATE_LIMIT)
        val creatorRows = dao.searchMediaCreators(prepared.exact, prepared.prefixPattern, prepared.containsPattern, MEDIA_CANDIDATE_LIMIT)
        val genreRows = dao.searchMediaGenres(prepared.exact, prepared.prefixPattern, prepared.containsPattern, MEDIA_CANDIDATE_LIMIT)
        val tagRows = dao.searchMediaTags(prepared.exact, prepared.prefixPattern, prepared.containsPattern, MEDIA_CANDIDATE_LIMIT)
        val tags = dao.searchTags(prepared.exact, prepared.prefixPattern, prepared.containsPattern, FACET_CANDIDATE_LIMIT)
        val creators = dao.searchCreators(prepared.exact, prepared.prefixPattern, prepared.containsPattern, FACET_CANDIDATE_LIMIT)
        val genres = dao.searchGenres(prepared.exact, prepared.prefixPattern, prepared.containsPattern, FACET_CANDIDATE_LIMIT)
        val textEnabled = prepared.exact.length >= TEXT_QUERY_LENGTH
        val quotes = if (textEnabled) {
            dao.searchQuotes(prepared.exact, prepared.prefixPattern, prepared.containsPattern, TEXT_RESULT_LIMIT)
        } else {
            emptyList()
        }
        val reflections = if (textEnabled) {
            dao.searchReflections(prepared.exact, prepared.prefixPattern, prepared.containsPattern, TEXT_RESULT_LIMIT)
        } else {
            emptyList()
        }

        val rankedMedia = mergeAndRankMedia(prepared.exact, titleRows, creatorRows, genreRows, tagRows)
        val rankedTags = rankFacets(tags, SearchFacetType.TAG, prepared.exact)
        val rankedCreators = rankFacets(creators, SearchFacetType.CREATOR, prepared.exact)
        val rankedGenres = rankFacets(genres, SearchFacetType.GENRE, prepared.exact)
        val facets = rankedTags + rankedCreators + rankedGenres
        return GlobalSearchSnapshot(
            query = query.trim(),
            media = rankedMedia.take(MEDIA_RESULT_LIMIT),
            tags = rankedTags,
            creators = rankedCreators,
            genres = rankedGenres,
            quotes = rankText(quotes, prepared.exact, quote = true),
            reflections = rankText(reflections, prepared.exact, quote = false),
            associatedMedia = facets.associate { facet ->
                facet.key to rankedMedia.filter { it.matches(facet) }.take(MEDIA_RESULT_LIMIT)
            },
        )
    }

    private fun mergeAndRankMedia(
        query: String,
        titles: List<GlobalSearchMediaRow>,
        creators: List<GlobalSearchMediaRow>,
        genres: List<GlobalSearchMediaRow>,
        tags: List<GlobalSearchMediaRow>,
    ): List<MediaSearchResult> {
        val results = linkedMapOf<String, MediaAccumulator>()

        fun add(row: GlobalSearchMediaRow, reason: SearchMatchReason) {
            val accumulator = results.getOrPut(row.mediaId) {
                MediaAccumulator(row.mediaId, row.title, row.mediaType, row.posterUrl)
            }
            accumulator.reasons += reason
        }

        titles.forEach { add(it, SearchMatchReason.Title) }
        creators.forEach { row ->
            if (row.reasonId != null && row.reasonName != null && row.creatorRole != null) {
                add(row, SearchMatchReason.Creator(row.reasonId, row.reasonName, row.creatorRole))
            }
        }
        genres.forEach { row ->
            if (row.reasonId != null && row.reasonName != null) {
                add(row, SearchMatchReason.Genre(row.reasonId, row.reasonName))
            }
        }
        tags.forEach { row ->
            if (row.reasonId != null && row.reasonName != null) {
                add(row, SearchMatchReason.Tag(row.reasonId, row.reasonName))
            }
        }

        return results.values
            .map(MediaAccumulator::toResult)
            .sortedWith(
                compareBy<MediaSearchResult> { mediaRank(it, query) }
                    .thenByDescending { it.matchReasons.size }
                    .thenBy { it.title.lowercase(Locale.ROOT) }
                    .thenBy(MediaSearchResult::mediaId),
            )
    }

    private fun rankFacets(
        rows: List<GlobalSearchFacetRow>,
        type: SearchFacetType,
        query: String,
    ): List<FacetSearchResult> = rows
        .map { row -> FacetSearchResult(row.id, row.name, type, row.mediaCount, row.creatorRole) }
        .sortedWith(
            compareBy<FacetSearchResult> { textMatchRank(it.name, query) }
                .thenBy { it.name.lowercase(Locale.ROOT) }
                .thenBy(FacetSearchResult::id),
        )
        .take(FACET_RESULT_LIMIT)

    private fun rankText(rows: List<GlobalSearchTextRow>, query: String, quote: Boolean): List<TextSearchResult> = rows
        .asSequence()
        .filter { row -> if (quote) row.reflectionType == ReflectionType.QUOTE else row.reflectionType != ReflectionType.QUOTE }
        .sortedWith(
            compareBy<GlobalSearchTextRow> { textMatchRank(it.content, query) }
                .thenBy { if (quote) 0 else reflectionPriority(it.reflectionType) }
                .thenByDescending(GlobalSearchTextRow::createdAt)
                .thenBy(GlobalSearchTextRow::reflectionId),
        )
        .take(TEXT_RESULT_LIMIT)
        .map { row ->
            TextSearchResult(
                reflectionId = row.reflectionId,
                mediaId = row.mediaId,
                mediaTitle = row.mediaTitle,
                mediaType = row.mediaType,
                reflectionType = row.reflectionType,
                excerpt = buildSearchExcerpt(row.content, query),
                createdAt = row.createdAt,
            )
        }
        .toList()

    private data class MediaAccumulator(
        val id: String,
        val title: String,
        val mediaType: com.memento.app.domain.model.MediaType,
        val posterUrl: String?,
        val reasons: MutableSet<SearchMatchReason> = linkedSetOf(),
    ) {
        fun toResult() = MediaSearchResult(
            mediaId = id,
            title = title,
            mediaType = mediaType,
            posterUrl = posterUrl,
            matchReasons = reasons.sortedWith(searchReasonComparator),
        )
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val TEXT_QUERY_LENGTH = 3
        const val MEDIA_CANDIDATE_LIMIT = 60
        const val FACET_CANDIDATE_LIMIT = 30
        const val MEDIA_RESULT_LIMIT = 20
        const val FACET_RESULT_LIMIT = 10
        const val TEXT_RESULT_LIMIT = 20
    }
}

fun buildSearchExcerpt(text: String, query: String, maxLength: Int = 150): String {
    require(maxLength >= 20)
    val clean = text.trim().replace(Regex("\\s+"), " ")
    if (clean.length <= maxLength) return clean
    val matchIndex = clean.lowercase(Locale.ROOT).indexOf(TagNameNormalizer.normalize(query))
    if (matchIndex < 0) return clean.wordSafeSlice(0, maxLength, prefix = false, suffix = true)

    val leftBudget = ((maxLength - query.length).coerceAtLeast(0) / 2)
    var start = (matchIndex - leftBudget).coerceAtLeast(0)
    var end = (start + maxLength).coerceAtMost(clean.length)
    if (end == clean.length) start = (end - maxLength).coerceAtLeast(0)
    return clean.wordSafeSlice(start, end, prefix = start > 0, suffix = end < clean.length)
}

private fun String.wordSafeSlice(startIndex: Int, endIndex: Int, prefix: Boolean, suffix: Boolean): String {
    var start = startIndex
    var end = endIndex
    if (start > 0 && this[start - 1] != ' ') {
        val nextSpace = indexOf(' ', start)
        if (nextSpace in start until end) start = nextSpace + 1
    }
    if (end < length && this[end] != ' ') {
        val previousSpace = lastIndexOf(' ', end)
        if (previousSpace > start) end = previousSpace
    }
    return buildString {
        if (prefix && start > 0) append("…")
        append(this@wordSafeSlice.substring(start, end).trim())
        if (suffix && end < this@wordSafeSlice.length) append("…")
    }
}

private fun mediaRank(result: MediaSearchResult, query: String): Int {
    if (SearchMatchReason.Title in result.matchReasons) return textMatchRank(result.title, query)
    val relatedRank = result.matchReasons.minOfOrNull { reason ->
        when (reason) {
            SearchMatchReason.Title -> Int.MAX_VALUE
            is SearchMatchReason.Tag -> textMatchRank(reason.name, query)
            is SearchMatchReason.Creator -> textMatchRank(reason.name, query)
            is SearchMatchReason.Genre -> textMatchRank(reason.name, query)
        }
    } ?: Int.MAX_VALUE
    return if (relatedRank == 0) 3 else 4
}

private fun textMatchRank(text: String, query: String): Int {
    val normalizedText = TagNameNormalizer.normalize(text)
    return when {
        normalizedText == query -> 0
        normalizedText.startsWith(query) -> 1
        else -> 2
    }
}

private fun reflectionPriority(type: ReflectionType): Int = when (type) {
    ReflectionType.FINAL_REFLECTION -> 0
    ReflectionType.LATER_REFLECTION -> 1
    ReflectionType.NOTE -> 2
    ReflectionType.QUOTE -> 3
}

private val searchReasonComparator = compareBy<SearchMatchReason> {
    when (it) {
        SearchMatchReason.Title -> 0
        is SearchMatchReason.Creator -> 1
        is SearchMatchReason.Tag -> 2
        is SearchMatchReason.Genre -> 3
    }
}.thenBy {
    when (it) {
        SearchMatchReason.Title -> ""
        is SearchMatchReason.Creator -> it.name.lowercase(Locale.ROOT)
        is SearchMatchReason.Tag -> it.name.lowercase(Locale.ROOT)
        is SearchMatchReason.Genre -> it.name.lowercase(Locale.ROOT)
    }
}
