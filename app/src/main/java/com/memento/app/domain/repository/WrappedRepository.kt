package com.memento.app.domain.repository

import com.memento.app.domain.wrapped.WrappedSource
import kotlinx.coroutines.flow.Flow

interface WrappedRepository {
    fun observeSource(): Flow<WrappedSource>
}
