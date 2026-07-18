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

import com.najmi.sprint.core.domain.model.ProjectDocument
import com.najmi.sprint.core.domain.repository.ProjectRepository
import com.najmi.sprint.core.security.SecretRepository
import com.najmi.sprint.core.sync.client.GithubClient
import com.najmi.sprint.core.domain.repository.ProjectDocumentRepository

data class KanbanState(
    val isLoading: Boolean = true,
    val tasksByStatus: Map<TaskStatus, List<Task>> = emptyMap(),
    val contexts: List<Context> = emptyList(),
    val selectedContextId: String? = null,
    val activeProjectDocs: List<ProjectDocument> = emptyList()
)

@HiltViewModel
class KanbanViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val contextRepository: ContextRepository,
    private val globalContextManager: GlobalContextManager,
    private val projectRepository: ProjectRepository,
    private val secretRepository: SecretRepository,
    private val githubClient: GithubClient,
    private val projectDocumentRepository: ProjectDocumentRepository
) : ViewModel() {

    val state: StateFlow<KanbanState> = combine(
        taskRepository.observeAllTasks(),
        contextRepository.observeActiveContexts(),
        globalContextManager.selectedContextId,
        projectDocumentRepository.observeAllDocuments()
    ) { allTasks, contexts, selectedContextId, allDocs ->
        
        val filteredTasks = if (selectedContextId != null) {
            allTasks.filter { it.contextId == selectedContextId }
        } else {
            allTasks
        }

        val groupedTasks = TaskStatus.entries.associateWith { status ->
            filteredTasks.filter { it.status == status }.sortedByDescending { it.updatedAt }
        }
        
        // Find all active projects from IN_PROGRESS tasks
        val activeProjectIds = groupedTasks[TaskStatus.IN_PROGRESS]?.mapNotNull { it.projectId }?.distinct() ?: emptyList()
        val activeProjectDocs = allDocs.filter { it.projectId in activeProjectIds }

        KanbanState(
            isLoading = false,
            tasksByStatus = groupedTasks,
            contexts = contexts,
            selectedContextId = selectedContextId,
            activeProjectDocs = activeProjectDocs
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
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            taskRepository.updateTaskStatus(taskId, newStatus)
            
            // Auto-close on Github if moving to DONE and it's a Github issue
            val taskProjectId = task.projectId
            val taskGithubIssueNumber = task.githubIssueNumber
            
            if (newStatus == TaskStatus.DONE && taskGithubIssueNumber != null && taskProjectId != null) {
                try {
                    val project = projectRepository.getProjectById(taskProjectId)
                    val projectGithubOwner = project?.githubOwner
                    val projectGithubRepo = project?.githubRepo
                    
                    if (projectGithubOwner != null && projectGithubRepo != null) {
                        val patSecret = secretRepository.observeSecretsByProject(project.id)
                            .firstOrNull()?.find { it.label == "GITHUB_PAT" }
                        if (patSecret != null) {
                            val pat = secretRepository.revealSecret(patSecret.id)
                            if (pat != null) {
                                githubClient.closeIssue(
                                    owner = projectGithubOwner,
                                    repo = projectGithubRepo,
                                    pat = pat,
                                    issueNumber = taskGithubIssueNumber
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }
}
