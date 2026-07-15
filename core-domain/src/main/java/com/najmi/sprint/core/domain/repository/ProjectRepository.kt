package com.najmi.sprint.core.domain.repository

import com.najmi.sprint.core.domain.model.Project
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for [Project] operations.
 */
interface ProjectRepository {
    fun observeProjectsByContext(contextId: String): Flow<List<Project>>
    suspend fun getProjectById(id: String): Project?
    suspend fun insertProject(project: Project)
    suspend fun updateProject(project: Project)
    suspend fun deleteProject(id: String)
}
