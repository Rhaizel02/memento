package com.memento.app.domain.repository

import com.memento.app.domain.search.GlobalSearchSnapshot

interface GlobalSearchRepository {
    suspend fun search(query: String): GlobalSearchSnapshot
}
