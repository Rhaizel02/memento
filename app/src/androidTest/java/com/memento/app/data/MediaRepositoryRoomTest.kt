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
import com.memento.app.domain.model.EditMediaInput
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.ReflectionType
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
    fun rememberExposureIsInsertedAtMostOncePerLogicalDay() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        try {
            val mediaRepository = RoomMediaRepository(database, database.mediaDao(), database.consumptionDao())
            val mediaId = mediaRepository.addManual(AddMediaInput(MediaType.BOOK, "Memoria"), ConsumptionStatus.COMPLETED)
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
}
