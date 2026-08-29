package com.memento.app.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.model.MediaType
import com.memento.app.ui.components.icon
import com.memento.app.ui.settings.BackupNotice
import com.memento.app.ui.settings.SettingsUiState
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.theme.mediaTypeColor

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
        val heroHeight = if (compact) 214.dp else 286.dp
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
    val cardWidth = if (compact) 82.dp else 102.dp
    val cardHeight = if (compact) 124.dp else 154.dp
    val haloSize = if (compact) 190.dp else 258.dp
    val paletteStart = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
    val paletteEnd = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.08f)

    Box(modifier = modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(haloSize)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(paletteStart, paletteEnd))),
        )
        AbstractMemoryCard(
            type = MediaType.SERIES,
            width = cardWidth,
            height = cardHeight,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = -cardWidth * 0.78f, y = -12.dp)
                .graphicsLayer(rotationZ = -11f),
        )
        AbstractMemoryCard(
            type = MediaType.MOVIE,
            width = cardWidth,
            height = cardHeight,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = cardWidth * 0.78f, y = -16.dp)
                .graphicsLayer(rotationZ = 11f),
        )
        AbstractMemoryCard(
            type = MediaType.GAME,
            width = cardWidth,
            height = cardHeight,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = -cardWidth * 0.33f, y = 18.dp)
                .graphicsLayer(rotationZ = -4f),
        )
        AbstractMemoryCard(
            type = MediaType.BOOK,
            width = cardWidth,
            height = cardHeight,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = cardWidth * 0.34f, y = 22.dp)
                .graphicsLayer(rotationZ = 4f),
        )
    }
}

@Composable
private fun AbstractMemoryCard(
    type: MediaType,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.mediaTypeColor(type)
    Surface(
        modifier = modifier.width(width).height(height),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(MementoSpacing.medium),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(accent))
            Box(
                modifier = Modifier
                    .size(if (width < 90.dp) 36.dp else 44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    type.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(if (width < 90.dp) 20.dp else 24.dp),
                    tint = accent,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.78f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.52f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
                )
            }
        }
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
                    fontWeight = FontWeight.SemiBold,
                )
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
