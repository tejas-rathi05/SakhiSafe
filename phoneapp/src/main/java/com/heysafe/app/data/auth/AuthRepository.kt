package com.heysafe.app.data.auth

data class AuthUser(val uid: String, val email: String)

sealed class AuthResult {
    data class Success(val user: AuthUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface AuthBackend {
    suspend fun signIn(email: String, password: String): AuthUser
    suspend fun signUp(email: String, password: String, displayName: String): AuthUser
    fun signOut()
    fun currentUser(): AuthUser?
}

class AuthRepository(private val backend: AuthBackend) {
    suspend fun signIn(email: String, password: String): AuthResult =
        runCatching { backend.signIn(email, password) }
            .fold({ AuthResult.Success(it) }, { AuthResult.Error(it.message ?: "Unknown error") })

    suspend fun signUp(email: String, password: String, displayName: String): AuthResult =
        runCatching { backend.signUp(email, password, displayName) }
            .fold({ AuthResult.Success(it) }, { AuthResult.Error(it.message ?: "Unknown error") })

    fun signOut() = backend.signOut()
    fun currentUser(): AuthUser? = backend.currentUser()
}
