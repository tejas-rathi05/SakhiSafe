package com.heysafe.app.ui.help

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen() {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Help", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap a helpline to dial.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))
            Text("Emergency helplines", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            HelplineCard("Police", "112") { dial(context, "112") }
            HelplineCard("Women's helpline (India)", "181") { dial(context, "181") }
            HelplineCard("National Commission for Women", "+91-7827170170") { dial(context, "+917827170170") }
            HelplineCard("Childline (children in distress)", "1098") { dial(context, "1098") }

            Spacer(Modifier.height(20.dp))
            Text("Safety tips", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            TipCard("Share your live location with someone you trust before heading out.")
            TipCard("If something feels wrong, trust your instinct and leave.")
            TipCard("Pretend you're on a call with someone if you feel unsafe.")
            TipCard("Avoid isolated areas at night when alone.")
            TipCard("Keep your phone charged and the SOS function within easy reach.")

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun dial(ctx: Context, number: String) {
    ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
}

@Composable
private fun HelplineCard(name: String, number: String, onCall: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onCall() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    number,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Icon(
                Icons.Outlined.Phone,
                contentDescription = "Call",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TipCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(text, modifier = Modifier.padding(16.dp))
    }
}
