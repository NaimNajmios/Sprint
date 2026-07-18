package com.najmi.sprint.core.domain.model

import kotlinx.datetime.Instant

data class ClassificationRule(
    val packageName: String,
    val contextId: String,
    val lastConfirmedAt: Instant,
    val isIgnored: Boolean = false
)
