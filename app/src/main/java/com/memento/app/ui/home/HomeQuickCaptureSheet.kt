package com.memento.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.model.MediaType
import com.memento.app.ui.components.NumericTextField
import com.memento.app.ui.theme.MementoSpacing

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun HomeQuickCaptureSheet(
    sheet: QuickCaptureSheet,
    onDismiss: () -> Unit,
    onProgressChanged: (QuickProgressField, String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSaveProgress: () -> Unit,
    onSaveNote: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        when (sheet) {
            is QuickCaptureSheet.Progress -> QuickProgressContent(
                sheet = sheet,
                onDismiss = onDismiss,
                onChanged = onProgressChanged,
                onSave = onSaveProgress,
            )
            is QuickCaptureSheet.Note -> QuickNoteContent(
                sheet = sheet,
                onDismiss = onDismiss,
                onChanged = onNoteChanged,
                onSave = onSaveNote,
            )
        }
    }
}

@Composable
private fun QuickProgressContent(
    sheet: QuickCaptureSheet.Progress,
    onDismiss: () -> Unit,
    onChanged: (QuickProgressField, String) -> Unit,
    onSave: () -> Unit,
) {
    val firstField = remember(sheet.item.consumptionId) { FocusRequester() }
    LaunchedEffect(firstField) { firstField.requestFocus() }
    SheetColumn {
        Text(stringResource(R.string.update_progress), style = MaterialTheme.typography.headlineSmall)
        Text(sheet.item.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (sheet.item.type) {
            MediaType.BOOK -> {
                NumericTextField(
                    sheet.currentValue,
                    { onChanged(QuickProgressField.CURRENT, it) },
                    R.string.quick_current_page,
                    decimal = false,
                    modifier = Modifier.fillMaxWidth().focusRequester(firstField),
                )
                if (!sheet.isTotalEditable && sheet.totalValue.isNotBlank()) {
                    Text(
                        stringResource(R.string.quick_page_total, sheet.totalValue),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    NumericTextField(
                        sheet.totalValue,
                        { onChanged(QuickProgressField.TOTAL, it) },
                        R.string.quick_total_pages,
                        decimal = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            MediaType.SERIES -> {
                NumericTextField(
                    sheet.season,
                    { onChanged(QuickProgressField.SEASON, it) },
                    R.string.season_number,
                    decimal = false,
                    modifier = Modifier.fillMaxWidth().focusRequester(firstField),
                )
                NumericTextField(
                    sheet.episode,
                    { onChanged(QuickProgressField.EPISODE, it) },
                    R.string.episode_number,
                    decimal = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            MediaType.GAME -> {
                NumericTextField(
                    sheet.currentValue,
                    { onChanged(QuickProgressField.CURRENT, it) },
                    R.string.quick_game_hours,
                    decimal = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(firstField),
                )
                NumericTextField(
                    sheet.totalValue,
                    { onChanged(QuickProgressField.TOTAL, it) },
                    R.string.quick_game_percent,
                    decimal = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            MediaType.MOVIE -> NumericTextField(
                sheet.currentValue,
                { onChanged(QuickProgressField.CURRENT, it) },
                R.string.progress_minutes,
                decimal = false,
                modifier = Modifier.fillMaxWidth().focusRequester(firstField),
            )
        }
        sheet.error?.let { SheetError(it) }
        SheetActions(
            isSaving = sheet.isSaving,
            canSave = when (sheet.item.type) {
                MediaType.SERIES -> sheet.season.isNotBlank() && sheet.episode.isNotBlank()
                else -> sheet.currentValue.isNotBlank()
            },
            onDismiss = onDismiss,
            onSave = onSave,
        )
    }
}

@Composable
private fun QuickNoteContent(
    sheet: QuickCaptureSheet.Note,
    onDismiss: () -> Unit,
    onChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    val focusRequester = remember(sheet.item.consumptionId) { FocusRequester() }
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    SheetColumn {
        Text(stringResource(R.string.quick_note_prompt), style = MaterialTheme.typography.headlineSmall)
        Text(sheet.item.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = sheet.content,
            onValueChange = onChanged,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            placeholder = { Text(stringResource(R.string.note_hint)) },
            minLines = 4,
            enabled = !sheet.isSaving,
        )
        sheet.error?.let { SheetError(it) }
        SheetActions(
            isSaving = sheet.isSaving,
            canSave = sheet.content.isNotBlank(),
            onDismiss = onDismiss,
            onSave = onSave,
        )
    }
}

@Composable
private fun SheetColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(start = MementoSpacing.normal, end = MementoSpacing.normal, bottom = MementoSpacing.large),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
        content = content,
    )
}

@Composable
private fun SheetError(message: String) {
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun SheetActions(
    isSaving: Boolean,
    canSave: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small, Alignment.End),
    ) {
        TextButton(onClick = onDismiss, enabled = !isSaving) { Text(stringResource(R.string.cancel)) }
        Button(onClick = onSave, enabled = canSave && !isSaving) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.save))
            }
        }
    }
}
