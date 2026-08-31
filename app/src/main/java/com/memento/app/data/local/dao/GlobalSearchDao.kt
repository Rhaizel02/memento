package com.memento.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.memento.app.domain.model.CreatorRole
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ReflectionType
import java.time.Instant

data class GlobalSearchMediaRow(
    val mediaId: String,
    val mediaType: MediaType,
    val title: String,
    val posterUrl: String?,
    val reasonId: String? = null,
    val reasonName: String? = null,
    val creatorRole: CreatorRole? = null,
)

data class GlobalSearchFacetRow(
    val id: String,
    val name: String,
    val mediaCount: Int,
    val creatorRole: CreatorRole? = null,
)

data class GlobalSearchTextRow(
    val reflectionId: String,
    val mediaId: String,
    val mediaTitle: String,
    val mediaType: MediaType,
    val reflectionType: ReflectionType,
    val content: String,
    val createdAt: Instant,
)

@Dao
interface GlobalSearchDao {
    @Query(
        """
        SELECT m.id AS mediaId, m.type AS mediaType, m.title, m.posterUrl,
            NULL AS reasonId, NULL AS reasonName, NULL AS creatorRole
        FROM media_items m
        WHERE lower(m.title) LIKE :contains ESCAPE '\'
        ORDER BY CASE
            WHEN lower(m.title) = :exact THEN 0
            WHEN lower(m.title) LIKE :prefix ESCAPE '\' THEN 1
            ELSE 2 END,
            m.title COLLATE NOCASE, m.id
        LIMIT :limit
        """,
    )
    suspend fun searchMediaTitles(exact: String, prefix: String, contains: String, limit: Int): List<GlobalSearchMediaRow>

    @Query(
        """
        SELECT m.id AS mediaId, m.type AS mediaType, m.title, m.posterUrl,
            c.id AS reasonId, c.name AS reasonName, mc.role AS creatorRole
        FROM creators c
        INNER JOIN media_creator_cross_ref mc ON mc.creatorId = c.id
        INNER JOIN media_items m ON m.id = mc.mediaItemId
        WHERE c.normalizedName LIKE :contains ESCAPE '\' AND mc.role != 'OTHER'
        ORDER BY CASE
            WHEN c.normalizedName = :exact THEN 0
            WHEN c.normalizedName LIKE :prefix ESCAPE '\' THEN 1
            ELSE 2 END,
            c.name COLLATE NOCASE, m.title COLLATE NOCASE, m.id
        LIMIT :limit
        """,
    )
    suspend fun searchMediaCreators(exact: String, prefix: String, contains: String, limit: Int): List<GlobalSearchMediaRow>

    @Query(
        """
        SELECT m.id AS mediaId, m.type AS mediaType, m.title, m.posterUrl,
            g.id AS reasonId, g.name AS reasonName, NULL AS creatorRole
        FROM genres g
        INNER JOIN media_genre_cross_ref mg ON mg.genreId = g.id
        INNER JOIN media_items m ON m.id = mg.mediaItemId
        WHERE g.normalizedName LIKE :contains ESCAPE '\'
        ORDER BY CASE
            WHEN g.normalizedName = :exact THEN 0
            WHEN g.normalizedName LIKE :prefix ESCAPE '\' THEN 1
            ELSE 2 END,
            g.name COLLATE NOCASE, m.title COLLATE NOCASE, m.id
        LIMIT :limit
        """,
    )
    suspend fun searchMediaGenres(exact: String, prefix: String, contains: String, limit: Int): List<GlobalSearchMediaRow>

    @Query(
        """
        SELECT m.id AS mediaId, m.type AS mediaType, m.title, m.posterUrl,
            t.id AS reasonId, t.name AS reasonName, NULL AS creatorRole
        FROM tags t
        INNER JOIN media_tag_cross_ref mt ON mt.tagId = t.id
        INNER JOIN media_items m ON m.id = mt.mediaItemId
        WHERE t.normalizedName LIKE :contains ESCAPE '\'
        ORDER BY CASE
            WHEN t.normalizedName = :exact THEN 0
            WHEN t.normalizedName LIKE :prefix ESCAPE '\' THEN 1
            ELSE 2 END,
            t.name COLLATE NOCASE, m.title COLLATE NOCASE, m.id
        LIMIT :limit
        """,
    )
    suspend fun searchMediaTags(exact: String, prefix: String, contains: String, limit: Int): List<GlobalSearchMediaRow>

    @Query(
        """
        SELECT t.id, t.name, COUNT(DISTINCT mt.mediaItemId) AS mediaCount, NULL AS creatorRole
        FROM tags t
        INNER JOIN media_tag_cross_ref mt ON mt.tagId = t.id
        WHERE t.normalizedName LIKE :contains ESCAPE '\'
        GROUP BY t.id, t.name, t.normalizedName
        ORDER BY CASE
            WHEN t.normalizedName = :exact THEN 0
            WHEN t.normalizedName LIKE :prefix ESCAPE '\' THEN 1
            ELSE 2 END,
            t.name COLLATE NOCASE, t.id
        LIMIT :limit
        """,
    )
    suspend fun searchTags(exact: String, prefix: String, contains: String, limit: Int): List<GlobalSearchFacetRow>

    @Query(
        """
        SELECT c.id, c.name, COUNT(DISTINCT mc.mediaItemId) AS mediaCount,
            CASE
                WHEN SUM(CASE WHEN mc.role = 'AUTHOR' THEN 1 ELSE 0 END) > 0 THEN 'AUTHOR'
                WHEN SUM(CASE WHEN mc.role = 'DIRECTOR' THEN 1 ELSE 0 END) > 0 THEN 'DIRECTOR'
                WHEN SUM(CASE WHEN mc.role = 'DEVELOPER' THEN 1 ELSE 0 END) > 0 THEN 'DEVELOPER'
                ELSE 'CREATOR' END AS creatorRole
        FROM creators c
        INNER JOIN media_creator_cross_ref mc ON mc.creatorId = c.id
        WHERE c.normalizedName LIKE :contains ESCAPE '\' AND mc.role != 'OTHER'
        GROUP BY c.id, c.name, c.normalizedName
        ORDER BY CASE
            WHEN c.normalizedName = :exact THEN 0
            WHEN c.normalizedName LIKE :prefix ESCAPE '\' THEN 1
            ELSE 2 END,
            c.name COLLATE NOCASE, c.id
        LIMIT :limit
        """,
    )
    suspend fun searchCreators(exact: String, prefix: String, contains: String, limit: Int): List<GlobalSearchFacetRow>

    @Query(
        """
        SELECT g.id, g.name, COUNT(DISTINCT mg.mediaItemId) AS mediaCount, NULL AS creatorRole
        FROM genres g
        INNER JOIN media_genre_cross_ref mg ON mg.genreId = g.id
        WHERE g.normalizedName LIKE :contains ESCAPE '\'
        GROUP BY g.id, g.name, g.normalizedName
        ORDER BY CASE
            WHEN g.normalizedName = :exact THEN 0
            WHEN g.normalizedName LIKE :prefix ESCAPE '\' THEN 1
            ELSE 2 END,
            g.name COLLATE NOCASE, g.id
        LIMIT :limit
        """,
    )
    suspend fun searchGenres(exact: String, prefix: String, contains: String, limit: Int): List<GlobalSearchFacetRow>

    @Query(
        """
        SELECT r.id AS reflectionId, m.id AS mediaId, m.title AS mediaTitle,
            m.type AS mediaType, r.type AS reflectionType, r.content, r.createdAt
        FROM reflections r
        INNER JOIN consumptions c ON c.id = r.consumptionId
        INNER JOIN media_items m ON m.id = c.mediaItemId
        WHERE r.type = 'QUOTE' AND lower(r.content) LIKE :contains ESCAPE '\'
        ORDER BY CASE
            WHEN lower(r.content) = :exact THEN 0
            WHEN lower(r.content) LIKE :prefix ESCAPE '\' THEN 1
            ELSE 2 END,
            r.createdAt DESC, r.id
        LIMIT :limit
        """,
    )
    suspend fun searchQuotes(exact: String, prefix: String, contains: String, limit: Int): List<GlobalSearchTextRow>

    @Query(
        """
        SELECT r.id AS reflectionId, m.id AS mediaId, m.title AS mediaTitle,
            m.type AS mediaType, r.type AS reflectionType, r.content, r.createdAt
        FROM reflections r
        INNER JOIN consumptions c ON c.id = r.consumptionId
        INNER JOIN media_items m ON m.id = c.mediaItemId
        WHERE r.type IN ('FINAL_REFLECTION', 'LATER_REFLECTION', 'NOTE')
            AND lower(r.content) LIKE :contains ESCAPE '\'
        ORDER BY CASE
            WHEN lower(r.content) = :exact THEN 0
            WHEN lower(r.content) LIKE :prefix ESCAPE '\' THEN 1
            ELSE 2 END,
            CASE r.type WHEN 'FINAL_REFLECTION' THEN 0 WHEN 'LATER_REFLECTION' THEN 1 ELSE 2 END,
            r.createdAt DESC, r.id
        LIMIT :limit
        """,
    )
    suspend fun searchReflections(exact: String, prefix: String, contains: String, limit: Int): List<GlobalSearchTextRow>
}
