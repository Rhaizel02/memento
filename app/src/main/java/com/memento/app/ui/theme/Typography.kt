package com.memento.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.memento.app.R

val Newsreader = FontFamily(
    Font(R.font.newsreader, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.newsreader, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
)

val WorkSans = FontFamily(
    Font(R.font.work_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.work_sans, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.work_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

private fun editorial(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Newsreader, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)
private fun functional(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = WorkSans, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

val MementoTypography = Typography(
    displayLarge = editorial(48, 56),
    displayMedium = editorial(40, 48),
    displaySmall = editorial(36, 44),
    headlineLarge = editorial(32, 40),
    headlineMedium = editorial(28, 36),
    headlineSmall = editorial(24, 32, FontWeight.Medium),
    titleLarge = editorial(24, 32, FontWeight.Medium),
    titleMedium = editorial(20, 28, FontWeight.Medium),
    titleSmall = functional(16, 24, FontWeight.Medium),
    bodyLarge = functional(16, 24),
    bodyMedium = functional(14, 20),
    bodySmall = functional(14, 20),
    labelLarge = functional(14, 20, FontWeight.SemiBold),
    labelMedium = functional(14, 20, FontWeight.Medium),
    labelSmall = functional(12, 16, FontWeight.Medium),
)

val EditorialQuoteStyle = editorial(18, 28)
