package com.memento.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.memento.app.domain.model.MediaType

@Immutable
data class MementoSemanticColors(
    val book: Color,
    val onBook: Color,
    val movie: Color,
    val onMovie: Color,
    val series: Color,
    val onSeries: Color,
    val game: Color,
    val onGame: Color,
) {
    fun color(type: MediaType): Color = when (type) {
        MediaType.BOOK -> book
        MediaType.MOVIE -> movie
        MediaType.SERIES -> series
        MediaType.GAME -> game
    }

    fun onColor(type: MediaType): Color = when (type) {
        MediaType.BOOK -> onBook
        MediaType.MOVIE -> onMovie
        MediaType.SERIES -> onSeries
        MediaType.GAME -> onGame
    }
}

@Immutable
data class MementoThemeInfo(
    val isDark: Boolean,
)

private val LightSemanticColors = MementoSemanticColors(
    book = Color(0xFF775A15),
    onBook = Color(0xFFFFFFFF),
    movie = Color(0xFF842F4A),
    onMovie = Color(0xFFFFFFFF),
    series = Color(0xFF4F4B8B),
    onSeries = Color(0xFFFFFFFF),
    game = Color(0xFF006B61),
    onGame = Color(0xFFFFFFFF),
)

private val DarkSemanticColors = MementoSemanticColors(
    book = Color(0xFFE8C36A),
    onBook = Color(0xFF3E2E00),
    movie = Color(0xFFF1B4C3),
    onMovie = Color(0xFF54152A),
    series = Color(0xFFC9C4FF),
    onSeries = Color(0xFF302B68),
    game = Color(0xFF70D7C9),
    onGame = Color(0xFF003731),
)

internal val LocalMementoSemanticColors = staticCompositionLocalOf { LightSemanticColors }
internal val LocalMementoThemeInfo = staticCompositionLocalOf { MementoThemeInfo(isDark = false) }

val MaterialTheme.mementoSemanticColors: MementoSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMementoSemanticColors.current

val MaterialTheme.mementoThemeInfo: MementoThemeInfo
    @Composable
    @ReadOnlyComposable
    get() = LocalMementoThemeInfo.current

@Composable
@ReadOnlyComposable
fun MaterialTheme.mediaTypeColor(type: MediaType): Color = mementoSemanticColors.color(type)

@Composable
@ReadOnlyComposable
fun MaterialTheme.onMediaTypeColor(type: MediaType): Color = mementoSemanticColors.onColor(type)

fun semanticColorsFor(dark: Boolean): MementoSemanticColors =
    if (dark) DarkSemanticColors else LightSemanticColors
