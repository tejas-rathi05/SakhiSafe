package com.heysafe.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heysafe.app.di.ServiceLocator

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onGoBack: () -> Unit,
    vm: AuthViewModel = viewModel { AuthViewModel(ServiceLocator.authRepository) },
) {
    val state by vm.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state) { if (state is AuthUiState.Authenticated) onRegistered() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Create account", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Stay safe with VSafe",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (6+ chars)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { vm.signUp(email.trim(), password, name.trim()) },
                enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 6 && state !is AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create account")
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = onGoBack,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Have an account? Sign in") }
            (state as? AuthUiState.Error)?.let {
                Spacer(Modifier.height(12.dp))
                Text(it.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
