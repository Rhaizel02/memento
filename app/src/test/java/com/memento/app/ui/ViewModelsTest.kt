package com.memento.app.ui

import com.memento.app.FakeMediaRepository
import com.memento.app.MainDispatcherRule
import com.memento.app.FakeRememberRepository
import com.memento.app.FakeMetadataRepository
import com.memento.app.FakeRecommendationRepository
import com.memento.app.domain.model.ConsumptionStatus
import com.memento.app.domain.model.MediaItem
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.model.LibrarySort
import com.memento.app.domain.model.EditMediaInput
import com.memento.app.domain.model.MetadataProvider
import com.memento.app.domain.model.MetadataSearchResult
import com.memento.app.domain.repository.MetadataDetailsOutcome
import com.memento.app.ui.add.AddMediaViewModel
import com.memento.app.ui.detail.MediaDetailViewModel
import com.memento.app.ui.home.HomeViewModel
import com.memento.app.ui.library.LibraryViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import com.memento.app.domain.remember.RememberCandidate
import com.memento.app.domain.model.ReflectionType

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val media = MediaItem(
        id = "m1",
        type = MediaType.BOOK,
        title = "Dune",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test fun `home exposes in progress and completed sections`() = runTest {
        val repository = FakeMediaRepository().apply {
            inProgress.value = listOf(media)
            completed.value = listOf(media.copy(id = "m2"))
            completedCounts.value = mapOf(MediaType.BOOK to 1)
        }
        val viewModel = HomeViewModel(repository, FakeRememberRepository(), FakeRecommendationRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.inProgress.size)
        assertEquals(1, viewModel.state.value.recentlyCompleted.size)
        assertEquals(1, viewModel.state.value.completedByType[MediaType.BOOK])
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test fun `home hero records a remember exposure when shown`() = runTest {
        val remember = FakeRememberRepository().apply {
            memory.value = RememberCandidate(
                "c1", "m1", "Dune", LocalDate.of(2024, 1, 1), 9, true, null, null,
                "r1", ReflectionType.FINAL_REFLECTION, "Idea", 1, null,
            )
        }
        val viewModel = HomeViewModel(FakeMediaRepository(), remember, FakeRecommendationRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        advanceUntilIdle()

        assertEquals(listOf("c1"), remember.recordedExposures)
    }

    @Test fun `library forwards search and type filters`() = runTest {
        val repository = FakeMediaRepository().apply { library.value = listOf(media) }
        val viewModel = LibraryViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        viewModel.setQuery("dune")
        viewModel.setType(MediaType.BOOK)
        viewModel.setFavoritesOnly(true)
        viewModel.setMinRating(8)
        viewModel.setSort(LibrarySort.RATING)
        advanceUntilIdle()

        assertEquals("dune", repository.lastQuery)
        assertEquals(MediaType.BOOK, repository.lastType)
        assertEquals(true, repository.lastFilters.favoritesOnly)
        assertEquals(8, repository.lastFilters.minRatingHalfStars)
        assertEquals(LibrarySort.RATING, repository.lastFilters.sort)
        assertEquals(listOf(media), viewModel.state.value.items)
    }

    @Test fun `add media sends complete manual input`() = runTest {
        val repository = FakeMediaRepository()
        val viewModel = AddMediaViewModel(repository, FakeMetadataRepository())
        viewModel.showManual()
        viewModel.setTitle("Dune")
        viewModel.setCreator("Frank Herbert")
        viewModel.save(ConsumptionStatus.PLANNED)
        advanceUntilIdle()

        assertEquals("Dune", repository.addedInput?.title)
        assertEquals("Frank Herbert", repository.addedInput?.creator)
        assertEquals(ConsumptionStatus.PLANNED, repository.addedStatus)
        assertEquals("new-media", viewModel.state.value.savedMediaId)
    }

    @Test fun `a new add session starts clean after a previous form was used`() = runTest {
        val repository = FakeMediaRepository()
        val firstSession = AddMediaViewModel(repository, FakeMetadataRepository()).apply {
            showManual()
            setTitle("Primera obra")
            setCreator("Autora")
        }
        assertEquals("Primera obra", firstSession.state.value.title)

        val secondSession = AddMediaViewModel(repository, FakeMetadataRepository())

        assertEquals("", secondSession.state.value.query)
        assertEquals("", secondSession.state.value.title)
        assertEquals(MediaType.BOOK, secondSession.state.value.type)
        assertEquals(com.memento.app.ui.add.AddMediaMode.SEARCH, secondSession.state.value.mode)
    }

    @Test fun `manual draft survives search round trip in the same add session`() = runTest {
        val viewModel = AddMediaViewModel(FakeMediaRepository(), FakeMetadataRepository())
        viewModel.showManual()
        viewModel.setType(MediaType.GAME)
        viewModel.setTitle("Dune")
        viewModel.setYear("1965")
        viewModel.setDescription("Desierto")

        viewModel.returnToSearch()
        assertEquals(MediaType.BOOK, viewModel.state.value.type)
        viewModel.setQuery("otra obra")
        viewModel.showManual()

        assertEquals("Dune", viewModel.state.value.title)
        assertEquals("1965", viewModel.state.value.year)
        assertEquals("Desierto", viewModel.state.value.description)
        assertEquals(MediaType.GAME, viewModel.state.value.type)
        assertEquals("otra obra", viewModel.state.value.query)
        assertEquals(null, viewModel.state.value.selectedExternal)
    }

    @Test fun `external details do not overwrite the manual draft`() = runTest {
        val remote = MetadataSearchResult(MetadataProvider.OPEN_LIBRARY, "remote", null, MediaType.BOOK, "Remota")
        val viewModel = AddMediaViewModel(FakeMediaRepository(), FakeMetadataRepository())
        viewModel.showManual()
        viewModel.setTitle("Manual")
        viewModel.returnToSearch()

        viewModel.selectResult(remote)
        advanceUntilIdle()
        assertEquals("Remota", viewModel.state.value.title)
        viewModel.returnToSearch()
        viewModel.showManual()

        assertEquals("Manual", viewModel.state.value.title)
        assertEquals("", viewModel.state.value.query)
        assertEquals(null, viewModel.state.value.selectedExternal)
    }

    @Test fun `completed add waits for historical details and forwards them once`() = runTest {
        val repository = FakeMediaRepository()
        val viewModel = AddMediaViewModel(repository, FakeMetadataRepository())
        viewModel.showManual()
        viewModel.setTitle("Película histórica")

        viewModel.save(ConsumptionStatus.COMPLETED)
        assertEquals(0, repository.addManualInvocations)
        assertEquals(com.memento.app.ui.add.AddMediaMode.COMPLETE_DETAILS, viewModel.state.value.mode)
        viewModel.setCompletedDate("2022-04-12")
        viewModel.setCompletedRating(9)
        viewModel.setCompletedFavorite(true)
        viewModel.setCompletedReflection("  Una reflexión  ")
        viewModel.saveCompleted()
        viewModel.saveCompleted()
        advanceUntilIdle()
        viewModel.saveCompleted()
        advanceUntilIdle()

        assertEquals(1, repository.addManualInvocations)
        assertEquals(ConsumptionStatus.COMPLETED, repository.addedStatus)
        assertEquals(LocalDate.of(2022, 4, 12), repository.completedInput?.completedDate)
        assertEquals(9, repository.completedInput?.ratingHalfStars)
        assertEquals(true, repository.completedInput?.favorite)
        assertEquals("Una reflexión", repository.completedInput?.finalReflection)
    }

    @Test fun `completed add defaults its required date to today`() = runTest {
        val viewModel = AddMediaViewModel(FakeMediaRepository(), FakeMetadataRepository())
        viewModel.showManual()
        viewModel.setTitle("Terminada hoy")

        viewModel.save(ConsumptionStatus.COMPLETED)

        assertEquals(LocalDate.now(), viewModel.state.value.completedDraft?.completedDate)
        assertEquals(true, viewModel.state.value.canSaveCompletion)
    }

    @Test fun `completed add accepts no rating and drops blank final reflection`() = runTest {
        val repository = FakeMediaRepository()
        val viewModel = AddMediaViewModel(repository, FakeMetadataRepository())
        viewModel.showManual()
        viewModel.setTitle("Sin valoración")
        viewModel.save(ConsumptionStatus.COMPLETED)
        viewModel.setCompletedDate("2020-01-02")
        viewModel.setCompletedReflection("   ")
        viewModel.saveCompleted()
        advanceUntilIdle()

        assertEquals(null, repository.completedInput?.ratingHalfStars)
        assertEquals(null, repository.completedInput?.finalReflection)
    }

    @Test fun `cancelling completed step persists nothing and restores manual draft`() = runTest {
        val repository = FakeMediaRepository()
        val viewModel = AddMediaViewModel(repository, FakeMetadataRepository())
        viewModel.showManual()
        viewModel.setTitle("Borrador intacto")
        viewModel.save(ConsumptionStatus.COMPLETED)
        viewModel.setCompletedDate("2021-05-06")

        viewModel.cancelCompletion()

        assertEquals(0, repository.addManualInvocations)
        assertEquals(com.memento.app.ui.add.AddMediaMode.MANUAL, viewModel.state.value.mode)
        assertEquals("Borrador intacto", viewModel.state.value.title)
    }

    @Test fun `completed add does not save an empty completion date`() = runTest {
        val repository = FakeMediaRepository()
        val viewModel = AddMediaViewModel(repository, FakeMetadataRepository())
        viewModel.showManual()
        viewModel.setTitle("Sin fecha")
        viewModel.save(ConsumptionStatus.COMPLETED)
        viewModel.setCompletedDate("")

        viewModel.saveCompleted()
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.canSaveCompletion)
        assertEquals(0, repository.addManualInvocations)
    }

    @Test fun `external completed add still waits for enriched details`() = runTest {
        val repository = FakeMediaRepository()
        val summary = MetadataSearchResult(MetadataProvider.OPEN_LIBRARY, "OL2W", null, MediaType.BOOK, "Resumen")
        val metadata = FakeMetadataRepository().apply {
            detailOutcome = MetadataDetailsOutcome.Complete(summary.copy(title = "Detalle", pageCount = 400))
        }
        val viewModel = AddMediaViewModel(repository, metadata)

        viewModel.selectResult(summary)
        advanceUntilIdle()
        viewModel.save(ConsumptionStatus.COMPLETED)
        assertEquals(0, repository.addExternalInvocations)
        viewModel.setCompletedDate("2019-08-09")
        viewModel.saveCompleted()
        advanceUntilIdle()

        assertEquals(1, metadata.detailRequests)
        assertEquals("Detalle", repository.externalResult?.title)
        assertEquals(400, repository.externalResult?.pageCount)
        assertEquals(LocalDate.of(2019, 8, 9), repository.completedInput?.completedDate)
    }

    @Test fun `double save gesture only starts one persistence operation`() = runTest {
        val repository = FakeMediaRepository()
        val viewModel = AddMediaViewModel(repository, FakeMetadataRepository())
        viewModel.showManual()
        viewModel.setTitle("Dune")

        viewModel.save(ConsumptionStatus.PLANNED)
        viewModel.save(ConsumptionStatus.PLANNED)
        advanceUntilIdle()

        assertEquals(1, repository.addManualInvocations)
    }

    @Test fun `newer search replaces an older in flight request`() = runTest {
        val oldResult = MetadataSearchResult(MetadataProvider.OPEN_LIBRARY, "old", null, MediaType.BOOK, "Antigua")
        val newResult = MetadataSearchResult(MetadataProvider.OPEN_LIBRARY, "new", null, MediaType.BOOK, "Nueva")
        val metadata = FakeMetadataRepository().apply {
            searchHandler = { _, query ->
                delay(if (query == "antigua") 1_000 else 10)
                com.memento.app.domain.model.MetadataSearchOutcome.Success(
                    MetadataProvider.OPEN_LIBRARY,
                    listOf(if (query == "antigua") oldResult else newResult),
                )
            }
        }
        val viewModel = AddMediaViewModel(FakeMediaRepository(), metadata)

        viewModel.setQuery("antigua")
        advanceTimeBy(351)
        viewModel.setQuery("nueva")
        advanceUntilIdle()

        assertEquals(listOf(newResult), viewModel.state.value.searchResults)
        assertFalse(viewModel.state.value.isSearching)
    }

    @Test fun `clearing search cancels pending work and removes results`() = runTest {
        val metadata = FakeMetadataRepository().apply {
            searchHandler = { _, _ ->
                delay(1_000)
                outcome
            }
        }
        val viewModel = AddMediaViewModel(FakeMediaRepository(), metadata)

        viewModel.setQuery("dune")
        advanceTimeBy(351)
        assertTrue(viewModel.state.value.isSearching)
        viewModel.setQuery("")
        advanceUntilIdle()

        assertEquals(emptyList<MetadataSearchResult>(), viewModel.state.value.searchResults)
        assertFalse(viewModel.state.value.isSearching)
    }

    @Test fun `latest selected result wins when detail requests overlap`() = runTest {
        val first = MetadataSearchResult(MetadataProvider.OPEN_LIBRARY, "first", null, MediaType.BOOK, "Primera")
        val second = MetadataSearchResult(MetadataProvider.OPEN_LIBRARY, "second", null, MediaType.BOOK, "Segunda")
        val metadata = FakeMetadataRepository().apply {
            detailsHandler = { result ->
                delay(if (result.externalId == "first") 1_000 else 10)
                MetadataDetailsOutcome.Complete(result.copy(title = "Detalle ${result.title}"))
            }
        }
        val viewModel = AddMediaViewModel(FakeMediaRepository(), metadata)

        viewModel.selectResult(first)
        viewModel.selectResult(second)
        advanceUntilIdle()

        assertEquals("Detalle Segunda", viewModel.state.value.title)
        assertEquals("second", viewModel.state.value.selectedExternal?.externalId)
    }

    @Test fun `external selection fetches full detail before enabling persistence`() = runTest {
        val repository = FakeMediaRepository()
        val summary = MetadataSearchResult(MetadataProvider.OPEN_LIBRARY, "OL1W", null, MediaType.BOOK, "Resumen")
        val metadata = FakeMetadataRepository().apply {
            detailOutcome = MetadataDetailsOutcome.Complete(summary.copy(title = "Detalle", pageCount = 321))
        }
        val viewModel = AddMediaViewModel(repository, metadata)

        viewModel.selectResult(summary)
        assertFalse(viewModel.state.value.canSave)
        advanceUntilIdle()
        viewModel.save(ConsumptionStatus.PLANNED)
        advanceUntilIdle()

        assertEquals(1, metadata.detailRequests)
        assertEquals("Detalle", repository.externalResult?.title)
        assertEquals(321, repository.externalResult?.pageCount)
    }

    @Test fun `failed detail fetch keeps partial search result persistable with notice`() = runTest {
        val repository = FakeMediaRepository()
        val summary = MetadataSearchResult(MetadataProvider.RAWG, "7", null, MediaType.GAME, "Resultado parcial")
        val metadata = FakeMetadataRepository().apply { detailOutcome = MetadataDetailsOutcome.Partial(summary) }
        val viewModel = AddMediaViewModel(repository, metadata)

        viewModel.selectResult(summary)
        advanceUntilIdle()
        assertEquals(true, viewModel.state.value.metadataIsPartial)
        assertEquals(true, viewModel.state.value.canSave)
        viewModel.save(ConsumptionStatus.PLANNED)
        advanceUntilIdle()

        assertEquals("Resultado parcial", repository.externalResult?.title)
    }

    @Test fun `detail starts a new consumption for loaded media`() = runTest {
        val repository = FakeMediaRepository()
        val viewModel = MediaDetailViewModel(repository)
        viewModel.load("m1")
        viewModel.start()
        advanceUntilIdle()

        assertEquals("m1", repository.startedMediaId)
    }

    @Test fun `detail edits user owned metadata and deletes only selected consumption`() = runTest {
        val repository = FakeMediaRepository()
        val viewModel = MediaDetailViewModel(repository)
        viewModel.load("m1")
        val input = EditMediaInput("Dune revisado", 1965, "Descripción", listOf("Frank Herbert"), "https://image")

        viewModel.updateMetadata(input)
        advanceUntilIdle()
        viewModel.deleteConsumption("c2")
        advanceUntilIdle()
        viewModel.updateReflection("r1", "Texto corregido por la persona")
        advanceUntilIdle()

        assertEquals("m1", repository.editedMediaId)
        assertEquals(input, repository.editedInput)
        assertEquals("c2", repository.deletedConsumptionId)
        assertEquals("r1", repository.editedReflectionId)
        assertEquals("Texto corregido por la persona", repository.editedReflectionContent)
    }
}
