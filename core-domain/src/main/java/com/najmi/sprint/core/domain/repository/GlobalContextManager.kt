package com.najmi.sprint.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the globally selected Context filter for the UI.
 * Allows the user to select "Work" or "Life" globally, and all tabs (Dashboard, Kanban)
 * will reactively filter their datasets to match.
 */
interface GlobalContextManager {
    /**
     * The ID of the currently selected Context.
     * Null represents "Global" (no filter).
     */
    val selectedContextId: StateFlow<String?>

    /**
     * Update the globally selected context.
     */
    fun selectContext(contextId: String?)
}
