package com.memento.app.ui.recommendation

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import com.memento.app.ui.components.MementoPrimaryButton as Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.memento.app.ui.components.MementoSecondaryButton as OutlinedButton
import com.memento.app.ui.components.MementoTextField as OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.memento.app.R
import com.memento.app.domain.model.CompletedMediaInput
import com.memento.app.domain.recommendation.RecommendationCategory
import com.memento.app.domain.recommendation.RecommendationReason
import com.memento.app.ui.components.ExpandableText
import com.memento.app.ui.components.PosterArtwork
import com.memento.app.ui.components.RatingSelector
import com.memento.app.ui.components.StaticTag
import com.memento.app.ui.components.mediaTypeLabel
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.watch.WatchAvailabilitySection
import com.memento.app.ui.watch.WatchAvailabilityUiState
import java.time.LocalDate

@Composable
fun RecommendationDetailScreen(
    state: RecommendationDetailUiState,
    onBack: () -> Unit,
    onAddToPlanned: () -> Unit,
    onStartNow: () -> Unit,
    onComplete: (CompletedMediaInput) -> Unit,
    onNotInterested: () -> Unit,
) {
    val candidate = state.candidate
    var showCompletion by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val externalUrl = candidate?.externalUrl?.takeIf(::isSafeExternalUrl)

    if (candidate == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(MementoSpacing.normal)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.large))
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
    ) {
        item {
            com.memento.app.ui.components.MediaHero(candidate.type, candidate.title, candidate.posterUrl,
                candidate.backdropUrl, listOfNotNull(candidate.creators.firstOrNull(), candidate.releaseYear?.toString()).joinToString(" · "), onBack)
            Column(Modifier.fillMaxWidth().padding(MementoSpacing.normal),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.recommendation?.let { com.memento.app.ui.components.RecommendationTierLabel(it.category) }
                candidate.externalRating?.let { rating ->
                    Text("★ " + rating + " · " + when (candidate.provider) {
                        com.memento.app.domain.model.MetadataProvider.TMDB -> "TMDB"
                        com.memento.app.domain.model.MetadataProvider.RAWG -> "RAWG"
                        com.memento.app.domain.model.MetadataProvider.OPEN_LIBRARY -> "Open Library"
                    }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (candidate.genres.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier.padding(horizontal = MementoSpacing.normal),
                    horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small),
                ) { items(candidate.genres.take(6)) { StaticTag(it) } }
            }
        }
        if (state.recommendation?.reasons?.isNotEmpty() == true) {
            item {
                Column(Modifier.padding(horizontal = MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.archive_recommendation_evidence), style = MaterialTheme.typography.titleLarge)
                    state.recommendation.reasons.take(3).forEach {
                        Text(com.memento.app.ui.components.recommendationReasonText(it),
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (state.watchAvailability != WatchAvailabilityUiState.Hidden) {
            item {
                WatchAvailabilitySection(
                    state = state.watchAvailability,
                    modifier = Modifier.padding(horizontal = MementoSpacing.normal),
                )
            }
        }
        candidate.description?.let { description ->
            item {
                ExpandableText(
                    text = description,
                    modifier = Modifier.padding(horizontal = MementoSpacing.normal),
                    style = MaterialTheme.typography.bodyLarge,
                    collapsedMaxLines = 10,
                )
            }
        }
        if (state.isWorking) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal)) }
        state.error?.let { error ->
            item { Text(error, modifier = Modifier.padding(horizontal = MementoSpacing.normal), color = MaterialTheme.colorScheme.error) }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal),
                verticalArrangement = Arrangement.spacedBy(MementoSpacing.small),
            ) {
                Button(onClick = onAddToPlanned, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.add_to_planned))
                }
                OutlinedButton(onClick = onStartNow, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.start_now))
                }
                OutlinedButton(onClick = { showCompletion = true }, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.already_completed))
                }
                TextButton(onClick = onNotInterested, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.not_interested))
                }
                if (externalUrl != null) {
                    TextButton(
                        onClick = { runCatching { uriHandler.openUri(externalUrl) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                        Text(stringResource(R.string.view_outside_memento), modifier = Modifier.padding(start = MementoSpacing.small))
                    }
                }
            }
        }
        item { Box(Modifier.height(MementoSpacing.huge)) }
    }

    if (showCompletion) {
        RecommendationCompletionDialog(
            onDismiss = { showCompletion = false },
            onConfirm = { input -> showCompletion = false; onComplete(input) },
        )
    }
}

@Composable
private fun RecommendationCompletionDialog(onDismiss: () -> Unit, onConfirm: (CompletedMediaInput) -> Unit) {
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var rating by rememberSaveable { mutableStateOf<Int?>(null) }
    var favorite by rememberSaveable { mutableStateOf(false) }
    var reflection by rememberSaveable { mutableStateOf("") }
    val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.already_completed)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it.filter { character -> character.isDigit() || character == '-' }.take(10) },
                    labelText = stringResource(R.string.completion_date),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = date == null,
                    singleLine = true,
                )
                Text(stringResource(R.string.rating), style = MaterialTheme.typography.labelLarge)
                RatingSelector(ratingHalfStars = rating, onRatingChanged = { rating = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = favorite, onCheckedChange = { favorite = it })
                    Text(stringResource(R.string.mark_favorite))
                }
                OutlinedTextField(
                    value = reflection,
                    onValueChange = { reflection = it },
                    labelText = stringResource(R.string.final_reflection_prompt),
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    date?.let {
                        onConfirm(CompletedMediaInput(it, rating, favorite, reflection.takeIf(String::isNotBlank)))
                    }
                },
                enabled = date != null,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun categoryLabel(category: RecommendationCategory): String = stringResource(
    when (category) {
        RecommendationCategory.VERY_AFFINE -> R.string.recommendation_very_affine
        RecommendationCategory.GOOD_BET -> R.string.recommendation_good_bet
        RecommendationCategory.EXPLORATION -> R.string.recommendation_exploration
    },
)

@Composable
private fun recommendationReason(reason: RecommendationReason): String = when (reason) {
    is RecommendationReason.AnchorWorks -> stringResource(R.string.reason_anchors, reason.titles.joinToString(" y "))
    is RecommendationReason.Genre -> stringResource(R.string.reason_genre, reason.name)
    is RecommendationReason.Creator -> stringResource(R.string.reason_creator, reason.name)
    is RecommendationReason.MediaKind -> stringResource(R.string.reason_type, mediaTypeLabel(reason.type))
    is RecommendationReason.Exploration -> reason.anchorTitle?.let { stringResource(R.string.reason_exploration_anchor, it) }
        ?: stringResource(R.string.reason_exploration)
}

private fun isSafeExternalUrl(value: String): Boolean = runCatching {
    val uri = Uri.parse(value)
    (uri.scheme == "https" || uri.scheme == "http") && !uri.host.isNullOrBlank()
}.getOrDefault(false)
