package com.najmi.sprint.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScreenStateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var trackingEngine: TrackingEngine

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> {
                trackingEngine.onScreenOff()
            }
            Intent.ACTION_SCREEN_ON -> {
                trackingEngine.onScreenOn()
            }
        }
    }
}
