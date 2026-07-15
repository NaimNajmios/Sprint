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
    val colorHex: String?
)

fun ProjectEntity.toDomain() = Project(id, contextId, name, colorHex)
fun Project.toEntity() = ProjectEntity(id, contextId, name, colorHex)

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
    val deviceId: String
)

fun TaskEntity.toDomain() = Task(
    id, contextId, projectId, title, status, estimatePoints, createdAt, updatedAt, deviceId
)
fun Task.toEntity() = TaskEntity(
    id, contextId, projectId, title, status, estimatePoints, createdAt, updatedAt, deviceId
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
