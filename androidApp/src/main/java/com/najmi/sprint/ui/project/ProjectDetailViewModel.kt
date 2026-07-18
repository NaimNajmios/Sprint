package com.najmi.sprint.ui.project

import android.net.Uri
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.najmi.sprint.core.data.local.dao.GithubCacheDao
import com.najmi.sprint.core.data.local.entity.GithubIssueCacheEntity
import com.najmi.sprint.core.domain.model.Project
import com.najmi.sprint.core.domain.model.ProjectDocument
import com.najmi.sprint.core.domain.model.Secret
import com.najmi.sprint.core.domain.repository.ProjectDocumentRepository
import com.najmi.sprint.core.domain.repository.ProjectRepository
import com.najmi.sprint.core.security.SecretRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.najmi.sprint.tracking.GithubSyncWorker

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectDocumentRepository: ProjectDocumentRepository,
    private val secretRepository: SecretRepository,
    private val githubCacheDao: GithubCacheDao,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val projectId: String = savedStateHandle.get<String>("projectId") ?: ""

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    val githubIssues: StateFlow<List<GithubIssueCacheEntity>> = githubCacheDao.observeIssuesForProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectDocuments: StateFlow<List<ProjectDocument>> = projectDocumentRepository.observeDocumentsForProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val githubPatExists: StateFlow<Boolean> = secretRepository.observeSecretsByProject(projectId)
        .map { secrets -> secrets.any { it.label == "GITHUB_PAT" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            _project.value = projectRepository.getProjectById(projectId)
        }
    }

    fun saveGithubConfig(owner: String, repo: String, pat: String?) {
        val currentProject = _project.value ?: return
        viewModelScope.launch {
            val updatedProject = currentProject.copy(
                githubOwner = owner.takeIf { it.isNotBlank() },
                githubRepo = repo.takeIf { it.isNotBlank() }
            )
            projectRepository.updateProject(updatedProject)
            _project.value = updatedProject

            if (!pat.isNullOrBlank()) {
                val secretId = UUID.randomUUID().toString()
                secretRepository.insertSecret(
                    secretId = secretId,
                    projectId = projectId,
                    label = "GITHUB_PAT",
                    plaintextValue = pat
                )
            }

            // Trigger sync immediately
            val syncRequest = OneTimeWorkRequestBuilder<GithubSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(syncRequest)
        }
    }

    fun addDocument(uri: Uri, title: String) {
        viewModelScope.launch {
            val doc = ProjectDocument(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                uri = uri.toString(),
                title = title
            )
            projectDocumentRepository.insertDocument(doc)
        }
    }

    fun removeDocument(id: String) {
        viewModelScope.launch {
            projectDocumentRepository.deleteDocument(id)
        }
    }
}
