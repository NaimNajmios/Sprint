package com.najmi.sprint.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : Service() {

    @Inject
    lateinit var trackingEngine: TrackingEngine

    @Inject
    lateinit var physicalSignalTracker: PhysicalSignalTracker

    private lateinit var screenStateReceiver: ScreenStateReceiver

    override fun onCreate() {
        super.onCreate()
        screenStateReceiver = ScreenStateReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification()
        
        // Start as foreground service to prevent Android from killing it
        startForeground(NOTIFICATION_ID, notification)
        
        // Begin tracking
        trackingEngine.startTracking()
        physicalSignalTracker.startListening()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenStateReceiver)
        trackingEngine.stopTracking()
        physicalSignalTracker.stopListening()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sprint Passive Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Sprint tracker running in the background to log context."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sprint Active")
            .setContentText("Passively logging activity context.")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history) // Placeholder icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "SprintTrackingChannel"
        private const val NOTIFICATION_ID = 1001
    }
}
