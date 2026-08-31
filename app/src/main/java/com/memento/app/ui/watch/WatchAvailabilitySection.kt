package com.memento.app.ui.watch

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.memento.app.R
import com.memento.app.domain.watch.WatchAvailability
import com.memento.app.domain.watch.WatchAvailabilityResult
import com.memento.app.domain.watch.WatchProvider
import com.memento.app.ui.theme.MementoSpacing

sealed interface WatchAvailabilityUiState {
    data object Hidden : WatchAvailabilityUiState
    data object Loading : WatchAvailabilityUiState
    data class Empty(val region: String) : WatchAvailabilityUiState
    data class Available(val availability: WatchAvailability) : WatchAvailabilityUiState
}

fun WatchAvailabilityResult.toUiState(): WatchAvailabilityUiState = when (this) {
    is WatchAvailabilityResult.Available -> WatchAvailabilityUiState.Available(availability)
    is WatchAvailabilityResult.Empty -> WatchAvailabilityUiState.Empty(region)
    WatchAvailabilityResult.Unsupported -> WatchAvailabilityUiState.Hidden
}

@Composable
fun WatchAvailabilitySection(
    state: WatchAvailabilityUiState,
    modifier: Modifier = Modifier,
) {
    if (state == WatchAvailabilityUiState.Hidden) return
    Card(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(MementoSpacing.normal),
            verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
        ) {
            Text(stringResource(R.string.watch_availability_title), style = MaterialTheme.typography.titleLarge)
            when (state) {
                WatchAvailabilityUiState.Hidden -> Unit
                WatchAvailabilityUiState.Loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium),
                ) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.watch_availability_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is WatchAvailabilityUiState.Empty -> {
                    Text(
                        stringResource(R.string.watch_availability_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    JustWatchAttribution()
                }
                is WatchAvailabilityUiState.Available -> AvailableProviders(state.availability)
            }
        }
    }
}

@Composable
private fun AvailableProviders(availability: WatchAvailability) {
    ProviderCategory(stringResource(R.string.watch_streaming), availability.streaming)
    ProviderCategory(stringResource(R.string.watch_rent), availability.rent)
    ProviderCategory(stringResource(R.string.watch_buy), availability.buy)
    val uriHandler = LocalUriHandler.current
    availability.link?.takeIf(::isSafeExternalUrl)?.let { link ->
        TextButton(onClick = { runCatching { uriHandler.openUri(link) } }) {
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
            Text(
                stringResource(R.string.watch_view_options),
                modifier = Modifier.padding(start = MementoSpacing.small),
            )
        }
    }
    JustWatchAttribution()
}

@Composable
private fun ProviderCategory(title: String, providers: List<WatchProvider>) {
    if (providers.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
            items(providers, key = WatchProvider::id) { provider -> ProviderItem(provider) }
        }
    }
}

@Composable
private fun ProviderItem(provider: WatchProvider) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MementoSpacing.medium, vertical = MementoSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small),
        ) {
            provider.logoPath?.let { path ->
                AsyncImage(
                    model = "$TMDB_LOGO_BASE$path",
                    contentDescription = null,
                    modifier = Modifier.size(28.dp).clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                provider.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun JustWatchAttribution() {
    Text(
        stringResource(R.string.watch_justwatch_attribution),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun isSafeExternalUrl(value: String): Boolean = runCatching {
    val uri = Uri.parse(value)
    (uri.scheme == "https" || uri.scheme == "http") && !uri.host.isNullOrBlank()
}.getOrDefault(false)

private const val TMDB_LOGO_BASE = "https://image.tmdb.org/t/p/w92"
