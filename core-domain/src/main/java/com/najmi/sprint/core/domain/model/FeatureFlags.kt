package com.najmi.sprint.core.domain.model

/**
 * Local feature-flag system for gating incomplete code paths.
 *
 * Lets you ship the Android MVP without half-built location code interfering.
 * Check flags at runtime to enable/disable features without removing code.
 *
 * Usage:
 * ```kotlin
 * if (FeatureFlags.PHYSICAL_TRACKING) {
 *     // Phase 12 geofence/activity recognition code
 * }
 * ```
 */
object FeatureFlags {

    /** Phase 2: Automatic foreground app tracking via UsageStatsManager */
    const val APP_TRACKING: Boolean = true

    /** Phase 3: AI-powered session classification (actor-critic) */
    const val AI_CLASSIFICATION: Boolean = false

    /** Phase 4: Kanban board for task management */
    const val KANBAN_BOARD: Boolean = false

    /** Phase 5: Weekly AI retrospective generation */
    const val WEEKLY_RETRO: Boolean = false

    /** Phase 6: Home-screen widget (Jetpack Glance) */
    const val WIDGET: Boolean = false

    /** Phase 8: Multi-device sync */
    const val SYNC: Boolean = false

    /** Phase 9: Client-side encryption of session payloads */
    const val ENCRYPTION: Boolean = false

    /** Phase 10: Windows desktop tracking via KMP */
    const val DESKTOP_TRACKING: Boolean = false

    /** Phase 12: Physical signal tracking (geofence + activity recognition) */
    const val PHYSICAL_TRACKING: Boolean = false

    /** Phase 6a: Low-touch / vacation mode */
    const val VACATION_MODE: Boolean = false
}
