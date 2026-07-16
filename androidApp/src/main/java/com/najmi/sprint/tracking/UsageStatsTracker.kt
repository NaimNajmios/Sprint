package com.najmi.sprint.tracking

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private var lastQueryTime: Long = Clock.System.now().toEpochMilliseconds()

    /**
     * Polls UsageStatsManager for new ACTIVITY_RESUMED events since the last query.
     * Returns the package name of the most recently resumed app, if any.
     */
    fun pollRecentForegroundApp(): ForegroundEvent? {
        val now = Clock.System.now().toEpochMilliseconds()
        // We query from lastQueryTime to now
        val events = usageStatsManager.queryEvents(lastQueryTime, now)
        
        var latestEvent: ForegroundEvent? = null
        val event = UsageEvents.Event()
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // We only care about activity resumed (app comes to foreground)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                latestEvent = ForegroundEvent(
                    packageName = event.packageName,
                    timestamp = event.timeStamp
                )
            }
        }
        
        lastQueryTime = now
        return latestEvent
    }

    data class ForegroundEvent(
        val packageName: String,
        val timestamp: Long
    )

    data class HistoricalEvent(
        val packageName: String,
        val timestamp: Long,
        val eventType: Int
    )

    /**
     * Gets all resumed/paused events over the last [days] days for backfilling.
     */
    fun getHistoricalEvents(days: Int = 1): List<HistoricalEvent> {
        val now = Clock.System.now().toEpochMilliseconds()
        val start = now - (days * 24 * 60 * 60 * 1000L)
        val events = usageStatsManager.queryEvents(start, now)
        
        val result = mutableListOf<HistoricalEvent>()
        val event = UsageEvents.Event()
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || 
                event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                result.add(
                    HistoricalEvent(
                        packageName = event.packageName,
                        timestamp = event.timeStamp,
                        eventType = event.eventType
                    )
                )
            }
        }
        return result
    }

    /**
     * Gets the top most used applications over the last [days] days.
     * Used for the Phase 3a Onboarding Screen to seed classification rules.
     */
    fun getTopRecentApps(days: Int = 3, limit: Int = 10): List<String> {
        val now = Clock.System.now().toEpochMilliseconds()
        val start = now - (days * 24 * 60 * 60 * 1000L)
        
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            now
        )
        
        return stats
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .map { it.packageName }
            .distinct()
            .take(limit)
    }
}
