package com.heysafe.app.ui.contacts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heysafe.app.data.contacts.Contact
import com.heysafe.app.data.contacts.ContactGroup
import com.heysafe.app.di.ServiceLocator
import com.heysafe.app.ui.components.Avatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onAddContact: () -> Unit,
    onBack: () -> Unit,
    vm: ContactsViewModel = viewModel { ContactsViewModel(ServiceLocator.authRepository, ServiceLocator.contactsRepository) },
) {
    var selectedTab by remember { mutableStateOf(0) }   // 0 = family, 1 = friends
    val all by vm.items.collectAsState()
    val visible = all.filter {
        if (selectedTab == 0) it.group == ContactGroup.FAMILY else it.group == ContactGroup.FRIENDS
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                actions = {
                    IconButton(onClick = onAddContact) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add contact")
                    }
                },
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Family") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Friends") })
            }
            if (visible.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No contacts yet. Tap + to add one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visible, key = { it.id }) { c ->
                        ContactRow(c, onDelete = { vm.delete(c.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(c: Contact, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(c.name, sizeDp = 48)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    c.relationship.ifBlank { c.phone },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (c.relationship.isNotBlank()) {
                    Text(c.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete contact")
            }
        }
    }
}
