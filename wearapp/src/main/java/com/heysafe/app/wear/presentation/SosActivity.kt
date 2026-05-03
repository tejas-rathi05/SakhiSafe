package com.heysafe.app.wear.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heysafe.app.wear.presentation.theme.MyAppTheme
import com.heysafe.app.wear.transport.DataLayerSender
import com.heysafe.app.wear.transport.WearMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SosActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val triggerSource = intent.getStringExtra("triggerSource") ?: WearMessages.SOURCE_MANUAL
        val hrWindow = intent.getFloatArrayExtra("hrWindow") ?: floatArrayOf()
        val motionWindow = intent.getFloatArrayExtra("motionWindow") ?: floatArrayOf()
        val silentMode = intent.getBooleanExtra("silentMode", false)
        val sender = DataLayerSender(this)

        if (silentMode) {
            // Skip countdown: fire alert immediately, then go straight to AlertActivity.
            scope.launch {
                runCatching { sender.sendAlertConfirmed("silent-manual", hrWindow, motionWindow) }
            }
            startActivity(Intent(this, AlertActivity::class.java))
            finish()
            return
        }

        setContent {
            MyAppTheme {
                CountdownScreen(
                    triggerSource = triggerSource,
                    onSafe = {
                        scope.launch { runCatching { sender.sendAlertCanceled() } }
                        finish()
                    },
                    onTimeout = {
                        scope.launch {
                            runCatching { sender.sendAlertConfirmed(triggerSource, hrWindow, motionWindow) }
                        }
                        startActivity(Intent(this@SosActivity, AlertActivity::class.java))
                        finish()
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

@Composable
fun CountdownScreen(
    triggerSource: String = WearMessages.SOURCE_MANUAL,
    durationMillis: Int = 15_000,
    onSafe: () -> Unit,
    onTimeout: () -> Unit,
) {
    var remaining by remember { mutableIntStateOf(durationMillis / 1000) }
    var canceled by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = remaining.toFloat() / (durationMillis / 1000f),
        animationSpec = tween(900, easing = LinearEasing),
        label = "countdown-progress",
    )

    LaunchedEffect(canceled) {
        if (canceled) return@LaunchedEffect
        repeat(durationMillis / 1000) {
            delay(1000)
            if (canceled) return@LaunchedEffect
            remaining = (remaining - 1).coerceAtLeast(0)
        }
        if (!canceled) onTimeout()
    }

    val infinite = rememberInfiniteTransition(label = "ring")
    val pulseScale by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ring-scale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF3A0A0A), Color.Black),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 24.dp),
        ) {
            Text(
                text = "SOS in",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseScale),
                ) {
                    val stroke = 10.dp.toPx()
                    drawArc(
                        color = Color.White.copy(alpha = 0.08f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFFFF6B6B), Color(0xFFE53935), Color(0xFFB71C1C), Color(0xFFFF6B6B)),
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = remaining.toString(),
                        color = Color.White,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "seconds",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (triggerSource == WearMessages.SOURCE_MANUAL)
                    "Manual SOS triggered"
                else
                    "Stress detected · ${triggerSource.uppercase()}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    canceled = true
                    onSafe()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
            ) {
                Text(
                    text = "I'M SAFE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Preview
@Composable
fun CountdownPreview() {
    MyAppTheme {
        CountdownScreen(onSafe = {}, onTimeout = {})
    }
}
