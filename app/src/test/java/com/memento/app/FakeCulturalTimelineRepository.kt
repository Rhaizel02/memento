package com.memento.app

import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.CulturalTimelineWindow
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.repository.CulturalTimelineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

class FakeCulturalTimelineRepository : CulturalTimelineRepository {
    val onThisDay = MutableStateFlow<List<CulturalTimelineEvent>>(emptyList())
    val rangeEvents = MutableStateFlow<List<CulturalTimelineEvent>>(emptyList())
    var requestedOnThisDayDate: LocalDate? = null
    var requestedRange: Pair<LocalDate, LocalDate>? = null

    override fun observeWindow(mediaType: MediaType?, limit: Int): Flow<CulturalTimelineWindow> =
        MutableStateFlow(CulturalTimelineWindow(emptyList(), false))

    override fun observeRange(from: LocalDate, until: LocalDate): Flow<List<CulturalTimelineEvent>> {
        requestedRange = from to until
        return rangeEvents
    }

    override fun observeOnThisDay(date: LocalDate, limit: Int): Flow<List<CulturalTimelineEvent>> {
        requestedOnThisDayDate = date
        return onThisDay
    }
}
