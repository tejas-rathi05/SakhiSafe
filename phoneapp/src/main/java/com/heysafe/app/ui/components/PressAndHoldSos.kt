package com.heysafe.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PressAndHoldSos(
    onTriggered: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Long = 3_000,
) {
    var holding by remember { mutableStateOf(false) }
    var fired by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(holding) {
        if (holding && !fired) {
            val tickMs = 16L
            val totalTicks = holdMillis / tickMs
            var i = 0L
            while (holding && i <= totalTicks) {
                holdProgress = (i.toFloat() / totalTicks).coerceAtMost(1f)
                delay(tickMs)
                i++
            }
            if (holding && holdProgress >= 0.99f) {
                fired = true
                onTriggered()
            }
        } else if (!holding) {
            holdProgress = 0f
            fired = false
        }
    }

    val infinite = rememberInfiniteTransition(label = "sos-pulse")
    val ring1 by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "ring1",
    )
    val ring2 by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.32f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "ring2",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (holding) 0.94f else 1f,
        animationSpec = tween(160),
        label = "press",
    )

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer halo ring 2
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(ring2)
                .background(Color(0xFFE53935).copy(alpha = 0.10f), CircleShape),
        )
        // Outer halo ring 1
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(ring1)
                .background(Color(0xFFE53935).copy(alpha = 0.18f), CircleShape),
        )
        // Hold-progress arc
        if (holding) {
            Canvas(modifier = Modifier.size(190.dp)) {
                val stroke = 8.dp.toPx()
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color(0xFFFFF1F1), Color(0xFFFF6B6B), Color(0xFFE53935)),
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * holdProgress,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        // Core button
        Box(
            modifier = Modifier
                .size(170.dp)
                .scale(pressScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFF6B6B), Color(0xFFE53935), Color(0xFFB71C1C)),
                        radius = 320f,
                    ),
                    shape = CircleShape,
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            holding = true
                            tryAwaitRelease()
                            holding = false
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "SOS",
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    if (holding) "Hold to confirm…" else "Press and hold",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
