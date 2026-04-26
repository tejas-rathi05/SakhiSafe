package com.heysafe.app.wear.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.heysafe.app.wear.presentation.theme.MyAppTheme
import com.heysafe.app.wear.sensors.SensorService

class MainActivity : ComponentActivity() {

    // 3x button SOS — preserved
    private var pressCount = 0
    private val requiredPresses = 3
    private val pressTimeout = 1000L
    private val handler = Handler(Looper.getMainLooper())
    private val pressResetRunnable = Runnable { pressCount = 0 }

    private val TAG = "HeySafe"
    private var monitoringStarted by mutableStateOf(false)

    private val requestBodySensors =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) maybeStartMonitoring()
            else Log.d(TAG, "BODY_SENSORS denied")
        }

    private val requestPostNotifs =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "POST_NOTIFICATIONS granted=$granted")
            // notifications denial doesn't block the service starting on >=O,
            // it just means no visible foreground notification on Android 13+
            maybeStartMonitoring()
        }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_STEM_1) {
            pressCount++
            handler.removeCallbacks(pressResetRunnable)
            handler.postDelayed(pressResetRunnable, pressTimeout)
            if (pressCount >= requiredPresses) {
                pressCount = 0
                Log.d(TAG, "3x button → SOS")
                startActivity(Intent(this, SosActivity::class.java))
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp(monitoring = monitoringStarted, onStart = { onStartMonitoringTapped() })
        }
    }

    private fun onStartMonitoringTapped() {
        val needsBodySensors = ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED
        val needsPostNotifs = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

        when {
            needsBodySensors -> requestBodySensors.launch(Manifest.permission.BODY_SENSORS)
            needsPostNotifs -> requestPostNotifs.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> maybeStartMonitoring()
        }
    }

    private fun maybeStartMonitoring() {
        if (monitoringStarted) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            return // still not granted — nothing to do
        }
        ContextCompat.startForegroundService(this, Intent(this, SensorService::class.java))
        monitoringStarted = true
        Log.d(TAG, "SensorService started")
    }
}

@Composable
fun WearApp(monitoring: Boolean, onStart: () -> Unit) {
    MyAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.wear.compose.material.MaterialTheme.colors.background),
            contentAlignment = Alignment.Center,
        ) {
            TimeText()
            Button(
                onClick = onStart,
                enabled = !monitoring,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = if (monitoring) "Monitoring..." else "Start monitoring",
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(monitoring = false, onStart = {})
}
