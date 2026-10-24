package com.memento.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.data.preferences.ThemePalette

private val MementoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

object MementoSpacing {
    val xSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val normal = 20.dp
    val gutterCompact = 16.dp
    val editorial = 40.dp
    val large = 24.dp
    val xLarge = 32.dp
    val huge = 48.dp
}

@Composable
fun MementoTheme(
    themeMode: ThemeMode,
    palette: ThemePalette = ThemePalette.MEMENTO,
    content: @Composable () -> Unit,
) {
    val dark = resolveDarkTheme(themeMode, isSystemInDarkTheme())
    ApplySystemBars(dark)
    CompositionLocalProvider(
        LocalMementoSemanticColors provides semanticColorsFor(dark),
        LocalMementoThemeInfo provides MementoThemeInfo(isDark = dark),
    ) {
        MaterialTheme(
            colorScheme = colorSchemeFor(palette, dark),
            typography = MementoTypography,
            shapes = MementoShapes,
            content = content,
        )
    }
}

fun resolveDarkTheme(themeMode: ThemeMode, systemDark: Boolean): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
private fun ApplySystemBars(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val activity = view.context.findActivity() as? ComponentActivity ?: return
    SideEffect {
        val transparent = Color.TRANSPARENT
        val style = SystemBarStyle.auto(transparent, transparent) { dark }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
