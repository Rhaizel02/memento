package com.memento.app.data.repository

import com.memento.app.data.local.dao.TimelineDao
import com.memento.app.domain.model.Reflection
import com.memento.app.domain.model.TimelineMediaContext
import com.memento.app.domain.repository.CulturalProfileRepository
import com.memento.app.domain.repository.WrappedRepository
import com.memento.app.domain.timeline.CulturalTimelineMapper
import com.memento.app.domain.wrapped.WrappedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomWrappedRepository @Inject constructor(
    private val culturalProfileRepository: CulturalProfileRepository,
    private val timelineDao: TimelineDao,
) : WrappedRepository {
    override fun observeSource(): Flow<WrappedSource> = combine(
        culturalProfileRepository.observeSource(),
        timelineDao.observeReflections(mediaType = null, limit = Int.MAX_VALUE),
    ) { profileSource, reflectionRows ->
        WrappedSource(
            profileSource = profileSource,
            timelineEvents = reflectionRows.map { row ->
                CulturalTimelineMapper.reflection(
                    media = TimelineMediaContext(
                        id = row.mediaItemId,
                        type = row.mediaType,
                        title = row.title,
                        posterUrl = row.posterUrl,
                        isFavorite = row.isFavorite,
                    ),
                    reflection = Reflection(
                        id = row.reflectionId,
                        consumptionId = row.consumptionId,
                        type = row.reflectionType,
                        content = row.content,
                        createdAt = row.createdAt,
                        updatedAt = row.updatedAt,
                    ),
                )
            },
        )
    }
}
