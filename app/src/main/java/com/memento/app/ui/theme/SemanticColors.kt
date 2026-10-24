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
    val success: Color,
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
    success = Color(0xFF48623F),
    book = Color(0xFF6E6259), onBook = Color(0xFFFFF8F3),
    movie = Color(0xFF6E6259), onMovie = Color(0xFFFFF8F3),
    series = Color(0xFF6E6259), onSeries = Color(0xFFFFF8F3),
    game = Color(0xFF6E6259), onGame = Color(0xFFFFF8F3),
)

private val DarkSemanticColors = MementoSemanticColors(
    success = Color(0xFFA3B48E),
    book = Color(0xFFB5A69B), onBook = Color(0xFF181412),
    movie = Color(0xFFB5A69B), onMovie = Color(0xFF181412),
    series = Color(0xFFB5A69B), onSeries = Color(0xFF181412),
    game = Color(0xFFB5A69B), onGame = Color(0xFF181412),
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

val MaterialTheme.archiveSuccess: Color
    @Composable
    @ReadOnlyComposable
    get() = mementoSemanticColors.success

@Composable
@ReadOnlyComposable
fun MaterialTheme.mediaTypeColor(type: MediaType): Color = mementoSemanticColors.color(type)

@Composable
@ReadOnlyComposable
fun MaterialTheme.onMediaTypeColor(type: MediaType): Color = mementoSemanticColors.onColor(type)

fun semanticColorsFor(dark: Boolean): MementoSemanticColors =
    if (dark) DarkSemanticColors else LightSemanticColors
