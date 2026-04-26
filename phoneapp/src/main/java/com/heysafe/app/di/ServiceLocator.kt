package com.heysafe.app.di

import android.content.Context
import com.heysafe.app.data.alerts.AlertsRepository
import com.heysafe.app.data.alerts.FirebaseAlertsBackend
import com.heysafe.app.data.auth.AuthRepository
import com.heysafe.app.data.auth.FirebaseAuthBackend
import com.heysafe.app.data.contacts.ContactsRepository
import com.heysafe.app.data.contacts.FirestoreContactsBackend
import com.heysafe.app.data.vitals.VitalsRepository
import com.heysafe.app.domain.alert.AlertOrchestrator
import com.heysafe.app.domain.alert.DefaultAlertOrchestrator
import com.heysafe.app.domain.alert.WhatsAppLauncher
import com.heysafe.app.domain.audio.AudioRecorder
import com.heysafe.app.domain.audio.SoundAlarmController
import com.heysafe.app.location.LocationProvider

object ServiceLocator {
    @Volatile private var initialized = false
    lateinit var authRepository: AuthRepository
    lateinit var contactsRepository: ContactsRepository
    lateinit var vitalsRepository: VitalsRepository
    lateinit var alertsRepository: AlertsRepository
    lateinit var alertOrchestrator: AlertOrchestrator
    lateinit var soundAlarmController: SoundAlarmController

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appCtx = context.applicationContext
            authRepository = AuthRepository(FirebaseAuthBackend())
            contactsRepository = ContactsRepository(FirestoreContactsBackend())
            vitalsRepository = VitalsRepository()
            alertsRepository = AlertsRepository(FirebaseAlertsBackend())
            alertOrchestrator = DefaultAlertOrchestrator(
                context = appCtx,
                auth = authRepository,
                contactsRepo = contactsRepository,
                alertsRepo = alertsRepository,
                locationProvider = LocationProvider(appCtx),
                audioRecorder = AudioRecorder(appCtx),
                whatsAppLauncher = WhatsAppLauncher(appCtx),
            )
            soundAlarmController = SoundAlarmController(appCtx)
            initialized = true
        }
    }
}
