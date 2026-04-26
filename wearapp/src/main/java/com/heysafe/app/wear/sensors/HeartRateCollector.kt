package com.heysafe.app.wear.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class HeartRateCollector(context: Context) {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    /** Emits HR samples in BPM as the device produces them. Closes if no HR sensor. */
    fun samples(): Flow<Float> = callbackFlow {
        if (sensor == null) { close(); return@callbackFlow }
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { trySend(e.values[0]) }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sm.unregisterListener(l) }
    }
}
