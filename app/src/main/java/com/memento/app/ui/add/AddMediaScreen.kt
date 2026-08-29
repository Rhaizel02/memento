package com.memento.app.ui.add

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.ui.components.EmptyState
import com.memento.app.ui.components.MementoSearchField
import com.memento.app.ui.components.PosterArtwork
import com.memento.app.ui.components.RatingSelector
import com.memento.app.ui.components.mediaTypeLabel
import com.memento.app.ui.components.creatorRoleLabel
import com.memento.app.ui.theme.MementoSpacing

@Composable
fun AddMediaScreen(
    state: AddMediaUiState,
    onBack: () -> Unit,
    onTypeChanged: (MediaType) -> Unit,
    onQueryChanged: (String) -> Unit,
    onResultSelected: (MetadataSearchResult) -> Unit,
    onShowManual: () -> Unit,
    onReturnToSearch: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onYearChanged: (String) -> Unit,
    onCreatorChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onImageChanged: (String) -> Unit,
    onPageCountChanged: (String) -> Unit,
    onSave: (ConsumptionStatus) -> Unit,
    onCompletedDateChanged: (String) -> Unit,
    onCompletedRatingChanged: (Int?) -> Unit,
    onCompletedFavoriteChanged: (Boolean) -> Unit,
    onCompletedReflectionChanged: (String) -> Unit,
    onCancelCompletion: () -> Unit,
    onSaveCompleted: () -> Unit,
) {
    BackHandler(enabled = state.mode == AddMediaMode.COMPLETE_DETAILS, onBack = onCancelCompletion)
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(MementoSpacing.normal),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
    ) {
        item {
            Row {
                IconButton(
                    onClick = if (state.mode == AddMediaMode.COMPLETE_DETAILS) onCancelCompletion else onBack,
                    enabled = state.mode != AddMediaMode.COMPLETE_DETAILS || !state.isSaving,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(
                    text = when (state.mode) {
                        AddMediaMode.SEARCH -> stringResource(R.string.add_work)
                        AddMediaMode.MANUAL -> stringResource(R.string.manual_add_title)
                        AddMediaMode.CONFIRM_EXTERNAL -> stringResource(R.string.confirm_work)
                        AddMediaMode.COMPLETE_DETAILS -> stringResource(R.string.complete_add_title)
                    },
                    modifier = Modifier.padding(start = MementoSpacing.small, top = MementoSpacing.small),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }

        if (state.mode == AddMediaMode.COMPLETE_DETAILS) {
            item {
                CompletedForm(
                    state = state,
                    onDateChanged = onCompletedDateChanged,
                    onRatingChanged = onCompletedRatingChanged,
                    onFavoriteChanged = onCompletedFavoriteChanged,
                    onReflectionChanged = onCompletedReflectionChanged,
                )
            }
            state.error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
                ) {
                    OutlinedButton(
                        onClick = onCancelCompletion,
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.cancel)) }
                    Button(
                        onClick = onSaveCompleted,
                        enabled = state.canSaveCompletion,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(if (state.isSaving) R.string.saving else R.string.save)) }
                }
            }
            return@LazyColumn
        }

        item { TypeChips(state.type, onTypeChanged) }

        if (state.mode == AddMediaMode.SEARCH) {
            item {
                MementoSearchField(
                    value = state.query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.search_external),
                )
            }
            if (state.isSearching) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.searchIssue?.let { issue ->
                item {
                    EmptyState(
                        title = stringResource(issue.titleResource()),
                        body = stringResource(issue.bodyResource(state.searchProvider)),
                        action = { OutlinedButton(onClick = onShowManual) { Text(stringResource(R.string.add_manually)) } },
                    )
                }
            }
            items(state.searchResults.size, key = { state.searchResults[it].provider.name + state.searchResults[it].externalId }) { index ->
                SearchResultCard(state.searchResults[index], onResultSelected)
            }
            item {
                OutlinedButton(onClick = onShowManual, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cant_find_add_manually))
                }
            }
        } else {
            if (state.mode == AddMediaMode.CONFIRM_EXTERNAL) {
                item {
                    Text(
                        stringResource(R.string.imported_from, providerLabel(state.selectedExternal?.provider)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (state.isLoadingDetails) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                if (state.metadataIsPartial) {
                    item {
                        Text(
                            stringResource(R.string.metadata_partial_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { MediaForm(state, onTitleChanged, onYearChanged, onCreatorChanged, onDescriptionChanged, onImageChanged, onPageCountChanged) }
            state.error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            item { SaveActions(state, onSave) }
            item {
                OutlinedButton(onClick = onReturnToSearch, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.return_to_search))
                }
            }
        }
    }
}

@Composable
private fun CompletedForm(
    state: AddMediaUiState,
    onDateChanged: (String) -> Unit,
    onRatingChanged: (Int?) -> Unit,
    onFavoriteChanged: (Boolean) -> Unit,
    onReflectionChanged: (String) -> Unit,
) {
    val completed = requireNotNull(state.completedDraft)
    Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.normal)) {
        Text(state.title, style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = completed.completedDateText,
            onValueChange = onDateChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.completion_date)) },
            supportingText = {
                Text(
                    stringResource(
                        if (completed.completedDate == null) R.string.date_format_error else R.string.date_format_hint,
                    ),
                    color = if (completed.completedDate == null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            isError = completed.completedDate == null,
            enabled = !state.isSaving,
            singleLine = true,
        )
        Text(stringResource(R.string.rating_optional), style = MaterialTheme.typography.labelLarge)
        RatingSelector(
            ratingHalfStars = completed.ratingHalfStars,
            onRatingChanged = onRatingChanged,
            enabled = !state.isSaving,
        )
        Row(
            modifier = Modifier.fillMaxWidth().clickable(
                enabled = !state.isSaving,
                role = Role.Checkbox,
            ) { onFavoriteChanged(!completed.favorite) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = completed.favorite, onCheckedChange = null, enabled = !state.isSaving)
            Text(stringResource(R.string.mark_favorite), modifier = Modifier.padding(start = MementoSpacing.small))
        }
        OutlinedTextField(
            value = completed.finalReflection,
            onValueChange = onReflectionChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.final_reflection_prompt)) },
            supportingText = { Text(stringResource(R.string.optional)) },
            enabled = !state.isSaving,
            minLines = 5,
        )
    }
}

@Composable
private fun TypeChips(selected: MediaType, onTypeChanged: (MediaType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
        Text(stringResource(R.string.type), style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
            items(MediaType.entries.size) { index ->
                val type = MediaType.entries[index]
                FilterChip(
                    selected = selected == type,
                    onClick = { onTypeChanged(type) },
                    label = { Text(mediaTypeLabel(type)) },
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(result: MetadataSearchResult, onSelected: (MetadataSearchResult) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(role = Role.Button) { onSelected(result) }) {
        Row(Modifier.fillMaxWidth().padding(MementoSpacing.medium), horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
            PosterArtwork(
                type = result.type,
                title = result.title,
                imageUrl = result.posterUrl,
                modifier = Modifier.size(width = 72.dp, height = 108.dp).aspectRatio(2f / 3f),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                Text(result.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                result.creators.firstOrNull()?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                result.releaseYear?.let { Text(it.toString(), style = MaterialTheme.typography.bodySmall) }
                Text(providerLabel(result.provider), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun MediaForm(
    state: AddMediaUiState,
    onTitleChanged: (String) -> Unit,
    onYearChanged: (String) -> Unit,
    onCreatorChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onImageChanged: (String) -> Unit,
    onPageCountChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.normal)) {
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.title_required)) },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
            OutlinedTextField(
                value = state.year,
                onValueChange = onYearChanged,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.year)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            if (state.type == MediaType.BOOK) {
                OutlinedTextField(
                    value = state.pageCount,
                    onValueChange = onPageCountChanged,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.page_count)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        }
        OutlinedTextField(
            value = state.creator,
            onValueChange = onCreatorChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(creatorRoleLabel(state.type)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.description)) },
            minLines = 4,
        )
        OutlinedTextField(
            value = state.imageUrl,
            onValueChange = onImageChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.image_optional)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
        )
    }
}

@Composable
private fun SaveActions(state: AddMediaUiState, onSave: (ConsumptionStatus) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
        Button(
            onClick = { onSave(ConsumptionStatus.IN_PROGRESS) },
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.isSaving) stringResource(R.string.saving) else stringResource(R.string.start_now)) }
        OutlinedButton(
            onClick = { onSave(ConsumptionStatus.PLANNED) },
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.add_to_planned)) }
        OutlinedButton(
            onClick = { onSave(ConsumptionStatus.COMPLETED) },
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.already_completed)) }
    }
}

private fun SearchIssue.titleResource(): Int = when (this) {
    SearchIssue.NOT_CONFIGURED -> R.string.provider_not_configured_title
    SearchIssue.UNAVAILABLE -> R.string.provider_unavailable_title
    SearchIssue.NO_RESULTS -> R.string.no_external_results_title
}

private fun SearchIssue.bodyResource(provider: MetadataProvider?): Int = when (this) {
    SearchIssue.NOT_CONFIGURED -> when (provider) {
        MetadataProvider.TMDB -> R.string.tmdb_not_configured_body
        MetadataProvider.RAWG -> R.string.rawg_not_configured_body
        else -> R.string.provider_unavailable_body
    }
    SearchIssue.UNAVAILABLE -> R.string.provider_unavailable_body
    SearchIssue.NO_RESULTS -> R.string.no_external_results_body
}

@Composable
private fun providerLabel(provider: MetadataProvider?): String = when (provider) {
    MetadataProvider.TMDB -> "TMDB"
    MetadataProvider.OPEN_LIBRARY -> "Open Library"
    MetadataProvider.RAWG -> "RAWG"
    null -> ""
}
