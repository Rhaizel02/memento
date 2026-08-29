package com.memento.app

import com.memento.app.domain.remember.RememberCandidate
import com.memento.app.domain.repository.RememberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRememberRepository : RememberRepository {
    val memory = MutableStateFlow<RememberCandidate?>(null)
    val recordedExposures = mutableListOf<String>()
    override fun observeRemember(): Flow<RememberCandidate?> = memory
    override fun observeRemember(consumptionId: String): Flow<RememberCandidate?> = memory
    override suspend fun recordExposure(consumptionId: String) { recordedExposures += consumptionId }
}
