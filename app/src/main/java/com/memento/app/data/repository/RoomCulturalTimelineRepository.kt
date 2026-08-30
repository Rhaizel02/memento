package com.memento.app.data.repository

import com.memento.app.data.local.dao.TimelineConsumptionRow
import com.memento.app.data.local.dao.TimelineDao
import com.memento.app.data.local.dao.TimelineProgressRow
import com.memento.app.data.local.dao.TimelineReflectionRow
import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.CulturalTimelineEvent
import com.memento.app.domain.model.CulturalTimelineWindow
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ProgressEntry
import com.memento.app.domain.model.Reflection
import com.memento.app.domain.model.TimelineMediaContext
import com.memento.app.domain.repository.CulturalTimelineRepository
import com.memento.app.domain.timeline.CulturalTimelineMapper
import com.memento.app.domain.timeline.OnThisDaySelector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCulturalTimelineRepository @Inject constructor(
    private val timelineDao: TimelineDao,
) : CulturalTimelineRepository {
    override fun observeWindow(mediaType: MediaType?, limit: Int): Flow<CulturalTimelineWindow> {
        require(limit > 0) { "Timeline limit must be positive" }
        val sourceLimit = limit + 1
        return combine(
            timelineDao.observeStarted(mediaType, sourceLimit),
            timelineDao.observeCompleted(mediaType, sourceLimit),
            timelineDao.observeProgress(mediaType, sourceLimit),
            timelineDao.observeReflections(mediaType, sourceLimit),
        ) { startedRows, completedRows, progressRows, reflectionRows ->
            val merged = CulturalTimelineMapper.sort(
                buildList {
                    startedRows.forEach { row ->
                        CulturalTimelineMapper.started(row.media(), row.consumption(), row.isReconsumption)?.let(::add)
                    }
                    completedRows.forEach { row ->
                        CulturalTimelineMapper.completed(row.media(), row.consumption(), row.isReconsumption)?.let(::add)
                    }
                    progressRows.forEach { row -> add(CulturalTimelineMapper.progress(row.media(), row.progress())) }
                    reflectionRows.forEach { row -> add(CulturalTimelineMapper.reflection(row.media(), row.reflection())) }
                },
            )
            CulturalTimelineWindow(events = merged.take(limit), hasMore = merged.size > limit)
        }
    }

    override fun observeOnThisDay(date: LocalDate, limit: Int): Flow<List<CulturalTimelineEvent>> {
        require(limit > 0) { "On-this-day limit must be positive" }
        val monthDay = String.format(Locale.ROOT, "%02d-%02d", date.monthValue, date.dayOfMonth)
        val sourceLimit = maxOf(limit * 4, 12)
        return combine(
            timelineDao.observeStartedOnThisDay(monthDay, date.year, sourceLimit),
            timelineDao.observeCompletedOnThisDay(monthDay, date.year, sourceLimit),
            timelineDao.observeProgressOnThisDay(monthDay, date.year, sourceLimit),
            timelineDao.observeReflectionsOnThisDay(monthDay, date.year, sourceLimit),
        ) { startedRows, completedRows, progressRows, reflectionRows ->
            val candidates = buildList {
                startedRows.forEach { row ->
                    CulturalTimelineMapper.started(row.media(), row.consumption(), row.isReconsumption)?.let(::add)
                }
                completedRows.forEach { row ->
                    CulturalTimelineMapper.completed(row.media(), row.consumption(), row.isReconsumption)?.let(::add)
                }
                progressRows.forEach { row -> add(CulturalTimelineMapper.progress(row.media(), row.progress())) }
                reflectionRows.forEach { row -> add(CulturalTimelineMapper.reflection(row.media(), row.reflection())) }
            }
            OnThisDaySelector.select(candidates, date, limit)
        }
    }
}

private fun TimelineConsumptionRow.media() = TimelineMediaContext(mediaItemId, mediaType, title, posterUrl, isFavorite)

private fun TimelineConsumptionRow.consumption() = Consumption(
    id = consumptionId,
    mediaItemId = mediaItemId,
    status = status,
    startedDate = startedDate,
    completedDate = completedDate,
    ratingHalfStars = ratingHalfStars,
    createdAt = consumptionCreatedAt,
    updatedAt = consumptionUpdatedAt,
)

private fun TimelineProgressRow.media() = TimelineMediaContext(mediaItemId, mediaType, title, posterUrl, isFavorite)

private fun TimelineProgressRow.progress() = ProgressEntry(
    id = progressId,
    consumptionId = consumptionId,
    progressType = progressType,
    currentValue = currentValue,
    totalValue = totalValue,
    season = season,
    episode = episode,
    recordedAt = recordedAt,
)

private fun TimelineReflectionRow.media() = TimelineMediaContext(mediaItemId, mediaType, title, posterUrl, isFavorite)

private fun TimelineReflectionRow.reflection() = Reflection(
    id = reflectionId,
    consumptionId = consumptionId,
    type = reflectionType,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
