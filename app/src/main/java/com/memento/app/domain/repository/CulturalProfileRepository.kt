package com.memento.app.domain.repository

import com.memento.app.domain.culturalprofile.CulturalProfileSource
import kotlinx.coroutines.flow.Flow

interface CulturalProfileRepository {
    fun observeSource(): Flow<CulturalProfileSource>
}
