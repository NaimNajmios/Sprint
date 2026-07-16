package com.najmi.sprint.ui.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.Project
import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProjectManagerViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val contextRepository: ContextRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val contextId: String = savedStateHandle.get<String>("contextId") ?: ""

    private val _parentContext = MutableStateFlow<Context?>(null)
    val parentContext: StateFlow<Context?> = _parentContext.asStateFlow()

    val projects: StateFlow<List<Project>> = projectRepository.observeProjectsByContext(contextId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            _parentContext.value = contextRepository.getContextById(contextId)
        }
    }

    fun addProject(name: String, colorHex: String?) {
        viewModelScope.launch {
            val newProject = Project(
                id = UUID.randomUUID().toString(),
                contextId = contextId,
                name = name,
                colorHex = colorHex
            )
            projectRepository.insertProject(newProject)
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            projectRepository.updateProject(project)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
        }
    }
}
