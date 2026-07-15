package com.najmi.sprint.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Optional shallow sub-grouping within a [Context].
 *
 * Provides "which side project" granularity (e.g. Sprint, Vulgaris, Corvus)
 * without making the classifier's job harder — classifier only ever outputs
 * a Context, never a Project.
 */
@Serializable
data class Project(
    val id: String,
    val contextId: String,
    val name: String,
    val colorHex: String? = null  // inherits context color if null
)
