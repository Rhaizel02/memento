package com.memento.app.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recommendation_candidates` (
                `provider` TEXT NOT NULL,
                `externalId` TEXT NOT NULL,
                `mediaType` TEXT NOT NULL,
                `externalUrl` TEXT,
                `title` TEXT NOT NULL,
                `originalTitle` TEXT,
                `description` TEXT,
                `releaseDate` TEXT,
                `releaseYear` INTEGER,
                `posterUrl` TEXT,
                `backdropUrl` TEXT,
                `creatorsJson` TEXT NOT NULL,
                `genresJson` TEXT NOT NULL,
                `runtimeMinutes` INTEGER,
                `pageCount` INTEGER,
                `seasonCount` INTEGER,
                `episodeCount` INTEGER,
                `fetchedAt` INTEGER NOT NULL,
                PRIMARY KEY(`provider`, `externalId`, `mediaType`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recommendation_candidates_mediaType` ON `recommendation_candidates` (`mediaType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recommendation_candidates_fetchedAt` ON `recommendation_candidates` (`fetchedAt`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recommendation_feedback` (
                `id` TEXT NOT NULL,
                `provider` TEXT NOT NULL,
                `externalId` TEXT NOT NULL,
                `mediaType` TEXT NOT NULL,
                `feedbackType` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_recommendation_feedback_provider_externalId_mediaType` " +
                "ON `recommendation_feedback` (`provider`, `externalId`, `mediaType`)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recommendation_feedback_createdAt` ON `recommendation_feedback` (`createdAt`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ai_insights` (
                `id` TEXT NOT NULL,
                `reflectionId` TEXT NOT NULL,
                `capability` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`reflectionId`) REFERENCES `reflections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_insights_reflectionId` ON `ai_insights` (`reflectionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_insights_createdAt` ON `ai_insights` (`createdAt`)")
    }
}
