package com.memento.app.domain

import com.memento.app.ui.components.formatHalfStars
import org.junit.Assert.assertEquals
import org.junit.Test

class RatingTest {
    @Test fun `half star storage renders exact values`() {
        assertEquals("0.5", formatHalfStars(1))
        assertEquals("4.5", formatHalfStars(9))
        assertEquals("5", formatHalfStars(10))
    }
}

