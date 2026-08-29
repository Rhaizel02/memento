package com.memento.app.backup

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.memento.app.data.local.database.MementoDatabase
import com.memento.app.data.local.entity.ConsumptionEntity
import com.memento.app.data.local.entity.MediaItemEntity
import com.memento.app.data.local.entity.ReflectionEntity
import com.memento.app.data.local.entity.AiInsightEntity
import com.memento.app.data.local.entity.AiInsightSourceCrossRef
import com.memento.app.data.repository.RoomBackupRepository
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ReflectionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {
    @Test
    fun databaseExportClearImportProducesEquivalentPersonalData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        val dao = database.backupDao()
        val repository = RoomBackupRepository(database, dao)
        val now = Instant.parse("2026-01-01T10:00:00Z")
        dao.insertMediaItems(listOf(MediaItemEntity("m1", MediaType.BOOK, "Libro", null, null, null, 2026, null, null, true, true, null, 320, null, null, now, now)))
        dao.insertConsumptions(listOf(ConsumptionEntity("c1", "m1", ConsumptionStatus.COMPLETED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10), 9, now, now)))
        dao.insertReflections(listOf(ReflectionEntity("r1", "c1", ReflectionType.FINAL_REFLECTION, "Idea", now, now)))
        dao.insertAiInsights(listOf(AiInsightEntity("ai1", "SUMMARIZE", "Idea resumida", now)))
        dao.insertAiInsightSources(listOf(AiInsightSourceCrossRef("ai1", "r1")))

        val exported = repository.exportJson()
        val before = BackupCodec.decodeAndValidate(exported).data
        database.withTransaction {
            dao.clearReflections(); dao.clearConsumptions(); dao.clearMediaItems()
        }
        assertEquals(0, dao.mediaItems().size)

        repository.restoreReplaceAll(exported)
        val after = BackupCodec.decodeAndValidate(repository.exportJson()).data

        assertEquals(before, after)
        database.close()
    }
}
