package com.memento.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.memento.app.data.local.database.MementoDatabase
import com.memento.app.data.local.entity.ConsumptionEntity
import com.memento.app.data.local.entity.MediaItemEntity
import com.memento.app.data.local.entity.ProgressEntryEntity
import com.memento.app.data.local.entity.ReflectionEntity
import com.memento.app.data.repository.RoomCulturalTimelineRepository
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.TimelineEventType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CulturalTimelineRepositoryRoomTest {
    @Test
    fun timelineMergesSourcesOrdersEventsAndKeepsReconsumptions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            val mediaDao = database.mediaDao()
            val consumptionDao = database.consumptionDao()
            mediaDao.insert(media("book", MediaType.BOOK, "Dune"))
            mediaDao.insert(media("movie", MediaType.MOVIE, "Arrival"))
            mediaDao.insert(media("game", MediaType.GAME, "Clair Obscur"))

            consumptionDao.insert(consumption("book-1", "book", "2024-01-01", "2024-02-01", "2024-01-01T10:00:00Z"))
            consumptionDao.insert(consumption("book-2", "book", "2026-08-20", null, "2026-08-20T10:00:00Z"))
            consumptionDao.insert(consumption("movie-1", "movie", "2026-08-01", "2026-08-25", "2026-08-01T10:00:00Z", 9))
            consumptionDao.insert(consumption("game-1", "game", "2026-08-10", null, "2026-08-10T10:00:00Z"))
            consumptionDao.insertProgress(
                ProgressEntryEntity("progress-1", "game-1", ProgressType.HOURS, 32.0, null, null, null, Instant.parse("2026-08-29T18:00:00Z")),
            )
            consumptionDao.insertReflection(
                ReflectionEntity(
                    "reflection-1",
                    "movie-1",
                    ReflectionType.FINAL_REFLECTION,
                    "El lenguaje también modifica la memoria.",
                    Instant.parse("2026-08-25T20:00:00Z"),
                    Instant.parse("2026-08-25T20:00:00Z"),
                ),
            )
            val repository = RoomCulturalTimelineRepository(database.timelineDao())

            val window = repository.observeWindow(null, 20).first()

            assertEquals(TimelineEventType.PROGRESS, window.events[0].eventType)
            assertEquals("game", window.events[0].mediaItemId)
            assertEquals(TimelineEventType.FINAL_REFLECTION, window.events[1].eventType)
            assertEquals(TimelineEventType.COMPLETED, window.events[2].eventType)
            assertEquals(9, window.events[2].ratingHalfStars)
            assertTrue(window.events.first { it.id == "started:book-2" }.isReconsumption)
            assertTrue(window.events.any { it.id == "started:book-1" })
            assertEquals(2, window.events.count { it.mediaItemId == "book" && it.eventType == TimelineEventType.STARTED })
            assertTrue(!window.hasMore)

            val gameWindow = repository.observeWindow(MediaType.GAME, 1).first()
            assertEquals(MediaType.GAME, gameWindow.events.single().mediaType)
            assertTrue(gameWindow.hasMore)
        } finally {
            database.close()
        }
    }

    @Test
    fun retrospectiveConsumptionUsesHistoricalDatesForReconsumption() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            database.mediaDao().insert(media("book", MediaType.BOOK, "Dune"))
            val consumptionDao = database.consumptionDao()
            consumptionDao.insert(
                consumption("book-2025", "book", "2025-08-30", "2025-09-10", "2025-09-10T10:00:00Z"),
            )
            consumptionDao.insert(
                consumption("book-2021", "book", "2021-08-30", "2021-09-10", "2026-08-30T10:00:00Z"),
            )

            val events = RoomCulturalTimelineRepository(database.timelineDao()).observeWindow(null, 20).first().events

            assertTrue(events.filter { it.consumptionId == "book-2021" }.all { !it.isReconsumption })
            assertTrue(events.filter { it.consumptionId == "book-2025" }.all { it.isReconsumption })
        } finally {
            database.close()
        }
    }

    @Test
    fun onThisDayQueriesOnlyPriorMatchingDatesAndAppliesPriority() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            database.mediaDao().insert(media("book", MediaType.BOOK, "Dune"))
            val consumptionDao = database.consumptionDao()
            consumptionDao.insert(consumption("matching", "book", "2024-08-01", "2024-08-30", "2024-08-01T10:00:00Z"))
            consumptionDao.insert(consumption("current", "book", "2026-08-01", "2026-08-30", "2026-08-01T10:00:00Z"))
            consumptionDao.insert(consumption("different", "book", "2025-08-01", "2025-08-29", "2025-08-01T10:00:00Z"))
            consumptionDao.insertProgress(
                ProgressEntryEntity(
                    "matching-progress",
                    "matching",
                    ProgressType.PAGES,
                    200.0,
                    400.0,
                    null,
                    null,
                    Instant.parse("2025-08-30T12:00:00Z"),
                ),
            )
            consumptionDao.insertReflection(
                ReflectionEntity(
                    "matching-reflection",
                    "matching",
                    ReflectionType.FINAL_REFLECTION,
                    "Una idea que permanece.",
                    Instant.parse("2022-08-30T12:00:00Z"),
                    Instant.parse("2022-08-30T12:00:00Z"),
                ),
            )

            val events = RoomCulturalTimelineRepository(database.timelineDao())
                .observeOnThisDay(LocalDate.of(2026, 8, 30))
                .first()

            assertEquals(
                listOf("reflection:matching-reflection", "completed:matching", "progress:matching-progress"),
                events.map { it.id },
            )
        } finally {
            database.close()
        }
    }

    private fun media(id: String, type: MediaType, title: String) = MediaItemEntity(
        id = id,
        type = type,
        title = title,
        originalTitle = null,
        description = null,
        releaseDate = null,
        releaseYear = null,
        posterUrl = null,
        backdropUrl = null,
        isFavorite = id == "movie",
        isManual = true,
        runtimeMinutes = null,
        pageCount = null,
        seasonCount = null,
        episodeCount = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun consumption(
        id: String,
        mediaId: String,
        started: String,
        completed: String?,
        createdAt: String,
        rating: Int? = null,
    ): ConsumptionEntity {
        val timestamp = Instant.parse(createdAt)
        return ConsumptionEntity(
            id = id,
            mediaItemId = mediaId,
            status = if (completed == null) ConsumptionStatus.IN_PROGRESS else ConsumptionStatus.COMPLETED,
            startedDate = LocalDate.parse(started),
            completedDate = completed?.let(LocalDate::parse),
            ratingHalfStars = rating,
            createdAt = timestamp,
            updatedAt = timestamp,
        )
    }
}
