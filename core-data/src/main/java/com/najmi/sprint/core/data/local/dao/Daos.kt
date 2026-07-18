package com.najmi.sprint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.najmi.sprint.core.data.local.entity.ContextEntity
import com.najmi.sprint.core.data.local.entity.ProjectEntity
import com.najmi.sprint.core.data.local.entity.RetroEntryEntity
import com.najmi.sprint.core.data.local.entity.ProjectDocumentEntity
import com.najmi.sprint.core.data.local.entity.SessionEntity
import com.najmi.sprint.core.data.local.entity.TaskEntity
import com.najmi.sprint.core.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Dao
interface ContextDao {
    @Query("SELECT * FROM contexts WHERE isActive = 1")
    fun observeActiveContexts(): Flow<List<ContextEntity>>

    @Query("SELECT * FROM contexts")
    fun observeAllContexts(): Flow<List<ContextEntity>>

    @Query("SELECT * FROM contexts WHERE id = :id")
    suspend fun getContextById(id: String): ContextEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContext(context: ContextEntity)

    @Update
    suspend fun updateContext(context: ContextEntity)

    @Query("UPDATE contexts SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteContext(id: String)

    @Query("SELECT COUNT(*) FROM contexts WHERE isActive = 1")
    suspend fun getActiveContextCount(): Int
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE contextId = :contextId")
    fun observeProjectsByContext(contextId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)
}

@Dao
interface ProjectDocumentDao {
    @Query("SELECT * FROM project_documents ORDER BY lastOpenedAt DESC")
    fun observeAllDocuments(): Flow<List<ProjectDocumentEntity>>

    @Query("SELECT * FROM project_documents WHERE projectId = :projectId ORDER BY lastOpenedAt DESC")
    fun observeDocumentsForProject(projectId: String): Flow<List<ProjectDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: ProjectDocumentEntity)

    @Update
    suspend fun updateDocument(document: ProjectDocumentEntity)

    @Query("DELETE FROM project_documents WHERE id = :id")
    suspend fun deleteDocument(id: String)
}

@Dao
interface SessionDao {
    // Note: Room can't natively query kotlinx.datetime.Instant date components easily without SQLite date functions,
    // so we'll do an exact start/end range query based on the LocalDate's min/max instants in the repository.
    @Query("SELECT * FROM sessions WHERE startTime >= :startOfDay AND startTime < :endOfDay")
    fun observeSessionsForDate(startOfDay: Instant, endOfDay: Instant): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE contextId IS NULL")
    fun observeUnclassifiedSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("UPDATE sessions SET endTime = :endTime WHERE endTime IS NULL")
    suspend fun closeActiveSession(endTime: Instant)

    @Query("SELECT * FROM sessions WHERE startTime >= :start AND startTime <= :end")
    suspend fun getSessionsBetween(start: Instant, end: Instant): List<SessionEntity>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("SELECT * FROM sessions WHERE endTime IS NOT NULL ORDER BY endTime DESC LIMIT 1")
    suspend fun getLastClosedSession(): SessionEntity?
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE contextId = :contextId")
    fun observeTasksByContext(contextId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status")
    fun observeTasksByStatus(status: TaskStatus): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: TaskStatus)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: String)
}

@Dao
interface RetroDao {
    @Query("SELECT * FROM retros ORDER BY weekOf DESC")
    fun observeRetros(): Flow<List<RetroEntryEntity>>

    @Query("SELECT * FROM retros WHERE weekOf = :weekOf")
    suspend fun getRetroForWeek(weekOf: LocalDate): RetroEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRetro(retro: RetroEntryEntity)

    @Query("SELECT * FROM retros ORDER BY weekOf DESC LIMIT 1")
    suspend fun getLatestRetro(): RetroEntryEntity?
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM classification_rules WHERE packageName = :packageName")
    suspend fun getRuleForPackage(packageName: String): com.najmi.sprint.core.data.local.entity.ClassificationRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: com.najmi.sprint.core.data.local.entity.ClassificationRuleEntity)

    @Query("SELECT * FROM classification_rules")
    suspend fun getAllRules(): List<com.najmi.sprint.core.data.local.entity.ClassificationRuleEntity>

    @Query("DELETE FROM classification_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("SELECT * FROM classification_rules WHERE isIgnored = 1")
    fun observeIgnoredRules(): Flow<List<com.najmi.sprint.core.data.local.entity.ClassificationRuleEntity>>

    @Query("UPDATE classification_rules SET isIgnored = :isIgnored WHERE packageName = :packageName")
    suspend fun setRuleIgnored(packageName: String, isIgnored: Boolean)
}
