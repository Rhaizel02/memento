package com.memento.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.model.CreatorRole
import com.memento.app.domain.search.FacetSearchResult
import com.memento.app.domain.search.MediaSearchResult
import com.memento.app.domain.search.SearchMatchReason
import com.memento.app.domain.search.TextSearchResult
import com.memento.app.ui.components.MementoSearchField
import com.memento.app.ui.components.PosterArtwork
import com.memento.app.ui.components.mediaTypeLabel
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.theme.mediaTypeColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GlobalSearchScreen(
    state: GlobalSearchUiState,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onOpenMedia: (String) -> Unit,
    onSelectFacet: (FacetSearchResult) -> Unit,
    onClearFacet: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.global_search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MementoSearchField(
                value = state.query,
                onValueChange = onQueryChanged,
                label = stringResource(R.string.global_search_field),
                modifier = Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal),
                autoFocus = true,
            )
            Box(Modifier.fillMaxWidth().height(4.dp)) {
                if (state.isSearching) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            SearchContent(
                state = state,
                onOpenMedia = onOpenMedia,
                onSelectFacet = onSelectFacet,
                onClearFacet = onClearFacet,
            )
        }
    }
}

@Composable
private fun SearchContent(
    state: GlobalSearchUiState,
    onOpenMedia: (String) -> Unit,
    onSelectFacet: (FacetSearchResult) -> Unit,
    onClearFacet: () -> Unit,
) {
    val cleanQuery = state.query.trim()
    if (cleanQuery.length < 2) {
        SearchHint(stringResource(R.string.global_search_hint))
        return
    }
    if (state.hasError) {
        SearchHint(stringResource(R.string.global_search_error))
        return
    }
    val snapshot = state.snapshot ?: return
    if (state.selectedFacet != null) {
        FacetMediaResults(state.selectedFacet, state.facetMedia, onClearFacet, onOpenMedia)
        return
    }
    if (snapshot.isEmpty) {
        SearchHint(stringResource(R.string.global_search_no_results, cleanQuery))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MementoSpacing.xLarge),
    ) {
        resultSection(
            title = R.string.global_search_media,
            values = snapshot.media,
            key = MediaSearchResult::mediaId,
        ) { result -> MediaResultRow(result, onOpenMedia) }
        facetSection(R.string.global_search_tags, snapshot.tags, Icons.AutoMirrored.Outlined.Label, onSelectFacet)
        facetSection(R.string.global_search_creators, snapshot.creators, Icons.Outlined.Person, onSelectFacet)
        facetSection(R.string.global_search_genres, snapshot.genres, Icons.Outlined.Category, onSelectFacet)
        resultSection(
            title = R.string.global_search_quotes,
            values = snapshot.quotes,
            key = TextSearchResult::reflectionId,
        ) { result -> TextResultRow(result, Icons.Outlined.FormatQuote, onOpenMedia) }
        resultSection(
            title = R.string.global_search_reflections,
            values = snapshot.reflections,
            key = TextSearchResult::reflectionId,
        ) { result -> TextResultRow(result, Icons.AutoMirrored.Outlined.Notes, onOpenMedia) }
    }
}

@Composable
private fun FacetMediaResults(
    facet: FacetSearchResult,
    media: List<MediaSearchResult>,
    onClearFacet: () -> Unit,
    onOpenMedia: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MementoSpacing.xLarge),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(facet.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        pluralStringResource(R.plurals.global_search_media_count, facet.mediaCount, facet.mediaCount),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onClearFacet) { Text(stringResource(R.string.global_search_all_results)) }
            }
        }
        if (media.isEmpty()) {
            item { SearchHint(stringResource(R.string.global_search_no_associated_media)) }
        } else {
            items(media, key = MediaSearchResult::mediaId) { result -> MediaResultRow(result, onOpenMedia) }
        }
    }
}

private fun <T> androidx.compose.foundation.lazy.LazyListScope.resultSection(
    title: Int,
    values: List<T>,
    key: (T) -> Any,
    content: @Composable (T) -> Unit,
) {
    if (values.isEmpty()) return
    item(key = "header:$title") { SectionHeader(stringResource(title)) }
    items(values, key = { value -> "$title:${key(value)}" }) { value -> content(value) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.facetSection(
    title: Int,
    values: List<FacetSearchResult>,
    icon: ImageVector,
    onSelect: (FacetSearchResult) -> Unit,
) = resultSection(title, values, FacetSearchResult::id) { facet ->
    FacetResultRow(facet, icon, onSelect)
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        modifier = Modifier.padding(
            start = MementoSpacing.normal,
            top = MementoSpacing.large,
            end = MementoSpacing.normal,
            bottom = MementoSpacing.small,
        ),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun MediaResultRow(result: MediaSearchResult, onOpenMedia: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMedia(result.mediaId) }
            .padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(58.dp)
                .background(MaterialTheme.mediaTypeColor(result.mediaType), MaterialTheme.shapes.small),
        )
        PosterArtwork(
            type = result.mediaType,
            title = result.title,
            imageUrl = result.posterUrl,
            modifier = Modifier.size(width = 40.dp, height = 58.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(result.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                mediaSupportingText(result),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun mediaSupportingText(result: MediaSearchResult): String {
    val reason = result.matchReasons.firstOrNull { it !is SearchMatchReason.Title }
    val match = when (reason) {
        null, SearchMatchReason.Title -> null
        is SearchMatchReason.Tag -> stringResource(R.string.global_search_match_tag, reason.name)
        is SearchMatchReason.Genre -> stringResource(R.string.global_search_match_genre, reason.name)
        is SearchMatchReason.Creator -> stringResource(
            R.string.global_search_match_creator,
            creatorRoleLabel(reason.role),
            reason.name,
        )
    }
    return listOfNotNull(mediaTypeLabel(result.mediaType), match).joinToString(" · ")
}

@Composable
private fun FacetResultRow(facet: FacetSearchResult, icon: ImageVector, onSelect: (FacetSearchResult) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(facet) }
            .padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
    ) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(MementoSpacing.small))
        }
        Column(Modifier.weight(1f)) {
            Text(facet.name, fontWeight = FontWeight.SemiBold)
            val role = facet.creatorRole?.let { creatorRoleLabel(it) }
            Text(
                listOfNotNull(
                    role,
                    pluralStringResource(R.plurals.global_search_media_count, facet.mediaCount, facet.mediaCount),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TextResultRow(result: TextSearchResult, icon: ImageVector, onOpenMedia: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMedia(result.mediaId) }
            .padding(horizontal = MementoSpacing.normal, vertical = MementoSpacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.mediaTypeColor(result.mediaType))
        Column(Modifier.weight(1f)) {
            Text(result.mediaTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                result.excerpt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchHint(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(MementoSpacing.large),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun creatorRoleLabel(role: CreatorRole): String = stringResource(
    when (role) {
        CreatorRole.AUTHOR -> R.string.creator_role_author
        CreatorRole.DIRECTOR -> R.string.creator_role_director
        CreatorRole.DEVELOPER -> R.string.creator_role_developer
        CreatorRole.CREATOR, CreatorRole.OTHER -> R.string.creator_role_series_creator
    },
)
