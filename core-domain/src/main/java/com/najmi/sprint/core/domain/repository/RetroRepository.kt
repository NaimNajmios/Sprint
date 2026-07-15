package com.najmi.sprint.core.domain.repository

import com.najmi.sprint.core.domain.model.RetroEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Repository interface for [RetroEntry] operations.
 */
interface RetroRepository {
    fun observeRetros(): Flow<List<RetroEntry>>
    suspend fun getRetroForWeek(weekOf: LocalDate): RetroEntry?
    suspend fun insertRetro(retro: RetroEntry)
    suspend fun getLatestRetro(): RetroEntry?
}
