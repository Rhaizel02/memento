package com.memento.app.ui.wrapped

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.repository.WrappedRepository
import com.memento.app.domain.wrapped.WrappedEngine
import com.memento.app.domain.wrapped.WrappedSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class WrappedUiState(
    val snapshot: WrappedSnapshot? = null,
    val selectedYear: Int? = null,
    val availableYears: List<Int> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class WrappedViewModel @Inject constructor(
    repository: WrappedRepository,
    private val clock: Clock,
) : ViewModel() {
    private val requestedYear = MutableStateFlow<Int?>(null)

    val state = combine(repository.observeSource(), requestedYear) { source, requested ->
        val today = LocalDate.now(clock)
        val years = WrappedEngine.availableYears(source, today)
        val selected = requested?.takeIf(years::contains) ?: years.firstOrNull()
        WrappedUiState(
            snapshot = selected?.let { WrappedEngine.create(source, it, today) },
            selectedYear = selected,
            availableYears = years,
            isLoading = false,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WrappedUiState(),
    )

    fun load(year: Int) {
        requestedYear.value = year
    }

    fun selectYear(year: Int) {
        requestedYear.value = year
    }
}
