package com.najmi.sprint.tracking

import com.najmi.sprint.core.domain.model.Session
import com.najmi.sprint.core.domain.model.SessionSource
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

    private suspend fun pollAndUpdate() {
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

        // Wait, debouncing logic (Phase 3b):
        // If the switch time is extremely close to the current session start, it might be a transient notification click.
        // For now (Phase 2), we just strictly log all distinct switches. Debouncing will be added in Phase 3.

        // Close the previous session
        closeActiveSession(switchTime)

        // Only start a new session if it's not the launcher/system UI (Optional filtering, but we track everything for MVP)
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
        currentPackage = newPackage
    }

    private suspend fun closeActiveSession(endTime: Instant) {
        activeSessionId?.let { id ->
            val session = sessionRepository.getSessionById(id)
            if (session != null && session.endTime == null) {
                sessionRepository.updateSession(session.copy(endTime = endTime))
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
