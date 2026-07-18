package com.najmi.sprint.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Kanban board status for a task.
 */
@Serializable
enum class TaskStatus {
    BACKLOG,
    IN_PROGRESS,
    REVIEW,
    DONE
}

/**
 * A user-created task on the Kanban board.
 *
 * Tasks belong to a [Context] and optionally to a [Project] within that context.
 * Uses [deviceId] for last-write-wins tiebreak during sync.
 */
@Serializable
data class Task(
    val id: String,
    val contextId: String,
    val projectId: String? = null,
    val title: String,
    val status: TaskStatus = TaskStatus.BACKLOG,
    val estimatePoints: Int? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deviceId: String,
    val githubIssueNumber: Int? = null,
    val githubIssueUrl: String? = null
)
