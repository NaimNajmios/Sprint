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

@Singleton
class TrackingEngine @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val ruleRepository: RuleRepository,
    private val usageStatsTracker: UsageStatsTracker
) {
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
                
                // Debounce: If the session was less than 10 seconds, it's a transient switch (e.g., notification glance). Drop it.
                if (durationMs < 10_000L) {
                    sessionRepository.deleteSession(previousSession.id)
                }
            }
        }

        // Merge: Check if we are switching back to the same app we were using recently
        val lastClosed = sessionRepository.getLastClosedSession()
        if (lastClosed != null && lastClosed.rawLabel == newPackage && lastClosed.endTime != null) {
            val gapMs = switchTime.toEpochMilliseconds() - lastClosed.endTime!!.toEpochMilliseconds()
            // If the gap is less than 2 minutes, merge them by reopening the last closed session
            if (gapMs < 120_000L) {
                sessionToReopen = lastClosed
            }
        }

        if (sessionToReopen != null) {
            // Reopen the existing session rather than creating a new one
            sessionRepository.updateSession(sessionToReopen.copy(endTime = null))
            activeSessionId = sessionToReopen.id
        } else {
            // Create a brand new session
            val newSessionId = UUID.randomUUID().toString()
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
                
                sessionRepository.updateSession(session.copy(
                    endTime = endTime,
                    contextId = resolvedContextId
                ))
            }
        }
        activeSessionId = null
        currentPackage = null
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
}
