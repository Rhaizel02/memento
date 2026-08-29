package com.memento.app.domain.insight

import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.Reflection

data class ReflectionConnectionMatch(
    val mediaTitle: String,
    val reflection: Reflection,
    val score: Int,
)

/** Pure local strategy: meaningful word overlap first, recency as deterministic tie-breaker. */
object ReflectionConnectionSelector {
    fun select(currentMediaId: String, currentText: String, library: List<MediaDetail>): ReflectionConnectionMatch? {
        val currentWords = currentText.meaningfulWords()
        return library.asSequence()
            .filter { it.media.id != currentMediaId }
            .flatMap { detail -> detail.reflections.asSequence().map { detail to it } }
            .filter { (_, reflection) -> reflection.content.isNotBlank() }
            .map { (detail, reflection) ->
                val overlap = currentWords.intersect(reflection.content.meaningfulWords()).size
                ReflectionConnectionMatch(detail.media.title, reflection, overlap)
            }
            .filter { it.score > 0 }
            .sortedWith(compareByDescending<ReflectionConnectionMatch> { it.score }
                .thenByDescending { it.reflection.createdAt }
                .thenBy { it.reflection.id })
            .firstOrNull()
    }
}

private fun String.meaningfulWords(): Set<String> = lowercase()
    .split("[^\\p{L}\\p{N}]+".toRegex())
    .asSequence()
    .filter { it.length >= 4 && it !in stopWords }
    .toSet()

private val stopWords = setOf(
    "para", "pero", "como", "esta", "este", "esto", "desde", "sobre", "entre", "porque", "también",
    "with", "that", "this", "from", "have", "were", "about",
)
