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
import kotlinx.coroutines.async
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
class RoomRecommendationRepository @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val recommendationDao: RecommendationDao,
    private val metadataRepository: MetadataRepository,
) : RecommendationRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val history: Flow<List<MediaDetail>> = mediaRepository.observeAllDetails()

    override fun observeFeed(): Flow<RecommendationFeed> = combine(
        history,
        recommendationDao.observeUnseenCandidates(),
        recommendationDao.observeFeedback(),
    ) { history, candidates, feedback ->
        val profile = RecommendationEngine.buildTasteProfile(history)
        val feedbackMap = feedback.associate { entity ->
            RecommendationKey(entity.provider, entity.externalId, entity.mediaType) to entity.feedbackType
        }
        RecommendationFeed(
            profile = profile,
            recommendations = RecommendationEngine.recommend(
                profile,
                candidates.map { it.toDomain() },
                feedbackMap,
            ),
        )
    }

    override suspend fun refreshCandidates(force: Boolean) {
        val now = Instant.now()
        if (!force && !RecommendationCachePolicy.shouldRefresh(recommendationDao.latestCandidateFetchAt(), now)) return
        val profile = RecommendationEngine.buildTasteProfile(history.first())
        if (!profile.isReady) return
        val genres = profile.genreWeights.filterValues { it > 0 }.entries.sortedByDescending { it.value }.map { it.key }.take(3)
        val creators = profile.creatorWeights.filterValues { it > 0 }.entries.sortedByDescending { it.value }.map { it.key }.take(3)
        val candidates = coroutineScope {
            MediaType.entries.map { type ->
                async {
                    runCatching { metadataRepository.recommendationCandidates(type, genres, creators, profile.anchors) }
                        .getOrDefault(emptyList())
                }
            }.flatMap { it.await() }
        }.distinctBy { Triple(it.provider, it.externalId, it.type) }
        if (candidates.isNotEmpty()) {
            recommendationDao.upsertCandidates(candidates.map { it.toEntity(now) })
            recommendationDao.deleteCandidatesOlderThan(now.minus(RecommendationCachePolicy.retentionWindow))
        }
    }

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
