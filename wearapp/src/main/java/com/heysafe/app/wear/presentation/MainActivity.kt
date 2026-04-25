package com.heysafe.app.wear.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.heysafe.app.wear.presentation.theme.MyAppTheme
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity(), SensorEventListener {

    private var pressCount = 0
    private val requiredPresses = 3
    private val pressTimeout = 1000L // Timeout in milliseconds
    private val handler = Handler(Looper.getMainLooper())
    private val pressResetRunnable = Runnable {
        pressCount = 0  // Reset press count after timeout
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_STEM_1) { // Adjust this key code based on your watch's hardware
            pressCount++
            Log.d("PressCount", "Button pressed $pressCount times")

            // Remove any previously scheduled reset
            handler.removeCallbacks(pressResetRunnable)

            // Schedule a reset after timeout
            handler.postDelayed(pressResetRunnable, pressTimeout)

            if (pressCount >= requiredPresses) {
                pressCount = 0  // Reset the counter after the action is triggered
                Log.d("PressCount", "Button pressed thrice!")
                openNewScreen()  // Call your function to trigger the action
            }
            return true  // Indicate the event is handled
        }
        return super.onKeyDown(keyCode, event)
    }

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private val TAG = "HeartRateLog"

    // Permission request launcher using ActivityResultContracts
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Permission granted, ready to measure heart rate.")
            } else {
                Log.d(TAG, "Permission denied to access sensors")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearApp(onMeasureHeartRate = {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
                    initializeHeartRateSensor()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
                }
            })
        }
    }

    private fun openNewScreen() {
        // Example: Start a new activity when button is pressed three times
        val intent = Intent(this, SosActivity::class.java)
        startActivity(intent)
    }

    // Initializes the heart rate sensor
    private fun initializeHeartRateSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        if (heartRateSensor != null) {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Heart rate sensor initialized")
        } else {
            Log.d(TAG, "Heart rate sensor not available")
        }
    }

    private fun sendHeartRateToPhone(heartRate: Float) {
        val dataClient: DataClient = Wearable.getDataClient(this)
        val putDataMapRequest = PutDataMapRequest.create("/heart_rate_data_path")
        putDataMapRequest.dataMap.putFloat("heart_rate", heartRate)
        val putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent()

        dataClient.putDataItem(putDataRequest)
            .addOnSuccessListener {
                Log.d(TAG, "Heart rate data sent to phone: $heartRate")
            }
            .addOnFailureListener {
                Log.d(TAG, "Failed to send heart rate data to phone")
            }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
            val heartRate = event.values[0]
            Log.d(TAG, "Heart rate: $heartRate")
            sendHeartRateToPhone(heartRate)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}

@Composable
fun WearApp(onMeasureHeartRate: () -> Unit) {
    MyAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.wear.compose.material.MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText() // Shows the time at the top

            val measuring = remember { mutableStateOf(false) }
            val heartRateText = if (measuring.value) "Measuring..." else "Measure Heart Rate"

            // Button to measure heart rate
            Button(
                onClick = {
                    measuring.value = true
                    onMeasureHeartRate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = heartRateText, textAlign = TextAlign.Center)
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(onMeasureHeartRate = {})
}