package com.heysafe.app.ui.trip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heysafe.app.di.ServiceLocator
import com.heysafe.app.domain.trip.TripState

@Composable
fun TripScreen(
    vm: TripViewModel = viewModel { TripViewModel(ServiceLocator.tripMonitor) },
) {
    val trip by vm.trip.collectAsState()
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Text(
                "Safe trip",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Tell us where you're going. If you don't tap \"I made it\" by your ETA, we auto-trigger an SOS.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            )

            AnimatedVisibility(visible = trip == null, enter = fadeIn(), exit = fadeOut()) {
                SetupView(onStart = vm::startTrip)
            }
            AnimatedVisibility(visible = trip != null, enter = fadeIn(), exit = fadeOut()) {
                trip?.let { ActiveTripView(state = it, onArrived = vm::arrived) }
            }
        }
    }
}

@Composable
private fun SetupView(onStart: (String, Int) -> Unit) {
    var destination by remember { mutableStateOf("") }
    var etaMinutes by remember { mutableIntStateOf(30) }
    val presets = listOf(15, 30, 45, 60, 90)

    Column {
        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("Destination") },
            placeholder = { Text("e.g. Home, Library, Office") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Expected time to arrive",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { m ->
                EtaChip(label = "${m}m", selected = etaMinutes == m) { etaMinutes = m }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = etaMinutes.toString(),
            onValueChange = { v -> etaMinutes = v.toIntOrNull()?.coerceIn(1, 360) ?: etaMinutes },
            label = { Text("Or custom (minutes)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onStart(destination, etaMinutes) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5B4DFF),
                contentColor = Color.White,
            ),
        ) {
            Text("Start trip", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EtaChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFF5B4DFF) else Color.Gray.copy(alpha = 0.12f)
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActiveTripView(state: TripState, onArrived: () -> Unit) {
    val remainingMs = (state.deadlineMillis - state.now).coerceAtLeast(0)
    val totalMs = state.etaMillis.coerceAtLeast(1)
    val progress = remainingMs.toFloat() / totalMs.toFloat()
    val mm = (remainingMs / 60_000).toInt()
    val ss = ((remainingMs / 1000) % 60).toInt()
    val overdueSoon = progress < 0.15f

    val infinite = rememberInfiniteTransition(label = "trip-pulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (overdueSoon) 1.06f else 1.02f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            "On the way to",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            state.destinationLabel,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(28.dp))

        Box(modifier = Modifier.size(220.dp).scale(pulse), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                drawArc(
                    color = Color.Gray.copy(alpha = 0.15f),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        if (overdueSoon)
                            listOf(Color(0xFFFF6B6B), Color(0xFFE53935), Color(0xFFFF6B6B))
                        else
                            listOf(Color(0xFF5B4DFF), Color(0xFF7BE0FF), Color(0xFF5B4DFF)),
                    ),
                    startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%d:%02d".format(mm, ss),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    if (overdueSoon) "auto-SOS soon" else "until SOS",
                    color = if (overdueSoon) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onArrived,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF34C759),
                contentColor = Color.Black,
            ),
        ) {
            Text("I made it safely", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF5B4DFF)))
            Spacer(Modifier.size(6.dp))
            Text("Trip in progress", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
