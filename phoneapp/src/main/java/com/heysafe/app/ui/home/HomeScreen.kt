package com.heysafe.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heysafe.app.di.ServiceLocator
import com.heysafe.app.ui.alert.AlertViewModel

@Composable
fun HomeScreen(
    onOpenContacts: () -> Unit = {},
    alertVm: AlertViewModel = viewModel { AlertViewModel(ServiceLocator.alertOrchestrator) },
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("HeySafe", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Phase 7 will replace this with the Figma home design.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onOpenContacts, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Open contacts")
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { alertVm.manualAlert() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Test SOS (manual)")
            }
        }
    }
}
