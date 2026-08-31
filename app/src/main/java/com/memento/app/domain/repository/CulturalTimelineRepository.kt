package com.memento.app.domain.repository

import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.CulturalTimelineWindow
import com.memento.app.domain.model.MediaType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface CulturalTimelineRepository {
    fun observeWindow(mediaType: MediaType?, limit: Int): Flow<CulturalTimelineWindow>
    fun observeRange(from: LocalDate, until: LocalDate): Flow<List<CulturalTimelineEvent>>
    fun observeOnThisDay(date: LocalDate, limit: Int = 5): Flow<List<CulturalTimelineEvent>>
}
