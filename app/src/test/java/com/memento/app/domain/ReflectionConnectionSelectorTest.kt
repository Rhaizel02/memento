package com.memento.app.domain

import com.memento.app.domain.insight.ReflectionConnectionSelector
import com.memento.app.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ReflectionConnectionSelectorTest {
    @Test fun `connection always selects a reflection from another work`() {
        val current = detail("m1", "Actual", "r1", "La memoria transforma nuestra identidad")
        val other = detail("m2", "Otra", "r2", "La identidad y la memoria cambian con el tiempo")

        val result = ReflectionConnectionSelector.select("m1", current.reflections.single().content, listOf(current, other))

        assertEquals("r2", result?.reflection?.id)
        assertEquals("Otra", result?.mediaTitle)
    }

    @Test fun `connection does not reuse the same work`() {
        val current = detail("m1", "Actual", "r1", "memoria identidad")
        assertNull(ReflectionConnectionSelector.select("m1", "memoria identidad", listOf(current)))
    }

    private fun detail(mediaId: String, title: String, reflectionId: String, content: String): MediaDetail {
        val consumption = Consumption("c-$mediaId", mediaId, ConsumptionStatus.COMPLETED, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)
        return MediaDetail(
            MediaItem(mediaId, MediaType.BOOK, title, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH),
            emptyList(), emptyList(), listOf(consumption), emptyList(),
            listOf(Reflection(reflectionId, consumption.id, ReflectionType.FINAL_REFLECTION, content, Instant.EPOCH, Instant.EPOCH)),
        )
    }
}
