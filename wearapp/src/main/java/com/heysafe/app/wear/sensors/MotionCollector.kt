package com.heysafe.app.wear.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

class MotionCollector(context: Context) {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** Emits acceleration magnitude (gravity removed if linear-accel sensor available). */
    fun samples(): Flow<Float> = callbackFlow {
        if (sensor == null) { close(); return@callbackFlow }
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val (x, y, z) = e.values
                trySend(sqrt(x * x + y * y + z * z))
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sm.unregisterListener(l) }
    }
}
