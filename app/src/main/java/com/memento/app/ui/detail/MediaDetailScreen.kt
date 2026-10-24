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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.AlertDialog
import com.memento.app.ui.components.MementoPrimaryButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import com.memento.app.ui.components.MementoFilterChip as FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import com.memento.app.ui.components.MementoSecondaryButton as OutlinedButton
import com.memento.app.ui.components.MementoTextField as OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.memento.app.ui.components.MementoBottomSheet as ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.memento.app.domain.model.Tag
import com.memento.app.ui.components.PosterArtwork
import com.memento.app.ui.components.NumericTextField
import com.memento.app.domain.usecase.ProgressCapturePolicy
import com.memento.app.ui.components.ExpandableText
import com.memento.app.ui.components.RatingText
import com.memento.app.ui.components.RatingSelector
import com.memento.app.ui.components.mediaTypeLabel
import com.memento.app.ui.components.creatorRoleLabel
import com.memento.app.ui.components.StaticTag
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.watch.WatchAvailabilitySection
import com.memento.app.ui.watch.WatchAvailabilityUiState
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
    onAddQuote: (String) -> Unit = {},
    onCreateTag: (String) -> Unit = {},
    onAttachTag: (String) -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    onAddProgress: (ProgressType, Double?, Double?, Int?, Int?) -> Unit,
    onUpdateMetadata: (EditMediaInput) -> Unit,
    onDeleteConsumption: (String) -> Unit,
    onUpdateReflection: (String, String) -> Unit,
    onDelete: () -> Unit,
) {
    val detail = state.detail
    if (detail == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (state.isLoading) com.memento.app.ui.components.ArchiveLoading()
            else Text(stringResource(R.string.archive_missing_work), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(MementoSpacing.normal)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        }
        return
    }

    var showComplete by rememberSaveable(detail.media.id) { mutableStateOf(false) }
    var showNote by rememberSaveable(detail.media.id) { mutableStateOf(false) }
    var showProgress by rememberSaveable(detail.media.id) { mutableStateOf(false) }
    var showQuote by rememberSaveable(detail.media.id) { mutableStateOf(false) }
    var showTags by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var pendingDeleteConsumption by remember { mutableStateOf<String?>(null) }
    var pendingEditReflection by remember { mutableStateOf<Reflection?>(null) }
    val drafts = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    var pendingCapture by rememberSaveable(detail.media.id) { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(state.actionRevision) {
        pendingCapture?.let { key ->
            if (state.actionRevision > 0) {
                showComplete = false; showNote = false; showQuote = false; showProgress = false
                drafts.removeState(key)
                pendingCapture = null
            }
        }
    }
    val active = detail.activeConsumption
    val latest = detail.latestConsumption

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = MementoSpacing.huge),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
    ) {
        item("hero") {
            com.memento.app.ui.components.MediaHero(
                detail.media.type, detail.media.title, detail.media.posterUrl, detail.media.backdropUrl,
                listOfNotNull(detail.creators.firstOrNull(), detail.media.releaseYear?.toString()).joinToString(" · "), onBack,
            ) {
                IconButton(onClick = { showEdit = true }) { Icon(Icons.Outlined.Edit, stringResource(R.string.edit_work)) }
                IconButton(onClick = onToggleFavorite, enabled = !state.isWorking) {
                    Icon(if (detail.media.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        stringResource(if (detail.media.isFavorite) R.string.remove_favorite else R.string.mark_favorite),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (detail.genres.isNotEmpty()) Text(detail.genres.take(3).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                latest?.let {
                    StaticTag(statusLabel(it.status))
                    RatingText(it.ratingHalfStars)
                }
            }
        }
        if (state.isWorking) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.message?.let { message ->
            item { Text(message, Modifier.padding(horizontal = MementoSpacing.normal), color = MaterialTheme.colorScheme.error) }
        }
        item("personal") {
            com.memento.app.ui.components.EditorialSectionHeader(stringResource(R.string.archive_your_story),
                Modifier.padding(horizontal = MementoSpacing.normal))
            detail.progress.firstOrNull { it.consumptionId == active?.id }?.let { progress ->
                SurfaceProgress(progress, Modifier.padding(horizontal = MementoSpacing.normal, vertical = 12.dp))
            }
            ActionSection(detail, !state.isWorking, onStart, { showProgress = true }, { showNote = true },
                { showComplete = true }, onDrop)
        }
        item("tags") {
            PersonalTagsSection(detail.tags, !state.isWorking, { showTags = true }, onRemoveTag)
        }
        item("journal-header") {
            Row(Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.quotes), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                if (detail.consumptions.isNotEmpty()) TextButton(onClick = { showQuote = true }, enabled = !state.isWorking) {
                    Icon(Icons.Outlined.Add, null)
                    Text(stringResource(R.string.add_quote))
                }
            }
        }
        val journal = detail.reflections.sortedByDescending { it.createdAt }
        items(journal, key = { "journal:${it.id}" }) { reflection ->
            if (reflection.type == ReflectionType.QUOTE) {
                QuoteCard(reflection) { pendingEditReflection = reflection }
            } else {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal),
                    color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.padding(MementoSpacing.normal)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(when (reflection.type) {
                                ReflectionType.FINAL_REFLECTION -> R.string.timeline_final_reflection
                                ReflectionType.LATER_REFLECTION -> R.string.timeline_later_reflection
                                else -> R.string.timeline_note
                            }), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(onClick = { pendingEditReflection = reflection }) {
                                Icon(Icons.Outlined.Edit, stringResource(R.string.edit_reflection))
                            }
                        }
                        ExpandableText(reflection.content, style = com.memento.app.ui.theme.EditorialQuoteStyle, collapsedMaxLines = 6)
                    }
                }
            }
        }
        if (!detail.media.description.isNullOrBlank()) item("about") {
            Column(Modifier.padding(horizontal = MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider()
                Text(stringResource(R.string.archive_about_work), style = MaterialTheme.typography.titleLarge)
                ExpandableText(detail.media.description, style = MaterialTheme.typography.bodyLarge, collapsedMaxLines = 5)
            }
        }
        if (state.watchAvailability != WatchAvailabilityUiState.Hidden) item("watch") {
            WatchAvailabilitySection(state.watchAvailability, Modifier.padding(horizontal = MementoSpacing.normal))
        }
        if (detail.consumptions.isNotEmpty()) {
            item { Text(stringResource(R.string.previous_consumptions),
                Modifier.padding(horizontal = MementoSpacing.normal), style = MaterialTheme.typography.titleLarge) }
            val consumptions = detail.consumptions.sortedByDescending { it.createdAt }
            itemsIndexed(consumptions, key = { _, consumption -> "consumption:${consumption.id}" }) { index, consumption ->
                ConsumptionCard(consumption, consumptions.size - index,
                    detail.progress.count { it.consumptionId == consumption.id },
                    detail.reflections.count { it.consumptionId == consumption.id },
                    onDelete = { pendingDeleteConsumption = consumption.id })
            }
        }
        item { Text(stringResource(R.string.personal_history), Modifier.padding(horizontal = MementoSpacing.normal),
            style = MaterialTheme.typography.titleLarge) }
        items(state.timeline.filterNot { it is TimelineEvent.ReflectionWritten },
            key = { "event:${it.sortInstant.toEpochMilli()}:${it.hashCode()}" }) { event ->
            TimelineRow(event, onEditReflection = { pendingEditReflection = it })
        }
        item {
            HorizontalDivider()
            TextButton(onClick = { showDelete = true }, modifier = Modifier.padding(horizontal = MementoSpacing.normal)) {
                Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.delete), Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showComplete) { drafts.SaveableStateProvider("complete") {
        CompleteDialog(
            enabled = !state.isWorking,
            error = state.message,
            onDismiss = { showComplete = false },
            onConfirm = { date, rating, reflection ->
                pendingCapture = "complete"
                onComplete(date, rating, reflection)
            },
        )
    }
    }
    if (showNote) { drafts.SaveableStateProvider("note") {
        TextEditorSheet(
            title = stringResource(R.string.add_note),
            hint = stringResource(R.string.note_hint),
            onDismiss = { showNote = false },
            enabled = !state.isWorking,
            error = state.message,
            onSave = { text -> pendingCapture = "note"; onAddNote(text) },
        )
    }
    }
    if (showQuote) { drafts.SaveableStateProvider("quote") {
        TextEditorSheet(
            title = stringResource(R.string.add_quote),
            hint = stringResource(R.string.quote_hint),
            enabled = !state.isWorking,
            error = state.message,
            onDismiss = { showQuote = false },
            onSave = { text -> pendingCapture = "quote"; onAddQuote(text) },
        )
    }
    }
    if (showTags) {
        TagEditorSheet(
            attachedTags = detail.tags,
            availableTags = state.availableTags,
            enabled = !state.isWorking,
            onDismiss = { showTags = false },
            onCreate = onCreateTag,
            onAttach = onAttachTag,
            onRemove = onRemoveTag,
        )
    }
    if (showProgress && active != null) { drafts.SaveableStateProvider("progress") {
        ProgressDialog(
            enabled = !state.isWorking,
            error = state.message,
            type = detail.media.type,
            latest = detail.progress.firstOrNull { it.consumptionId == active.id },
            pageCount = detail.media.pageCount,
            onDismiss = { showProgress = false },
            onSave = { type, current, total, season, episode ->
                pendingCapture = "progress"
                onAddProgress(type, current, total, season, episode)
            },
        )
    }
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
private fun PersonalTagsSection(
    tags: List<Tag>,
    enabled: Boolean,
    onManage: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.small),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.your_tags), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onManage, enabled = enabled) {
                Icon(Icons.Outlined.LocalOffer, contentDescription = null)
                Text(stringResource(R.string.edit_tags))
            }
        }
        if (tags.isEmpty()) {
            Text(
                stringResource(R.string.no_personal_tags),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                items(tags, key = { it.id }) { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onRemove(tag.id) },
                        enabled = enabled,
                        label = { Text(tag.name) },
                        trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.remove_tag)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuoteCard(quote: Reflection, onEdit: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.small)) {
        Row(Modifier.fillMaxWidth().padding(MementoSpacing.normal), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                quote.content,
                modifier = Modifier.weight(1f).padding(horizontal = MementoSpacing.medium),
                style = com.memento.app.ui.theme.EditorialQuoteStyle,
                fontStyle = FontStyle.Italic,
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit_reflection))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TextEditorSheet(
    title: String,
    hint: String,
    enabled: Boolean,
    error: String? = null,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(hint) },
                minLines = 5,
                enabled = enabled,
            )
            Button(
                onClick = { onSave(text) },
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = { text = ""; onDismiss() }, enabled = enabled) {
                Text(stringResource(R.string.archive_discard_draft), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TagEditorSheet(
    attachedTags: List<Tag>,
    availableTags: List<Tag>,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onAttach: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    val attachedIds = attachedTags.mapTo(mutableSetOf()) { it.id }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().padding(bottom = MementoSpacing.huge),
            verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
        ) {
            Text(
                stringResource(R.string.your_tags),
                modifier = Modifier.padding(horizontal = MementoSpacing.normal),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (availableTags.isNotEmpty()) {
                Text(
                    stringResource(R.string.select_existing_tag),
                    modifier = Modifier.padding(horizontal = MementoSpacing.normal),
                    style = MaterialTheme.typography.labelLarge,
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = MementoSpacing.normal),
                    horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small),
                ) {
                    items(availableTags, key = { it.id }) { tag ->
                        val selected = tag.id in attachedIds
                        FilterChip(
                            selected = selected,
                            onClick = { if (selected) onRemove(tag.id) else onAttach(tag.id) },
                            enabled = enabled,
                            label = { Text(tag.name) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal),
                labelText = stringResource(R.string.new_tag),
                singleLine = true,
            )
            Button(
                onClick = { onCreate(name); name = "" },
                enabled = enabled && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal),
            ) { Text(stringResource(R.string.create_and_add_tag)) }
        }
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
                        labelText = stringResource(R.string.title_required),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it.filter(Char::isDigit).take(4) },
                        labelText = stringResource(R.string.year),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = creators,
                        onValueChange = { creators = it },
                        labelText = creatorRoleLabel(detail.media.type),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        labelText = stringResource(R.string.description),
                        minLines = 3,
                    )
                }
                item {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        labelText = stringResource(R.string.image_optional),
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
private fun CaptureForm(
    title: String, onDismiss: () -> Unit, onSave: () -> Unit, canSave: Boolean = true,
    enabled: Boolean = true, error: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            content()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!enabled) LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(onClick = onSave, enabled = canSave && enabled, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.cancel)) }
        }
    }
}

@Composable
private fun CompleteDialog(onDismiss: () -> Unit, enabled: Boolean, error: String?, onConfirm: (LocalDate, Int?, String?) -> Unit) {
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var rating by rememberSaveable { mutableStateOf<Int?>(null) }
    var reflection by rememberSaveable { mutableStateOf("") }
    val parsedDate = runCatching { LocalDate.parse(dateText) }.getOrNull()
    CaptureForm(stringResource(R.string.finish_work), onDismiss,
        onSave = { parsedDate?.let { onConfirm(it, rating, reflection.takeIf(String::isNotBlank)) } },
        canSave = parsedDate != null, enabled = enabled, error = error) {
        OutlinedTextField(dateText, { dateText = it.filter { c -> c.isDigit() || c == '-' }.take(10) },
            labelText = stringResource(R.string.completion_date), singleLine = true,
            isError = parsedDate == null, supportingText = { Text(stringResource(
                if (parsedDate == null) R.string.date_format_error else R.string.date_format_hint)) })
        Text(stringResource(R.string.rating_optional), style = MaterialTheme.typography.labelLarge)
        RatingSelector(rating, { rating = it })
        OutlinedTextField(reflection, { reflection = it }, labelText = stringResource(R.string.final_reflection_prompt), minLines = 4)
    }
}

@Composable
private fun ProgressDialog(
    type: MediaType, latest: ProgressEntry?, pageCount: Int?, onDismiss: () -> Unit,
    enabled: Boolean, error: String?,
    onSave: (ProgressType, Double?, Double?, Int?, Int?) -> Unit,
) {
    var first by rememberSaveable { mutableStateOf(latest?.currentValue?.toString().orEmpty()) }
    var second by rememberSaveable { mutableStateOf(latest?.totalValue?.toString() ?: pageCount?.toString().orEmpty()) }
    var season by rememberSaveable { mutableStateOf(latest?.season?.toString().orEmpty()) }
    var episode by rememberSaveable { mutableStateOf(latest?.episode?.toString().orEmpty()) }
    CaptureForm(stringResource(R.string.update_progress), onDismiss, onSave = {
        onSave(ProgressCapturePolicy.typeFor(type),
            if (type != MediaType.SERIES) first.toDoubleOrNull() else null,
            if (type == MediaType.BOOK || type == MediaType.GAME) second.toDoubleOrNull() else null,
            if (type == MediaType.SERIES) season.toIntOrNull() else null,
            if (type == MediaType.SERIES) episode.toIntOrNull() else null)
    }, canSave = if (type == MediaType.SERIES) season.isNotBlank() && episode.isNotBlank() else first.isNotBlank(),
        enabled = enabled, error = error) {
        latest?.let { Text(progressLabel(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        when (type) {
            MediaType.BOOK -> {
                NumericTextField(first, { first = it }, R.string.quick_current_page, false)
                NumericTextField(second, { second = it }, R.string.quick_total_pages, false)
                com.memento.app.ui.components.ProgressIncrements(listOf(1.0, 5.0, 10.0, 25.0), first, { first = it }, second.toDoubleOrNull())
            }
            MediaType.SERIES -> {
                NumericTextField(season, { season = it }, R.string.season_number, false)
                NumericTextField(episode, { episode = it }, R.string.episode_number, false)
                com.memento.app.ui.components.ProgressIncrements(listOf(1.0), episode, { episode = it })
            }
            MediaType.GAME -> {
                NumericTextField(first, { first = it }, R.string.progress_hours, true)
                com.memento.app.ui.components.ProgressIncrements(listOf(0.5, 1.0), first, { first = it })
                NumericTextField(second, { second = it }, R.string.approx_percent, true)
                com.memento.app.ui.components.ProgressIncrements(listOf(5.0, 10.0), second, { second = it }, 100.0)
            }
            MediaType.MOVIE -> {
                NumericTextField(first, { first = it }, R.string.progress_minutes, false)
                com.memento.app.ui.components.ProgressIncrements(listOf(5.0, 15.0), first, { first = it })
            }
        }
    }
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
                                ReflectionType.QUOTE -> R.string.timeline_quote
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

@Composable
private fun SurfaceProgress(entry: ProgressEntry, modifier: Modifier) {
    androidx.compose.material3.Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.archive_current_progress), style = MaterialTheme.typography.labelLarge)
            Text(progressLabel(entry), style = MaterialTheme.typography.bodyLarge)
            if (entry.progressType == ProgressType.PAGES && entry.totalValue != null && entry.totalValue > 0) {
                LinearProgressIndicator(progress = { ((entry.currentValue ?: 0.0) / entry.totalValue).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
