package com.memento.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.memento.app.data.preferences.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF745166),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3DDE8),
    onPrimaryContainer = Color(0xFF2C1724),
    secondary = Color(0xFF7B5E46),
    onSecondary = Color.White,
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF241F21),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF241F21),
    surfaceVariant = Color(0xFFF0E5E5),
    onSurfaceVariant = Color(0xFF50464B),
    outline = Color(0xFF82747A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE2B8CF),
    onPrimary = Color(0xFF422536),
    primaryContainer = Color(0xFF5A3B4D),
    onPrimaryContainer = Color(0xFFFED8EB),
    secondary = Color(0xFFDDBD9F),
    onSecondary = Color(0xFF3E2D20),
    background = Color(0xFF1B1719),
    onBackground = Color(0xFFEDE0E4),
    surface = Color(0xFF1B1719),
    onSurface = Color(0xFFEDE0E4),
    surfaceVariant = Color(0xFF332D30),
    onSurfaceVariant = Color(0xFFD4C3C9),
    outline = Color(0xFF9D8D93),
)

private val MementoTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
)

private val MementoShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
)

object MementoSpacing {
    val xSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val normal = 16.dp
    val large = 24.dp
    val xLarge = 32.dp
    val huge = 48.dp
}

@Composable
fun MementoTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = MementoTypography,
        shapes = MementoShapes,
        content = content,
    )
}

