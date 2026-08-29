package com.memento.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.theme.MementoTheme
import com.memento.app.data.preferences.ThemeMode
import java.time.Instant
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun MediaCard(
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    Card(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column {
            PosterArtwork(media.type, media.title, media.posterUrl, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
            Column(Modifier.padding(MementoSpacing.medium)) {
                Text(
                    media.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                supportingText?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun PosterArtwork(
    type: MediaType,
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        PosterFallback(type, title, Modifier.fillMaxSize())
        imageUrl?.takeIf(String::isNotBlank)?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = title,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun PosterFallback(type: MediaType, title: String, modifier: Modifier = Modifier) {
    val icon = type.icon()
    val background = when (type) {
        MediaType.BOOK -> Color(0xFF6D4C54)
        MediaType.MOVIE -> Color(0xFF334C5A)
        MediaType.SERIES -> Color(0xFF4C4566)
        MediaType.GAME -> Color(0xFF3F5B4E)
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(MementoSpacing.normal),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(44.dp))
            Text(
                title,
                modifier = Modifier.padding(top = MementoSpacing.medium),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(MementoSpacing.large)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                body,
                modifier = Modifier.padding(top = MementoSpacing.small),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            action?.let { Row(Modifier.padding(top = MementoSpacing.normal)) { it() } }
        }
    }
}

@Composable
fun StaticTag(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = MementoSpacing.medium, vertical = MementoSpacing.small),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
fun RatingText(halfStars: Int?, modifier: Modifier = Modifier) {
    val text = halfStars?.let { stringResource(R.string.rating_format, formatHalfStars(it)) }
        ?: stringResource(R.string.no_rating)
    Text(text, modifier = modifier, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
}

fun formatHalfStars(value: Int): String = if (value % 2 == 0) "${value / 2}" else "${value / 2}.5"

@Composable
fun mediaTypeLabel(type: MediaType): String = stringResource(
    when (type) {
        MediaType.BOOK -> R.string.book
        MediaType.MOVIE -> R.string.movie
        MediaType.SERIES -> R.string.series_singular
        MediaType.GAME -> R.string.game
    },
)

fun MediaType.icon(): ImageVector = when (this) {
    MediaType.BOOK -> Icons.Outlined.AutoStories
    MediaType.MOVIE -> Icons.Outlined.Movie
    MediaType.SERIES -> Icons.Outlined.Tv
    MediaType.GAME -> Icons.Outlined.SportsEsports
}

@Preview
@Composable
private fun MediaCardPreview() {
    MementoTheme(ThemeMode.LIGHT) {
        MediaCard(
            media = MediaItem(
                id = "preview",
                type = MediaType.BOOK,
                title = "El camino de los reyes",
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
            onClick = {},
            supportingText = "En progreso",
            modifier = Modifier.size(width = 180.dp, height = 320.dp),
        )
    }
}
