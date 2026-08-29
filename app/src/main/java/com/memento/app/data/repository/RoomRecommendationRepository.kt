package com.memento.app.data.repository

import com.memento.app.data.local.dao.ConsumptionDao
import com.memento.app.data.local.dao.MediaDao
import com.memento.app.data.local.dao.RecommendationDao
import com.memento.app.data.local.entity.RecommendationCandidateEntity
import com.memento.app.data.local.entity.RecommendationFeedbackEntity
import com.memento.app.data.mapper.toDomain
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.RecommendationFeedbackType
import com.memento.app.domain.recommendation.RecommendationEngine
import com.memento.app.domain.recommendation.RecommendationKey
import com.memento.app.domain.repository.MetadataRepository
import com.memento.app.domain.repository.RecommendationFeed
import com.memento.app.domain.repository.RecommendationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class RoomRecommendationRepository @Inject constructor(
    private val mediaDao: MediaDao,
    private val consumptionDao: ConsumptionDao,
    private val recommendationDao: RecommendationDao,
    private val metadataRepository: MetadataRepository,
) : RecommendationRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val history: Flow<List<MediaDetail>> = mediaDao.observeAll().mapLatest(::loadHistory)

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

    override suspend fun refreshCandidates() {
        val profile = RecommendationEngine.buildTasteProfile(loadHistory(mediaDao.observeAll().first()))
        if (!profile.isReady) return
        val genres = profile.genreWeights.filterValues { it > 0 }.entries.sortedByDescending { it.value }.map { it.key }.take(3)
        val creators = profile.creatorWeights.filterValues { it > 0 }.entries.sortedByDescending { it.value }.map { it.key }.take(3)
        val preferredTypes = profile.typeWeights.filterValues { it > 0 }.keys
        val candidates = coroutineScope {
            preferredTypes.map { type ->
                async { metadataRepository.recommendationCandidates(type, genres, creators) }
            }.flatMap { it.await() }
        }.distinctBy { Triple(it.provider, it.externalId, it.type) }
        if (candidates.isNotEmpty()) recommendationDao.upsertCandidates(candidates.map { it.toEntity() })
        recommendationDao.deleteCandidatesOlderThan(Instant.now().minus(90, ChronoUnit.DAYS))
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

    private suspend fun loadHistory(items: List<com.memento.app.data.local.entity.MediaItemEntity>): List<MediaDetail> =
        items.map { media ->
            MediaDetail(
                media = media.toDomain(),
                creators = mediaDao.getCreatorNames(media.id),
                genres = mediaDao.getGenreNames(media.id),
                consumptions = consumptionDao.getForMedia(media.id).map { it.toDomain() },
                progress = emptyList(),
                reflections = emptyList(),
            )
        }

    private fun RecommendationCandidateEntity.toDomain() = MetadataSearchResult(
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
        genres = runCatching { json.decodeFromString<List<String>>(genresJson) }.getOrDefault(emptyList()),
        runtimeMinutes = runtimeMinutes,
        pageCount = pageCount,
        seasonCount = seasonCount,
        episodeCount = episodeCount,
    )

    private fun MetadataSearchResult.toEntity() = RecommendationCandidateEntity(
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
        genresJson = json.encodeToString(genres),
        runtimeMinutes = runtimeMinutes,
        pageCount = pageCount,
        seasonCount = seasonCount,
        episodeCount = episodeCount,
        fetchedAt = Instant.now(),
    )
}
