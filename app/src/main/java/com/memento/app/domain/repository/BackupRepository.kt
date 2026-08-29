package com.memento.app.domain.repository

import com.memento.app.backup.BackupPreview

interface BackupRepository {
    suspend fun exportJson(): String
    fun preview(json: String): BackupPreview
    suspend fun restoreReplaceAll(json: String): BackupPreview
}
