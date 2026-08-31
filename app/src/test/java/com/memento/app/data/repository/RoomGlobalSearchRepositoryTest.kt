package com.memento.app.data.repository

import com.memento.app.data.local.dao.GlobalSearchDao
import com.memento.app.data.local.dao.GlobalSearchFacetRow
import com.memento.app.data.local.dao.GlobalSearchMediaRow
import com.memento.app.data.local.dao.GlobalSearchTextRow
import com.memento.app.domain.model.CreatorRole
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.ReflectionType
import com.memento.app.domain.search.SearchMatchReason
import com.memento.app.domain.search.mediaFor
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomGlobalSearchRepositoryTest {
    @Test
    fun `exact title beats prefix and prefix beats contains while related matches are deduplicated`() = runTest {
        val dao = FakeGlobalSearchDao().apply {
            titles = listOf(
                media("contains", "My Dune Story"),
                media("prefix", "Dune Messiah"),
                media("exact", "Dune"),
            )
            creatorMedia = listOf(media("shared", "Arrival", "creator", "Denis Villeneuve", CreatorRole.DIRECTOR))
            tagMedia = listOf(media("shared", "Arrival", "tag", "Dune"))
            tags = listOf(GlobalSearchFacetRow("tag", "Dune", 1))
            creators = listOf(GlobalSearchFacetRow("creator", "Dune Author", 1, CreatorRole.AUTHOR))
        }

        val snapshot = RoomGlobalSearchRepository(dao).search("dune")

        assertEquals(listOf("exact", "prefix", "contains", "shared"), snapshot.media.map { it.mediaId })
        assertEquals(1, snapshot.media.count { it.mediaId == "shared" })
        assertEquals(2, snapshot.media.last().matchReasons.size)
        assertEquals("Dune", snapshot.tags.single().name)
        assertTrue(snapshot.media.last().matchReasons.any { it is SearchMatchReason.Creator })
        assertTrue(snapshot.media.last().matchReasons.any { it is SearchMatchReason.Tag })
    }

    @Test
    fun `tag and creator facets are returned with their associated media`() = runTest {
        val dao = FakeGlobalSearchDao().apply {
            tags = listOf(GlobalSearchFacetRow("comfort", "Comfort", 7))
            tagMedia = listOf(media("stardew", "Stardew Valley", "comfort", "Comfort"))
        }

        val comfort = RoomGlobalSearchRepository(dao).search("comfort")
        dao.tags = emptyList()
        dao.tagMedia = emptyList()
        dao.creators = listOf(GlobalSearchFacetRow("villeneuve", "Denis Villeneuve", 4, CreatorRole.DIRECTOR))
        dao.creatorMedia = listOf(media("arrival", "Arrival", "villeneuve", "Denis Villeneuve", CreatorRole.DIRECTOR))
        val creator = RoomGlobalSearchRepository(dao).search("villeneuve")

        assertEquals("Comfort", comfort.tags.single().name)
        assertEquals("stardew", comfort.media.single().mediaId)
        assertEquals("stardew", comfort.mediaFor(comfort.tags.single()).single().mediaId)
        assertEquals("Denis Villeneuve", creator.creators.single().name)
        assertEquals("arrival", creator.media.single().mediaId)
        assertEquals("arrival", creator.mediaFor(creator.creators.single()).single().mediaId)
    }

    @Test
    fun `quotes stay separate and reflections use final later note priority`() = runTest {
        val dao = FakeGlobalSearchDao().apply {
            quotes = listOf(text("quote", ReflectionType.QUOTE, "memory"))
            reflections = listOf(
                text("note", ReflectionType.NOTE, "memory", Instant.parse("2026-03-01T00:00:00Z")),
                text("later", ReflectionType.LATER_REFLECTION, "memory", Instant.parse("2026-02-01T00:00:00Z")),
                text("final", ReflectionType.FINAL_REFLECTION, "memory", Instant.parse("2026-01-01T00:00:00Z")),
            )
        }

        val snapshot = RoomGlobalSearchRepository(dao).search("memory")

        assertEquals(listOf("quote"), snapshot.quotes.map { it.reflectionId })
        assertEquals(listOf("final", "later", "note"), snapshot.reflections.map { it.reflectionId })
    }

    @Test
    fun `excerpt keeps the match and avoids cutting surrounding words`() {
        val excerpt = buildSearchExcerpt(
            "Antes de que aparezca nada relevante, seguimos caminando lentamente hasta que aparece el gusano y todo empieza a cambiar completamente en el desierto de Arrakis.",
            "gusano",
            maxLength = 72,
        )

        assertTrue(excerpt.contains("gusano"))
        assertTrue(excerpt.startsWith("…"))
        assertTrue(excerpt.endsWith("…"))
        assertTrue(excerpt.removePrefix("…").first().isLetter())
        assertTrue(excerpt.removeSuffix("…").last().let { it.isLetter() || it == '.' })
    }

    @Test
    fun `percent underscore and escape are protected as literal LIKE characters`() {
        val percent = prepareGlobalSearchQuery("100%")
        val underscore = prepareGlobalSearchQuery("a_b")
        val slash = prepareGlobalSearchQuery("a\\b")

        assertEquals("%100\\%%", percent.containsPattern)
        assertEquals("%a\\_b%", underscore.containsPattern)
        assertEquals("%a\\\\b%", slash.containsPattern)
    }

    @Test
    fun `result limits are enforced after deterministic ranking`() = runTest {
        val dao = FakeGlobalSearchDao().apply {
            titles = (1..25).map { media("media-$it", "Query $it") }
            tags = (1..15).map { GlobalSearchFacetRow("tag-$it", "Query $it", it) }
            creators = (1..15).map { GlobalSearchFacetRow("creator-$it", "Query $it", it, CreatorRole.AUTHOR) }
            genres = (1..15).map { GlobalSearchFacetRow("genre-$it", "Query $it", it) }
            quotes = (1..25).map { text("quote-$it", ReflectionType.QUOTE, "query") }
            reflections = (1..25).map { text("reflection-$it", ReflectionType.NOTE, "query") }
        }

        val snapshot = RoomGlobalSearchRepository(dao).search("query")

        assertEquals(20, snapshot.media.size)
        assertEquals(10, snapshot.tags.size)
        assertEquals(10, snapshot.creators.size)
        assertEquals(10, snapshot.genres.size)
        assertEquals(20, snapshot.quotes.size)
        assertEquals(20, snapshot.reflections.size)
    }

    @Test
    fun `short query returns empty without touching SQLite`() = runTest {
        val dao = FakeGlobalSearchDao()

        val snapshot = RoomGlobalSearchRepository(dao).search("a")

        assertTrue(snapshot.isEmpty)
        assertEquals(0, dao.calls)
    }

    private fun media(
        id: String,
        title: String,
        reasonId: String? = null,
        reasonName: String? = null,
        role: CreatorRole? = null,
    ) = GlobalSearchMediaRow(id, MediaType.MOVIE, title, null, reasonId, reasonName, role)

    private fun text(
        id: String,
        type: ReflectionType,
        content: String,
        createdAt: Instant = Instant.EPOCH,
    ) = GlobalSearchTextRow(id, "media-$id", "Work $id", MediaType.BOOK, type, content, createdAt)
}

private class FakeGlobalSearchDao : GlobalSearchDao {
    var titles: List<GlobalSearchMediaRow> = emptyList()
    var creatorMedia: List<GlobalSearchMediaRow> = emptyList()
    var genreMedia: List<GlobalSearchMediaRow> = emptyList()
    var tagMedia: List<GlobalSearchMediaRow> = emptyList()
    var tags: List<GlobalSearchFacetRow> = emptyList()
    var creators: List<GlobalSearchFacetRow> = emptyList()
    var genres: List<GlobalSearchFacetRow> = emptyList()
    var quotes: List<GlobalSearchTextRow> = emptyList()
    var reflections: List<GlobalSearchTextRow> = emptyList()
    var calls = 0

    override suspend fun searchMediaTitles(exact: String, prefix: String, contains: String, limit: Int) = titles.also { calls++ }
    override suspend fun searchMediaCreators(exact: String, prefix: String, contains: String, limit: Int) = creatorMedia.also { calls++ }
    override suspend fun searchMediaGenres(exact: String, prefix: String, contains: String, limit: Int) = genreMedia.also { calls++ }
    override suspend fun searchMediaTags(exact: String, prefix: String, contains: String, limit: Int) = tagMedia.also { calls++ }
    override suspend fun searchTags(exact: String, prefix: String, contains: String, limit: Int) = tags.also { calls++ }
    override suspend fun searchCreators(exact: String, prefix: String, contains: String, limit: Int) = creators.also { calls++ }
    override suspend fun searchGenres(exact: String, prefix: String, contains: String, limit: Int) = genres.also { calls++ }
    override suspend fun searchQuotes(exact: String, prefix: String, contains: String, limit: Int) = quotes.also { calls++ }
    override suspend fun searchReflections(exact: String, prefix: String, contains: String, limit: Int) = reflections.also { calls++ }
}
