package com.heysafe.app.ui.home

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heysafe.app.data.contacts.ContactGroup
import com.heysafe.app.di.ServiceLocator
import com.heysafe.app.ui.components.Avatar
import com.heysafe.app.ui.components.BellIconButton
import com.heysafe.app.ui.components.DarkGradientCard
import com.heysafe.app.ui.components.PressAndHoldSos
import com.heysafe.app.ui.theme.Accent
import com.heysafe.app.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onSoundAlarmTap: () -> Unit,
    onManageContacts: () -> Unit,
    vm: HomeViewModel = viewModel {
        HomeViewModel(
            ServiceLocator.authRepository,
            ServiceLocator.contactsRepository,
            ServiceLocator.alertOrchestrator,
        )
    },
) {
    val name = vm.displayName
    val group by vm.selectedGroup.collectAsState()
    val items by vm.groupedContacts.collectAsState()
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // 1. Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(name, sizeDp = 44)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Hi, $name",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                BellIconButton(onClick = { /* notifications screen — future */ })
            }

            Spacer(Modifier.height(12.dp))

            // 2. Family / Friends tabs
            TabRow(
                selectedTabIndex = if (group == ContactGroup.FAMILY) 0 else 1,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Tab(
                    selected = group == ContactGroup.FAMILY,
                    onClick = { vm.setGroup(ContactGroup.FAMILY) },
                    text = { Text("Family") },
                )
                Tab(
                    selected = group == ContactGroup.FRIENDS,
                    onClick = { vm.setGroup(ContactGroup.FRIENDS) },
                    text = { Text("Friends") },
                )
            }

            // "Manage" textbutton — keeps Contacts screen reachable
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onManageContacts) {
                    Text("Manage", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 3. Avatar carousel
            if (items.isEmpty()) {
                Text(
                    "No ${if (group == ContactGroup.FAMILY) "family" else "friends"} contacts yet. " +
                        "Add some from the Contacts screen.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(items, key = { it.id }) { c ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Avatar(c.name, sizeDp = 64)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                c.name.split(" ").first().take(8),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 4. Action pills (Video Call / Message)
            val firstContact = items.firstOrNull()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        firstContact?.let {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${it.phone}"))
                            )
                        }
                    },
                    enabled = firstContact != null,
                    modifier = Modifier.weight(1f),
                ) { Text("Video Call") }
                OutlinedButton(
                    onClick = {
                        firstContact?.let {
                            context.startActivity(
                                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${it.phone}"))
                            )
                        }
                    },
                    enabled = firstContact != null,
                    modifier = Modifier.weight(1f),
                ) { Text("Message") }
            }

            Spacer(Modifier.height(20.dp))

            // 5. Sound Alarm dark gradient card
            DarkGradientCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSoundAlarmTap() },
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Sound Alarm",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            "Tap to make alarm ringing",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // 6. Press-and-hold SOS
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PressAndHoldSos(onTriggered = vm::triggerManualSos)
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}
