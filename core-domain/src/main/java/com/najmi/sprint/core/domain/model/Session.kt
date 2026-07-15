package com.najmi.sprint.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * The source that generated a tracking session.
 *
 * Set from Phase 1 even though only [APP_USAGE] is used until Phase 12.
 */
@Serializable
enum class SessionSource {
    APP_USAGE,
    WINDOW_USAGE,
    LOCATION,
    ACTIVITY,
    MANUAL
}

/**
 * A tracked time session representing usage of a foreground app, window, or activity.
 *
 * [rawLabel] is the most sensitive field (literal app/window names).
 * Plan: null it out once classified and older than N days — reduces exposure.
 *
 * Classification flow:
 * 1. Rule-based pre-filter checks [rawLabel] against local lookup table
 * 2. Misses escalate to LLM actor-critic classification
 * 3. Confidence ≥ 0.85 auto-commits, 0.5–0.85 queues for review, < 0.5 always queues
 */
@Serializable
data class Session(
    val id: String,
    val deviceId: String,
    val source: SessionSource = SessionSource.APP_USAGE,
    val rawLabel: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val contextId: String? = null,
    val projectId: String? = null,
    val classificationConfidence: Float? = null,
    val isManuallyCorrected: Boolean = false
)
