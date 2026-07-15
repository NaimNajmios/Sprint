package com.najmi.sprint.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.najmi.sprint.core.domain.model.ClassificationRule
import kotlinx.datetime.Instant

@Entity(tableName = "classification_rules")
data class ClassificationRuleEntity(
    @PrimaryKey val packageName: String,
    val contextId: String,
    val lastConfirmedAt: Instant
)

fun ClassificationRuleEntity.toDomain() = ClassificationRule(
    packageName = packageName,
    contextId = contextId,
    lastConfirmedAt = lastConfirmedAt
)

fun ClassificationRule.toEntity() = ClassificationRuleEntity(
    packageName = packageName,
    contextId = contextId,
    lastConfirmedAt = lastConfirmedAt
)
