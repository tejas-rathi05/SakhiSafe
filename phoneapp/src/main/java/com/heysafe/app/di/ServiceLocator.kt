package com.heysafe.app.di

import android.content.Context
import com.heysafe.app.data.auth.AuthRepository
import com.heysafe.app.data.auth.FirebaseAuthBackend
import com.heysafe.app.data.contacts.ContactsRepository
import com.heysafe.app.data.contacts.FirestoreContactsBackend
import com.heysafe.app.data.vitals.VitalsRepository

object ServiceLocator {
    @Volatile private var initialized = false
    lateinit var authRepository: AuthRepository
    lateinit var contactsRepository: ContactsRepository
    lateinit var vitalsRepository: VitalsRepository

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            authRepository = AuthRepository(FirebaseAuthBackend())
            contactsRepository = ContactsRepository(FirestoreContactsBackend())
            vitalsRepository = VitalsRepository()
            initialized = true
        }
    }
}
