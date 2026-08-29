package com.memento.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.EditMediaInput
import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ProgressEntry
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.Reflection
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.TimelineEvent
import com.memento.app.ui.components.PosterArtwork
import com.memento.app.ui.components.ExpandableText
import com.memento.app.ui.components.RatingText
import com.memento.app.ui.components.RatingSelector
import com.memento.app.ui.components.mediaTypeLabel
import com.memento.app.ui.components.creatorRoleLabel
import com.memento.app.ui.components.StaticTag
import com.memento.app.ui.theme.MementoSpacing
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MediaDetailScreen(
    state: MediaDetailUiState,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onStart: () -> Unit,
    onComplete: (LocalDate, Int?, String?) -> Unit,
    onDrop: () -> Unit,
    onAddNote: (String) -> Unit,
    onAddProgress: (ProgressType, Double?, Double?, Int?, Int?) -> Unit,
    onUpdateMetadata: (EditMediaInput) -> Unit,
    onDeleteConsumption: (String) -> Unit,
    onUpdateReflection: (String, String) -> Unit,
    onDelete: () -> Unit,
) {
    val detail = state.detail
    if (detail == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(MementoSpacing.normal)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        }
        return
    }

    var showComplete by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var showProgress by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var pendingDeleteConsumption by remember { mutableStateOf<String?>(null) }
    var pendingEditReflection by remember { mutableStateOf<Reflection?>(null) }
    val active = detail.activeConsumption
    val latest = detail.latestConsumption

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = MementoSpacing.huge),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(MementoSpacing.normal),
                verticalAlignment = Alignment.Top,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = MementoSpacing.small)) {
                    Text(detail.media.title, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        listOfNotNull(detail.creators.firstOrNull(), detail.media.releaseYear?.toString()).joinToString(" · "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showEdit = true }) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit_work))
                }
                IconButton(onClick = onToggleFavorite, enabled = !state.isWorking) {
                    Icon(
                        if (detail.media.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(
                            if (detail.media.isFavorite) R.string.remove_favorite else R.string.mark_favorite,
                        ),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = MementoSpacing.normal)) {
                PosterArtwork(
                    detail.media.type,
                    detail.media.title,
                    detail.media.posterUrl,
                    Modifier.width(128.dp).height(192.dp),
                )
                Column(
                    modifier = Modifier.padding(start = MementoSpacing.normal),
                    verticalArrangement = Arrangement.spacedBy(MementoSpacing.small),
                ) {
                    StaticTag(mediaTypeLabel(detail.media.type))
                    latest?.let {
                        Text(statusLabel(it.status), style = MaterialTheme.typography.titleMedium)
                        RatingText(it.ratingHalfStars)
                    }
                    detail.genres.take(3).forEach { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        detail.media.description?.let { description ->
            item {
                ExpandableText(
                    text = description,
                    modifier = Modifier.padding(MementoSpacing.normal),
                    style = MaterialTheme.typography.bodyLarge,
                    collapsedMaxLines = 7,
                )
            }
        }
        if (state.isWorking) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal)) }
        }
        state.message?.let { message ->
            item {
                Text(
                    message,
                    modifier = Modifier.padding(horizontal = MementoSpacing.normal),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item {
            ActionSection(
                detail = detail,
                enabled = !state.isWorking,
                onStart = onStart,
                onProgress = { showProgress = true },
                onNote = { showNote = true },
                onComplete = { showComplete = true },
                onDrop = onDrop,
            )
        }
        val finalReflection = detail.reflections.firstOrNull { it.type == ReflectionType.FINAL_REFLECTION }
        finalReflection?.let { reflection ->
            item {
                Card(Modifier.fillMaxWidth().padding(MementoSpacing.normal)) {
                    Column(Modifier.padding(MementoSpacing.large)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.timeline_final_reflection),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            IconButton(onClick = { pendingEditReflection = reflection }) {
                                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit_reflection))
                            }
                        }
                        Text(
                            reflection.content,
                            modifier = Modifier.padding(top = MementoSpacing.medium),
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
            }
        }
        if (detail.consumptions.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.previous_consumptions),
                    modifier = Modifier.padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.medium),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            val consumptions = detail.consumptions.sortedByDescending { it.createdAt }
            itemsIndexed(consumptions, key = { _, consumption -> consumption.id }) { index, consumption ->
                ConsumptionCard(
                    consumption = consumption,
                    number = consumptions.size - index,
                    progressCount = detail.progress.count { it.consumptionId == consumption.id },
                    reflectionCount = detail.reflections.count { it.consumptionId == consumption.id },
                    onDelete = { pendingDeleteConsumption = consumption.id },
                )
            }
        }
        item {
            Text(
                stringResource(R.string.personal_history),
                modifier = Modifier.padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.medium),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (state.timeline.isEmpty()) {
            item { Text(stringResource(R.string.unknown_date), modifier = Modifier.padding(horizontal = MementoSpacing.normal)) }
        } else {
            items(state.timeline, key = { it.sortInstant.toEpochMilli().toString() + it.hashCode() }) { event ->
                TimelineRow(event, onEditReflection = { pendingEditReflection = it })
            }
        }
        item {
            HorizontalDivider(Modifier.padding(vertical = MementoSpacing.large))
            TextButton(
                onClick = { showDelete = true },
                modifier = Modifier.padding(horizontal = MementoSpacing.normal),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Text(stringResource(R.string.delete), modifier = Modifier.padding(start = MementoSpacing.small))
            }
        }
    }

    if (showComplete) {
        CompleteDialog(
            onDismiss = { showComplete = false },
            onConfirm = { date, rating, reflection ->
                showComplete = false
                onComplete(date, rating, reflection)
            },
        )
    }
    if (showNote) {
        TextEditorDialog(
            title = stringResource(R.string.add_note),
            hint = stringResource(R.string.note_hint),
            onDismiss = { showNote = false },
            onSave = { text -> showNote = false; onAddNote(text) },
        )
    }
    if (showProgress && active != null) {
        ProgressDialog(
            type = detail.media.type,
            latest = detail.progress.firstOrNull { it.consumptionId == active.id },
            pageCount = detail.media.pageCount,
            onDismiss = { showProgress = false },
            onSave = { type, current, total, season, episode ->
                showProgress = false
                onAddProgress(type, current, total, season, episode)
            },
        )
    }
    if (showEdit) {
        EditMediaDialog(
            detail = detail,
            onDismiss = { showEdit = false },
            onSave = { input -> showEdit = false; onUpdateMetadata(input) },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.delete_named_work_title, detail.media.title)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_work_body_counts,
                        detail.consumptions.size,
                        detail.progress.size,
                        detail.reflections.size,
                    ),
                )
            },
            confirmButton = { TextButton(onClick = { showDelete = false; onDelete() }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    pendingDeleteConsumption?.let { consumptionId ->
        val progressCount = detail.progress.count { it.consumptionId == consumptionId }
        val reflectionCount = detail.reflections.count { it.consumptionId == consumptionId }
        AlertDialog(
            onDismissRequest = { pendingDeleteConsumption = null },
            title = { Text(stringResource(R.string.delete_consumption_title)) },
            text = { Text(stringResource(R.string.delete_consumption_body, progressCount, reflectionCount)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteConsumption = null
                        onDeleteConsumption(consumptionId)
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteConsumption = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    pendingEditReflection?.let { reflection ->
        ReflectionEditDialog(
            reflection = reflection,
            onDismiss = { pendingEditReflection = null },
            onSave = { content ->
                pendingEditReflection = null
                onUpdateReflection(reflection.id, content)
            },
        )
    }
}

@Composable
private fun ConsumptionCard(
    consumption: Consumption,
    number: Int,
    progressCount: Int,
    reflectionCount: Int,
    onDelete: () -> Unit,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es")) }
    val date = consumption.completedDate ?: consumption.startedDate
        ?: consumption.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
    Card(Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.small)) {
        Column(Modifier.fillMaxWidth().padding(MementoSpacing.normal)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.consumption_number, number), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.consumption_status_date, statusLabel(consumption.status), date.format(formatter)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete_consumption))
                }
            }
            RatingText(consumption.ratingHalfStars)
            if (progressCount > 0 || reflectionCount > 0) {
                Text(
                    stringResource(R.string.consumption_contents, progressCount, reflectionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EditMediaDialog(detail: MediaDetail, onDismiss: () -> Unit, onSave: (EditMediaInput) -> Unit) {
    var title by rememberSaveable(detail.media.id) { mutableStateOf(detail.media.title) }
    var year by rememberSaveable(detail.media.id) { mutableStateOf(detail.media.releaseYear?.toString().orEmpty()) }
    var creators by rememberSaveable(detail.media.id) { mutableStateOf(detail.creators.joinToString(", ")) }
    var description by rememberSaveable(detail.media.id) { mutableStateOf(detail.media.description.orEmpty()) }
    var imageUrl by rememberSaveable(detail.media.id) { mutableStateOf(detail.media.posterUrl.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_work)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.title_required)) },
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.year)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = creators,
                        onValueChange = { creators = it },
                        label = { Text(creatorRoleLabel(detail.media.type)) },
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.description)) },
                        minLines = 3,
                    )
                }
                item {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text(stringResource(R.string.image_optional)) },
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        EditMediaInput(
                            title = title,
                            year = year.toIntOrNull(),
                            description = description,
                            creators = creators.split(',').map(String::trim).filter(String::isNotEmpty),
                            imageUrl = imageUrl,
                        ),
                    )
                },
                enabled = title.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ReflectionEditDialog(reflection: Reflection, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var content by rememberSaveable(reflection.id) { mutableStateOf(reflection.content) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_reflection)) },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 7,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(content) }, enabled = content.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ActionSection(
    detail: MediaDetail,
    enabled: Boolean,
    onStart: () -> Unit,
    onProgress: () -> Unit,
    onNote: () -> Unit,
    onComplete: () -> Unit,
    onDrop: () -> Unit,
) {
    val active = detail.activeConsumption
    Column(
        modifier = Modifier.fillMaxWidth().padding(MementoSpacing.normal),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.small),
    ) {
        if (active == null || active.status == ConsumptionStatus.PLANNED) {
            Button(onClick = onStart, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Text(
                    stringResource(if (active == null) R.string.consume_again else R.string.start_work),
                    modifier = Modifier.padding(start = MementoSpacing.small),
                )
            }
        }
        if (active?.status == ConsumptionStatus.IN_PROGRESS) {
            Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                OutlinedButton(onClick = onProgress, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Update, contentDescription = null)
                    Text(stringResource(R.string.update_progress), maxLines = 1)
                }
                OutlinedButton(onClick = onNote, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.EditNote, contentDescription = null)
                    Text(stringResource(R.string.add_note), maxLines = 1)
                }
            }
            Button(onClick = onComplete, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Flag, contentDescription = null)
                Text(stringResource(R.string.finish_work), modifier = Modifier.padding(start = MementoSpacing.small))
            }
            TextButton(onClick = onDrop, enabled = enabled, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.drop_work))
            }
        }
    }
}

@Composable
private fun CompleteDialog(onDismiss: () -> Unit, onConfirm: (LocalDate, Int?, String?) -> Unit) {
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var rating by rememberSaveable { mutableStateOf<Int?>(null) }
    var reflection by rememberSaveable { mutableStateOf("") }
    val parsedDate = runCatching { LocalDate.parse(dateText) }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.finish_work)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it.filter { character -> character.isDigit() || character == '-' }.take(10) },
                    label = { Text(stringResource(R.string.completion_date)) },
                    supportingText = {
                        Text(
                            stringResource(if (parsedDate == null) R.string.date_format_error else R.string.date_format_hint),
                            color = if (parsedDate == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    singleLine = true,
                    isError = parsedDate == null,
                )
                Text(stringResource(R.string.rating), style = MaterialTheme.typography.labelLarge)
                RatingSelector(ratingHalfStars = rating, onRatingChanged = { rating = it })
                OutlinedTextField(
                    value = reflection,
                    onValueChange = { reflection = it },
                    label = { Text(stringResource(R.string.final_reflection_prompt)) },
                    minLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    parsedDate?.let { date ->
                        onConfirm(date, rating, reflection.takeIf(String::isNotBlank))
                    }
                },
                enabled = parsedDate != null,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.now_not)) } },
    )
}

@Composable
private fun TextEditorDialog(title: String, hint: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text(hint) }, minLines = 7)
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ProgressDialog(
    type: MediaType,
    latest: ProgressEntry?,
    pageCount: Int?,
    onDismiss: () -> Unit,
    onSave: (ProgressType, Double?, Double?, Int?, Int?) -> Unit,
) {
    var first by remember { mutableStateOf(latest?.currentValue?.toString().orEmpty()) }
    var second by remember { mutableStateOf(latest?.totalValue?.toString().orEmpty()) }
    var season by remember { mutableStateOf(latest?.season?.toString().orEmpty()) }
    var episode by remember { mutableStateOf(latest?.episode?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_progress)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                when (type) {
                    MediaType.BOOK -> {
                        NumberField(first, { first = it }, R.string.current_value, decimal = false)
                        NumberField(second.ifEmpty { pageCount?.toString().orEmpty() }, { second = it }, R.string.total_value, decimal = false)
                    }
                    MediaType.SERIES -> {
                        NumberField(season, { season = it }, R.string.season_number, decimal = false)
                        NumberField(episode, { episode = it }, R.string.episode_number, decimal = false)
                    }
                    MediaType.GAME -> {
                        NumberField(first, { first = it }, R.string.progress_hours, decimal = true)
                        NumberField(second, { second = it }, R.string.approx_percent, decimal = true)
                    }
                    MediaType.MOVIE -> NumberField(first, { first = it }, R.string.progress_minutes, decimal = false)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (type) {
                        MediaType.BOOK -> onSave(ProgressType.PAGES, first.toDoubleOrNull(), second.ifEmpty { pageCount?.toString().orEmpty() }.toDoubleOrNull(), null, null)
                        MediaType.SERIES -> onSave(ProgressType.EPISODE, null, null, season.toIntOrNull(), episode.toIntOrNull())
                        MediaType.GAME -> onSave(ProgressType.HOURS, first.toDoubleOrNull(), second.toDoubleOrNull(), null, null)
                        MediaType.MOVIE -> onSave(ProgressType.MINUTES, first.toDoubleOrNull(), null, null, null)
                    }
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun NumberField(value: String, onValueChanged: (String) -> Unit, label: Int, decimal: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = { incoming -> onValueChanged(incoming.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.replace(',', '.')) },
        label = { Text(stringResource(label)) },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        singleLine = true,
    )
}

@Composable
private fun TimelineRow(event: TimelineEvent, onEditReflection: (Reflection) -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es")) }
    val date = event.sortInstant.atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
    Column(Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.medium)) {
        Text(date, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        when (event) {
            is TimelineEvent.ConsumptionStarted -> Text(stringResource(R.string.timeline_started), style = MaterialTheme.typography.bodyLarge)
            is TimelineEvent.ConsumptionCompleted -> {
                Text(stringResource(R.string.timeline_completed), style = MaterialTheme.typography.bodyLarge)
                RatingText(event.ratingHalfStars)
            }
            is TimelineEvent.ConsumptionDropped -> Text(stringResource(R.string.timeline_dropped), style = MaterialTheme.typography.bodyLarge)
            is TimelineEvent.ProgressUpdated -> Text(progressLabel(event.entry), style = MaterialTheme.typography.bodyLarge)
            is TimelineEvent.ReflectionWritten -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(
                            when (event.reflection.type) {
                                ReflectionType.NOTE -> R.string.timeline_note
                                ReflectionType.FINAL_REFLECTION -> R.string.timeline_final_reflection
                                ReflectionType.LATER_REFLECTION -> R.string.timeline_later_reflection
                            },
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = { onEditReflection(event.reflection) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit_reflection))
                    }
                }
                Text(event.reflection.content, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            }
        }
    }
}

@Composable
private fun progressLabel(entry: ProgressEntry): String = when (entry.progressType) {
    ProgressType.PAGES -> stringResource(R.string.pages_format, number(entry.currentValue), number(entry.totalValue))
    ProgressType.EPISODE -> stringResource(R.string.episode_format, entry.season ?: 0, entry.episode ?: 0)
    ProgressType.HOURS -> listOfNotNull(
        entry.currentValue?.let { stringResource(R.string.hours_format, number(it)) },
        entry.totalValue?.let { stringResource(R.string.percent_format, number(it)) },
    ).joinToString(" · ")
    ProgressType.PERCENT -> stringResource(R.string.percent_format, number(entry.currentValue))
    ProgressType.MINUTES -> stringResource(R.string.progress_minutes) + " · " + number(entry.currentValue)
}

private fun number(value: Double?): String = value?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "—"

@Composable
private fun statusLabel(status: ConsumptionStatus): String = stringResource(
    when (status) {
        ConsumptionStatus.PLANNED -> R.string.planned
        ConsumptionStatus.IN_PROGRESS -> R.string.status_in_progress
        ConsumptionStatus.COMPLETED -> R.string.completed
        ConsumptionStatus.DROPPED -> R.string.dropped
    },
)
