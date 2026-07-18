package com.najmi.sprint.core.sync.model

import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.RetroEntry
import com.najmi.sprint.core.domain.model.Session
import com.najmi.sprint.core.domain.model.SessionSource
import com.najmi.sprint.core.domain.model.Task
import com.najmi.sprint.core.domain.model.TaskStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContextDto(
    val id: String,
    val name: String,
    @SerialName("color_hex") val colorHex: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("updated_at") val updatedAt: Instant? = null
)

@Serializable
data class TaskDto(
    val id: String,
    @SerialName("context_id") val contextId: String,
    @SerialName("project_id") val projectId: String?,
    val title: String,
    val status: String,
    @SerialName("estimate_points") val estimatePoints: Int?,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
    @SerialName("device_id") val deviceId: String
)

@Serializable
data class SessionDto(
    val id: String,
    @SerialName("device_id") val deviceId: String,
    val source: String,
    @SerialName("raw_label") val rawLabel: String,
    @SerialName("start_time") val startTime: Instant,
    @SerialName("end_time") val endTime: Instant?,
    @SerialName("context_id") val contextId: String?,
    @SerialName("project_id") val projectId: String?,
    @SerialName("classification_confidence") val classificationConfidence: Float?,
    @SerialName("is_manually_corrected") val isManuallyCorrected: Boolean,
    @SerialName("updated_at") val updatedAt: Instant? = null
)

@Serializable
data class RetroEntryDto(
    val id: String,
    @SerialName("week_of") val weekOf: LocalDate,
    @SerialName("summary_text") val summaryText: String,
    @SerialName("flagged_context_id") val flaggedContextId: String?,
    @SerialName("generated_by_model") val generatedByModel: String,
    @SerialName("prompt_version") val promptVersion: String,
    @SerialName("critic_approved") val criticApproved: Boolean,
    @SerialName("updated_at") val updatedAt: Instant? = null
)

// Extension functions for mapping DTOs to Domain models and vice-versa

private fun isValidUuid(uuid: String?): Boolean {
    if (uuid == null) return false
    return uuid.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
}

fun Context.toDto() = ContextDto(id = id, name = name, colorHex = colorHex, isActive = isActive)
fun ContextDto.toDomain() = Context(id = id, name = name, colorHex = colorHex, isActive = isActive)

fun Task.toDto() = TaskDto(
    id = id,
    contextId = contextId,
    projectId = projectId,
    title = title,
    status = status.name,
    estimatePoints = estimatePoints,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deviceId = deviceId
)
fun TaskDto.toDomain() = Task(
    id = id,
    contextId = contextId,
    projectId = projectId,
    title = title,
    status = TaskStatus.valueOf(status),
    estimatePoints = estimatePoints,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deviceId = deviceId
)

fun Session.toDto() = SessionDto(
    id = id,
    deviceId = deviceId,
    source = source.name,
    rawLabel = rawLabel,
    startTime = startTime,
    endTime = endTime,
    contextId = if (isValidUuid(contextId)) contextId else null,
    projectId = if (isValidUuid(projectId)) projectId else null,
    classificationConfidence = classificationConfidence,
    isManuallyCorrected = isManuallyCorrected
)
fun SessionDto.toDomain() = Session(
    id = id,
    deviceId = deviceId,
    source = SessionSource.valueOf(source),
    rawLabel = rawLabel,
    startTime = startTime,
    endTime = endTime,
    contextId = contextId ?: "UNCLASSIFIED",
    projectId = projectId,
    classificationConfidence = classificationConfidence,
    isManuallyCorrected = isManuallyCorrected
)

fun RetroEntry.toDto() = RetroEntryDto(
    id = id,
    weekOf = weekOf,
    summaryText = summaryText,
    flaggedContextId = if (isValidUuid(flaggedContextId)) flaggedContextId else null,
    generatedByModel = generatedByModel,
    promptVersion = promptVersion,
    criticApproved = criticApproved
)
fun RetroEntryDto.toDomain() = RetroEntry(
    id = id,
    weekOf = weekOf,
    summaryText = summaryText,
    flaggedContextId = flaggedContextId,
    generatedByModel = generatedByModel,
    promptVersion = promptVersion,
    criticApproved = criticApproved
)
