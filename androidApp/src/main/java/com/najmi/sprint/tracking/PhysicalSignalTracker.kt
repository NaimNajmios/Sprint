package com.najmi.sprint.tracking

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.najmi.sprint.core.domain.logger.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhysicalSignalTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackingEngine: TrackingEngine
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isTracking = false
    private var isFaceDown = false

    fun startListening() {
        if (isTracking || accelerometer == null) return
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        isTracking = true
        AppLogger.d("PhysicalSignal", "Started listening to accelerometer (Flip-to-Pause)")
    }

    fun stopListening() {
        if (!isTracking) return
        sensorManager.unregisterListener(this)
        isTracking = false
        isFaceDown = false
        AppLogger.d("PhysicalSignal", "Stopped listening to accelerometer")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val z = event.values[2]
            
            // If z < -8.0, device is mostly face down. (Gravity pulls Z down).
            val currentlyFaceDown = z < -8.0f
            
            if (currentlyFaceDown && !isFaceDown) {
                isFaceDown = true
                AppLogger.d("PhysicalSignal", "Device flipped face down. Treating as Focus/Pause.")
                // Treat face-down as a proxy for screen off / deeply away
                trackingEngine.onScreenOff()
            } else if (!currentlyFaceDown && isFaceDown) {
                isFaceDown = false
                AppLogger.d("PhysicalSignal", "Device flipped face up. Resuming tracking.")
                trackingEngine.onScreenOn()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
