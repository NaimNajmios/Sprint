package com.najmi.sprint.feature.kanban

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.Task
import com.najmi.sprint.core.domain.model.TaskStatus
import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.GlobalContextManager
import com.najmi.sprint.core.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject

data class KanbanState(
    val isLoading: Boolean = true,
    val tasksByStatus: Map<TaskStatus, List<Task>> = emptyMap(),
    val contexts: List<Context> = emptyList(),
    val selectedContextId: String? = null
)

@HiltViewModel
class KanbanViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val contextRepository: ContextRepository,
    private val globalContextManager: GlobalContextManager
) : ViewModel() {

    val state: StateFlow<KanbanState> = combine(
        taskRepository.observeAllTasks(),
        contextRepository.observeActiveContexts(),
        globalContextManager.selectedContextId
    ) { allTasks, contexts, selectedContextId ->
        
        val filteredTasks = if (selectedContextId != null) {
            allTasks.filter { it.contextId == selectedContextId }
        } else {
            allTasks
        }

        val groupedTasks = TaskStatus.entries.associateWith { status ->
            filteredTasks.filter { it.status == status }.sortedByDescending { it.updatedAt }
        }

        KanbanState(
            isLoading = false,
            tasksByStatus = groupedTasks,
            contexts = contexts,
            selectedContextId = selectedContextId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KanbanState()
    )

    fun addTask(title: String) {
        val currentContextId = state.value.selectedContextId
            ?: state.value.contexts.firstOrNull()?.id
            ?: return // Cannot add task without a context

        viewModelScope.launch {
            val now = Clock.System.now()
            val newTask = Task(
                id = UUID.randomUUID().toString(),
                contextId = currentContextId,
                title = title,
                status = TaskStatus.BACKLOG,
                createdAt = now,
                updatedAt = now,
                deviceId = "local_device" // Will be replaced by real sync logic later
            )
            taskRepository.insertTask(newTask)
        }
    }

    fun moveTask(taskId: String, newStatus: TaskStatus) {
        viewModelScope.launch {
            taskRepository.updateTaskStatus(taskId, newStatus)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }
}
