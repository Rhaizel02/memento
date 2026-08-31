package com.memento.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.calendar.CulturalCalendarEngine
import com.memento.app.domain.calendar.CulturalCalendarMonth
import com.memento.app.domain.calendar.CulturalCalendarYear
import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.repository.CulturalTimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class CalendarViewMode { MONTH, YEAR }

data class CulturalCalendarUiState(
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val selectedMonth: YearMonth,
    val month: CulturalCalendarMonth = CulturalCalendarMonth(selectedMonth, emptyMap()),
    val year: CulturalCalendarYear? = null,
    val selectedDate: LocalDate? = null,
    val selectedEvents: List<CulturalTimelineEvent> = emptyList(),
    val today: LocalDate,
    val isLoading: Boolean = true,
    val isError: Boolean = false,
)

private data class CalendarSelection(val month: YearMonth, val mode: CalendarViewMode)

private data class CalendarLoad(
    val selection: CalendarSelection,
    val events: List<CulturalTimelineEvent> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CulturalCalendarViewModel @Inject constructor(
    private val repository: CulturalTimelineRepository,
    private val clock: Clock,
) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val selection = MutableStateFlow(CalendarSelection(YearMonth.from(today), CalendarViewMode.MONTH))
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val retrySignal = MutableStateFlow(0)

    private val load: Flow<CalendarLoad> = combine(selection, retrySignal) { selected, _ -> selected }
        .flatMapLatest { selected ->
            val from = if (selected.mode == CalendarViewMode.MONTH) {
                selected.month.atDay(1)
            } else {
                LocalDate.of(selected.month.year, 1, 1)
            }
            val until = if (selected.mode == CalendarViewMode.MONTH) {
                selected.month.plusMonths(1).atDay(1)
            } else {
                LocalDate.of(selected.month.year + 1, 1, 1)
            }
            flow {
                emit(CalendarLoad(selected, isLoading = true))
                emitAll(
                    repository.observeRange(from, until)
                        .map { events -> CalendarLoad(selected, events = events) },
                )
            }.catch { emit(CalendarLoad(selected, isError = true)) }
        }

    val state = combine(load, selectedDate) { loaded, date ->
        val monthSnapshot = if (loaded.selection.mode == CalendarViewMode.MONTH) {
            CulturalCalendarEngine.buildMonth(loaded.events, loaded.selection.month)
        } else {
            CulturalCalendarMonth(loaded.selection.month, emptyMap())
        }
        val validSelectedDate = date?.takeIf { loaded.selection.mode == CalendarViewMode.MONTH && YearMonth.from(it) == loaded.selection.month }
        CulturalCalendarUiState(
            viewMode = loaded.selection.mode,
            selectedMonth = loaded.selection.month,
            month = monthSnapshot,
            year = loaded.events.takeIf { loaded.selection.mode == CalendarViewMode.YEAR }
                ?.let { CulturalCalendarEngine.buildYear(it, loaded.selection.month.year) },
            selectedDate = validSelectedDate,
            selectedEvents = validSelectedDate?.let { monthSnapshot.days[it]?.events }.orEmpty(),
            today = today,
            isLoading = loaded.isLoading,
            isError = loaded.isError,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CulturalCalendarUiState(selectedMonth = YearMonth.from(today), today = today),
    )

    fun previousPeriod() {
        selectedDate.value = null
        selection.value = selection.value.let { selected ->
            selected.copy(
                month = if (selected.mode == CalendarViewMode.MONTH) {
                    CulturalCalendarEngine.previous(selected.month)
                } else {
                    selected.month.minusYears(1)
                },
            )
        }
    }

    fun nextPeriod() {
        selectedDate.value = null
        selection.value = selection.value.let { selected ->
            selected.copy(
                month = if (selected.mode == CalendarViewMode.MONTH) {
                    CulturalCalendarEngine.next(selected.month)
                } else {
                    selected.month.plusYears(1)
                },
            )
        }
    }

    fun showYear() {
        selectedDate.value = null
        selection.value = selection.value.copy(mode = CalendarViewMode.YEAR)
    }

    fun showMonth(month: YearMonth) {
        selectedDate.value = null
        selection.value = CalendarSelection(month, CalendarViewMode.MONTH)
    }

    fun selectDay(date: LocalDate) {
        if (YearMonth.from(date) != selection.value.month || selection.value.mode != CalendarViewMode.MONTH) return
        selectedDate.value = date
    }

    fun goToToday() {
        selectedDate.value = today
        selection.value = CalendarSelection(YearMonth.from(today), CalendarViewMode.MONTH)
    }

    fun retry() {
        retrySignal.value += 1
    }
}
