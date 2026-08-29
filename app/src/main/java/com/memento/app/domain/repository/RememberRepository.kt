package com.memento.app.domain.repository

import com.memento.app.domain.remember.RememberCandidate
import kotlinx.coroutines.flow.Flow

interface RememberRepository {
    fun observeRemember(): Flow<RememberCandidate?>
    fun observeRemember(consumptionId: String): Flow<RememberCandidate?>
    suspend fun recordExposure(consumptionId: String)
}

