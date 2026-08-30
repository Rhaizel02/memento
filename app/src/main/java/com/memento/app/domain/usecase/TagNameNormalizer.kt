package com.memento.app.domain.usecase

import java.util.Locale

object TagNameNormalizer {
    private val whitespace = Regex("\\s+")

    fun displayName(name: String): String = name.trim().replace(whitespace, " ")

    fun normalize(name: String): String = displayName(name).lowercase(Locale.ROOT)
}
