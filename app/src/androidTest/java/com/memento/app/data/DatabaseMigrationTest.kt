package com.memento.app.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.memento.app.data.local.database.MIGRATION_3_4
import com.memento.app.data.local.database.MIGRATION_4_5
import com.memento.app.data.local.database.MIGRATION_5_6
import com.memento.app.data.local.database.MementoDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MementoDatabase::class.java,
    )

    @Test
    fun migration3To4PreservesLegacySourceAndSupportsManySourcesWithCleanup() {
        helper.createDatabase(DB_NAME, 3).apply {
            seedReflectionGraph()
            execSQL("INSERT INTO ai_insights (id, reflectionId, capability, content, createdAt) VALUES ('ai1','r1','SUMMARIZE','Resumen',100)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 4, true, MIGRATION_3_4)
        assertEquals(1, db.count("SELECT COUNT(*) FROM ai_insight_sources WHERE aiInsightId='ai1' AND reflectionId='r1'"))
        db.execSQL("INSERT INTO reflections (id,consumptionId,type,content,createdAt,updatedAt) VALUES ('r2','c2','LATER_REFLECTION','Otra',200,200)")
        db.execSQL("INSERT INTO reflections (id,consumptionId,type,content,createdAt,updatedAt) VALUES ('r3','c2','NOTE','Tercera',300,300)")
        db.execSQL("INSERT INTO ai_insight_sources (aiInsightId,reflectionId) VALUES ('ai1','r2')")
        db.execSQL("INSERT INTO ai_insight_sources (aiInsightId,reflectionId) VALUES ('ai1','r3')")
        assertEquals(3, db.count("SELECT COUNT(*) FROM ai_insight_sources WHERE aiInsightId='ai1'"))

        db.execSQL("DELETE FROM reflections WHERE id='r1'")
        assertEquals(1, db.count("SELECT COUNT(*) FROM ai_insights WHERE id='ai1'"))
        assertEquals(2, db.count("SELECT COUNT(*) FROM ai_insight_sources WHERE aiInsightId='ai1'"))
        db.execSQL("DELETE FROM reflections WHERE id='r2'")
        assertEquals(1, db.count("SELECT COUNT(*) FROM ai_insights WHERE id='ai1'"))
        db.execSQL("DELETE FROM reflections WHERE id='r3'")
        assertEquals(0, db.count("SELECT COUNT(*) FROM ai_insights WHERE id='ai1'"))
        db.close()
    }

    @Test
    fun migration4To5AddsGlobalTimelineIndexes() {
        helper.createDatabase(DB_NAME_TIMELINE, 4).close()

        val db = helper.runMigrationsAndValidate(DB_NAME_TIMELINE, 5, true, MIGRATION_4_5)

        assertEquals(1, db.count("SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='index_consumptions_startedDate'"))
        assertEquals(1, db.count("SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='index_progress_entries_recordedAt'"))
        db.close()
    }

    @Test
    fun migration5To6AddsPersonalTagsAndPreservesPersonalHistory() {
        helper.createDatabase(DB_NAME_TAGS, 5).apply {
            seedReflectionGraph()
            execSQL("INSERT INTO progress_entries (id,consumptionId,progressType,currentValue,totalValue,season,episode,recordedAt) VALUES ('p1','c1','PAGES',20,100,NULL,NULL,50)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME_TAGS, 6, true, MIGRATION_5_6)

        assertEquals(2, db.count("SELECT COUNT(*) FROM media_items"))
        assertEquals(2, db.count("SELECT COUNT(*) FROM consumptions"))
        assertEquals(1, db.count("SELECT COUNT(*) FROM progress_entries"))
        assertEquals(1, db.count("SELECT COUNT(*) FROM reflections"))
        assertEquals(1, db.count("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='tags'"))
        assertEquals(1, db.count("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='media_tag_cross_ref'"))
        db.close()
    }

    private fun SupportSQLiteDatabase.seedReflectionGraph() {
        execSQL("INSERT INTO media_items (id,type,title,originalTitle,description,releaseDate,releaseYear,posterUrl,backdropUrl,isFavorite,isManual,runtimeMinutes,pageCount,seasonCount,episodeCount,createdAt,updatedAt) VALUES ('m1','BOOK','Uno',NULL,NULL,NULL,NULL,NULL,NULL,0,1,NULL,NULL,NULL,NULL,1,1)")
        execSQL("INSERT INTO media_items (id,type,title,originalTitle,description,releaseDate,releaseYear,posterUrl,backdropUrl,isFavorite,isManual,runtimeMinutes,pageCount,seasonCount,episodeCount,createdAt,updatedAt) VALUES ('m2','BOOK','Dos',NULL,NULL,NULL,NULL,NULL,NULL,0,1,NULL,NULL,NULL,NULL,1,1)")
        execSQL("INSERT INTO consumptions (id,mediaItemId,status,startedDate,completedDate,ratingHalfStars,createdAt,updatedAt) VALUES ('c1','m1','COMPLETED',NULL,NULL,NULL,1,1)")
        execSQL("INSERT INTO consumptions (id,mediaItemId,status,startedDate,completedDate,ratingHalfStars,createdAt,updatedAt) VALUES ('c2','m2','COMPLETED',NULL,NULL,NULL,1,1)")
        execSQL("INSERT INTO reflections (id,consumptionId,type,content,createdAt,updatedAt) VALUES ('r1','c1','FINAL_REFLECTION','Original',100,100)")
    }

    private fun SupportSQLiteDatabase.count(sql: String): Int = query(sql).use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }

    private companion object {
        const val DB_NAME = "migration-hardening"
        const val DB_NAME_TIMELINE = "migration-timeline"
        const val DB_NAME_TAGS = "migration-personal-tags"
    }
}
