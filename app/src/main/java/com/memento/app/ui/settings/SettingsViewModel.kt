package com.memento.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.backup.BackupPreview
import com.memento.app.backup.BackupCodec
import com.memento.app.domain.repository.BackupRepository
import com.memento.app.data.preferences.SettingsRepository
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.ai.AiAvailability
import com.memento.app.ai.AiProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

enum class BackupNotice { EXPORTED, RESTORED }

data class SettingsUiState(
    val isWorking: Boolean = false,
    val importPreview: BackupPreview? = null,
    val notice: BackupNotice? = null,
    val error: String? = null,
    val aiAvailability: AiAvailability? = null,
    val aiDownloadBytes: Long = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository,
    private val aiProcessor: AiProcessor,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    private var pendingImport: String? = null
    val state = mutableState.asStateFlow()
    val themeMode = settingsRepository.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    val onboardingCompleted = settingsRepository.onboardingCompleted
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init { refreshAiAvailability() }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setTheme(mode) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.completeOnboarding() }
    }

    fun refreshAiAvailability() {
        viewModelScope.launch {
            val availability = aiProcessor.availability()
            mutableState.update { it.copy(aiAvailability = availability) }
        }
    }

    fun downloadAiModel() {
        if (mutableState.value.aiAvailability == AiAvailability.DOWNLOADING) return
        mutableState.update { it.copy(aiAvailability = AiAvailability.DOWNLOADING, aiDownloadBytes = 0) }
        viewModelScope.launch {
            val result = aiProcessor.downloadModel { bytes ->
                mutableState.update { it.copy(aiDownloadBytes = bytes) }
            }
            mutableState.update { it.copy(aiAvailability = result) }
        }
    }

    fun exportTo(uri: Uri) {
        if (!beginBackupWork()) return
        viewModelScope.launch {
            runCatching {
                val json = backupRepository.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(json) }
                        ?: error("No se pudo abrir el archivo de destino")
                }
            }.onSuccess { mutableState.update { it.copy(isWorking = false, notice = BackupNotice.EXPORTED) } }
                .onFailure {
                    mutableState.update {
                        it.copy(isWorking = false, error = "No se pudo exportar el backup. Comprueba el destino e inténtalo de nuevo.")
                    }
                }
        }
    }

    fun prepareImport(uri: Uri) {
        if (!beginBackupWork()) return
        viewModelScope.launch {
            runCatching {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readUtf8Limited(BackupCodec.MAX_IMPORT_BYTES) }
                        ?: error("No se pudo abrir el backup")
                }
                val preview = backupRepository.preview(content)
                pendingImport = content
                preview
            }.onSuccess { preview -> mutableState.update { it.copy(isWorking = false, importPreview = preview) } }
                .onFailure {
                    mutableState.update {
                        it.copy(isWorking = false, error = "No se pudo leer o validar el backup seleccionado.")
                    }
                }
        }
    }

    fun confirmRestore() {
        val content = pendingImport ?: return
        if (!beginBackupWork()) return
        viewModelScope.launch {
            mutableState.update { it.copy(importPreview = null) }
            runCatching { backupRepository.restoreReplaceAll(content) }
                .onSuccess {
                    pendingImport = null
                    mutableState.update { it.copy(isWorking = false, notice = BackupNotice.RESTORED) }
                }
                .onFailure {
                    mutableState.update {
                        it.copy(isWorking = false, error = "No se pudo restaurar el backup. Tus datos actuales se conservan.")
                    }
                }
        }
    }

    fun cancelRestore() {
        pendingImport = null
        mutableState.update { it.copy(importPreview = null) }
    }

    private fun beginBackupWork(): Boolean {
        if (mutableState.value.isWorking) return false
        mutableState.update { it.copy(isWorking = true, error = null, notice = null) }
        return true
    }
}

private fun InputStream.readUtf8Limited(limit: Int): String {
    val output = ByteArrayOutputStream(minOf(limit, 8_192))
    val buffer = ByteArray(8_192)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        require(total <= limit) { "El backup supera el límite de 10 MB" }
        output.write(buffer, 0, count)
    }
    return output.toString(Charsets.UTF_8.name())
}
