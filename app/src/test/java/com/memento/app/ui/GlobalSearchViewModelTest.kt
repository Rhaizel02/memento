package com.memento.app.ui

import com.memento.app.MainDispatcherRule
import com.memento.app.domain.repository.GlobalSearchRepository
import com.memento.app.domain.search.GlobalSearchSnapshot
import com.memento.app.ui.search.GlobalSearchViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalSearchViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `empty and short queries do not search and valid query is debounced`() = runTest {
        val repository = RecordingSearchRepository()
        val viewModel = GlobalSearchViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

        viewModel.setQuery("")
        advanceTimeBy(300)
        runCurrent()
        viewModel.setQuery("a")
        advanceTimeBy(300)
        runCurrent()
        assertEquals(emptyList<String>(), repository.queries)

        viewModel.setQuery("du")
        advanceTimeBy(299)
        runCurrent()
        assertEquals(emptyList<String>(), repository.queries)
        advanceTimeBy(1)
        runCurrent()

        assertEquals(listOf("du"), repository.queries)
    }
}

private class RecordingSearchRepository : GlobalSearchRepository {
    val queries = mutableListOf<String>()

    override suspend fun search(query: String): GlobalSearchSnapshot {
        queries += query
        return GlobalSearchSnapshot(query)
    }
}
