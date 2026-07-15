package com.najmi.sprint.core.domain.repository

import com.najmi.sprint.core.domain.model.Context
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for [Context] operations.
 *
 * Implementations live in core-data; this interface stays in core-domain
 * to keep the dependency arrow pointing inward (Clean Architecture).
 */
interface ContextRepository {
    fun observeActiveContexts(): Flow<List<Context>>
    fun observeAllContexts(): Flow<List<Context>>
    suspend fun getContextById(id: String): Context?
    suspend fun insertContext(context: Context)
    suspend fun updateContext(context: Context)
    suspend fun softDeleteContext(id: String)
    suspend fun getActiveContextCount(): Int
}
