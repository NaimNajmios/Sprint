package com.najmi.sprint.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.Project
import com.najmi.sprint.core.domain.model.RetroEntry
import com.najmi.sprint.core.domain.model.Session
import com.najmi.sprint.core.domain.model.SessionSource
import com.najmi.sprint.core.domain.model.Task
import com.najmi.sprint.core.domain.model.TaskStatus
import com.najmi.sprint.core.domain.model.ProjectDocument
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "contexts")
data class ContextEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val isActive: Boolean
)

fun ContextEntity.toDomain() = Context(id, name, colorHex, isActive)
fun Context.toEntity() = ContextEntity(id, name, colorHex, isActive)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val contextId: String,
    val name: String,
    val colorHex: String?,
    val githubOwner: String?,
    val githubRepo: String?
)

fun ProjectEntity.toDomain() = Project(id, contextId, name, colorHex, githubOwner, githubRepo)
fun Project.toEntity() = ProjectEntity(id, contextId, name, colorHex, githubOwner, githubRepo)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val source: SessionSource,
    val rawLabel: String,
    val startTime: Instant,
    val endTime: Instant?,
    val contextId: String?,
    val projectId: String?,
    val classificationConfidence: Float?,
    val isManuallyCorrected: Boolean
)

fun SessionEntity.toDomain() = Session(
    id, deviceId, source, rawLabel, startTime, endTime, contextId, projectId, classificationConfidence, isManuallyCorrected
)
fun Session.toEntity() = SessionEntity(
    id, deviceId, source, rawLabel, startTime, endTime, contextId, projectId, classificationConfidence, isManuallyCorrected
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val contextId: String,
    val projectId: String?,
    val title: String,
    val status: TaskStatus,
    val estimatePoints: Int?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deviceId: String,
    val githubIssueNumber: Int?,
    val githubIssueUrl: String?
)

fun TaskEntity.toDomain() = Task(
    id, contextId, projectId, title, status, estimatePoints, createdAt, updatedAt, deviceId, githubIssueNumber, githubIssueUrl
)
fun Task.toEntity() = TaskEntity(
    id, contextId, projectId, title, status, estimatePoints, createdAt, updatedAt, deviceId, githubIssueNumber, githubIssueUrl
)

@Entity(tableName = "retros")
data class RetroEntryEntity(
    @PrimaryKey val id: String,
    val weekOf: LocalDate,
    val summaryText: String,
    val flaggedContextId: String?,
    val generatedByModel: String,
    val promptVersion: String,
    val criticApproved: Boolean
)

fun RetroEntryEntity.toDomain() = RetroEntry(
    id, weekOf, summaryText, flaggedContextId, generatedByModel, promptVersion, criticApproved
)
fun RetroEntry.toEntity() = RetroEntryEntity(
    id, weekOf, summaryText, flaggedContextId, generatedByModel, promptVersion, criticApproved
)

@Entity(tableName = "github_issues_cache")
data class GithubIssueCacheEntity(
    @PrimaryKey val id: String, // project_id + issue_number
    val projectId: String,
    val issueNumber: Int,
    val title: String,
    val state: String,
    val htmlUrl: String
)

@Entity(tableName = "github_commits_cache")
data class GithubCommitCacheEntity(
    @PrimaryKey val sha: String,
    val projectId: String,
    val message: String,
    val htmlUrl: String
)

@Entity(tableName = "project_documents")
data class ProjectDocumentEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val uri: String,
    val title: String,
    val lastOpenedAt: Instant?
)

fun ProjectDocumentEntity.toDomain() = ProjectDocument(id, projectId, uri, title, lastOpenedAt)
fun ProjectDocument.toEntity() = ProjectDocumentEntity(id, projectId, uri, title, lastOpenedAt)
