package com.heysafe.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class FirebaseAuthBackend(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) : AuthBackend {
    override suspend fun signIn(email: String, password: String): AuthUser {
        val r = auth.signInWithEmailAndPassword(email, password).await()
        val u = r.user ?: error("No user returned")
        return AuthUser(u.uid, u.email.orEmpty())
    }

    override suspend fun signUp(email: String, password: String, displayName: String): AuthUser {
        val r = auth.createUserWithEmailAndPassword(email, password).await()
        val u = r.user ?: error("No user returned")
        u.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName).build()).await()
        return AuthUser(u.uid, u.email.orEmpty())
    }

    override fun signOut() = auth.signOut()
    override fun currentUser(): AuthUser? = auth.currentUser?.let { AuthUser(it.uid, it.email.orEmpty()) }
}
