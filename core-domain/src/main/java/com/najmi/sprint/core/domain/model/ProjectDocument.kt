package com.najmi.sprint.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * A reference link to an external document (e.g., Obsidian Markdown file) 
 * associated with a Project. Sprint only links to these; it does not render them.
 */
@Serializable
data class ProjectDocument(
    val id: String,
    val projectId: String,
    val uri: String,        // SAF URI string
    val title: String,
    val lastOpenedAt: Instant? = null
)
