package com.memento.app.ui.remember

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.ai.AiAvailability
import com.memento.app.ai.AiCapability
import com.memento.app.domain.model.ReflectionType
import com.memento.app.ui.components.PosterArtwork
import com.memento.app.ui.components.RatingText
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.share.ShareCardContent
import com.memento.app.share.ShareCardRenderer
import com.memento.app.share.sharePng
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RememberScreen(
    state: RememberUiState,
    onBack: () -> Unit,
    onSaveThought: (String) -> Unit,
    onAiAction: (AiCapability) -> Unit,
    onSaveAiInsight: () -> Unit,
    onDiscardAi: () -> Unit,
) {
    val memory = state.memory
    val context = LocalContext.current
    if (memory == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(Modifier.padding(MementoSpacing.large))
        }
        return
    }
    val renderer = remember(context) { ShareCardRenderer(context.applicationContext) }
    val clipboardLabel = stringResource(R.string.ai_result)
    val shareAttribution = stringResource(R.string.share_provider_attribution)
    val scope = rememberCoroutineScope()
    var writing by rememberSaveable { mutableStateOf(false) }
    var thought by rememberSaveable(memory.consumptionId) { mutableStateOf("") }
    val later = state.detail?.reflections.orEmpty().filter {
        it.consumptionId == memory.consumptionId && it.type == ReflectionType.LATER_REFLECTION
    }.sortedBy { it.createdAt }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MementoSpacing.huge),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.large),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(MementoSpacing.normal)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(
                    stringResource(R.string.remember),
                    modifier = Modifier.padding(start = MementoSpacing.small, top = MementoSpacing.small),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
        item {
            PosterArtwork(
                state.detail?.media?.type ?: com.memento.app.domain.model.MediaType.BOOK,
                memory.title,
                state.detail?.media?.posterUrl,
                Modifier.fillMaxWidth().height(320.dp).padding(horizontal = MementoSpacing.normal),
            )
        }
        item {
            Column(Modifier.padding(horizontal = MementoSpacing.large)) {
                Text(memory.title, style = MaterialTheme.typography.displaySmall)
                Text(
                    stringResource(
                        R.string.remember_completed_date,
                        memory.completedDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RatingText(memory.ratingHalfStars, Modifier.padding(top = MementoSpacing.small))
                Button(
                    onClick = {
                        scope.launch {
                            val uri = renderer.render(
                                ShareCardContent(
                                    title = memory.title,
                                    subtitle = memory.completedDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))),
                                    body = memory.reflectionContent,
                                    imageUrl = memory.backdropUrl ?: memory.posterUrl,
                                    attribution = shareAttribution,
                                ),
                                "memento-remember-${memory.consumptionId}",
                            )
                            sharePng(context, uri)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = MementoSpacing.normal),
                ) { Text(stringResource(R.string.create_memory_card)) }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal)) {
                Column(Modifier.padding(MementoSpacing.large)) {
                    Text(stringResource(R.string.remember_original), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.quoted_reflection, memory.reflectionContent),
                        modifier = Modifier.padding(top = MementoSpacing.normal),
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        }
        later.forEach { reflection ->
            item(key = reflection.id) {
                Card(Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal)) {
                    Column(Modifier.padding(MementoSpacing.large)) {
                        Text(stringResource(R.string.remember_now), style = MaterialTheme.typography.titleLarge)
                        Text(reflection.content, modifier = Modifier.padding(top = MementoSpacing.normal), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (state.aiAvailability == AiAvailability.AVAILABLE) {
            item {
                AiToolsCard(
                    original = memory.reflectionContent,
                    hasLaterReflection = later.isNotEmpty(),
                    state = state,
                    onAction = onAiAction,
                    onSaveInsight = onSaveAiInsight,
                    onDiscard = onDiscardAi,
                    onCopy = { value ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(clipboardLabel, value))
                    },
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = MementoSpacing.normal)) {
                Text(stringResource(R.string.remember_question), style = MaterialTheme.typography.headlineMedium)
                if (writing) {
                    OutlinedTextField(
                        value = thought,
                        onValueChange = { thought = it },
                        modifier = Modifier.fillMaxWidth().padding(top = MementoSpacing.medium),
                        minLines = 6,
                        placeholder = { Text(stringResource(R.string.note_hint)) },
                    )
                    Button(
                        onClick = { onSaveThought(thought); thought = ""; writing = false },
                        enabled = thought.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().padding(top = MementoSpacing.medium),
                    ) { Text(stringResource(R.string.save)) }
                } else {
                    Button(
                        onClick = { writing = true },
                        modifier = Modifier.fillMaxWidth().padding(top = MementoSpacing.medium),
                    ) { Text(stringResource(R.string.remember_write_now)) }
                }
                if (state.saved) {
                    Text(
                        stringResource(R.string.remember_saved),
                        modifier = Modifier.padding(top = MementoSpacing.medium),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AiToolsCard(
    original: String,
    hasLaterReflection: Boolean,
    state: RememberUiState,
    onAction: (AiCapability) -> Unit,
    onSaveInsight: () -> Unit,
    onDiscard: () -> Unit,
    onCopy: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MementoSpacing.large),
            verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
        ) {
            Text(stringResource(R.string.ai_local_tools), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.ai_local_tools_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                item { AiActionButton(AiCapability.REWRITE, state, onAction) }
                item { AiActionButton(AiCapability.SUMMARIZE, state, onAction) }
                item { AiActionButton(AiCapability.EXTRACT_THEMES, state, onAction) }
                item { AiActionButton(AiCapability.REFLECTION_QUESTION, state, onAction) }
                if (hasLaterReflection) {
                    item { AiActionButton(AiCapability.COMPARE_REFLECTIONS, state, onAction) }
                }
                item { AiActionButton(AiCapability.CONNECT_REFLECTIONS, state, onAction) }
            }
            if (state.isAiWorking) {
                Row(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.ai_processing), style = MaterialTheme.typography.bodyLarge)
                }
            }
            state.aiError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.aiOutput?.let { output ->
                HorizontalDivider()
                if (state.aiCapability == AiCapability.REWRITE) {
                    Text(stringResource(R.string.ai_original_label), style = MaterialTheme.typography.labelLarge)
                    Text(original, style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.ai_proposal_label), style = MaterialTheme.typography.labelLarge)
                } else {
                    Text(stringResource(R.string.ai_result), style = MaterialTheme.typography.labelLarge)
                }
                Text(output, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
                    TextButton(onClick = { onCopy(output) }) { Text(stringResource(R.string.copy)) }
                    Button(onClick = onSaveInsight) { Text(stringResource(R.string.save_as_insight)) }
                    TextButton(onClick = onDiscard) { Text(stringResource(R.string.discard)) }
                }
            }
            if (state.insights.isNotEmpty()) {
                HorizontalDivider()
                Text(stringResource(R.string.saved_insights), style = MaterialTheme.typography.titleMedium)
                state.insights.forEach { insight ->
                    Text(aiCapabilityLabel(insight.capability), style = MaterialTheme.typography.labelMedium)
                    Text(insight.content, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(
                            R.string.ai_insight_sources,
                            insight.sources.joinToString { source ->
                                "${source.mediaTitle} · ${source.reflectionCreatedAt.atZone(java.time.ZoneId.systemDefault()).year}"
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AiActionButton(
    capability: AiCapability,
    state: RememberUiState,
    onAction: (AiCapability) -> Unit,
) {
    OutlinedButton(onClick = { onAction(capability) }, enabled = !state.isAiWorking) {
        Text(aiCapabilityLabel(capability))
    }
}

@Composable
private fun aiCapabilityLabel(capability: AiCapability): String = stringResource(
    when (capability) {
        AiCapability.REWRITE -> R.string.ai_rewrite
        AiCapability.SUMMARIZE -> R.string.ai_summarize
        AiCapability.EXTRACT_THEMES -> R.string.ai_extract_themes
        AiCapability.REFLECTION_QUESTION -> R.string.ai_reflection_question
        AiCapability.CONNECT_REFLECTIONS -> R.string.ai_connect
        AiCapability.COMPARE_REFLECTIONS -> R.string.ai_compare
    },
)
