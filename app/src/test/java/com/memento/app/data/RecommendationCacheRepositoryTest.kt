package com.memento.app.data

import com.memento.app.FakeMediaRepository
import com.memento.app.FakeMetadataRepository
import com.memento.app.data.local.dao.RecommendationDao
import com.memento.app.data.local.entity.RecommendationCandidateEntity
import com.memento.app.data.local.entity.RecommendationFeedbackEntity
import com.memento.app.data.repository.RoomRecommendationRepository
import com.memento.app.domain.model.*
import com.memento.app.domain.repository.CandidateBatch
import com.memento.app.domain.repository.CandidateIssue
import com.memento.app.ui.discover.toDiscoverState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationCacheRepositoryTest {
    @Test fun `fresh cache performs no remote calls`() = runTest {
        val dao = FakeRecommendationDao(latest = Instant.now())
        val metadata = FakeMetadataRepository()
        val repository = RoomRecommendationRepository(readyHistory(), dao, metadata, backgroundScope)

        repository.refreshCandidates()

        assertEquals(0, metadata.recommendationRequests)
    }

    @Test fun `expired cache refreshes while remote failure keeps stale rows`() = runTest {
        val stale = candidateEntity(Instant.now().minusSeconds(3 * 24 * 60 * 60))
        val dao = FakeRecommendationDao(latest = stale.fetchedAt).apply { candidates.value = listOf(stale) }
        val metadata = FakeMetadataRepository()
        val repository = RoomRecommendationRepository(readyHistory(), dao, metadata, backgroundScope)

        repository.refreshCandidates()

        assertTrue(metadata.recommendationRequests > 0)
        assertEquals(listOf(stale), dao.candidates.value)
        assertEquals(0, dao.deleteCalls)
    }

    @Test fun `missing cache and remote failure completes without persisting`() = runTest {
        val dao = FakeRecommendationDao(latest = null)
        val metadata = FakeMetadataRepository()
        RoomRecommendationRepository(readyHistory(), dao, metadata, backgroundScope).refreshCandidates()

        assertTrue(metadata.recommendationRequests > 0)
        assertTrue(dao.candidates.value.isEmpty())
    }

    @Test fun `legacy cached candidate remains rankable offline`() = runTest {
        val cached = candidateEntity(Instant.now())
        val dao = FakeRecommendationDao(latest = cached.fetchedAt).apply { candidates.value = listOf(cached) }
        val repository = RoomRecommendationRepository(readyHistory(), dao, FakeMetadataRepository(), backgroundScope)

        val feed = repository.observeFeed().first()

        assertEquals("OL1W", feed.recommendations.single().candidate.externalId)
    }

    @Test fun `fresh books do not block movies series or games`() = runTest {
        val dao = FakeRecommendationDao(latest = null).apply { latestByType[MediaType.BOOK] = Instant.now() }
        val metadata = FakeMetadataRepository()
        RoomRecommendationRepository(readyHistory(), dao, metadata, backgroundScope).refreshCandidates()
        assertEquals(setOf(MediaType.MOVIE, MediaType.SERIES, MediaType.GAME), metadata.requestedRecommendationTypes.toSet())
    }

    @Test fun `category feed keeps all six matches when mixed top twelve has only one`() = runTest {
        for (type in listOf(MediaType.MOVIE, MediaType.SERIES, MediaType.GAME)) {
            val dao = FakeRecommendationDao(null)
            val history = readyHistory().apply {
                val base = allDetails.value.first()
                allDetails.value += base.copy(
                    media = base.media.copy(id = "t1", type = type, title = "Otra obra A"),
                    creators = emptyList(),
                )
                allDetails.value += base.copy(
                    media = base.media.copy(id = "t2", type = type, title = "Otra obra B"),
                    creators = emptyList(),
                )
            }
            val now = Instant.now()
            fun row(media: MediaType, id: String, strong: Boolean, anchors: String): RecommendationCandidateEntity =
                candidateEntity(now).copy(
                    provider = when (media) {
                        MediaType.BOOK -> MetadataProvider.OPEN_LIBRARY
                        MediaType.GAME -> MetadataProvider.RAWG
                        else -> MetadataProvider.TMDB
                    },
                    externalId = id, mediaType = media, title = "Candidata $id",
                    creatorsJson = if (strong) """["Autora"]""" else "[]",
                    genresJson = """{"genres":${if (strong) """["Ficción","Drama"]""" else """["Drama"]"""},
                        "externalRating":8.5,"externalVoteCount":5000,"sourceAnchorMediaIds":$anchors}""",
                )
            dao.candidates.value =
                List(14) { row(MediaType.BOOK, "book$it", true, """["m1"]""") } +
                List(3) { row(MediaType.BOOK, "exploration$it", false, "[]") } +
                row(type, "strong", true, """["t1","t2"]""") +
                List(5) { row(type, "eligible$it", false, "[]") }

            val feed = RoomRecommendationRepository(history, dao, FakeMetadataRepository(), backgroundScope)
                .observeFeed().first()
            assertEquals(12, feed.recommendations.size)
            assertEquals(1, feed.recommendations.count { it.candidate.type == type })
            assertEquals(6, feed.toDiscoverState(type).recommendations.size)
        }
    }

    @Test fun `sparse recommendations page once and partial failure keeps stale candidates`() = runTest {
        val stale = candidateEntity(Instant.now().minusSeconds(9 * 24 * 60 * 60))
            .copy(provider = MetadataProvider.TMDB, mediaType = MediaType.MOVIE)
        val dao = FakeRecommendationDao(latest = null).apply { candidates.value = listOf(stale) }
        var pages = 0
        val movie = MetadataSearchResult(MetadataProvider.TMDB, "movie", null, MediaType.MOVIE, "Otra película",
            genres = listOf("Ficción", "Drama"), externalVoteCount = 1_000, externalRating = 8.0)
        val metadata = FakeMetadataRepository().apply {
            recommendationHandler = {
                CandidateBatch(listOf(movie), loadMore = {
                    pages++
                    CandidateBatch(issue = CandidateIssue.RATE_LIMITED)
                })
            }
            popularHandler = {
                CandidateBatch(listOf(movie.copy(externalId = "popular", title = "Selección")))
            }
        }
        val repository = RoomRecommendationRepository(readyHistory(), dao, metadata, backgroundScope)
        repository.refreshCandidates(type = MediaType.MOVIE, includePopular = true)
        val feed = repository.observeFeed().first { it.popularByType[MediaType.MOVIE]?.isNotEmpty() == true }
        assertEquals(1, pages)
        assertEquals(0, dao.deleteCalls)
        assertTrue(stale in dao.candidates.value)
        assertEquals(CandidateIssue.RATE_LIMITED, feed.statusByType[MediaType.MOVIE]?.issue)
        assertEquals("movie", feed.byType[MediaType.MOVIE]?.single()?.candidate?.externalId)
        assertEquals("popular", feed.toDiscoverState(MediaType.MOVIE).popular.single().externalId)
    }

    @Test fun `two callers reuse a pending request for the same category`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val metadata = FakeMetadataRepository().apply {
            recommendationHandler = {
                started.complete(Unit)
                gate.await()
                CandidateBatch()
            }
        }
        val repository = RoomRecommendationRepository(readyHistory(), FakeRecommendationDao(null), metadata, backgroundScope)
        val home = async { repository.refreshCandidates(force = true, type = MediaType.SERIES) }
        started.await()
        val discover = async { repository.refreshCandidates(force = true, type = MediaType.SERIES, includePopular = true) }
        runCurrent()
        gate.complete(Unit)
        home.await()
        discover.await()
        assertEquals(listOf(MediaType.SERIES), metadata.requestedRecommendationTypes)
    }

    private fun readyHistory() = FakeMediaRepository().apply {
        allDetails.value = (1..3).map { index ->
            val media = MediaItem("m$index", MediaType.BOOK, "Libro $index", createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)
            MediaDetail(
                media, listOf("Autora"), listOf("Ficción", "Drama"),
                listOf(Consumption("c$index", media.id, ConsumptionStatus.COMPLETED, ratingHalfStars = 9, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)),
                emptyList(), emptyList(),
            )
        }
    }

    private fun candidateEntity(fetchedAt: Instant) = RecommendationCandidateEntity(
        MetadataProvider.OPEN_LIBRARY, "OL1W", MediaType.BOOK, null, "Candidato", null, null, null, null,
        null, null, "[]", "[\"Ficción\"]", null, null, null, null, fetchedAt,
    )
}

private class FakeRecommendationDao(var latest: Instant?) : RecommendationDao {
    val candidates = MutableStateFlow<List<RecommendationCandidateEntity>>(emptyList())
    private val feedback = MutableStateFlow<List<RecommendationFeedbackEntity>>(emptyList())
    var deleteCalls = 0
    val latestByType = mutableMapOf<MediaType, Instant>()
    override fun observeUnseenCandidates(): Flow<List<RecommendationCandidateEntity>> = candidates
    override fun observeFeedback(): Flow<List<RecommendationFeedbackEntity>> = feedback
    override suspend fun upsertCandidates(candidates: List<RecommendationCandidateEntity>) {
        this.candidates.value = (this.candidates.value + candidates).associateBy {
            Triple(it.provider, it.externalId, it.mediaType)
        }.values.toList()
        candidates.groupBy { it.mediaType }.forEach { (type, items) ->
            latestByType[type] = items.maxOf { it.fetchedAt }
        }
    }
    override suspend fun upsertFeedback(feedback: RecommendationFeedbackEntity) { this.feedback.value = listOf(feedback) }
    override suspend fun latestCandidateFetchAt(type: MediaType): Instant? = latestByType[type] ?: latest
    override suspend fun deleteCandidatesOlderThan(type: MediaType, threshold: Instant) { deleteCalls++ }
}
