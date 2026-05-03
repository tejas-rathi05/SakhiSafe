package com.heysafe.app.ui.vitals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heysafe.app.di.ServiceLocator
import com.heysafe.app.ui.components.DarkGradientCard
import com.heysafe.app.ui.theme.Success
import com.heysafe.app.ui.theme.TextSecondary

@Composable
fun VitalsScreen(
    vm: VitalsViewModel = viewModel { VitalsViewModel(ServiceLocator.vitalsRepository) },
) {
    val latest by vm.latest.collectAsState()
    val history by vm.history.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "My Devices",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fossil Gen 5", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (latest != null) "Connected" else "Idle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(if (latest != null) Success else Color.Gray, CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            DarkGradientCard(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = "${latest?.hr?.toInt() ?: 0}",
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "BPM",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    EcgLine(
                        samples = history.map { it.hr },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                    )
                }
            }

            if (latest == null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Pair your watch and tap 'Start monitoring' on the watch to begin.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
