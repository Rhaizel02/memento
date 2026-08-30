package com.memento.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.memento.app.data.local.database.MementoDatabase
import com.memento.app.data.local.database.HARDENING_DATABASE_CALLBACK
import com.memento.app.data.local.entity.ConsumptionEntity
import com.memento.app.data.repository.RoomMediaRepository
import com.memento.app.data.repository.RoomRememberRepository
import com.memento.app.domain.model.AddMediaInput
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.CompletedMediaInput
import com.memento.app.domain.model.CreatorRole
import com.memento.app.domain.model.EditMediaInput
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.LibraryFilters
import com.memento.app.domain.model.TimelineEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class MediaRepositoryRoomTest {
    @Test
    fun personalTagsDeduplicateAttachAcrossWorksFilterAndRemoveOnlyRelation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            val repository = RoomMediaRepository(database, database.mediaDao(), database.consumptionDao())
            val firstMedia = repository.addManual(AddMediaInput(MediaType.BOOK, "Uno"), ConsumptionStatus.PLANNED)
            val secondMedia = repository.addManual(AddMediaInput(MediaType.MOVIE, "Dos"), ConsumptionStatus.PLANNED)
            val thirdMedia = repository.addManual(AddMediaInput(MediaType.GAME, "Tres"), ConsumptionStatus.PLANNED)

            val firstTag = repository.createAndAttachTag(firstMedia, "  Para releer  ")
            val sameTag = repository.createAndAttachTag(secondMedia, "PARA RELEER")
            repository.attachTag(thirdMedia, firstTag.id)

            assertEquals(firstTag.id, sameTag.id)
            assertEquals("Para releer", sameTag.name)
            assertEquals(listOf(firstTag.id), repository.observeMediaDetail(firstMedia).first { it?.tags?.isNotEmpty() == true }!!.tags.map { it.id })
            assertEquals(
                setOf(firstMedia, secondMedia, thirdMedia),
                repository.observeLibrary(filters = LibraryFilters(tagIds = setOf(firstTag.id))).first { it.size == 3 }.map { it.id }.toSet(),
            )

            repository.removeTag(firstMedia, firstTag.id)

            assertTrue(repository.observeMediaDetail(firstMedia).first { it != null }!!.tags.isEmpty())
            assertEquals(listOf(firstTag.id), repository.observeMediaDetail(secondMedia).first { it?.tags?.isNotEmpty() == true }!!.tags.map { it.id })
            assertEquals(listOf(firstTag.id), repository.observeTags().first().map { it.id })
            assertEquals(
                setOf(secondMedia, thirdMedia),
                repository.observeLibrary(filters = LibraryFilters(tagIds = setOf(firstTag.id))).first { it.size == 2 }.map { it.id }.toSet(),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun quotesTrimRejectBlankAndAppearAsReflectionTimelineEvents() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            val repository = RoomMediaRepository(database, database.mediaDao(), database.consumptionDao())
            val mediaId = repository.addManual(AddMediaInput(MediaType.BOOK, "Citas"), ConsumptionStatus.IN_PROGRESS)
            val consumptionId = repository.observeMediaDetail(mediaId).first { it != null }!!.activeConsumption!!.id

            repository.saveReflection(consumptionId, ReflectionType.QUOTE, "  El tiempo revela.  ")
            val blankRejected = runCatching {
                repository.saveReflection(consumptionId, ReflectionType.QUOTE, "   ")
            }.isFailure
            val detail = repository.observeMediaDetail(mediaId).first { it?.reflections?.isNotEmpty() == true }!!
            val timeline = repository.observeTimeline(mediaId).first { it.isNotEmpty() }

            assertTrue(blankRejected)
            assertEquals("El tiempo revela.", detail.reflections.single().content)
            assertEquals(ReflectionType.QUOTE, detail.reflections.single().type)
            assertTrue(timeline.any { it is TimelineEvent.ReflectionWritten && it.reflection.type == ReflectionType.QUOTE })
        } finally {
            database.close()
        }
    }

    @Test
    fun completedAddRequiresExplicitCompletionDataBeforeAnyInsert() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            val repository = RoomMediaRepository(database, database.mediaDao(), database.consumptionDao())

            val rejected = runCatching {
                repository.addManual(AddMediaInput(MediaType.BOOK, "Incompleta"), ConsumptionStatus.COMPLETED)
            }.isFailure

            assertTrue(rejected)
            assertTrue(database.backupDao().mediaItems().isEmpty())
            assertTrue(database.backupDao().consumptions().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun completedManualAddPersistsHistoricalDataAtomically() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            val repository = RoomMediaRepository(database, database.mediaDao(), database.consumptionDao())
            val mediaId = repository.addManual(
                AddMediaInput(MediaType.MOVIE, "Vista hace años"),
                ConsumptionStatus.COMPLETED,
                CompletedMediaInput(
                    completedDate = LocalDate.of(2022, 4, 12),
                    ratingHalfStars = 9,
                    favorite = true,
                    finalReflection = "  Sigue conmigo  ",
                ),
            )

            val detail = repository.observeMediaDetail(mediaId).first { it != null }!!

            assertTrue(detail.media.isFavorite)
            assertEquals(1, detail.consumptions.size)
            assertEquals(ConsumptionStatus.COMPLETED, detail.consumptions.single().status)
            assertEquals(LocalDate.of(2022, 4, 12), detail.consumptions.single().completedDate)
            assertEquals(9, detail.consumptions.single().ratingHalfStars)
            assertEquals(1, detail.reflections.size)
            assertEquals(ReflectionType.FINAL_REFLECTION, detail.reflections.single().type)
            assertEquals("Sigue conmigo", detail.reflections.single().content)
        } finally {
            database.close()
        }
    }

    @Test
    fun completedExternalAddAcceptsNoRatingAndDoesNotInsertBlankReflection() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            val repository = RoomMediaRepository(database, database.mediaDao(), database.consumptionDao())
            val mediaId = repository.addExternal(
                MetadataSearchResult(MetadataProvider.TMDB, "42", null, MediaType.MOVIE, "Obra"),
                ConsumptionStatus.COMPLETED,
                CompletedMediaInput(LocalDate.of(2018, 3, 4), finalReflection = "   "),
            ).mediaId

            val detail = repository.observeMediaDetail(mediaId).first { it != null }!!

            assertEquals(null, detail.consumptions.single().ratingHalfStars)
            assertEquals(LocalDate.of(2018, 3, 4), detail.consumptions.single().completedDate)
            assertTrue(detail.reflections.isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun externalCreatorsAreStoredWithTheRoleForTheirMediaType() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            val repository = RoomMediaRepository(database, database.mediaDao(), database.consumptionDao())
            val expectedRoles = mapOf(
                MediaType.BOOK to CreatorRole.AUTHOR,
                MediaType.MOVIE to CreatorRole.DIRECTOR,
                MediaType.SERIES to CreatorRole.CREATOR,
                MediaType.GAME to CreatorRole.DEVELOPER,
            )
            val mediaIds = expectedRoles.keys.associateWith { type ->
                repository.addExternal(
                    MetadataSearchResult(
                        provider = when (type) {
                            MediaType.BOOK -> MetadataProvider.OPEN_LIBRARY
                            MediaType.MOVIE, MediaType.SERIES -> MetadataProvider.TMDB
                            MediaType.GAME -> MetadataProvider.RAWG
                        },
                        externalId = "external-${type.name}",
                        externalUrl = null,
                        type = type,
                        title = "Obra ${type.name}",
                        creators = listOf("Creador ${type.name}"),
                    ),
                    ConsumptionStatus.PLANNED,
                ).mediaId
            }

            val storedRoles = database.backupDao().mediaCreators().associate { it.mediaItemId to it.role }

            expectedRoles.forEach { (type, role) -> assertEquals(role, storedRoles[mediaIds.getValue(type)]) }
        } finally {
            database.close()
        }
    }

    @Test
    fun rememberExposureIsInsertedAtMostOncePerLogicalDay() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            val mediaRepository = RoomMediaRepository(database, database.mediaDao(), database.consumptionDao())
            val mediaId = mediaRepository.addManual(
                AddMediaInput(MediaType.BOOK, "Memoria"),
                ConsumptionStatus.COMPLETED,
                CompletedMediaInput(LocalDate.now()),
            )
            val consumptionId = mediaRepository.observeMediaDetail(mediaId).first { it != null }!!.consumptions.single().id
            mediaRepository.saveReflection(consumptionId, ReflectionType.FINAL_REFLECTION, "Una idea")
            val rememberRepository = RoomRememberRepository(database.rememberDao())

            rememberRepository.recordExposure(consumptionId)
            rememberRepository.recordExposure(consumptionId)

            val dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            assertEquals(1, database.rememberDao().exposureCountSince(consumptionId, dayStart))
        } finally {
            database.close()
        }
    }

    @Test
    fun plannedThenStartReusesOneActiveConsumptionAndDatabaseRejectsASecond() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java)
            .addCallback(HARDENING_DATABASE_CALLBACK)
            .build()
        try {
            val repository = RoomMediaRepository(database, database.mediaDao(), database.consumptionDao())
            val mediaId = repository.addManual(AddMediaInput(MediaType.MOVIE, "Obra"), ConsumptionStatus.PLANNED)
            val firstId = repository.startConsumption(mediaId)
            val secondId = repository.startConsumption(mediaId)
            val detail = repository.observeMediaDetail(mediaId).first { it != null }!!

            assertEquals(firstId, secondId)
            assertEquals(1, detail.consumptions.count { it.status == ConsumptionStatus.PLANNED || it.status == ConsumptionStatus.IN_PROGRESS })

            val now = Instant.now()
            val rejected = runCatching {
                database.consumptionDao().insert(
                    ConsumptionEntity("illegal", mediaId, ConsumptionStatus.PLANNED, null, null, null, now, now),
                )
            }.isFailure
            assertTrue(rejected)
        } finally {
            database.close()
        }
    }

    @Test
    fun editMetadataAndDeleteConsumptionPreserveWorkButCascadePersonalEntries() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            val repository = RoomMediaRepository(database, database.mediaDao(), database.consumptionDao())
            val mediaId = repository.addManual(
                AddMediaInput(MediaType.BOOK, "Título inicial", 2020, "Autora inicial", pageCount = 300),
                ConsumptionStatus.PLANNED,
            )
            val consumptionId = repository.startConsumption(mediaId)
            repository.addProgress(consumptionId, ProgressType.PAGES, 120.0, 300.0)
            repository.saveReflection(consumptionId, ReflectionType.NOTE, "Una idea")

            repository.updateMedia(
                mediaId,
                EditMediaInput("Título propio", 2021, "Descripción propia", listOf("Autora A", "Autor B"), "https://image"),
            )
            val edited = repository.observeMediaDetail(mediaId).first { it?.media?.title == "Título propio" }!!

            assertEquals(listOf("Autor B", "Autora A"), edited.creators)
            assertEquals("Descripción propia", edited.media.description)
            assertEquals(1, edited.progress.size)
            assertEquals(1, edited.reflections.size)

            repository.deleteConsumption(consumptionId)
            val afterDelete = repository.observeMediaDetail(mediaId).first { it?.consumptions?.isEmpty() == true }!!

            assertTrue(afterDelete.consumptions.isEmpty())
            assertTrue(afterDelete.progress.isEmpty())
            assertTrue(afterDelete.reflections.isEmpty())
            assertEquals("Título propio", afterDelete.media.title)
        } finally {
            database.close()
        }
    }

    @Test
    fun quickCaptureRejectsProgressAndNotesAfterConsumptionStopsBeingActive() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            val repository = RoomMediaRepository(database, database.mediaDao(), database.consumptionDao())
            val mediaId = repository.addManual(
                AddMediaInput(MediaType.BOOK, "Consumo concurrente", pageCount = 300),
                ConsumptionStatus.IN_PROGRESS,
            )
            val consumptionId = repository.observeMediaDetail(mediaId).first { it != null }!!.activeConsumption!!.id
            repository.completeConsumption(mediaId, LocalDate.of(2026, 8, 30))

            val progressRejected = runCatching {
                repository.addProgress(consumptionId, ProgressType.PAGES, 120.0, 300.0)
            }.isFailure
            val noteRejected = runCatching {
                repository.saveReflection(consumptionId, ReflectionType.NOTE, "Ya no debe guardarse")
            }.isFailure
            val detail = repository.observeMediaDetail(mediaId).first { it?.activeConsumption == null }!!

            assertTrue(progressRejected)
            assertTrue(noteRejected)
            assertTrue(detail.progress.isEmpty())
            assertTrue(detail.reflections.isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun homeProjectionReturnsRichRowsWithoutPerCardQueries() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            val repository = RoomMediaRepository(database, database.mediaDao(), database.consumptionDao())
            val inProgressId = repository.addExternal(
                MetadataSearchResult(
                    provider = MetadataProvider.OPEN_LIBRARY,
                    externalId = "home-in-progress",
                    externalUrl = null,
                    type = MediaType.BOOK,
                    title = "Dune",
                    releaseYear = 1965,
                    creators = listOf("Frank Herbert"),
                    genres = listOf("Ciencia ficción", "Aventura", "Política"),
                    pageCount = 412,
                ),
                ConsumptionStatus.IN_PROGRESS,
            ).mediaId
            val consumptionId = repository.observeMediaDetail(inProgressId).first { it != null }!!
                .consumptions.single().id
            repository.addProgress(consumptionId, ProgressType.PAGES, 103.0, 412.0)
            repository.setRating(consumptionId, 9)

            val completedId = repository.addExternal(
                MetadataSearchResult(
                    provider = MetadataProvider.TMDB,
                    externalId = "home-completed",
                    externalUrl = null,
                    type = MediaType.MOVIE,
                    title = "La llegada",
                    releaseYear = 2016,
                    creators = listOf("Denis Villeneuve"),
                    genres = listOf("Drama", "Ciencia ficción"),
                ),
                ConsumptionStatus.COMPLETED,
                CompletedMediaInput(LocalDate.of(2025, 4, 3), ratingHalfStars = 10),
            ).mediaId

            val home = repository.observeHomeMedia().first {
                it.mediaCount == 2 && it.inProgress.isNotEmpty() && it.recentlyCompleted.isNotEmpty()
            }
            val active = home.inProgress.single()
            val recent = home.recentlyCompleted.single()

            assertEquals(inProgressId, active.media.id)
            assertEquals("Frank Herbert", active.creator)
            assertEquals(listOf("Aventura", "Ciencia ficción"), active.genres)
            assertEquals(1, active.additionalGenreCount)
            assertEquals(9, active.ratingHalfStars)
            assertEquals(ProgressType.PAGES, active.latestProgress?.progressType)
            assertEquals(103.0, active.latestProgress?.currentValue)
            assertEquals(completedId, recent.media.id)
            assertEquals("Denis Villeneuve", recent.creator)
            assertEquals(10, recent.ratingHalfStars)
            assertEquals(LocalDate.of(2025, 4, 3), recent.completedDate)
        } finally {
            database.close()
        }
    }
}
