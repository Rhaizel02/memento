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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE `ai_insights_new` (
                `id` TEXT NOT NULL,
                `capability` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("INSERT INTO `ai_insights_new` (`id`,`capability`,`content`,`createdAt`) SELECT `id`,`capability`,`content`,`createdAt` FROM `ai_insights`")
        db.execSQL(
            """
            CREATE TABLE `ai_insight_sources` (
                `aiInsightId` TEXT NOT NULL,
                `reflectionId` TEXT NOT NULL,
                PRIMARY KEY(`aiInsightId`, `reflectionId`),
                FOREIGN KEY(`aiInsightId`) REFERENCES `ai_insights_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`reflectionId`) REFERENCES `reflections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("INSERT INTO `ai_insight_sources` (`aiInsightId`,`reflectionId`) SELECT `id`,`reflectionId` FROM `ai_insights`")
        db.execSQL("DROP TABLE `ai_insights`")
        db.execSQL("ALTER TABLE `ai_insights_new` RENAME TO `ai_insights`")
        db.execSQL("CREATE INDEX `index_ai_insights_createdAt` ON `ai_insights` (`createdAt`)")
        db.execSQL("CREATE INDEX `index_ai_insight_sources_aiInsightId` ON `ai_insight_sources` (`aiInsightId`)")
        db.execSQL("CREATE INDEX `index_ai_insight_sources_reflectionId` ON `ai_insight_sources` (`reflectionId`)")
        createIntegrityTriggers(db)
    }
}

val HARDENING_DATABASE_CALLBACK = object : androidx.room.RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) = createIntegrityTriggers(db)
    override fun onOpen(db: SupportSQLiteDatabase) = createIntegrityTriggers(db)
}

private fun createIntegrityTriggers(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS `cleanup_ai_insight_after_source_delete`
        AFTER DELETE ON `ai_insight_sources`
        WHEN NOT EXISTS (SELECT 1 FROM `ai_insight_sources` WHERE `aiInsightId` = OLD.`aiInsightId`)
        BEGIN
            DELETE FROM `ai_insights` WHERE `id` = OLD.`aiInsightId`;
        END
        """.trimIndent(),
    )
    db.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS `prevent_multiple_active_consumptions_insert`
        BEFORE INSERT ON `consumptions`
        WHEN NEW.`status` IN ('PLANNED', 'IN_PROGRESS') AND EXISTS (
            SELECT 1 FROM `consumptions`
            WHERE `mediaItemId` = NEW.`mediaItemId` AND `status` IN ('PLANNED', 'IN_PROGRESS')
        )
        BEGIN
            SELECT RAISE(ABORT, 'only one active consumption is allowed per media item');
        END
        """.trimIndent(),
    )
    db.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS `prevent_multiple_active_consumptions_update`
        BEFORE UPDATE OF `status`, `mediaItemId` ON `consumptions`
        WHEN NEW.`status` IN ('PLANNED', 'IN_PROGRESS') AND EXISTS (
            SELECT 1 FROM `consumptions`
            WHERE `mediaItemId` = NEW.`mediaItemId`
              AND `status` IN ('PLANNED', 'IN_PROGRESS')
              AND `id` != NEW.`id`
        )
        BEGIN
            SELECT RAISE(ABORT, 'only one active consumption is allowed per media item');
        END
        """.trimIndent(),
    )
}
