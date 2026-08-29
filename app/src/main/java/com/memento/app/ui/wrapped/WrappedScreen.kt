package com.memento.app.ui.wrapped

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import com.memento.app.R
import com.memento.app.domain.wrapped.WrappedSlide
import com.memento.app.ui.components.formatHalfStars
import com.memento.app.ui.components.mediaTypeLabel
import com.memento.app.ui.theme.MementoSpacing
import kotlinx.coroutines.launch
import com.memento.app.share.ShareCardContent
import com.memento.app.share.ShareCardRenderer
import com.memento.app.share.sharePng
import androidx.compose.runtime.remember

@Composable
fun WrappedScreen(state: WrappedUiState, onBack: () -> Unit) {
    val story = state.story ?: return
    val pagerState = rememberPagerState(pageCount = { story.slides.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val renderer = remember(context) { ShareCardRenderer(context.applicationContext) }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)),
        ),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            WrappedPage(story.slides[page])
        }
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(MementoSpacing.normal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.slide_position, pagerState.currentPage + 1, story.slides.size))
            val shareContent = wrappedShareContent(story.slides[pagerState.currentPage])
            IconButton(
                onClick = {
                    scope.launch {
                        val uri = renderer.render(shareContent, "memento-wrapped-${story.year}-${pagerState.currentPage + 1}")
                        sharePng(context, uri)
                    }
                },
            ) { Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share_current_slide)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(MementoSpacing.large),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                enabled = pagerState.currentPage > 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
            ) { Icon(Icons.Outlined.ChevronLeft, contentDescription = stringResource(R.string.previous_slide)) }
            if (pagerState.currentPage == story.slides.lastIndex) {
                Button(onClick = onBack) { Text(stringResource(R.string.finish)) }
            } else {
                IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.next_slide))
                }
            }
        }
    }
}

@Composable
private fun wrappedShareContent(slide: WrappedSlide): ShareCardContent = when (slide) {
    is WrappedSlide.Cover -> ShareCardContent(
        title = slide.year.toString(),
        subtitle = stringResource(R.string.wrapped_cover),
        body = stringResource(R.string.wrapped_share_intro),
    )
    is WrappedSlide.Completed -> {
        val typeSummary = slide.byType.filterValues { it > 0 }.entries.map { entry ->
            "${entry.value} ${mediaTypeLabel(entry.key)}"
        }.joinToString(" · ")
        ShareCardContent(
            title = slide.count.toString(),
            subtitle = stringResource(R.string.wrapped_completed),
            body = typeSummary,
        )
    }
    is WrappedSlide.TopGenre -> ShareCardContent(slide.name, stringResource(R.string.wrapped_top_genre, slide.count), stringResource(R.string.wrapped_share_genre))
    is WrappedSlide.BestRated -> ShareCardContent(slide.work.title, stringResource(R.string.wrapped_best_rated, slide.work.ratingHalfStars?.let(::formatHalfStars).orEmpty()), stringResource(R.string.wrapped_share_best))
    is WrappedSlide.TopCreator -> ShareCardContent(slide.name, stringResource(R.string.wrapped_top_creator, slide.count), stringResource(R.string.wrapped_share_creator))
    is WrappedSlide.GameTime -> ShareCardContent(stringResource(R.string.number_one_decimal, slide.hours), stringResource(R.string.wrapped_game_time), stringResource(R.string.wrapped_share_game))
    is WrappedSlide.Reflections -> ShareCardContent(slide.count.toString(), stringResource(R.string.wrapped_reflections), stringResource(R.string.wrapped_share_reflections))
    is WrappedSlide.ReflectionSpotlight -> ShareCardContent(slide.workTitle, stringResource(R.string.wrapped_reflection_spotlight), slide.content)
    is WrappedSlide.Revisited -> ShareCardContent(slide.work.title, stringResource(R.string.wrapped_revisited), stringResource(R.string.wrapped_share_revisited))
    is WrappedSlide.Finale -> ShareCardContent(slide.year.toString(), stringResource(R.string.wrapped_finale, slide.completed, slide.favoriteCount), stringResource(R.string.wrapped_share_finale))
}

@Composable
private fun WrappedPage(slide: WrappedSlide) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = MementoSpacing.huge, vertical = MementoSpacing.huge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (slide) {
            is WrappedSlide.Cover -> {
                Hero(slide.year.toString(), stringResource(R.string.wrapped_cover))
            }
            is WrappedSlide.Completed -> {
                Hero(slide.count.toString(), stringResource(R.string.wrapped_completed))
                slide.byType.filterValues { it > 0 }.forEach { (type, count) ->
                    Text(stringResource(R.string.media_count_format, mediaTypeLabel(type), count), style = MaterialTheme.typography.titleMedium)
                }
            }
            is WrappedSlide.TopGenre -> Hero(slide.name, stringResource(R.string.wrapped_top_genre, slide.count))
            is WrappedSlide.BestRated -> Hero(
                slide.work.title,
                stringResource(R.string.wrapped_best_rated, slide.work.ratingHalfStars?.let(::formatHalfStars).orEmpty()),
            )
            is WrappedSlide.TopCreator -> Hero(slide.name, stringResource(R.string.wrapped_top_creator, slide.count))
            is WrappedSlide.GameTime -> Hero(
                stringResource(R.string.number_one_decimal, slide.hours),
                stringResource(R.string.wrapped_game_time),
            )
            is WrappedSlide.Reflections -> Hero(slide.count.toString(), stringResource(R.string.wrapped_reflections))
            is WrappedSlide.ReflectionSpotlight -> {
                Text(stringResource(R.string.wrapped_reflection_spotlight), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.quoted_reflection, slide.content),
                    modifier = Modifier.padding(top = MementoSpacing.large),
                    style = MaterialTheme.typography.headlineSmall,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                )
                if (slide.workTitle.isNotBlank()) Text(slide.workTitle, modifier = Modifier.padding(top = MementoSpacing.normal))
            }
            is WrappedSlide.Revisited -> Hero(slide.work.title, stringResource(R.string.wrapped_revisited))
            is WrappedSlide.Finale -> Hero(
                slide.year.toString(),
                stringResource(R.string.wrapped_finale, slide.completed, slide.favoriteCount),
            )
        }
    }
}

@Composable
private fun Hero(value: String, caption: String) {
    Text(
        value,
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
    Text(
        caption,
        modifier = Modifier.padding(top = MementoSpacing.large),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
}
