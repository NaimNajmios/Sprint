package com.najmi.sprint.core.sync

import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.RetroRepository
import com.najmi.sprint.core.domain.repository.SessionRepository
import com.najmi.sprint.core.domain.repository.TaskRepository
import com.najmi.sprint.core.sync.client.SupabaseApiService
import com.najmi.sprint.core.sync.model.toDomain
import com.najmi.sprint.core.sync.model.toDto
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncEngine @Inject constructor(
    private val api: SupabaseApiService,
    private val contextRepository: ContextRepository,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val retroRepository: RetroRepository
) {
    /**
     * Performs a full bidirectional sync.
     * 1. Pushes all local records to Supabase (upsert).
     * 2. Pulls all remote records from Supabase.
     * 3. Inserts/updates remote records locally.
     *
     * In a production app, we would use a Last-Modified timestamp or Event Log 
     * to only sync delta changes. This MVP syncs full snapshots.
     */
    suspend fun syncAll() {
        syncContexts()
        syncTasks()
        // Sessions can get large, but we'll do full sync for MVP
        syncSessions()
        syncRetros()
    }

    private suspend fun syncContexts() {
        // 1. Push local
        val localContexts = contextRepository.observeAllContexts().first()
        api.upsertContexts(localContexts.map { it.toDto() })

        // 2. Pull remote
        val remoteContexts = api.fetchContexts()
        
        // 3. Save remote locally
        remoteContexts.forEach { contextDto ->
            contextRepository.insertContext(contextDto.toDomain())
        }
    }

    private suspend fun syncTasks() {
        val localTasks = taskRepository.observeAllTasks().first()
        api.upsertTasks(localTasks.map { it.toDto() })

        val remoteTasks = api.fetchTasks()
        remoteTasks.forEach { taskDto ->
            taskRepository.insertTask(taskDto.toDomain())
        }
    }

    private suspend fun syncSessions() {
        // Currently sessionRepository doesn't have observeAllSessions.
        // For MVP, we will pull the last 1000 sessions or so to avoid OOM, 
        // or just use recent sessions. Let's use observeRecentSessions(limit = 1000).
        val localSessions = sessionRepository.observeRecentSessions(1000).first()
        api.upsertSessions(localSessions.map { it.toDto() })

        val remoteSessions = api.fetchSessions()
        remoteSessions.forEach { sessionDto ->
            sessionRepository.insertSession(sessionDto.toDomain())
        }
    }

    private suspend fun syncRetros() {
        val localRetros = retroRepository.observeRetros().first()
        api.upsertRetroEntries(localRetros.map { it.toDto() })

        val remoteRetros = api.fetchRetroEntries()
        remoteRetros.forEach { retroDto ->
            retroRepository.insertRetro(retroDto.toDomain())
        }
    }
}
