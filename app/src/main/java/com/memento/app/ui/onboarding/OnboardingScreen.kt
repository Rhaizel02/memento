package com.memento.app.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import com.memento.app.ui.components.MementoPrimaryButton as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.ui.settings.BackupNotice
import com.memento.app.ui.settings.SettingsUiState
import com.memento.app.ui.theme.MementoSpacing

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

    OnboardingContent(
        state = state,
        onStart = onStart,
        onRestoreRequested = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
    )

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
                TextButton(onClick = onConfirmRestore, enabled = !state.isWorking) {
                    Text(stringResource(R.string.restore_and_start))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelRestore) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
fun OnboardingContent(
    state: SettingsUiState,
    onStart: () -> Unit,
    onRestoreRequested: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val compact = maxHeight < 680.dp || maxWidth > maxHeight
        val heroHeight = if (compact) 144.dp else 184.dp
        val horizontalPadding = if (maxWidth < 360.dp) MementoSpacing.normal else MementoSpacing.large

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = MementoSpacing.large),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnboardingHero(
                compact = compact,
                modifier = Modifier.fillMaxWidth().height(heroHeight),
            )
            Spacer(Modifier.height(if (compact) MementoSpacing.normal else MementoSpacing.large))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.app_tagline),
                modifier = Modifier.padding(top = MementoSpacing.small),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(if (compact) MementoSpacing.large else MementoSpacing.xLarge))
            Button(
                onClick = onStart,
                enabled = !state.isWorking,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.start))
            }
            TextButton(
                onClick = onRestoreRequested,
                enabled = !state.isWorking,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.onboarding_import))
            }
            if (state.isWorking) {
                Row(
                    modifier = Modifier.padding(top = MementoSpacing.small),
                    horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.onboarding_import_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.error?.let { error ->
                OnboardingImportError(
                    detail = error,
                    modifier = Modifier.fillMaxWidth().padding(top = MementoSpacing.normal),
                )
            }
        }
    }
}

@Composable
private fun OnboardingHero(compact: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        com.memento.app.ui.components.MementoBrandMark(Modifier.width(if (compact) 176.dp else 208.dp))
    }
}

@Composable
private fun OnboardingImportError(detail: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MementoSpacing.normal),
            horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.xSmall)) {
                Text(
                    stringResource(R.string.onboarding_import_error_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
