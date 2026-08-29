package com.memento.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.repository.MediaRepository
import com.memento.app.domain.stats.StatsEngine
import com.memento.app.domain.stats.StatsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class StatsUiState(
    val selectedYear: Int = LocalDate.now().year,
    val availableYears: List<Int> = listOf(LocalDate.now().year),
    val summary: StatsSummary? = null,
)

@HiltViewModel
class StatsViewModel @Inject constructor(repository: MediaRepository) : ViewModel() {
    private val selectedYear = MutableStateFlow(LocalDate.now().year)
    val state = combine(repository.observeAllDetails(), selectedYear) { history, year ->
        val years = StatsEngine.availableYears(history, LocalDate.now().year)
        val validYear = year.takeIf(years::contains) ?: years.first()
        StatsUiState(validYear, years, StatsEngine.calculate(history, validYear))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun selectYear(year: Int) { selectedYear.value = year }
}
