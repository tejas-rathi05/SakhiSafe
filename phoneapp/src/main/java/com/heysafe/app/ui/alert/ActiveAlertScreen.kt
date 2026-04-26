package com.heysafe.app.ui.alert

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heysafe.app.di.ServiceLocator
import com.heysafe.app.ui.theme.Primary
import com.heysafe.app.ui.theme.SurfaceDarkBot
import com.heysafe.app.ui.theme.SurfaceDarkTop

@Composable
fun ActiveAlertScreen(
    onResolved: () -> Unit,
    vm: AlertViewModel = viewModel { AlertViewModel(ServiceLocator.alertOrchestrator) },
) {
    val p by vm.progress.collectAsState()
    LaunchedEffect(p.resolved) {
        if (p.resolved) {
            onResolved()
            vm.clear()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SurfaceDarkTop, SurfaceDarkBot)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                PulseRing()
                Spacer(Modifier.height(24.dp))
                Text(
                    "Sending alert to your emergency circle",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(Modifier.height(8.dp))
                p.errorMessage?.let {
                    Text(it, color = Primary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
                StatusLine(if (p.gpsCaptured) "📍 Location captured" else "📍 Locating…")
                StatusLine(if (p.audioRecording) "🎙 Recording 30s audio" else "🎙 Audio off")
                if (p.alertId != null) {
                    StatusLine("☁️ Alert ${p.alertId} written to cloud")
                }
                Spacer(Modifier.height(16.dp))
                p.contactsSent.forEach { Text("✅  $it", color = Color.White) }
                p.contactsPending.forEach { Text("⏱  $it", color = Color.White.copy(alpha = 0.7f)) }
                if (p.contactsSent.isEmpty() && p.contactsPending.isEmpty()) {
                    Text(
                        "No emergency contacts configured. Add some on the Contacts screen.",
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
            Button(
                onClick = { vm.resolve() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(16.dp),
            ) { Text("I'm Safe") }
        }
    }
}

@Composable
private fun StatusLine(text: String) {
    Text(text, color = Color.White, modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
private fun PulseRing() {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val scale by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse-scale",
    )
    val alpha by infinite.animateFloat(
        initialValue = 0.7f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse-alpha",
    )
    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size((96 * scale).dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Primary)
        )
        Text("SOS", color = Color.White, style = MaterialTheme.typography.headlineMedium)
    }
}
