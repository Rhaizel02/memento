package com.memento.app.data.mapper

import com.memento.app.data.local.entity.ConsumptionEntity
import com.memento.app.data.local.entity.MediaItemEntity
import com.memento.app.data.local.entity.ProgressEntryEntity
import com.memento.app.data.local.entity.ReflectionEntity
import com.memento.app.domain.model.Consumption
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.ProgressEntry
import com.memento.app.domain.model.Reflection

fun MediaItemEntity.toDomain() = MediaItem(
    id = id,
    type = type,
    title = title,
    originalTitle = originalTitle,
    description = description,
    releaseDate = releaseDate,
    releaseYear = releaseYear,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    isFavorite = isFavorite,
    isManual = isManual,
    runtimeMinutes = runtimeMinutes,
    pageCount = pageCount,
    seasonCount = seasonCount,
    episodeCount = episodeCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun MediaItem.toEntity() = MediaItemEntity(
    id = id,
    type = type,
    title = title,
    originalTitle = originalTitle,
    description = description,
    releaseDate = releaseDate,
    releaseYear = releaseYear,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    isFavorite = isFavorite,
    isManual = isManual,
    runtimeMinutes = runtimeMinutes,
    pageCount = pageCount,
    seasonCount = seasonCount,
    episodeCount = episodeCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ConsumptionEntity.toDomain() = Consumption(id, mediaItemId, status, startedDate, completedDate, ratingHalfStars, createdAt, updatedAt)
fun ProgressEntryEntity.toDomain() = ProgressEntry(id, consumptionId, progressType, currentValue, totalValue, season, episode, recordedAt)
fun ReflectionEntity.toDomain() = Reflection(id, consumptionId, type, content, createdAt, updatedAt)

