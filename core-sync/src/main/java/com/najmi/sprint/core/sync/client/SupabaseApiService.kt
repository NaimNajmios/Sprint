package com.najmi.sprint.core.sync.client

import com.najmi.sprint.core.sync.model.ContextDto
import com.najmi.sprint.core.sync.model.RetroEntryDto
import com.najmi.sprint.core.sync.model.SessionDto
import com.najmi.sprint.core.sync.model.TaskDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseApiService @Inject constructor(
    private val client: SupabaseClient
) {
    // --- Contexts ---
    suspend fun fetchContexts(): List<ContextDto> {
        return client.httpClient.get("contexts").body()
    }

    suspend fun upsertContexts(contexts: List<ContextDto>) {
        if (contexts.isEmpty()) return
        client.httpClient.post("contexts") {
            header("Prefer", "return=representation,resolution=merge-duplicates")
            url { parameters.append("on_conflict", "id") }
            setBody(contexts)
        }
    }

    // --- Tasks ---
    suspend fun fetchTasks(): List<TaskDto> {
        return client.httpClient.get("tasks").body()
    }

    suspend fun upsertTasks(tasks: List<TaskDto>) {
        if (tasks.isEmpty()) return
        client.httpClient.post("tasks") {
            header("Prefer", "return=representation,resolution=merge-duplicates")
            url { parameters.append("on_conflict", "id") }
            setBody(tasks)
        }
    }

    // --- Sessions ---
    suspend fun fetchSessions(): List<SessionDto> {
        return client.httpClient.get("sessions").body()
    }

    suspend fun upsertSessions(sessions: List<SessionDto>) {
        if (sessions.isEmpty()) return
        client.httpClient.post("sessions") {
            header("Prefer", "return=representation,resolution=merge-duplicates")
            url { parameters.append("on_conflict", "id") }
            setBody(sessions)
        }
    }

    // --- Retro Entries ---
    suspend fun fetchRetroEntries(): List<RetroEntryDto> {
        return client.httpClient.get("retro_entries").body()
    }

    suspend fun upsertRetroEntries(entries: List<RetroEntryDto>) {
        if (entries.isEmpty()) return
        client.httpClient.post("retro_entries") {
            header("Prefer", "return=representation,resolution=merge-duplicates")
            url { parameters.append("on_conflict", "id") }
            setBody(entries)
        }
    }
}
