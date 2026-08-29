package com.memento.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.memento.app.R
import com.memento.app.BuildConfig
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.ui.theme.MementoSpacing
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import android.net.Uri
import java.time.LocalDate
import com.memento.app.ai.AiAvailability

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    state: SettingsUiState,
    onThemeChanged: (ThemeMode) -> Unit,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit,
    onDownloadAi: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(onExport)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImport)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MementoSpacing.normal),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.large),
    ) {
        item { Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge) }
        item {
            SettingsSection(title = stringResource(R.string.appearance)) {
                Text(stringResource(R.string.theme), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                    items(ThemeMode.entries.size) { index ->
                        val mode = ThemeMode.entries[index]
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onThemeChanged(mode) },
                            label = { Text(themeLabel(mode)) },
                        )
                    }
                }
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.data)) {
                ListItem(
                    modifier = Modifier.clickable(enabled = !state.isWorking) {
                        exportLauncher.launch("memento-backup-${LocalDate.now()}.json")
                    },
                    headlineContent = { Text(stringResource(R.string.export_library)) },
                    leadingContent = { Icon(Icons.Outlined.Upload, contentDescription = null) },
                    supportingContent = { Text(stringResource(R.string.export_library_description)) },
                )
                ListItem(
                    modifier = Modifier.clickable(enabled = !state.isWorking) {
                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                    },
                    headlineContent = { Text(stringResource(R.string.import_library)) },
                    leadingContent = { Icon(Icons.Outlined.Download, contentDescription = null) },
                    supportingContent = { Text(stringResource(R.string.import_library_description)) },
                )
                if (state.isWorking) Text(stringResource(R.string.processing_backup), color = MaterialTheme.colorScheme.primary)
                state.notice?.let { notice ->
                    Text(
                        stringResource(if (notice == BackupNotice.EXPORTED) R.string.backup_exported else R.string.backup_restored),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.on_device_ai)) {
                Text(aiAvailabilityText(state.aiAvailability), style = MaterialTheme.typography.bodyLarge)
                if (state.aiAvailability == AiAvailability.MODEL_DOWNLOAD_REQUIRED) {
                    TextButton(onClick = onDownloadAi) { Text(stringResource(R.string.download_ai_model)) }
                }
                if (state.aiAvailability == AiAvailability.DOWNLOADING && state.aiDownloadBytes > 0) {
                    Text(stringResource(R.string.ai_download_progress, state.aiDownloadBytes / 1_048_576.0))
                }
                Text(
                    stringResource(R.string.ai_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.metadata_providers)) {
                AssistChip(
                    onClick = { uriHandler.openUri("https://www.themoviedb.org") },
                    label = { Text(stringResource(if (BuildConfig.TMDB_API_KEY.isBlank()) R.string.tmdb_status else R.string.tmdb_status_configured)) },
                )
                AssistChip(
                    onClick = { uriHandler.openUri("https://openlibrary.org") },
                    label = { Text(stringResource(R.string.open_library_status)) },
                )
                AssistChip(
                    onClick = { uriHandler.openUri("https://rawg.io") },
                    label = { Text(stringResource(if (BuildConfig.RAWG_API_KEY.isBlank()) R.string.rawg_status else R.string.rawg_status_configured)) },
                )
                Text(
                    stringResource(R.string.provider_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AsyncImage(
                    model = "https://www.themoviedb.org/assets/2/v4/logos/v2/blue_square_1-5bdc75aaebeb75dc7ae79426ddd9be3b2be1e342510f8202baf6bffa71d7f5c4.svg",
                    contentDescription = stringResource(R.string.tmdb_name),
                    modifier = Modifier.height(40.dp),
                )
                Text(stringResource(R.string.tmdb_attribution), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { uriHandler.openUri("https://www.themoviedb.org") }) {
                    Text(stringResource(R.string.provider_source_link, stringResource(R.string.tmdb_name)))
                }
                Text(stringResource(R.string.open_library_attribution), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { uriHandler.openUri("https://openlibrary.org") }) {
                    Text(stringResource(R.string.provider_source_link, stringResource(R.string.open_library_name)))
                }
                Text(stringResource(R.string.rawg_attribution), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { uriHandler.openUri("https://rawg.io") }) {
                    Text(stringResource(R.string.provider_source_link, stringResource(R.string.rawg_name)))
                }
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.about)) {
                Text(stringResource(R.string.version_text), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    state.importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = onCancelRestore,
            title = { Text(stringResource(R.string.restore_backup_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.restore_backup_summary,
                        preview.mediaItems,
                        preview.consumptions,
                        preview.reflections,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmRestore) { Text(stringResource(R.string.replace_all_confirm)) }
            },
            dismissButton = { TextButton(onClick = onCancelRestore) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun aiAvailabilityText(availability: AiAvailability?): String = stringResource(
    when (availability) {
        null -> R.string.ai_checking
        AiAvailability.AVAILABLE -> R.string.ai_available
        AiAvailability.MODEL_DOWNLOAD_REQUIRED -> R.string.ai_download_required
        AiAvailability.DOWNLOADING -> R.string.ai_downloading
        AiAvailability.DEVICE_NOT_SUPPORTED -> R.string.ai_not_supported
        AiAvailability.ERROR -> R.string.ai_unavailable
    },
)

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MementoSpacing.normal),
            verticalArrangement = Arrangement.spacedBy(MementoSpacing.small),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)
