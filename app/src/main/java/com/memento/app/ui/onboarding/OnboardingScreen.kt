package com.memento.app.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.settings.BackupNotice
import com.memento.app.ui.settings.SettingsUiState

@Composable
fun OnboardingScreen(
    state: SettingsUiState,
    onStart: () -> Unit,
    onImport: (Uri) -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit,
) {
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImport)
    }
    LaunchedEffect(state.notice) {
        if (state.notice == BackupNotice.RESTORED) onStart()
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(MementoSpacing.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.AutoStories,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(72.dp),
        )
        Spacer(Modifier.height(MementoSpacing.large))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(MementoSpacing.small))
        Text(
            stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MementoSpacing.xLarge))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.start))
        }
        TextButton(
            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
            enabled = !state.isWorking,
        ) { Text(stringResource(R.string.onboarding_import)) }
        if (state.isWorking) CircularProgressIndicator()
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
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
                TextButton(onClick = onConfirmRestore) { Text(stringResource(R.string.restore_and_start)) }
            },
            dismissButton = {
                TextButton(onClick = onCancelRestore) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
