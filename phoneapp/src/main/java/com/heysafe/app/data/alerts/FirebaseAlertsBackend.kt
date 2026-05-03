package com.heysafe.app.data.alerts

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAlertsBackend(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AlertsBackend {
    private val col = db.collection("alerts")

    override suspend fun create(alert: Alert): String {
        val data = mapOf(
            "userId" to alert.userId,
            "userName" to alert.userName,
            "triggerSource" to alert.triggerSource,
            "status" to alert.status,
            "createdAt" to FieldValue.serverTimestamp(),
            "location" to alert.location?.let {
                mapOf("lat" to it.lat, "lng" to it.lng, "accuracy" to it.accuracy)
            },
            "hrWindow" to alert.hrWindow,
            "motionWindow" to alert.motionWindow,
            "contactsNotified" to alert.contactsNotified,
            "audioBase64" to alert.audioBase64,
        )
        val ref = col.add(data).await()
        return ref.id
    }

    override suspend fun setAudioBase64(alertId: String, base64: String) {
        col.document(alertId).update("audioBase64", base64).await()
    }

    override suspend fun resolve(alertId: String) {
        col.document(alertId).update(
            mapOf(
                "status" to "resolved",
                "resolvedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
    }
}
