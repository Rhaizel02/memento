package com.memento.app.data.local.converter

import androidx.room.TypeConverter
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.CreatorRole
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.ProgressType
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.model.RecommendationFeedbackType
import java.time.Instant
import java.time.LocalDate

class RoomConverters {
    @TypeConverter fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
    @TypeConverter fun localDateToString(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun mediaTypeToString(value: MediaType?): String? = value?.name
    @TypeConverter fun stringToMediaType(value: String?): MediaType? = value?.let(MediaType::valueOf)
    @TypeConverter fun statusToString(value: ConsumptionStatus?): String? = value?.name
    @TypeConverter fun stringToStatus(value: String?): ConsumptionStatus? = value?.let(ConsumptionStatus::valueOf)
    @TypeConverter fun progressTypeToString(value: ProgressType?): String? = value?.name
    @TypeConverter fun stringToProgressType(value: String?): ProgressType? = value?.let(ProgressType::valueOf)
    @TypeConverter fun reflectionTypeToString(value: ReflectionType?): String? = value?.name
    @TypeConverter fun stringToReflectionType(value: String?): ReflectionType? = value?.let(ReflectionType::valueOf)
    @TypeConverter fun creatorRoleToString(value: CreatorRole?): String? = value?.name
    @TypeConverter fun stringToCreatorRole(value: String?): CreatorRole? = value?.let(CreatorRole::valueOf)
    @TypeConverter fun providerToString(value: MetadataProvider?): String? = value?.name
    @TypeConverter fun stringToProvider(value: String?): MetadataProvider? = value?.let(MetadataProvider::valueOf)
    @TypeConverter fun recommendationFeedbackToString(value: RecommendationFeedbackType?): String? = value?.name
    @TypeConverter fun stringToRecommendationFeedback(value: String?): RecommendationFeedbackType? =
        value?.let(RecommendationFeedbackType::valueOf)
}
