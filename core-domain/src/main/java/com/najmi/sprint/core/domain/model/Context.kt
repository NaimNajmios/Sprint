package com.najmi.sprint.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a high-level classification bucket for tracked time.
 *
 * Contexts are dynamic — seeded at onboarding (e.g. "Internship", "Coursework",
 * "Side Projects", "Life") but editable/expandable anytime from Settings.
 *
 * Soft-delete only: setting [isActive] to false hides the context from pickers/UI
 * but never removes the row or reassigns historical references.
 */
@Serializable
data class Context(
    val id: String,
    val name: String,
    val colorHex: String,
    val isActive: Boolean = true
)
