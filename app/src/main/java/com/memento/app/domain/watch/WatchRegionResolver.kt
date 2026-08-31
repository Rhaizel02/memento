package com.memento.app.domain.watch

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

fun interface WatchRegionResolver {
    fun resolve(): String
}

@Singleton
class LocaleWatchRegionResolver @Inject constructor() : WatchRegionResolver {
    override fun resolve(): String = resolve(Locale.getDefault())

    companion object {
        private const val FALLBACK_REGION = "ES"

        fun resolve(locale: Locale?): String = locale?.country
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.takeIf { it.length == 2 && it.all(Char::isLetter) }
            ?: FALLBACK_REGION
    }
}
