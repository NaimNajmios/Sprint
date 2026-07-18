package com.najmi.sprint.core.domain.repository

import com.najmi.sprint.core.domain.model.ProjectDocument
import kotlinx.coroutines.flow.Flow

interface ProjectDocumentRepository {
    fun observeDocumentsForProject(projectId: String): Flow<List<ProjectDocument>>
    suspend fun insertDocument(document: ProjectDocument)
    suspend fun updateDocument(document: ProjectDocument)
    suspend fun deleteDocument(id: String)
}
