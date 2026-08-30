package com.memento.app.domain.repository

import com.memento.app.domain.model.CulturalTimelineWindow
import com.memento.app.domain.model.MediaType
import kotlinx.coroutines.flow.Flow

interface CulturalTimelineRepository {
    fun observeWindow(mediaType: MediaType?, limit: Int): Flow<CulturalTimelineWindow>
}
