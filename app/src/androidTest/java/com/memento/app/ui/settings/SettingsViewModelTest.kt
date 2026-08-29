package com.memento.app.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.memento.app.ai.AiAvailability
import com.memento.app.ai.AiCapability
import com.memento.app.ai.AiProcessor
import com.memento.app.backup.BackupPreview
import com.memento.app.data.preferences.SettingsRepository
import com.memento.app.data.preferences.ThemePalette
import com.memento.app.domain.repository.BackupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {
    @Test
    fun selectingNoblePersistsItThroughTheRepository() = runBlocking {
        val repository = SettingsRepository.createForTest(InMemoryPreferencesDataStore())
        val viewModel = SettingsViewModel(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            backupRepository = NoOpBackupRepository,
            settingsRepository = repository,
            aiProcessor = NoOpAiProcessor,
        )

        viewModel.setThemePalette(ThemePalette.NOBLE)

        val stored = withTimeout(2_000) { repository.themePalette.first { it == ThemePalette.NOBLE } }
        assertEquals(ThemePalette.NOBLE, stored)
    }
}

private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())
    private val mutex = Mutex()
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = mutex.withLock {
        transform(state.value).also { state.value = it }
    }
}

private object NoOpBackupRepository : BackupRepository {
    override suspend fun exportJson(): String = ""
    override fun preview(json: String): BackupPreview = error("Not used")
    override suspend fun restoreReplaceAll(json: String): BackupPreview = error("Not used")
}

private object NoOpAiProcessor : AiProcessor {
    override suspend fun availability(): AiAvailability = AiAvailability.AVAILABLE
    override suspend fun downloadModel(onProgress: (Long) -> Unit): AiAvailability = AiAvailability.AVAILABLE
    override suspend fun process(capability: AiCapability, reflection: String, comparison: String?): String = ""
}
