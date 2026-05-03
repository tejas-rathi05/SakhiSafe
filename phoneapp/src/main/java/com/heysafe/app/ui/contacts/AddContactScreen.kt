package com.heysafe.app.ui.contacts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heysafe.app.data.contacts.Contact
import com.heysafe.app.data.contacts.ContactGroup
import com.heysafe.app.di.ServiceLocator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    vm: ContactsViewModel = viewModel { ContactsViewModel(ServiceLocator.authRepository, ServiceLocator.contactsRepository) },
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+91") }
    var relationship by remember { mutableStateOf("") }
    var group by remember { mutableStateOf(ContactGroup.FAMILY) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add contact") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Phone (E.164, e.g. +919876543210)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            OutlinedTextField(
                value = relationship, onValueChange = { relationship = it },
                label = { Text("Relationship (e.g. Mother, Friend)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text("Group", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = group == ContactGroup.FAMILY,
                    onClick = { group = ContactGroup.FAMILY },
                    label = { Text("Family") },
                )
                Spacer(Modifier.width(12.dp))
                FilterChip(
                    selected = group == ContactGroup.FRIENDS,
                    onClick = { group = ContactGroup.FRIENDS },
                    label = { Text("Friends") },
                )
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    error = null
                    saving = true
                    scope.launch {
                        val r = vm.add(
                            Contact(
                                name = name.trim(),
                                phone = phone.trim(),
                                relationship = relationship.trim(),
                                group = group,
                            )
                        )
                        saving = false
                        r.fold(
                            onSuccess = { onSaved() },
                            onFailure = { error = it.message ?: "Failed to save" },
                        )
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Save")
            }
        }
    }
}
