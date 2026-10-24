package com.memento.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.memento.app.MainActivity
import com.memento.app.data.local.database.MementoDatabase
import com.memento.app.data.preferences.SettingsRepository
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.data.repository.RoomMediaRepository
import com.memento.app.domain.model.*
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Real navigation and capture smoke test. Fixtures are confined to the review emulator. */
class WarmArchiveJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun archiveNavigationAndCaptureJourney() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, MementoDatabase::class.java, "memento.db").build()
        val repository = RoomMediaRepository(database, database.mediaDao(), database.consumptionDao())
        val settings = SettingsRepository(context)
        val today = LocalDate.now()
        val duneId = runBlocking {
            settings.setThemeMode(ThemeMode.LIGHT)
            settings.completeOnboarding()
            val dune = repository.addExternal(
                MetadataSearchResult(MetadataProvider.OPEN_LIBRARY, "warm-review-dune", null, MediaType.BOOK, "Dune",
                    releaseYear = 1965, pageCount = 680, creators = listOf("Frank Herbert"),
                    genres = listOf("Ciencia ficción"),
                    posterUrl = "https://covers.openlibrary.org/b/isbn/9780441172719-L.jpg",
                    description = "En el desierto de Arrakis, una historia sobre el poder, la memoria y las decisiones que cambian un mundo."),
                ConsumptionStatus.IN_PROGRESS,
            ).mediaId
            val active = repository.observeMediaDetail(dune).first { it?.activeConsumption != null }!!.activeConsumption!!
            if (repository.observeMediaDetail(dune).first { it != null }!!.progress.isEmpty()) {
                repository.addProgress(active.id, ProgressType.PAGES, 384.0, 680.0)
                repository.saveReflection(active.id, ReflectionType.QUOTE, "El misterio de la vida no es un problema que resolver, sino una realidad que experimentar.")
            }
            repository.createAndAttachTag(dune, "Para releer")
            val works = listOf(
                Triple("La mano izquierda de la oscuridad", "Ursula K. Le Guin", MediaType.BOOK),
                Triple("Arrival", "Denis Villeneuve", MediaType.MOVIE),
                Triple("Interstellar", "Christopher Nolan", MediaType.MOVIE),
                Triple("Solaris", "Andréi Tarkovski", MediaType.MOVIE),
                Triple("Klara y el sol", "Kazuo Ishiguro", MediaType.BOOK),
                Triple("Disco Elysium", "ZA/UM", MediaType.GAME),
            )
            works.forEachIndexed { index, (title, creator, type) ->
                val date = if (index == 0) today.minusYears(1) else today.minusDays(30L + index * 28)
                repository.addExternal(
                    MetadataSearchResult(MetadataProvider.OPEN_LIBRARY, "warm-review-$index", null, type, title,
                        creators = listOf(creator), genres = listOf("Ciencia ficción")),
                    ConsumptionStatus.COMPLETED,
                    CompletedMediaInput(date, 8 + index % 3, true,
                        "Volver a esta historia me recuerda por qué guardo mis lecturas: algunas preguntas siguen acompañándome mucho después del final."),
                )
            }
            dune
        }
        try {
            compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Biblioteca").fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(30_000) { compose.onAllNodesWithText("Dune").fetchSemanticsNodes().isNotEmpty() }
            capture("home-light")
            runBlocking { settings.setThemeMode(ThemeMode.DARK) }
            capture("home-dark")
            compose.onNodeWithContentDescription("Biblioteca").performClick()
            capture("library-dark")
            runBlocking { settings.setThemeMode(ThemeMode.LIGHT) }
            capture("library-light")
            compose.onNodeWithText("Dune").performScrollTo().performClick()
            capture("detail-light")
            runBlocking { settings.setThemeMode(ThemeMode.DARK) }
            capture("detail-dark")
            compose.onNodeWithText("Actualizar progreso").performScrollTo().performClick()
            compose.onNodeWithText("+5").performScrollTo().performClick()
            capture("quick-progress")
            compose.onNodeWithText("Guardar").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Guardar").fetchSemanticsNodes().isEmpty() }
            runBlocking {
                val progress = repository.observeMediaDetail(duneId).first { it?.progress?.firstOrNull()?.currentValue == 389.0 }
                assertEquals(389.0, progress!!.progress.first().currentValue!!, 0.0)
            }
            compose.onNodeWithText("Añadir nota").performScrollTo().performClick()
            val note = "Una pregunta que quiero recordar cuando vuelva a esta historia."
            compose.onAllNodes(hasSetTextAction())[0].performTextInput(note)
            capture("quick-note-keyboard")
            compose.onNodeWithText("Guardar").performScrollTo().performClick()
            runBlocking {
                val detail = repository.observeMediaDetail(duneId).first { it?.reflections?.any { r -> r.content == note } == true }
                assertTrue(detail!!.reflections.any { it.content == note })
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Guardar").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithContentDescription("Volver").performClick()
            compose.onNodeWithContentDescription("Historia").performClick()
            capture("history")
            compose.onNodeWithContentDescription("Abrir calendario cultural").performClick()
            capture("calendar")
            compose.onNodeWithContentDescription("Volver").performClick()
            compose.onNodeWithContentDescription("Descubrir").performClick()
            capture("discover")
            if (compose.onAllNodesWithContentDescription("Ver recomendación").fetchSemanticsNodes().isNotEmpty()) {
                compose.onAllNodesWithContentDescription("Ver recomendación")[0].performClick()
                capture("recommendation-detail")
                compose.onNodeWithContentDescription("Volver").performClick()
            }
            compose.onNodeWithContentDescription("Ajustes").performClick()
            capture("settings")
            compose.onNodeWithContentDescription("Volver").performClick()
        } finally {
            database.close()
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(700)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "warm-archive").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(folder, "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
