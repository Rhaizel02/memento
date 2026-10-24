package com.memento.app.ui.library

import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.LibraryFilters
import com.memento.app.domain.model.LibrarySort
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryUiPolicyTest {
    @Test fun `visible status and ordering do not inflate advanced filter badge`() {
        assertEquals(0, advancedFilterCount(LibraryUiState(filters = LibraryFilters(
            status = ConsumptionStatus.COMPLETED, sort = LibrarySort.RATING,
        ))))
    }

    @Test fun `badge counts advanced filter categories and clears completely`() {
        val filtered = LibraryUiState(filters = LibraryFilters(
            minRatingHalfStars = 8, favoritesOnly = true, year = 2026,
            tagIds = setOf("comfort", "relectura"),
        ))
        assertEquals(4, advancedFilterCount(filtered))
        assertEquals(0, advancedFilterCount(filtered.copy(filters = LibraryFilters())))
    }
}
