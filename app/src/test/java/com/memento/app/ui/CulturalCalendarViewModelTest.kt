package com.memento.app.ui

import com.memento.app.FakeCulturalTimelineRepository
import com.memento.app.MainDispatcherRule
import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.TimelineEventType
import com.memento.app.ui.calendar.CulturalCalendarViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CulturalCalendarViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `current month requests one exclusive repository range`() = runTest {
        val repository = FakeCulturalTimelineRepository().apply {
            rangeEvents.value = listOf(event("inside", "2026-08-17"), event("outside", "2026-09-01"))
        }
        val clock = Clock.fixed(Instant.parse("2026-08-17T12:00:00Z"), ZoneOffset.UTC)
        val viewModel = CulturalCalendarViewModel(repository, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

        advanceUntilIdle()

        assertEquals(LocalDate.of(2026, 8, 1) to LocalDate.of(2026, 9, 1), repository.requestedRange)
        assertEquals(listOf("inside"), viewModel.state.value.month.days.values.flatMap { it.events }.map { it.id })
    }

    private fun event(id: String, date: String) = CulturalTimelineEvent(
        id = id,
        date = LocalDate.parse(date),
        occurredAt = null,
        mediaItemId = "media-$id",
        consumptionId = "consumption-$id",
        mediaType = MediaType.BOOK,
        title = id,
        posterUrl = null,
        eventType = TimelineEventType.STARTED,
    )
}
