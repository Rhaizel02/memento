package com.memento.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.memento.app.data.local.entity.CreatorEntity
import com.memento.app.data.local.entity.GenreEntity
import com.memento.app.data.local.entity.ExternalMediaRefEntity
import com.memento.app.data.local.entity.MediaCreatorCrossRef
import com.memento.app.data.local.entity.MediaGenreCrossRef
import com.memento.app.data.local.entity.MediaItemEntity
import com.memento.app.data.local.entity.MediaTagCrossRef
import com.memento.app.data.local.entity.TagEntity
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.ProgressType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

data class MediaTypeCountRow(val type: MediaType, val count: Int)
data class MediaNameRow(val mediaItemId: String, val name: String)
data class CulturalProfileMediaRow(
    val mediaItemId: String,
    val mediaType: MediaType,
    val isFavorite: Boolean,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
)
data class MediaTagRow(
    val mediaItemId: String,
    val tagId: String,
    val name: String,
    val normalizedName: String,
    val createdAt: Instant,
)
data class HomeMediaRow(
    @Embedded val media: MediaItemEntity,
    val consumptionId: String,
    val ratingHalfStars: Int?,
    val completedDate: LocalDate?,
    val creatorName: String?,
    val genreOne: String?,
    val genreTwo: String?,
    val genreCount: Int,
    val progressId: String?,
    val progressType: ProgressType?,
    val progressCurrentValue: Double?,
    val progressTotalValue: Double?,
    val progressSeason: Int?,
    val progressEpisode: Int?,
    val progressRecordedAt: Instant?,
)

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(media: MediaItemEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExternalRef(reference: ExternalMediaRefEntity)

    @Query("SELECT mediaItemId FROM external_media_refs WHERE provider = :provider AND externalId = :externalId AND mediaType = :mediaType LIMIT 1")
    suspend fun findByExternalRef(provider: MetadataProvider, externalId: String, mediaType: MediaType): String?

    @Update
    suspend fun update(media: MediaItemEntity)

    @Delete
    suspend fun delete(media: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id = :mediaId")
    suspend fun deleteById(mediaId: String)

    @Query("SELECT * FROM media_items WHERE id = :mediaId")
    fun observeById(mediaId: String): Flow<MediaItemEntity?>

    @Query("SELECT * FROM media_items WHERE id = :mediaId")
    suspend fun getById(mediaId: String): MediaItemEntity?

    @Query(
        """
        SELECT DISTINCT m.* FROM media_items m
        LEFT JOIN media_creator_cross_ref mc ON mc.mediaItemId = m.id
        LEFT JOIN creators c ON c.id = mc.creatorId
        WHERE (:query = '' OR lower(m.title) LIKE '%' || lower(:query) || '%'
            OR lower(COALESCE(m.originalTitle, '')) LIKE '%' || lower(:query) || '%'
            OR lower(COALESCE(c.name, '')) LIKE '%' || lower(:query) || '%')
        AND (:type IS NULL OR m.type = :type)
        AND (:status IS NULL OR EXISTS (
            SELECT 1 FROM consumptions cs WHERE cs.mediaItemId = m.id AND cs.status = :status
        ))
        AND (:favoritesOnly = 0 OR m.isFavorite = 1)
        AND (:minRating IS NULL OR EXISTS (
            SELECT 1 FROM consumptions cr WHERE cr.mediaItemId = m.id AND cr.ratingHalfStars >= :minRating
        ))
        AND (:year IS NULL OR m.releaseYear = :year OR EXISTS (
            SELECT 1 FROM consumptions cy
            WHERE cy.mediaItemId = m.id AND CAST(substr(cy.completedDate, 1, 4) AS INTEGER) = :year
        ))
        AND (:tagCount = 0 OR EXISTS (
            SELECT 1 FROM media_tag_cross_ref mt
            WHERE mt.mediaItemId = m.id AND mt.tagId IN (:tagIds)
        ))
        ORDER BY
            CASE WHEN :sort = 'TITLE' THEN m.title END COLLATE NOCASE ASC,
            CASE WHEN :sort = 'RATING' THEN (SELECT MAX(cra.ratingHalfStars) FROM consumptions cra WHERE cra.mediaItemId = m.id) END DESC,
            CASE WHEN :sort = 'COMPLETED_DATE' THEN (SELECT MAX(cc.completedDate) FROM consumptions cc WHERE cc.mediaItemId = m.id) END DESC,
            CASE WHEN :sort = 'ADDED_DATE' THEN m.createdAt END DESC,
            CASE WHEN :sort = 'RECENT' THEN m.updatedAt END DESC,
            m.title COLLATE NOCASE ASC
        """,
    )
    fun observeLibrary(
        query: String,
        type: MediaType?,
        status: ConsumptionStatus?,
        favoritesOnly: Boolean,
        minRating: Int?,
        year: Int?,
        sort: String,
        tagIds: List<String>,
        tagCount: Int,
    ): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MediaItemEntity>>

    @Query("SELECT COUNT(*) FROM media_items")
    fun observeMediaCount(): Flow<Int>

    @Query(
        """
        SELECT id AS mediaItemId, type AS mediaType, isFavorite, title, posterUrl, backdropUrl
        FROM media_items
        ORDER BY id
        """,
    )
    fun observeCulturalProfileMedia(): Flow<List<CulturalProfileMediaRow>>

    @Query(
        """
        SELECT mc.mediaItemId AS mediaItemId, c.name AS name
        FROM media_creator_cross_ref mc
        INNER JOIN creators c ON c.id = mc.creatorId
        ORDER BY mc.mediaItemId, c.name
        """,
    )
    fun observeAllCreatorNames(): Flow<List<MediaNameRow>>

    @Query(
        """
        SELECT mc.mediaItemId AS mediaItemId, c.name AS name
        FROM media_creator_cross_ref mc
        INNER JOIN creators c ON c.id = mc.creatorId
        WHERE mc.role != 'OTHER'
        ORDER BY mc.mediaItemId, c.name
        """,
    )
    fun observeCulturalProfileCreatorNames(): Flow<List<MediaNameRow>>

    @Query(
        """
        SELECT mg.mediaItemId AS mediaItemId, g.name AS name
        FROM media_genre_cross_ref mg
        INNER JOIN genres g ON g.id = mg.genreId
        ORDER BY mg.mediaItemId, g.name
        """,
    )
    fun observeAllGenreNames(): Flow<List<MediaNameRow>>

    @Query(
        """
        SELECT mt.mediaItemId AS mediaItemId, t.id AS tagId, t.name AS name,
            t.normalizedName AS normalizedName, t.createdAt AS createdAt
        FROM media_tag_cross_ref mt
        INNER JOIN tags t ON t.id = mt.tagId
        ORDER BY mt.mediaItemId, t.name COLLATE NOCASE
        """,
    )
    fun observeAllTags(): Flow<List<MediaTagRow>>

    @Query("SELECT * FROM external_media_refs ORDER BY mediaItemId, provider, externalId")
    fun observeAllExternalRefs(): Flow<List<ExternalMediaRefEntity>>

    @Query(
        """
        SELECT DISTINCT m.* FROM media_items m
        INNER JOIN consumptions c ON c.mediaItemId = m.id
        WHERE c.status = 'IN_PROGRESS'
        ORDER BY c.updatedAt DESC
        """,
    )
    fun observeInProgress(): Flow<List<MediaItemEntity>>

    @Query(
        """
        SELECT DISTINCT m.* FROM media_items m
        INNER JOIN consumptions c ON c.mediaItemId = m.id
        WHERE c.status = 'COMPLETED'
        ORDER BY c.completedDate DESC, c.updatedAt DESC
        LIMIT :limit
        """,
    )
    fun observeRecentlyCompleted(limit: Int): Flow<List<MediaItemEntity>>

    @Query(
        """
        SELECT
            m.*,
            c.id AS consumptionId,
            c.ratingHalfStars AS ratingHalfStars,
            c.completedDate AS completedDate,
            (SELECT creator.name FROM creators creator
                INNER JOIN media_creator_cross_ref mc ON mc.creatorId = creator.id
                WHERE mc.mediaItemId = m.id ORDER BY creator.name LIMIT 1) AS creatorName,
            (SELECT genre.name FROM genres genre
                INNER JOIN media_genre_cross_ref mg ON mg.genreId = genre.id
                WHERE mg.mediaItemId = m.id ORDER BY genre.name LIMIT 1) AS genreOne,
            (SELECT genre.name FROM genres genre
                INNER JOIN media_genre_cross_ref mg ON mg.genreId = genre.id
                WHERE mg.mediaItemId = m.id ORDER BY genre.name LIMIT 1 OFFSET 1) AS genreTwo,
            (SELECT COUNT(*) FROM media_genre_cross_ref mg WHERE mg.mediaItemId = m.id) AS genreCount,
            p.id AS progressId,
            p.progressType AS progressType,
            p.currentValue AS progressCurrentValue,
            p.totalValue AS progressTotalValue,
            p.season AS progressSeason,
            p.episode AS progressEpisode,
            p.recordedAt AS progressRecordedAt
        FROM media_items m
        INNER JOIN consumptions c ON c.id = (
            SELECT active.id FROM consumptions active
            WHERE active.mediaItemId = m.id AND active.status = 'IN_PROGRESS'
            ORDER BY active.updatedAt DESC LIMIT 1
        )
        LEFT JOIN progress_entries p ON p.id = (
            SELECT latest.id FROM progress_entries latest
            WHERE latest.consumptionId = c.id
            ORDER BY latest.recordedAt DESC LIMIT 1
        )
        ORDER BY c.updatedAt DESC
        LIMIT :limit
        """,
    )
    fun observeHomeInProgress(limit: Int): Flow<List<HomeMediaRow>>

    @Query(
        """
        SELECT
            m.*,
            c.id AS consumptionId,
            c.ratingHalfStars AS ratingHalfStars,
            c.completedDate AS completedDate,
            (SELECT creator.name FROM creators creator
                INNER JOIN media_creator_cross_ref mc ON mc.creatorId = creator.id
                WHERE mc.mediaItemId = m.id ORDER BY creator.name LIMIT 1) AS creatorName,
            (SELECT genre.name FROM genres genre
                INNER JOIN media_genre_cross_ref mg ON mg.genreId = genre.id
                WHERE mg.mediaItemId = m.id ORDER BY genre.name LIMIT 1) AS genreOne,
            (SELECT genre.name FROM genres genre
                INNER JOIN media_genre_cross_ref mg ON mg.genreId = genre.id
                WHERE mg.mediaItemId = m.id ORDER BY genre.name LIMIT 1 OFFSET 1) AS genreTwo,
            (SELECT COUNT(*) FROM media_genre_cross_ref mg WHERE mg.mediaItemId = m.id) AS genreCount,
            NULL AS progressId,
            NULL AS progressType,
            NULL AS progressCurrentValue,
            NULL AS progressTotalValue,
            NULL AS progressSeason,
            NULL AS progressEpisode,
            NULL AS progressRecordedAt
        FROM media_items m
        INNER JOIN consumptions c ON c.id = (
            SELECT completed.id FROM consumptions completed
            WHERE completed.mediaItemId = m.id AND completed.status = 'COMPLETED'
            ORDER BY completed.completedDate DESC, completed.updatedAt DESC LIMIT 1
        )
        ORDER BY c.completedDate DESC, c.updatedAt DESC
        LIMIT :limit
        """,
    )
    fun observeHomeRecentlyCompleted(limit: Int): Flow<List<HomeMediaRow>>

    @Query(
        """
        SELECT m.type AS type, COUNT(c.id) AS count
        FROM consumptions c
        INNER JOIN media_items m ON m.id = c.mediaItemId
        WHERE c.status = 'COMPLETED' AND c.completedDate >= :from AND c.completedDate < :until
        GROUP BY m.type
        """,
    )
    fun observeCompletedCounts(from: LocalDate, until: LocalDate): Flow<List<MediaTypeCountRow>>

    @Query("UPDATE media_items SET isFavorite = NOT isFavorite, updatedAt = :updatedAt WHERE id = :mediaId")
    suspend fun toggleFavorite(mediaId: String, updatedAt: java.time.Instant)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCreator(creator: CreatorEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaCreator(crossRef: MediaCreatorCrossRef)

    @Query("DELETE FROM media_creator_cross_ref WHERE mediaItemId = :mediaId")
    suspend fun deleteMediaCreators(mediaId: String)

    @Query("DELETE FROM creators WHERE id NOT IN (SELECT DISTINCT creatorId FROM media_creator_cross_ref)")
    suspend fun deleteOrphanCreators()

    @Query("DELETE FROM genres WHERE id NOT IN (SELECT DISTINCT genreId FROM media_genre_cross_ref)")
    suspend fun deleteOrphanGenres()

    @Query("SELECT * FROM creators WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findCreator(normalizedName: String): CreatorEntity?

    @Query("SELECT * FROM genres WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findGenre(normalizedName: String): GenreEntity?

    @Query(
        """
        SELECT c.name FROM creators c
        INNER JOIN media_creator_cross_ref mc ON mc.creatorId = c.id
        WHERE mc.mediaItemId = :mediaId
        ORDER BY c.name
        """,
    )
    fun observeCreatorNames(mediaId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGenre(genre: GenreEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaGenre(crossRef: MediaGenreCrossRef)

    @Query(
        """
        SELECT g.name FROM genres g
        INNER JOIN media_genre_cross_ref mg ON mg.genreId = g.id
        WHERE mg.mediaItemId = :mediaId
        ORDER BY g.name
        """,
    )
    fun observeGenreNames(mediaId: String): Flow<List<String>>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeTags(): Flow<List<TagEntity>>

    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN media_tag_cross_ref mt ON mt.tagId = t.id
        WHERE mt.mediaItemId = :mediaId
        ORDER BY t.name COLLATE NOCASE
        """,
    )
    fun observeTagsForMedia(mediaId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findTag(normalizedName: String): TagEntity?

    @Query("SELECT * FROM tags WHERE id = :tagId LIMIT 1")
    suspend fun getTagById(tagId: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaTag(crossRef: MediaTagCrossRef)

    @Query("DELETE FROM media_tag_cross_ref WHERE mediaItemId = :mediaId AND tagId = :tagId")
    suspend fun deleteMediaTag(mediaId: String, tagId: String)

}
