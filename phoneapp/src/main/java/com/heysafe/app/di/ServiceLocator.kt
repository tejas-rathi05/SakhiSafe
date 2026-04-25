package com.heysafe.app.di

import android.content.Context
import com.heysafe.app.data.auth.AuthRepository
import com.heysafe.app.data.auth.FirebaseAuthBackend

object ServiceLocator {
    @Volatile private var initialized = false
    lateinit var authRepository: AuthRepository

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            authRepository = AuthRepository(FirebaseAuthBackend())
            initialized = true
        }
    }
}
