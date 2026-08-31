package com.memento.app.data.remote.mapper

import com.memento.app.data.remote.api.TmdbWatchProviderDto
import com.memento.app.data.remote.api.TmdbWatchProvidersResponse
import com.memento.app.domain.watch.WatchAvailability
import com.memento.app.domain.watch.WatchProvider
import java.util.Locale

fun TmdbWatchProvidersResponse.toWatchAvailability(region: String): WatchAvailability? {
    val normalizedRegion = region.uppercase(Locale.ROOT)
    val regional = results.entries.firstOrNull { it.key.equals(normalizedRegion, ignoreCase = true) }?.value
        ?: return null
    val streaming = regional.flatrate.toProviders()
    val rent = regional.rent.toProviders()
    val buy = regional.buy.toProviders()
    if (streaming.isEmpty() && rent.isEmpty() && buy.isEmpty()) return null
    return WatchAvailability(
        region = normalizedRegion,
        link = regional.link?.trim()?.takeIf(String::isNotEmpty),
        streaming = streaming,
        rent = rent,
        buy = buy,
    )
}

private fun List<TmdbWatchProviderDto>.toProviders(): List<WatchProvider> = asSequence()
    .filter { it.providerId > 0 && it.providerName.isNotBlank() }
    .sortedWith(
        compareBy<TmdbWatchProviderDto> { it.displayPriority ?: Int.MAX_VALUE }
            .thenBy { it.providerName.lowercase(Locale.ROOT) }
            .thenBy(TmdbWatchProviderDto::providerId),
    )
    .distinctBy(TmdbWatchProviderDto::providerId)
    .map { provider ->
        WatchProvider(
            id = provider.providerId,
            name = provider.providerName.trim(),
            logoPath = provider.logoPath?.trim()?.takeIf(String::isNotEmpty),
            displayPriority = provider.displayPriority,
        )
    }
    .toList()
