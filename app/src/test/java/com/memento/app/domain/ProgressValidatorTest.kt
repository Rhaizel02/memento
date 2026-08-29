package com.memento.app.domain

import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.usecase.ProgressValidator
import org.junit.Assert.assertThrows
import org.junit.Test

class ProgressValidatorTest {
    @Test fun `pages cannot exceed known total`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProgressValidator.validate(ProgressType.PAGES, 401.0, 400.0, null, null)
        }
    }

    @Test fun `series coordinates start at one`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProgressValidator.validate(ProgressType.EPISODE, null, null, 0, 1)
        }
    }

    @Test fun `percent values stay within bounds`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProgressValidator.validate(ProgressType.HOURS, 12.5, 101.0, null, null)
        }
        ProgressValidator.validate(ProgressType.PERCENT, 100.0, null, null, null)
    }
}
