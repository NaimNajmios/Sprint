package com.najmi.sprint.core.domain.repository

import com.najmi.sprint.core.domain.model.Task
import com.najmi.sprint.core.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for [Task] operations.
 */
interface TaskRepository {
    fun observeTasksByContext(contextId: String): Flow<List<Task>>
    fun observeTasksByStatus(status: TaskStatus): Flow<List<Task>>
    suspend fun getTaskById(id: String): Task?
    suspend fun insertTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun updateTaskStatus(id: String, status: TaskStatus)
    suspend fun deleteTask(id: String)
}
