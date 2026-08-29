package com.memento.app.domain

import com.memento.app.domain.model.CreatorRole
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.defaultCreatorRole
import org.junit.Assert.assertEquals
import org.junit.Test

class CreatorRoleTest {
    @Test
    fun `each media type maps to its semantic creator role`() {
        assertEquals(CreatorRole.AUTHOR, MediaType.BOOK.defaultCreatorRole())
        assertEquals(CreatorRole.DIRECTOR, MediaType.MOVIE.defaultCreatorRole())
        assertEquals(CreatorRole.CREATOR, MediaType.SERIES.defaultCreatorRole())
        assertEquals(CreatorRole.DEVELOPER, MediaType.GAME.defaultCreatorRole())
    }
}
