package com.memento.app.data.repository

import com.memento.app.data.local.dao.ConsumptionDao
import com.memento.app.data.local.dao.MediaDao
import com.memento.app.domain.culturalprofile.CulturalCompletion
import com.memento.app.domain.culturalprofile.CulturalProfileSource
import com.memento.app.domain.culturalprofile.CulturalProfileWork
import com.memento.app.domain.repository.CulturalProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCulturalProfileRepository @Inject constructor(
    private val mediaDao: MediaDao,
    private val consumptionDao: ConsumptionDao,
) : CulturalProfileRepository {
    override fun observeSource(): Flow<CulturalProfileSource> = combine(
        mediaDao.observeCulturalProfileMedia(),
        consumptionDao.observeCulturalProfileCompletions(),
        mediaDao.observeAllGenreNames(),
        mediaDao.observeCulturalProfileCreatorNames(),
        mediaDao.observeAllTags(),
    ) { mediaRows, completionRows, genreRows, creatorRows, tagRows ->
        val genres = genreRows.groupBy({ it.mediaItemId }, { it.name })
        val creators = creatorRows.groupBy({ it.mediaItemId }, { it.name })
        val tags = tagRows.groupBy({ it.mediaItemId }, { it.name })
        CulturalProfileSource(
            works = mediaRows.map { row ->
                CulturalProfileWork(
                    mediaId = row.mediaItemId,
                    mediaType = row.mediaType,
                    isFavorite = row.isFavorite,
                    genres = genres[row.mediaItemId].orEmpty(),
                    creators = creators[row.mediaItemId].orEmpty(),
                    tags = tags[row.mediaItemId].orEmpty(),
                    title = row.title,
                    posterUrl = row.posterUrl,
                    backdropUrl = row.backdropUrl,
                )
            },
            completions = completionRows.mapNotNull { row ->
                row.completedDate?.let { date ->
                    CulturalCompletion(row.mediaItemId, date, row.ratingHalfStars, row.updatedAt)
                }
            },
        )
    }
}
