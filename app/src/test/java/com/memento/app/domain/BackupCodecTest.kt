package com.memento.app.domain

import com.memento.app.backup.BackupCodec
import com.memento.app.backup.BackupConsumption
import com.memento.app.backup.BackupData
import com.memento.app.backup.BackupEnvelope
import com.memento.app.backup.BackupMediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {
    @Test
    fun `valid backup survives JSON round trip`() {
        val envelope = BackupEnvelope(
            schemaVersion = 1,
            exportedAt = "2026-08-29T10:00:00Z",
            appVersion = "test",
            data = BackupData(
                mediaItems = listOf(media()),
                consumptions = listOf(
                    BackupConsumption("c1", "m1", "COMPLETED", null, "2026-01-01", 9, "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z"),
                ),
            ),
        )

        assertEquals(envelope, BackupCodec.decodeAndValidate(BackupCodec.encode(envelope)))
    }

    @Test
    fun `unsupported schema is rejected before import`() {
        val json = BackupCodec.encode(
            BackupEnvelope(99, "2026-08-29T10:00:00Z", "test", BackupData(mediaItems = listOf(media()))),
        )

        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decodeAndValidate(json) }
    }

    @Test
    fun `invalid enum is rejected during preview rather than during database mutation`() {
        val valid = BackupEnvelope(
            1,
            "2026-08-29T10:00:00Z",
            "test",
            BackupData(mediaItems = listOf(media().copy(type = "UNKNOWN"))),
        )

        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decodeAndValidate(BackupCodec.encode(valid)) }
    }

    @Test
    fun `multiple active consumptions for one work are rejected before import`() {
        val data = BackupData(
            mediaItems = listOf(media()),
            consumptions = listOf(
                BackupConsumption("c1", "m1", "PLANNED", null, null, null, "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z"),
                BackupConsumption("c2", "m1", "IN_PROGRESS", null, null, null, "2026-01-02T00:00:00Z", "2026-01-02T00:00:00Z"),
            ),
        )
        val json = BackupCodec.encode(BackupEnvelope(BackupCodec.SCHEMA_VERSION, "2026-08-29T10:00:00Z", "test", data))

        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decodeAndValidate(json) }
    }

    private fun media() = BackupMediaItem(
        id = "m1", type = "BOOK", title = "Libro", originalTitle = null, description = null,
        releaseDate = null, releaseYear = null, posterUrl = null, backdropUrl = null,
        isFavorite = false, isManual = true, runtimeMinutes = null, pageCount = null,
        seasonCount = null, episodeCount = null, createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )
}
