package com.heysafe.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heysafe.app.data.auth.AuthRepository
import com.heysafe.app.data.auth.AuthResult
import com.heysafe.app.data.auth.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val user: AuthUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state

    fun signIn(email: String, password: String) {
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = repo.signIn(email, password)) {
                is AuthResult.Success -> AuthUiState.Authenticated(r.user)
                is AuthResult.Error -> AuthUiState.Error(r.message)
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = repo.signUp(email, password, displayName)) {
                is AuthResult.Success -> AuthUiState.Authenticated(r.user)
                is AuthResult.Error -> AuthUiState.Error(r.message)
            }
        }
    }
}
