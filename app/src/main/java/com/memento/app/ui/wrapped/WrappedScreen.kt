package com.memento.app.ui.wrapped

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.memento.app.R
import com.memento.app.domain.culturalprofile.CulturalInsight
import com.memento.app.domain.culturalprofile.CulturalPeriod
import com.memento.app.domain.culturalprofile.CulturalPeriodKind
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.wrapped.WrappedCard
import com.memento.app.domain.wrapped.WrappedReflection
import com.memento.app.domain.wrapped.WrappedSharePolicy
import com.memento.app.domain.wrapped.WrappedSnapshot
import com.memento.app.domain.wrapped.WrappedWork
import com.memento.app.share.ShareCardContent
import com.memento.app.share.ShareCardRenderer
import com.memento.app.share.sharePng
import com.memento.app.ui.components.PosterArtwork
import com.memento.app.ui.components.formatHalfStars
import com.memento.app.ui.components.mediaTypeLabel
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.theme.mediaTypeColor
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun WrappedScreen(
    state: WrappedUiState,
    onBack: () -> Unit,
    onSelectYear: (Int) -> Unit,
) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val snapshot = state.snapshot
    if (snapshot == null) {
        WrappedEmpty(onBack)
        return
    }

    val pagerState = rememberPagerState(pageCount = { snapshot.cards.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val renderer = remember(context) { ShareCardRenderer(context.applicationContext) }
    var yearMenuExpanded by remember { mutableStateOf(false) }
    var explicitShare by remember { mutableStateOf<ShareCardContent?>(null) }

    LaunchedEffect(snapshot.year) { pagerState.scrollToPage(0) }

    fun share(content: ShareCardContent) {
        scope.launch {
            val uri = renderer.render(
                content,
                "memento-wrapped-${snapshot.year}-${pagerState.currentPage + 1}",
            )
            sharePng(context, uri)
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            WrappedPage(snapshot.cards[page])
        }

        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(MementoSpacing.normal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Box {
                TextButton(onClick = { yearMenuExpanded = true }) {
                    Text(snapshot.year.toString())
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = stringResource(R.string.wrapped_select_year))
                }
                DropdownMenu(expanded = yearMenuExpanded, onDismissRequest = { yearMenuExpanded = false }) {
                    state.availableYears.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year.toString()) },
                            onClick = {
                                yearMenuExpanded = false
                                onSelectYear(year)
                            },
                        )
                    }
                }
            }
            val currentCard = snapshot.cards[pagerState.currentPage]
            val shareContent = wrappedShareContent(currentCard, snapshot)
            if (shareContent != null) {
                IconButton(
                    onClick = {
                        if (currentCard.sharePolicy == WrappedSharePolicy.EXPLICIT_REFLECTION) {
                            explicitShare = shareContent
                        } else {
                            share(shareContent)
                        }
                    },
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share_current_slide))
                }
            } else {
                Box(Modifier.size(48.dp))
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(MementoSpacing.normal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.slide_position, pagerState.currentPage + 1, snapshot.cards.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    enabled = pagerState.currentPage > 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                ) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = stringResource(R.string.previous_slide))
                }
                if (pagerState.currentPage == snapshot.cards.lastIndex) {
                    Button(onClick = onBack) { Text(stringResource(R.string.finish)) }
                } else {
                    IconButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    ) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.next_slide))
                    }
                }
            }
        }
    }

    explicitShare?.let { content ->
        AlertDialog(
            onDismissRequest = { explicitShare = null },
            title = { Text(stringResource(R.string.wrapped_share_reflection_title)) },
            text = { Text(stringResource(R.string.wrapped_share_reflection_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        explicitShare = null
                        share(content)
                    },
                ) { Text(stringResource(R.string.share)) }
            },
            dismissButton = {
                TextButton(onClick = { explicitShare = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun WrappedEmpty(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        IconButton(onClick = onBack, modifier = Modifier.padding(MementoSpacing.normal)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Column(
            Modifier.align(Alignment.Center).padding(MementoSpacing.xLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.wrapped_empty_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.wrapped_empty_body),
                modifier = Modifier.padding(top = MementoSpacing.normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WrappedPage(card: WrappedCard) {
    val imageUrl = when (card) {
        is WrappedCard.Cover -> card.heroImageUrl
        is WrappedCard.WorkOfYear -> card.work.backdropUrl ?: card.work.posterUrl
        is WrappedCard.ReflectionSpotlight -> card.reflection.posterUrl
        else -> null
    }
    Box(Modifier.fillMaxSize()) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (imageUrl == null) 1f else 0.82f),
                        MaterialTheme.colorScheme.surface.copy(alpha = if (imageUrl == null) 1f else 0.94f),
                    ),
                ),
            ),
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = MementoSpacing.xLarge, vertical = 96.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (card) {
                is WrappedCard.Cover -> CoverPage(card)
                is WrappedCard.MediaSummary -> MediaSummaryPage(card)
                is WrappedCard.GenreOfYear -> EditorialPage(
                    eyebrow = stringResource(R.string.wrapped_genre_of_year),
                    hero = card.genre.label,
                    body = card.genre.averageRating?.let { rating ->
                        stringResource(R.string.wrapped_genre_body, card.genre.count, rating)
                    } ?: pluralStringResource(
                        R.plurals.cultural_works_count,
                        card.genre.count,
                        card.genre.count,
                    ),
                )
                is WrappedCard.BestRatedMedium -> EditorialPage(
                    eyebrow = stringResource(R.string.wrapped_best_medium),
                    hero = mediaTypeLabel(card.medium.mediaType),
                    body = stringResource(R.string.wrapped_best_medium_body, card.medium.averageRating ?: 0.0),
                    accent = MaterialTheme.mediaTypeColor(card.medium.mediaType),
                )
                is WrappedCard.FeaturedCreator -> EditorialPage(
                    eyebrow = stringResource(R.string.wrapped_featured_creator),
                    hero = card.creator.label,
                    body = metricBody(card.creator.count, card.creator.averageRating),
                )
                is WrappedCard.PersonalTags -> TagsPage(card)
                is WrappedCard.IntenseMonth -> EditorialPage(
                    eyebrow = stringResource(R.string.wrapped_intense_month),
                    hero = monthName(card.month),
                    body = pluralStringResource(
                        R.plurals.wrapped_finished_in_month,
                        card.completedWorks,
                        card.completedWorks,
                    ),
                )
                is WrappedCard.CulturalEra -> EraPage(card.period)
                is WrappedCard.WorkOfYear -> WorkPage(card.work)
                is WrappedCard.ReflectionSpotlight -> ReflectionPage(card.reflection)
                is WrappedCard.Favorites -> FavoritesPage(card.works)
                is WrappedCard.Comparisons -> ComparisonsPage(card.insights)
                is WrappedCard.Finale -> EditorialPage(
                    eyebrow = stringResource(R.string.app_name),
                    hero = card.year.toString(),
                    body = stringResource(R.string.wrapped_finale, card.completed, card.favoriteCount),
                )
            }
        }
    }
}

@Composable
private fun CoverPage(card: WrappedCard.Cover) {
    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(
        if (card.isYearToDate) stringResource(R.string.wrapped_year_to_date, card.year)
        else stringResource(R.string.wrapped_year, card.year),
        modifier = Modifier.padding(top = MementoSpacing.normal),
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Text(
        if (card.storyCount < 3) stringResource(R.string.wrapped_taking_shape, card.year)
        else pluralStringResource(R.plurals.wrapped_stories, card.storyCount, card.storyCount),
        modifier = Modifier.padding(top = MementoSpacing.large),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun MediaSummaryPage(card: WrappedCard.MediaSummary) {
    Text(stringResource(R.string.wrapped_media_summary), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = MementoSpacing.large),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
    ) {
        card.byType.filterValues { it > 0 }.forEach { (type, count) ->
            Surface(
                color = MaterialTheme.mediaTypeColor(type).copy(alpha = 0.22f),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(MementoSpacing.normal),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(mediaTypeLabel(type), style = MaterialTheme.typography.titleLarge)
                    Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditorialPage(
    eyebrow: String,
    hero: String,
    body: String,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = accent)
    Text(
        hero,
        modifier = Modifier.padding(top = MementoSpacing.normal),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Text(
        body,
        modifier = Modifier.padding(top = MementoSpacing.large),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TagsPage(card: WrappedCard.PersonalTags) {
    EditorialPage(
        eyebrow = stringResource(R.string.wrapped_personal_tags),
        hero = card.mostUsed.label,
        body = card.bestRated?.let {
            stringResource(R.string.wrapped_tag_rating_body, it.count, it.averageRating ?: 0.0)
        } ?: pluralStringResource(
            R.plurals.wrapped_tag_count_body,
            card.mostUsed.count,
            card.mostUsed.count,
        ),
    )
}

@Composable
private fun EraPage(period: CulturalPeriod) {
    val title = when (period.kind) {
        CulturalPeriodKind.GENRE -> stringResource(R.string.wrapped_era_genre, period.label)
        CulturalPeriodKind.TAG -> stringResource(R.string.wrapped_era_tag, period.label)
        CulturalPeriodKind.MEDIA_TYPE -> stringResource(R.string.wrapped_era_medium, period.label.lowercase())
    }
    EditorialPage(
        eyebrow = stringResource(R.string.wrapped_cultural_era),
        hero = title,
        body = buildString {
            append(stringResource(R.string.wrapped_era_dates, shortMonth(period.from), shortMonth(period.until)))
            append("\n")
            append(pluralStringResource(R.plurals.cultural_works_count, period.matchingWorks, period.matchingWorks))
            period.averageRating?.let { append(" · ★%.1f".format(spanishLocale, it)) }
        },
        accent = period.mediaType?.let { MaterialTheme.mediaTypeColor(it) } ?: MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun WorkPage(work: WrappedWork) {
    Text(stringResource(R.string.wrapped_work_of_year), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    PosterArtwork(
        type = work.mediaType,
        title = work.title,
        imageUrl = work.posterUrl,
        modifier = Modifier.padding(top = MementoSpacing.normal).width(180.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(20.dp)),
    )
    Text(
        work.title,
        modifier = Modifier.padding(top = MementoSpacing.large),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    val favoriteLabel = stringResource(R.string.timeline_favorite)
    val signals = buildList {
        work.ratingHalfStars?.let { add("★${formatHalfStars(it)}") }
        if (work.isFavorite) add(favoriteLabel)
        addAll(work.tags.take(2))
    }
    if (signals.isNotEmpty()) {
        Text(
            signals.joinToString(" · "),
            modifier = Modifier.padding(top = MementoSpacing.medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReflectionPage(reflection: WrappedReflection) {
    Text(stringResource(R.string.wrapped_reflection_spotlight), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Text(
        stringResource(R.string.quoted_reflection, reflection.content),
        modifier = Modifier.padding(top = MementoSpacing.large),
        style = MaterialTheme.typography.headlineSmall,
        fontStyle = FontStyle.Italic,
        textAlign = TextAlign.Center,
    )
    Text(
        reflection.workTitle,
        modifier = Modifier.padding(top = MementoSpacing.large),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun FavoritesPage(works: List<WrappedWork>) {
    Text(stringResource(R.string.wrapped_favorites), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = MementoSpacing.large),
        horizontalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
    ) {
        works.forEach { work ->
            Column(Modifier.width(124.dp)) {
                PosterArtwork(
                    type = work.mediaType,
                    title = work.title,
                    imageUrl = work.posterUrl,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(16.dp)),
                )
                Text(
                    work.title,
                    modifier = Modifier.padding(top = MementoSpacing.small),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun ComparisonsPage(insights: List<CulturalInsight>) {
    Text(stringResource(R.string.wrapped_comparison), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    insights.forEach { insight ->
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = MementoSpacing.large),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp),
        ) {
            Text(
                comparisonText(insight),
                modifier = Modifier.padding(MementoSpacing.large),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun comparisonText(insight: CulturalInsight): String = when (insight) {
    is CulturalInsight.MediaTypeYearChange -> stringResource(
        R.string.wrapped_media_comparison,
        abs(insight.percentChange),
        if (insight.percentChange >= 0) stringResource(R.string.cultural_more) else stringResource(R.string.cultural_less),
        mediaTypeLabel(insight.mediaType).lowercase(),
        insight.previousYear,
    )
    is CulturalInsight.TotalYearChange -> stringResource(
        R.string.wrapped_total_comparison,
        abs(insight.percentChange),
        if (insight.percentChange >= 0) stringResource(R.string.cultural_more) else stringResource(R.string.cultural_less),
        insight.previousYear,
    )
    is CulturalInsight.AverageRatingYearChange -> stringResource(
        R.string.cultural_rating_year_change,
        insight.previousRating,
        insight.previousYear,
        insight.currentRating,
        insight.currentYear,
    )
    else -> ""
}

@Composable
private fun wrappedShareContent(card: WrappedCard, snapshot: WrappedSnapshot): ShareCardContent? = when (card) {
    is WrappedCard.Cover -> ShareCardContent(
        title = if (card.isYearToDate) stringResource(R.string.wrapped_year_to_date, card.year)
        else stringResource(R.string.wrapped_year, card.year),
        subtitle = stringResource(R.string.app_name),
        body = pluralStringResource(R.plurals.wrapped_stories, card.storyCount, card.storyCount),
        imageUrl = card.heroImageUrl,
    )
    is WrappedCard.MediaSummary -> ShareCardContent(
        title = stringResource(R.string.wrapped_year, snapshot.year),
        subtitle = stringResource(R.string.wrapped_media_summary),
        body = mediaSummaryText(card.byType),
    )
    is WrappedCard.WorkOfYear -> {
        val favoriteLabel = stringResource(R.string.timeline_favorite)
        val fallback = stringResource(R.string.wrapped_share_best)
        ShareCardContent(
            title = card.work.title,
            subtitle = stringResource(R.string.wrapped_work_of_year),
            body = buildList {
                card.work.ratingHalfStars?.let { add("★${formatHalfStars(it)}") }
                if (card.work.isFavorite) add(favoriteLabel)
                addAll(card.work.tags.take(2))
            }.joinToString(" · ").ifBlank { fallback },
            imageUrl = card.work.posterUrl,
        )
    }
    is WrappedCard.CulturalEra -> ShareCardContent(
        title = card.period.label,
        subtitle = stringResource(R.string.wrapped_cultural_era),
        body = stringResource(R.string.wrapped_era_dates, shortMonth(card.period.from), shortMonth(card.period.until)),
    )
    is WrappedCard.ReflectionSpotlight -> ShareCardContent(
        title = card.reflection.workTitle,
        subtitle = stringResource(R.string.wrapped_reflection_spotlight),
        body = card.reflection.content,
        imageUrl = card.reflection.posterUrl,
    )
    else -> null
}

@Composable
private fun mediaSummaryText(byType: Map<MediaType, Int>): String {
    val parts = mutableListOf<String>()
    for ((type, count) in byType) {
        if (count > 0) parts += "$count ${mediaTypeLabel(type).lowercase()}"
    }
    return parts.joinToString(" · ")
}

private fun metricBody(count: Int, rating: Double?): String = buildString {
    append("$count obras")
    rating?.let { append(" · ★%.1f".format(spanishLocale, it)) }
}

private fun monthName(month: YearMonth): String = month.month.getDisplayName(TextStyle.FULL, spanishLocale)
    .replaceFirstChar { it.titlecase(spanishLocale) }

private fun shortMonth(month: YearMonth): String = month.format(DateTimeFormatter.ofPattern("MMMM", spanishLocale))
    .replaceFirstChar { it.titlecase(spanishLocale) }

private val spanishLocale = Locale.forLanguageTag("es")
