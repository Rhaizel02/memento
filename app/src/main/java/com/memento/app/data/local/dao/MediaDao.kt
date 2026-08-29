package com.memento.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
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
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.ConsumptionStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class MediaTypeCountRow(val type: MediaType, val count: Int)

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
    ): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MediaItemEntity>>

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

    @Query(
        """
        SELECT c.name FROM creators c
        INNER JOIN media_creator_cross_ref mc ON mc.creatorId = c.id
        WHERE mc.mediaItemId = :mediaId
        ORDER BY c.name
        """,
    )
    suspend fun getCreatorNames(mediaId: String): List<String>

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

    @Query(
        """
        SELECT g.name FROM genres g
        INNER JOIN media_genre_cross_ref mg ON mg.genreId = g.id
        WHERE mg.mediaItemId = :mediaId
        ORDER BY g.name
        """,
    )
    suspend fun getGenreNames(mediaId: String): List<String>
}
