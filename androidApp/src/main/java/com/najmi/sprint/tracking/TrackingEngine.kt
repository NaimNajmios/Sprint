package com.najmi.sprint.tracking

import com.najmi.sprint.core.domain.model.Session
import com.najmi.sprint.core.domain.model.SessionSource
import com.najmi.sprint.core.domain.repository.RuleRepository
import com.najmi.sprint.core.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import androidx.glance.appwidget.updateAll
import com.najmi.sprint.widget.SprintWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import com.najmi.sprint.core.domain.logger.AppLogger

@Singleton
class TrackingEngine @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val ruleRepository: RuleRepository,
    private val usageStatsTracker: UsageStatsTracker,
    @ApplicationContext private val context: Context
) {
    companion object {
        /**
         * System and launcher packages that should never appear as tracked sessions.
         * When the OS briefly brings these to the foreground (e.g., notification shade,
         * keyboard, installer overlay), we skip them entirely so they don't break
         * session continuity or pollute the merge chain.
         */
        val IGNORED_PACKAGES: Set<String> = setOf(
            // Android system UI / overlays
            "com.android.systemui",
            "com.android.launcher3",
            "com.android.inputmethod.latin",
            "com.android.packageinstaller",
            // Google launchers & input
            "com.google.android.apps.nexuslauncher",
            "com.google.android.inputmethod.latin",
            "com.google.android.permissioncontroller",
            // Samsung
            "com.sec.android.app.launcher",
            "com.samsung.android.honeyboard",
            // Xiaomi
            "com.miui.home",
            // Huawei
            "com.huawei.android.launcher",
            // Oppo / Realme
            "com.oppo.launcher",
        )

        /** Sessions shorter than this are considered transient and dropped */
        const val MIN_SESSION_DURATION_MS = 30_000L

        /** Sessions of the same app separated by less than this are merged */
        const val MERGE_GAP_THRESHOLD_MS = 120_000L
    }

    private var trackingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val deviceId = "local-device" // Should be fetched from DataStore in Phase 8

    private var activeSessionId: String? = null
    private var currentPackage: String? = null

    /** Adaptive polling intervals in milliseconds */
    private val activePollingInterval = 10_000L
    private val idlePollingInterval = 60_000L
    private var currentInterval = activePollingInterval

    fun startTracking() {
        if (trackingJob?.isActive == true) return
        
        trackingJob = scope.launch {
            while (isActive) {
                pollAndUpdate()
                delay(currentInterval)
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        
        // Close out the final session
        scope.launch {
            closeActiveSession(Instant.fromEpochMilliseconds(System.currentTimeMillis()))
        }
    }

    internal suspend fun pollAndUpdate() {
        val event = usageStatsTracker.pollRecentForegroundApp()
        
        if (event == null) {
            // No app switches detected, relax the polling interval to save battery
            currentInterval = idlePollingInterval
            return
        }

        // We detected a switch, tighten the interval back up
        currentInterval = activePollingInterval

        val newPackage = event.packageName
        val switchTime = Instant.fromEpochMilliseconds(event.timestamp)

        // Ignore system packages — treat as if the blip never happened
        if (newPackage in IGNORED_PACKAGES) return

        // Dynamically configured ignore list
        val rule = ruleRepository.getRuleForPackage(newPackage)
        if (rule?.isIgnored == true) return

        // Ignore if it's the exact same app we are already tracking
        if (newPackage == currentPackage) return

        // Phase 3b: Debounce & Merge
        val previousSessionId = activeSessionId
        closeActiveSession(switchTime)

        var sessionToReopen: Session? = null

        if (previousSessionId != null) {
            val previousSession = sessionRepository.getSessionById(previousSessionId)
            if (previousSession != null && previousSession.endTime != null) {
                val durationMs = previousSession.endTime!!.toEpochMilliseconds() - previousSession.startTime.toEpochMilliseconds()
                
                // Debounce: If the session was shorter than the minimum, it's a transient switch (e.g., notification glance). Drop it.
                if (durationMs < MIN_SESSION_DURATION_MS) {
                    sessionRepository.deleteSession(previousSession.id)
                }
            }
        }

        // Merge: Check if we are switching back to the same app we were using recently
        val lastClosed = sessionRepository.getLastClosedSession()
        if (lastClosed != null && lastClosed.rawLabel == newPackage && lastClosed.endTime != null) {
            val gapMs = switchTime.toEpochMilliseconds() - lastClosed.endTime!!.toEpochMilliseconds()
            // If the gap is within the merge threshold, merge them by reopening the last closed session
            if (gapMs < MERGE_GAP_THRESHOLD_MS) {
                sessionToReopen = lastClosed
            }
        }

        if (sessionToReopen != null) {
            // Reopen the existing session rather than creating a new one
            AppLogger.d("TrackingEngine", "Reopening session ${sessionToReopen.id} for $newPackage")
            sessionRepository.updateSession(sessionToReopen.copy(endTime = null))
            activeSessionId = sessionToReopen.id
        } else {
            // Create a brand new session
            val newSessionId = UUID.randomUUID().toString()
            AppLogger.d("TrackingEngine", "Creating new session $newSessionId for $newPackage")
            val newSession = Session(
                id = newSessionId,
                deviceId = deviceId,
                source = SessionSource.APP_USAGE,
                rawLabel = newPackage,
                startTime = switchTime
            )
            sessionRepository.insertSession(newSession)
            activeSessionId = newSessionId
        }
        
        currentPackage = newPackage
    }

    internal suspend fun closeActiveSession(endTime: Instant) {
        activeSessionId?.let { id ->
            val session = sessionRepository.getSessionById(id)
            if (session != null && session.endTime == null) {
                // Phase 3c: Rule-Based Pre-filter
                val rule = ruleRepository.getRuleForPackage(session.rawLabel)
                val resolvedContextId = rule?.contextId
                
                AppLogger.d("TrackingEngine", "Closing session ${session.id} for ${session.rawLabel} at $endTime")
                sessionRepository.updateSession(session.copy(
                    endTime = endTime,
                    contextId = resolvedContextId
                ))
            }
        }
        activeSessionId = null
        currentPackage = null
        
        // Phase 7 Polish: Instantly update the home screen widget with new logged time
        try {
            SprintWidget().updateAll(context)
        } catch (e: Exception) {
            // Ignore for JVM unit tests where Glance is not mocked
        } catch (e: Error) {
            // Ignore NoClassDefFoundError in tests
        }
    }

    /** Called by BroadcastReceiver when screen turns off */
    fun onScreenOff() {
        scope.launch {
            // Close session at current time
            closeActiveSession(Instant.fromEpochMilliseconds(System.currentTimeMillis()))
            // We can pause the tracking loop while screen is off to save battery
            trackingJob?.cancel()
        }
    }

    /** Called by BroadcastReceiver when screen turns on */
    fun onScreenOn() {
        // Reset query time so we don't process background activity that happened while screen was off
        usageStatsTracker.pollRecentForegroundApp() 
        startTracking()
    }

    /** 
     * Reads the past [days] of UsageEvents and generates Sprint Sessions. 
     * Useful for seeding the app with historical data on first launch.
     */
    suspend fun backfillHistoricalData(days: Int = 3) {
        val events = usageStatsTracker.getHistoricalEvents(days)
        if (events.isEmpty()) return

        // To avoid duplicates, we only want to backfill if the DB is mostly empty,
        // or we could check existing sessions. For simplicity, we just insert.
        // In a production app, we would query the latest session timestamp and only backfill before it.

        var currentPkg: String? = null
        var currentStartTime: Long? = null
        val newSessions = mutableListOf<Session>()

        for (event in events) {
            // Skip system packages in backfill too
            if (event.packageName in IGNORED_PACKAGES) continue
            val rule = ruleRepository.getRuleForPackage(event.packageName)
            if (rule?.isIgnored == true) continue

            // ACTIVITY_RESUMED = 1
            if (event.eventType == 1) { 
                if (currentPkg != null && currentStartTime != null) {
                    val durationMs = event.timestamp - currentStartTime
                    if (durationMs > MIN_SESSION_DURATION_MS) {
                        val rule = ruleRepository.getRuleForPackage(currentPkg)
                        newSessions.add(
                            Session(
                                id = UUID.randomUUID().toString(),
                                deviceId = deviceId,
                                source = SessionSource.APP_USAGE,
                                rawLabel = currentPkg,
                                startTime = Instant.fromEpochMilliseconds(currentStartTime),
                                endTime = Instant.fromEpochMilliseconds(event.timestamp),
                                contextId = rule?.contextId
                            )
                        )
                    }
                }
                currentPkg = event.packageName
                currentStartTime = event.timestamp
            } 
            // ACTIVITY_PAUSED = 2, ACTIVITY_STOPPED = 23 (often we just use PAUSED)
            else if (event.eventType == 2) { 
                if (currentPkg == event.packageName && currentStartTime != null) {
                    val durationMs = event.timestamp - currentStartTime
                    if (durationMs > MIN_SESSION_DURATION_MS) {
                        val rule = ruleRepository.getRuleForPackage(currentPkg)
                        newSessions.add(
                            Session(
                                id = UUID.randomUUID().toString(),
                                deviceId = deviceId,
                                source = SessionSource.APP_USAGE,
                                rawLabel = currentPkg,
                                startTime = Instant.fromEpochMilliseconds(currentStartTime),
                                endTime = Instant.fromEpochMilliseconds(event.timestamp),
                                contextId = rule?.contextId
                            )
                        )
                    }
                    currentPkg = null
                    currentStartTime = null
                }
            }
        }

        // Insert all parsed historical sessions
        newSessions.forEach {
            sessionRepository.insertSession(it)
        }
    }
}
