package com.najmi.sprint.core.data.repository

import com.najmi.sprint.core.data.local.dao.ContextDao
import com.najmi.sprint.core.data.local.dao.ProjectDao
import com.najmi.sprint.core.data.local.dao.RetroDao
import com.najmi.sprint.core.data.local.dao.SessionDao
import com.najmi.sprint.core.data.local.dao.TaskDao
import com.najmi.sprint.core.data.local.entity.toDomain
import com.najmi.sprint.core.data.local.entity.toEntity
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.Project
import com.najmi.sprint.core.domain.model.ProjectDocument
import com.najmi.sprint.core.domain.model.RetroEntry
import com.najmi.sprint.core.domain.model.Session
import com.najmi.sprint.core.domain.model.Task
import com.najmi.sprint.core.domain.model.TaskStatus
import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.ProjectRepository
import com.najmi.sprint.core.domain.repository.RetroRepository
import com.najmi.sprint.core.domain.repository.SessionRepository
import com.najmi.sprint.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import javax.inject.Inject

class RoomContextRepository @Inject constructor(
    private val dao: ContextDao
) : ContextRepository {
    override fun observeActiveContexts(): Flow<List<Context>> = 
        dao.observeActiveContexts().map { list -> list.map { it.toDomain() } }

    override fun observeAllContexts(): Flow<List<Context>> = 
        dao.observeAllContexts().map { list -> list.map { it.toDomain() } }

    override suspend fun getContextById(id: String): Context? = 
        dao.getContextById(id)?.toDomain()

    override suspend fun insertContext(context: Context) {
        dao.insertContext(context.toEntity())
    }

    override suspend fun updateContext(context: Context) {
        dao.updateContext(context.toEntity())
    }

    override suspend fun softDeleteContext(id: String) {
        dao.softDeleteContext(id)
    }

    override suspend fun getActiveContextCount(): Int = 
        dao.getActiveContextCount()
}

class RoomProjectRepository @Inject constructor(
    private val dao: ProjectDao
) : ProjectRepository {
    override fun observeAllProjects(): Flow<List<Project>> =
        dao.observeAllProjects().map { list -> list.map { it.toDomain() } }

    override fun observeProjectsByContext(contextId: String): Flow<List<Project>> =
        dao.observeProjectsByContext(contextId).map { list -> list.map { it.toDomain() } }

    override suspend fun getProjectById(id: String): Project? =
        dao.getProjectById(id)?.toDomain()

    override suspend fun insertProject(project: Project) {
        dao.insertProject(project.toEntity())
    }

    override suspend fun updateProject(project: Project) {
        dao.updateProject(project.toEntity())
    }

    override suspend fun deleteProject(id: String) {
        dao.deleteProject(id)
    }
}

class RoomSessionRepository @Inject constructor(
    private val dao: SessionDao
) : SessionRepository {
    override fun observeSessionsForDate(date: LocalDate): Flow<List<Session>> {
        val startOfDay = date.atStartOfDayIn(TimeZone.currentSystemDefault())
        val endOfDay = date.plus(kotlinx.datetime.DatePeriod(days = 1)).atStartOfDayIn(TimeZone.currentSystemDefault())
        return dao.observeSessionsForDate(startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }
    }

    override fun observeUnclassifiedSessions(): Flow<List<Session>> =
        dao.observeUnclassifiedSessions().map { list -> list.map { it.toDomain() } }

    override fun observeRecentSessions(limit: Int): Flow<List<Session>> =
        dao.observeRecentSessions(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun getSessionById(id: String): Session? =
        dao.getSessionById(id)?.toDomain()

    override suspend fun insertSession(session: Session) {
        dao.insertSession(session.toEntity())
    }

    override suspend fun updateSession(session: Session) {
        dao.updateSession(session.toEntity())
    }

    override suspend fun closeActiveSession(endTime: Instant) {
        dao.closeActiveSession(endTime)
    }

    override suspend fun getSessionsBetween(start: Instant, end: Instant): List<Session> =
        dao.getSessionsBetween(start, end).map { it.toDomain() }

    override suspend fun deleteSession(id: String) {
        dao.deleteSession(id)
    }

    override suspend fun getLastClosedSession(): Session? =
        dao.getLastClosedSession()?.toDomain()
}

class RoomTaskRepository @Inject constructor(
    private val dao: TaskDao
) : TaskRepository {
    override fun observeAllTasks(): Flow<List<Task>> =
        dao.observeAllTasks().map { list -> list.map { it.toDomain() } }

    override fun observeTasksByContext(contextId: String): Flow<List<Task>> =
        dao.observeTasksByContext(contextId).map { list -> list.map { it.toDomain() } }

    override fun observeTasksByStatus(status: TaskStatus): Flow<List<Task>> =
        dao.observeTasksByStatus(status).map { list -> list.map { it.toDomain() } }

    override suspend fun getTaskById(id: String): Task? =
        dao.getTaskById(id)?.toDomain()

    override suspend fun insertTask(task: Task) {
        dao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        dao.updateTask(task.toEntity())
    }

    override suspend fun updateTaskStatus(id: String, status: TaskStatus) {
        dao.updateTaskStatus(id, status)
    }

    override suspend fun deleteTask(id: String) {
        dao.deleteTask(id)
    }
}

class RoomRetroRepository @Inject constructor(
    private val dao: RetroDao
) : RetroRepository {
    override fun observeRetros(): Flow<List<RetroEntry>> =
        dao.observeRetros().map { list -> list.map { it.toDomain() } }

    override suspend fun getRetroForWeek(weekOf: LocalDate): RetroEntry? =
        dao.getRetroForWeek(weekOf)?.toDomain()

    override suspend fun insertRetro(retro: RetroEntry) {
        dao.insertRetro(retro.toEntity())
    }

    override suspend fun getLatestRetro(): RetroEntry? =
        dao.getLatestRetro()?.toDomain()
}

class RoomRuleRepository @Inject constructor(
    private val dao: com.najmi.sprint.core.data.local.dao.RuleDao
) : com.najmi.sprint.core.domain.repository.RuleRepository {
    override suspend fun getRuleForPackage(packageName: String): com.najmi.sprint.core.domain.model.ClassificationRule? =
        dao.getRuleForPackage(packageName)?.toDomain()

    override suspend fun insertOrUpdateRule(rule: com.najmi.sprint.core.domain.model.ClassificationRule) {
        dao.insertRule(rule.toEntity())
    }

    override suspend fun getAllRules(): List<com.najmi.sprint.core.domain.model.ClassificationRule> =
        dao.getAllRules().map { it.toDomain() }

    override suspend fun deleteRule(packageName: String) {
        dao.deleteRule(packageName)
    }

    override fun observeIgnoredRules(): kotlinx.coroutines.flow.Flow<List<com.najmi.sprint.core.domain.model.ClassificationRule>> =
        dao.observeIgnoredRules().map { list -> list.map { it.toDomain() } }

    override suspend fun setPackageIgnored(packageName: String, isIgnored: Boolean) {
        val existing = dao.getRuleForPackage(packageName)
        if (existing != null) {
            dao.setRuleIgnored(packageName, isIgnored)
        } else {
            dao.insertRule(com.najmi.sprint.core.data.local.entity.ClassificationRuleEntity(
                packageName = packageName,
                contextId = "UNCLASSIFIED",
                lastConfirmedAt = kotlinx.datetime.Clock.System.now(),
                isIgnored = isIgnored
            ))
        }
    }
}

class RoomProjectDocumentRepository @Inject constructor(
    private val dao: com.najmi.sprint.core.data.local.dao.ProjectDocumentDao
) : com.najmi.sprint.core.domain.repository.ProjectDocumentRepository {
    override fun observeDocumentsForProject(projectId: String): Flow<List<ProjectDocument>> =
        dao.observeDocumentsForProject(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun insertDocument(document: ProjectDocument) {
        dao.insertDocument(document.toEntity())
    }

    override suspend fun updateDocument(document: ProjectDocument) {
        dao.updateDocument(document.toEntity())
    }

    override suspend fun deleteDocument(id: String) {
        dao.deleteDocument(id)
    }
}
