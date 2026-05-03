package com.heysafe.app.ui.alert

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heysafe.app.di.ServiceLocator
import com.heysafe.app.ui.theme.Primary
import com.heysafe.app.ui.theme.SlateDark
import com.heysafe.app.ui.theme.SlateMid
import kotlinx.coroutines.delay

private const val COUNTDOWN_SECONDS = 15

@Composable
fun ActiveAlertScreen(
    onResolved: () -> Unit,
    vm: AlertViewModel = viewModel { AlertViewModel(ServiceLocator.alertOrchestrator) },
) {
    val p by vm.progress.collectAsState()
    val name = remember {
        ServiceLocator.authRepository.currentUser()?.email
            ?.substringBefore("@")
            ?.replaceFirstChar { it.uppercase() } ?: "there"
    }

    LaunchedEffect(p.resolved) {
        if (p.resolved) {
            onResolved()
            vm.clear()
        }
    }

    var secondsLeft by remember { mutableStateOf(COUNTDOWN_SECONDS) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }
    val progressFraction = 1f - (secondsLeft.toFloat() / COUNTDOWN_SECONDS)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Stay Calm, $name.",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "We are sending an alert to your emergency circle, everyone nearby and Law Enforcement",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(Modifier.weight(0.4f))

            // Countdown number
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (secondsLeft > 0) secondsLeft.toString() else "GO",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 96.sp,
                )
            }

            Spacer(Modifier.weight(0.3f))

            // Progress ring + SOS center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(260.dp)) {
                    val stroke = 18.dp.toPx()
                    val inset = stroke / 2
                    drawArc(
                        color = SlateMid,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = Primary,
                        startAngle = -90f,
                        sweepAngle = 360f * progressFraction,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "SOS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 56.sp,
                    )
                }
            }

            Spacer(Modifier.weight(0.4f))

            // Compact alert status
            p.errorMessage?.let {
                Text(it, color = Primary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }
            val statusLine = buildString {
                append(if (p.gpsCaptured) "📍 Location sent  " else "📍 Locating…  ")
                append(if (p.audioRecording) "🎙 Recording" else "🎙 —")
            }
            Text(
                statusLine,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
            )
            if (p.contactsSent.isNotEmpty()) {
                Text(
                    "Notified: ${p.contactsSent.joinToString(", ")}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { vm.resolve() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("I'm Safe", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
