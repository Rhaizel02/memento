package com.memento.app.ui.culturalprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memento.app.domain.culturalprofile.CulturalProfile
import com.memento.app.domain.culturalprofile.CulturalProfileEngine
import com.memento.app.domain.repository.CulturalProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CulturalProfileUiState(
    val profile: CulturalProfile = CulturalProfile(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class CulturalProfileViewModel @Inject constructor(
    repository: CulturalProfileRepository,
    clock: Clock,
) : ViewModel() {
    val state = repository.observeSource()
        .map { source -> CulturalProfileUiState(CulturalProfileEngine.build(source, LocalDate.now(clock)), false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CulturalProfileUiState())
}
