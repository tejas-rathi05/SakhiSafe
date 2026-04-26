package com.heysafe.app.di

import android.content.Context
import com.heysafe.app.data.alerts.AlertsRepository
import com.heysafe.app.data.alerts.FirebaseAlertsBackend
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
    lateinit var alertsRepository: AlertsRepository

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            authRepository = AuthRepository(FirebaseAuthBackend())
            contactsRepository = ContactsRepository(FirestoreContactsBackend())
            vitalsRepository = VitalsRepository()
            alertsRepository = AlertsRepository(FirebaseAlertsBackend())
            initialized = true
        }
    }
}
