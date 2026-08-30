package com.memento.app.domain.usecase

import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ProgressType

object ProgressCapturePolicy {
    fun typeFor(mediaType: MediaType): ProgressType = when (mediaType) {
        MediaType.BOOK -> ProgressType.PAGES
        MediaType.SERIES -> ProgressType.EPISODE
        MediaType.GAME -> ProgressType.HOURS
        MediaType.MOVIE -> ProgressType.MINUTES
    }
}
