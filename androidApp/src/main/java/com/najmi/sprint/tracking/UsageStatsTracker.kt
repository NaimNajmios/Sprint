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
}
