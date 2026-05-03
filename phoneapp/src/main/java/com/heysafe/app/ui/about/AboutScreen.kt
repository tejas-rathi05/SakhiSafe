package com.heysafe.app.ui.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heysafe.app.di.ServiceLocator

@Composable
fun AboutScreen(onSignedOut: () -> Unit, onOpenHelp: () -> Unit = {}) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("About VSafe", style = MaterialTheme.typography.headlineLarge)
            Text(
                "v1.0 — SRM Major Project",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )

            Section(
                title = "What this prototype does",
                body = "• Continuously monitors heart rate and motion from a Wear OS watch.\n" +
                        "• Detects distress passively using a heuristic + ML model trained on the public WESAD dataset.\n" +
                        "• Manual SOS via 3× watch-button press or 3-sec long-press on phone.\n" +
                        "• Fan-outs alerts to all emergency contacts via WhatsApp with live GPS link.\n" +
                        "• Records 30 sec of audio evidence on alert (base64-inlined in Firestore).\n" +
                        "• Real-time Guardian Dashboard for trusted contacts / responders.",
            )

            Section(
                title = "Heuristic vs ML",
                body = "• Heuristic: HR > baseline + 30 BPM AND motion variance > 3.0 m/s² for ≥10 s.\n" +
                        "• ML: TFLite model on-device. Architecture sized for low-power Wear OS inference (16→8→1 sigmoid, ~5 KB).\n" +
                        "• Trigger fires on (heuristic OR ml_score > 0.75) — OR-gate keeps recall high; the 15 s 'Are you safe?' countdown lets users cancel false positives.",
            )

            Section(
                title = "Limitations",
                body = "• EDA (electrodermal activity / GSR) is in our research design but Fossil Gen 5 has no GSR sensor. Future work — would need an Empatica E4 or similar wearable.\n" +
                        "• Multi-class threat classification needs labeled assault data, which is ethically unavailable. We use stress-as-proxy from WESAD.\n" +
                        "• Police integration requires department APIs we don't have. The Guardian Dashboard simulates the view a responder would see.\n" +
                        "• Audio is currently inlined as base64 in the alert document (Firebase Storage now requires Blaze billing). Production deployment would move to a dedicated encrypted-blob store with end-to-end encryption.",
            )

            Section(
                title = "Research basis",
                body = "1. Wearable rape sensor research, MIT, 2018\n" +
                        "2. Stress detection using multimodal physiological signals, IEEE TBME, 2024\n" +
                        "3. IoT-based women safety systems, IEEE Access, 2023\n" +
                        "4. Machine learning approaches for stress detection, JMIR Mental Health, 2024\n" +
                        "5. AI-driven women safety analytics, IJCRT, 2025",
            )

            Section(
                title = "Team",
                body = "• Tejas Rathi\n• Harsh\n• Vinayak Parashar\n\nGuide: Mr. Kshitiz Saxena, Asst. Prof, CSE, SRM IST",
            )

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = onOpenHelp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text("Help & emergency contacts") }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    ServiceLocator.authRepository.signOut()
                    onSignedOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text("Sign out") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Spacer(Modifier.height(20.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium)
}
