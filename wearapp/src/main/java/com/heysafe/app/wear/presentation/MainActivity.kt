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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.heysafe.app.wear.presentation.theme.MyAppTheme
import com.heysafe.app.wear.sensors.SensorService
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var pressCount = 0
    private val requiredPresses = 3
    private val pressTimeout = 1000L
    private val handler = Handler(Looper.getMainLooper())
    private val pressResetRunnable = Runnable { pressCount = 0 }

    private val TAG = "HeySafe"

    private val requestBodySensors =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) maybeStartMonitoring()
            else Log.d(TAG, "BODY_SENSORS denied")
        }

    private val requestPostNotifs =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "POST_NOTIFICATIONS granted=$granted")
            maybeStartMonitoring()
        }

    private var stemDownAt: Long = 0L
    private val silentLongPressMs = 1500L

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_STEM_1) {
            if (event?.repeatCount == 0) {
                stemDownAt = System.currentTimeMillis()
            }
            // Long press → silent SOS (no countdown, no UI)
            if (event != null && System.currentTimeMillis() - stemDownAt >= silentLongPressMs && stemDownAt > 0) {
                stemDownAt = 0L
                Log.d(TAG, "Long-press button → silent SOS")
                startActivity(Intent(this, SosActivity::class.java).putExtra("silentMode", true))
                return true
            }
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

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_STEM_1) {
            stemDownAt = 0L
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp(
                onStart = { onStartMonitoringTapped() },
                onStop = { stopMonitoring() },
                onSos = { startActivity(Intent(this, SosActivity::class.java)) },
            )
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
        if (SensorService.isMonitoring.value) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) return
        ContextCompat.startForegroundService(this, Intent(this, SensorService::class.java))
        Log.d(TAG, "SensorService started")
    }

    private fun stopMonitoring() {
        startService(Intent(this, SensorService::class.java).apply { action = SensorService.ACTION_STOP })
    }
}

@Composable
fun WearApp(onStart: () -> Unit, onStop: () -> Unit, onSos: () -> Unit) {
    val monitoring by SensorService.isMonitoring.collectAsState()
    val hr by SensorService.latestHr.collectAsState()
    val startedAt by SensorService.startedAt.collectAsState()

    MyAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(Color(0xFF1A0F1A), Color.Black))),
        ) {
            TimeText()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                StatusPill(monitoring)
                HeartCenter(monitoring, hr)
                ElapsedText(monitoring, startedAt)
                ActionRow(monitoring, onStart = onStart, onStop = onStop, onSos = onSos)
            }
        }
    }
}

@Composable
private fun StatusPill(monitoring: Boolean) {
    val color = if (monitoring) Color(0xFF34C759) else Color(0xFF8E8E93)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (monitoring) "MONITORING" else "IDLE",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HeartCenter(monitoring: Boolean, hr: Float?) {
    val infinite = rememberInfiniteTransition(label = "heart")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (monitoring) 1.18f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (monitoring) 700 else 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heart-scale",
    )
    val ring by animateFloatAsState(
        targetValue = if (monitoring) 1f else 0.4f,
        animationSpec = tween(500),
        label = "ring",
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFE53935).copy(alpha = 0.45f * ring),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = hr?.toInt()?.toString() ?: "—",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "bpm",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ElapsedText(monitoring: Boolean, startedAt: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(monitoring, startedAt) {
        while (monitoring) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val text = if (monitoring && startedAt > 0) {
        val sec = ((now - startedAt) / 1000).coerceAtLeast(0)
        val mm = sec / 60
        val ss = sec % 60
        "elapsed %d:%02d".format(mm, ss)
    } else "ready to monitor"
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.65f),
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ActionRow(monitoring: Boolean, onStart: () -> Unit, onStop: () -> Unit, onSos: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (monitoring) {
            Button(
                onClick = onStop,
                modifier = Modifier.weight(1f).height(38.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.14f),
                    contentColor = Color.White,
                ),
            ) { Text("Stop", fontSize = 12.sp) }
        } else {
            Button(
                onClick = onStart,
                modifier = Modifier.weight(1f).height(38.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34C759),
                    contentColor = Color.Black,
                ),
            ) { Text("Start", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        Button(
            onClick = onSos,
            modifier = Modifier.weight(1f).height(38.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE53935),
                contentColor = Color.White,
            ),
        ) { Text("SOS", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(onStart = {}, onStop = {}, onSos = {})
}
