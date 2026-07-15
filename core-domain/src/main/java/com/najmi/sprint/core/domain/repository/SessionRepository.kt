package com.najmi.sprint.core.domain.repository

import com.najmi.sprint.core.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Repository interface for [Session] operations.
 */
interface SessionRepository {
    fun observeSessionsForDate(date: kotlinx.datetime.LocalDate): Flow<List<Session>>
    fun observeUnclassifiedSessions(): Flow<List<Session>>
    fun observeRecentSessions(limit: Int = 50): Flow<List<Session>>
    suspend fun getSessionById(id: String): Session?
    suspend fun insertSession(session: Session)
    suspend fun updateSession(session: Session)
    suspend fun closeActiveSession(endTime: Instant)
    suspend fun getSessionsBetween(start: Instant, end: Instant): List<Session>
}
