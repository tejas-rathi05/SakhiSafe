package com.heysafe.app.domain.alert

import android.content.Context
import android.util.Base64
import android.util.Log
import com.heysafe.app.data.alerts.Alert
import com.heysafe.app.data.alerts.AlertsRepository
import com.heysafe.app.data.alerts.GeoPoint
import com.heysafe.app.data.auth.AuthRepository
import com.heysafe.app.data.contacts.ContactsRepository
import com.heysafe.app.domain.audio.AudioRecorder
import com.heysafe.app.location.LocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AlertProgress(
    val alertId: String? = null,
    val gpsCaptured: Boolean = false,
    val audioRecording: Boolean = false,
    val contactsSent: List<String> = emptyList(),
    val contactsPending: List<String> = emptyList(),
    val resolved: Boolean = false,
    val errorMessage: String? = null,
)

interface AlertOrchestrator {
    val progress: StateFlow<AlertProgress>
    suspend fun onWearAlert(triggerSource: String, hrWindow: FloatArray, motionWindow: FloatArray)
    suspend fun onManualAlert()
    suspend fun resolve()
    fun clear()
}

class DefaultAlertOrchestrator(
    private val context: Context,
    private val auth: AuthRepository,
    private val contactsRepo: ContactsRepository,
    private val alertsRepo: AlertsRepository,
    private val locationProvider: LocationProvider,
    private val audioRecorder: AudioRecorder,
    private val whatsAppLauncher: WhatsAppLauncher,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : AlertOrchestrator {

    private val _progress = MutableStateFlow(AlertProgress())
    override val progress: StateFlow<AlertProgress> = _progress

    private var currentAlertId: String? = null

    override suspend fun onWearAlert(triggerSource: String, hrWindow: FloatArray, motionWindow: FloatArray) =
        runAlert(triggerSource, hrWindow.toList(), motionWindow.toList())

    override suspend fun onManualAlert() = runAlert("manual", emptyList(), emptyList())

    override suspend fun resolve() {
        currentAlertId?.let { alertsRepo.resolve(it) }
        _progress.value = _progress.value.copy(resolved = true)
        currentAlertId = null
    }

    override fun clear() {
        _progress.value = AlertProgress()
        currentAlertId = null
    }

    private suspend fun runAlert(triggerSource: String, hrWindow: List<Float>, motionWindow: List<Float>) {
        val user = auth.currentUser() ?: run {
            _progress.value = AlertProgress(errorMessage = "Not signed in"); return
        }
        val contacts = runCatching { contactsRepo.observe(user.uid).first() }.getOrDefault(emptyList())
        _progress.value = AlertProgress(contactsPending = contacts.map { it.name })

        // 1. GPS
        val loc = locationProvider.currentLocation()
        val geo = loc?.let { GeoPoint(it.latitude, it.longitude, it.accuracy) }
        _progress.value = _progress.value.copy(gpsCaptured = geo != null)

        // 2. Start audio
        val audioFile = runCatching { audioRecorder.start() }.getOrNull()
        _progress.value = _progress.value.copy(audioRecording = audioFile != null)

        // 3. Create alert doc
        val alertId = alertsRepo.create(
            Alert(
                userId = user.uid,
                userName = user.email,
                triggerSource = triggerSource,
                location = geo,
                hrWindow = hrWindow,
                motionWindow = motionWindow,
                contactsNotified = contacts.map { it.phone },
            )
        ).getOrNull() ?: run {
            _progress.value = _progress.value.copy(errorMessage = "Failed to create alert")
            // try to stop audio so we don't leak the recorder
            runCatching { audioRecorder.stop() }
            return
        }
        currentAlertId = alertId
        _progress.value = _progress.value.copy(alertId = alertId)

        // 4. Fan-out WhatsApp — open first contact, queue rest visible in UI
        if (contacts.isNotEmpty() && geo != null) {
            val msg = whatsAppLauncher.composeMessage(user.email, geo.lat, geo.lng)
            val first = contacts.first()
            whatsAppLauncher.launch(first.phone, msg)
            _progress.value = _progress.value.copy(
                contactsSent = listOf(first.name),
                contactsPending = contacts.drop(1).map { it.name },
            )
        } else if (contacts.isEmpty()) {
            Log.w("HeySafe", "Alert created but no emergency contacts to notify")
        }

        // 5. Wait 30s, stop audio, base64-encode, patch onto alert doc
        scope.launch {
            delay(30_000)
            val file = runCatching { audioRecorder.stop() }.getOrNull()
            if (file != null && file.exists()) {
                val bytes = file.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                runCatching { alertsRepo.setAudioBase64(alertId, base64) }
            }
        }
    }
}
