package com.memento.app.ui.navigation

import org.junit.Assert.assertNotEquals
import org.junit.Test

class NavigationSessionTest {
    @Test
    fun `add flow keys distinguish independent session identities`() {
        assertNotEquals(AddMediaKey("session-one"), AddMediaKey("session-two"))
    }
}
