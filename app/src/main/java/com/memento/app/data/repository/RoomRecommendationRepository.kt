package com.memento.app.data.repository

import com.memento.app.data.local.dao.RecommendationDao
import com.memento.app.data.local.entity.RecommendationCandidateEntity
import com.memento.app.data.local.entity.RecommendationFeedbackEntity
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.RecommendationFeedbackType
import com.memento.app.domain.recommendation.RecommendationEngine
import com.memento.app.domain.recommendation.RecommendationKey
import com.memento.app.domain.recommendation.RecommendationCachePolicy
import com.memento.app.domain.repository.MediaRepository
import com.memento.app.domain.repository.MetadataRepository
import com.memento.app.domain.repository.RecommendationFeed
import com.memento.app.domain.repository.RecommendationRepository
import com.memento.app.domain.recommendation.recommendationKey
import com.memento.app.domain.repository.CandidateBatch
import com.memento.app.domain.repository.CandidateIssue
import com.memento.app.domain.repository.CandidateRefreshStatus
import com.memento.app.domain.repository.DISCOVER_TARGET
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRecommendationRepository(
    private val mediaRepository: MediaRepository,
    private val recommendationDao: RecommendationDao,
    private val metadataRepository: MetadataRepository,
    scope: CoroutineScope,
) : RecommendationRepository {
    @Inject constructor(
        mediaRepository: MediaRepository,
        recommendationDao: RecommendationDao,
        metadataRepository: MetadataRepository,
    ) : this(mediaRepository, recommendationDao, metadataRepository, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private val json = Json { ignoreUnknownKeys = true }
    private val personalRequests = CategoryRequests<Int>(scope)
    private val popularRequests = CategoryRequests<Unit>(scope)
    private val personalAttempts = ConcurrentHashMap<MediaType, Instant>()
    private val popularAttempts = ConcurrentHashMap<MediaType, Instant>()
    private val popularUpdatedAt = ConcurrentHashMap<MediaType, Instant>()
    private val personalStatus = MutableStateFlow<Map<MediaType, CandidateRefreshStatus>>(emptyMap())
    private val popularStatus = MutableStateFlow<Map<MediaType, CandidateRefreshStatus>>(emptyMap())
    private val popular = MutableStateFlow<Map<MediaType, List<MetadataSearchResult>>>(emptyMap())

    private val rankedFeed = combine(
        mediaRepository.observeRecommendationHistory(),
        recommendationDao.observeUnseenCandidates(),
        recommendationDao.observeFeedback(),
    ) { history, candidates, feedback ->
        val profile = RecommendationEngine.buildTasteProfile(history)
        val feedbackByKey = feedback.asMap()
        val ranked = RecommendationEngine.rankCandidates(profile, candidates.map { it.toDomain() }, feedbackByKey)
        val feed = RecommendationFeed(
            profile = profile,
            recommendations = RecommendationEngine.diversify(ranked, limit = 12),
            byType = ranked.groupBy { it.candidate.type }.mapValues { (_, items) ->
                RecommendationEngine.diversify(items, limit = 12)
            },
        )
        feed to feedbackByKey
    }

    private val feed = combine(rankedFeed, popular, personalStatus, popularStatus) { ranked, popularItems, personal, popularState ->
        val (base, feedback) = ranked
        base.copy(
            popularByType = popularItems.mapValues { (_, items) ->
                items.filter { RecommendationEngine.isDiscoverable(base.profile, it, feedback) }
            },
            statusByType = (personal.keys + popularState.keys).associateWith { type ->
                val fallback = popularState[type].takeIf { base.byType[type].orEmpty().size < DISCOVER_TARGET }
                CandidateRefreshStatus(
                    loading = personal[type]?.loading == true || fallback?.loading == true,
                    issue = fallback?.issue ?: personal[type]?.issue,
                )
            },
        )
    }.shareIn(scope, SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0), replay = 1)

    override fun observeFeed(): Flow<RecommendationFeed> = feed

    override suspend fun refreshCandidates(force: Boolean, type: MediaType?, includePopular: Boolean) {
        val snapshot = feed.first()
        coroutineScope {
            (type?.let(::listOf) ?: MediaType.entries).forEach { category ->
                launch {
                    val count = personalRequests.run(category) { refreshPersonal(category, force, snapshot) }
                    if (includePopular && category != MediaType.BOOK && count < DISCOVER_TARGET) {
                        popularRequests.run(category) { refreshPopular(category, force) }
                    }
                }
            }
        }
    }

    private suspend fun refreshPersonal(type: MediaType, force: Boolean, snapshot: RecommendationFeed): Int {
        val cachedCount = snapshot.byType[type].orEmpty().size
        if (!snapshot.profile.isReady) return cachedCount
        val now = Instant.now()
        if (!force && (isRecentAttempt(personalAttempts[type], now) ||
            !RecommendationCachePolicy.shouldRefresh(recommendationDao.latestCandidateFetchAt(type), now))
        ) return cachedCount

        personalAttempts[type] = now
        personalStatus.set(type, CandidateRefreshStatus(loading = true))
        var issue: CandidateIssue? = null
        try {
            val profile = snapshot.profile
            val genres = profile.genreWeights.entries.filter { it.value > 0 }.sortedByDescending { it.value }.map { it.key }
            val feedback = recommendationDao.observeFeedback().first().asMap()
            val first = metadataRepository.recommendationCandidates(type, genres, profile.anchors)
            issue = first.issue
            val items = first.items.toMutableList()
            val existingKeys = items.mapTo(mutableSetOf()) { it.recommendationKey() }
            val firstPageCount = RecommendationEngine.rankCandidates(profile, items, feedback).size
            var eligibleCount = firstPageCount
            if (type != MediaType.BOOK && firstPageCount < DISCOVER_TARGET) {
                first.loadMore?.invoke()?.let { second ->
                    issue = second.issue ?: issue
                    val additional = second.items.filter { existingKeys.add(it.recommendationKey()) }
                    items += additional
                    eligibleCount += RecommendationEngine.rankCandidates(profile, additional, feedback).size
                }
            }
            if (items.isNotEmpty()) {
                recommendationDao.upsertCandidates(items.map { it.toEntity(now) })
                if (issue == null) recommendationDao.deleteCandidatesOlderThan(type, now.minus(RecommendationCachePolicy.retentionWindow))
            }
            return if (items.isEmpty() || issue != null) maxOf(cachedCount, eligibleCount) else eligibleCount
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            issue = CandidateIssue.UNAVAILABLE
            return cachedCount
        } finally {
            personalStatus.set(type, CandidateRefreshStatus(issue = issue))
        }
    }

    private suspend fun refreshPopular(type: MediaType, force: Boolean) {
        val now = Instant.now()
        if (!force && (isRecentAttempt(popularAttempts[type], now) ||
            !RecommendationCachePolicy.shouldRefresh(popularUpdatedAt[type], now))
        ) return
        popularAttempts[type] = now
        popularStatus.set(type, CandidateRefreshStatus(loading = true))
        var issue: CandidateIssue? = null
        try {
            val batch = metadataRepository.popularCandidates(type)
            issue = batch.issue
            if (issue == null) {
                val snapshot = feed.first()
                val personalKeys = snapshot.byType[type].orEmpty().mapTo(mutableSetOf()) { it.candidate.recommendationKey() }
                val feedback = recommendationDao.observeFeedback().first().asMap()
                val eligible = batch.items.count {
                    it.recommendationKey() !in personalKeys && RecommendationEngine.isDiscoverable(snapshot.profile, it, feedback)
                }
                val additional = if (eligible < DISCOVER_TARGET && batch.loadMore != null) batch.loadMore.invoke() else CandidateBatch()
                issue = additional.issue
                val combined = (batch.items + additional.items).distinctBy { it.recommendationKey() }
                if (combined.isNotEmpty() || issue == null) popular.update { it + (type to combined) }
                popularUpdatedAt[type] = now
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            issue = CandidateIssue.UNAVAILABLE
        } finally {
            popularStatus.set(type, CandidateRefreshStatus(issue = issue))
        }
    }

    private fun isRecentAttempt(attempt: Instant?, now: Instant): Boolean =
        attempt != null && attempt.plusSeconds(60).isAfter(now)

    private fun MutableStateFlow<Map<MediaType, CandidateRefreshStatus>>.set(type: MediaType, status: CandidateRefreshStatus) {
        update { it + (type to status) }
    }

    private fun List<RecommendationFeedbackEntity>.asMap(): Map<RecommendationKey, RecommendationFeedbackType> =
        associate { RecommendationKey(it.provider, it.externalId, it.mediaType) to it.feedbackType }

    override suspend fun setFeedback(key: RecommendationKey, feedback: RecommendationFeedbackType) {
        recommendationDao.upsertFeedback(
            RecommendationFeedbackEntity(
                id = UUID.randomUUID().toString(),
                provider = key.provider,
                externalId = key.externalId,
                mediaType = key.mediaType,
                feedbackType = feedback,
                createdAt = Instant.now(),
            ),
        )
    }

    private fun RecommendationCandidateEntity.toDomain(): MetadataSearchResult {
        val signals = decodeCandidateSignals(genresJson)
        return MetadataSearchResult(
            provider = provider,
            externalId = externalId,
            externalUrl = externalUrl,
            type = mediaType,
            title = title,
            originalTitle = originalTitle,
            description = description,
            releaseDate = releaseDate,
            releaseYear = releaseYear,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            creators = runCatching { json.decodeFromString<List<String>>(creatorsJson) }.getOrDefault(emptyList()),
            genres = signals.genres,
            runtimeMinutes = runtimeMinutes,
            pageCount = pageCount,
            seasonCount = seasonCount,
            episodeCount = episodeCount,
            externalRating = signals.externalRating,
            externalVoteCount = signals.externalVoteCount,
            popularity = signals.popularity,
            externalTags = signals.externalTags,
            sourceAnchorMediaIds = signals.sourceAnchorMediaIds,
            sourceAnchorTitles = signals.sourceAnchorTitles,
        )
    }

    private fun MetadataSearchResult.toEntity(fetchedAt: Instant) = RecommendationCandidateEntity(
        provider = provider,
        externalId = externalId,
        mediaType = type,
        externalUrl = externalUrl,
        title = title,
        originalTitle = originalTitle,
        description = description,
        releaseDate = releaseDate,
        releaseYear = releaseYear,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        creatorsJson = json.encodeToString(creators),
        genresJson = json.encodeToString(
            CachedCandidateSignals(
                genres = genres,
                externalRating = externalRating,
                externalVoteCount = externalVoteCount,
                popularity = popularity,
                externalTags = externalTags,
                sourceAnchorMediaIds = sourceAnchorMediaIds,
                sourceAnchorTitles = sourceAnchorTitles,
            ),
        ),
        runtimeMinutes = runtimeMinutes,
        pageCount = pageCount,
        seasonCount = seasonCount,
        episodeCount = episodeCount,
        fetchedAt = fetchedAt,
    )

    private fun decodeCandidateSignals(payload: String): CachedCandidateSignals =
        runCatching { json.decodeFromString<CachedCandidateSignals>(payload) }
            .getOrElse {
                CachedCandidateSignals(genres = runCatching { json.decodeFromString<List<String>>(payload) }.getOrDefault(emptyList()))
            }
}

@Serializable
private data class CachedCandidateSignals(
    val genres: List<String> = emptyList(),
    val externalRating: Double? = null,
    val externalVoteCount: Int? = null,
    val popularity: Double? = null,
    val externalTags: List<String> = emptyList(),
    val sourceAnchorMediaIds: List<String> = emptyList(),
    val sourceAnchorTitles: List<String> = emptyList(),
)

/** Application-scoped requests survive changes of destination and can be awaited by multiple callers. */
private class CategoryRequests<T>(private val scope: CoroutineScope) {
    private val active = mutableMapOf<MediaType, Deferred<T>>()

    suspend fun run(type: MediaType, block: suspend () -> T): T {
        val ongoing = synchronized(active) {
            active[type]?.takeUnless { it.isCompleted } ?: scope.async(start = CoroutineStart.LAZY) { block() }.also { task ->
                active[type] = task
                task.invokeOnCompletion {
                    synchronized(active) { if (active[type] === task) active.remove(type) }
                }
            }
        }
        return ongoing.await()
    }
}
