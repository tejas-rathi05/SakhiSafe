package com.heysafe.app.data.trips

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Firestore-backed trip storage so the guardian dashboard can subscribe live. */
class TripsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val col = db.collection("trips")

    suspend fun create(trip: Trip): String {
        val data = mapOf(
            "userId" to trip.userId,
            "userName" to trip.userName,
            "destinationLabel" to trip.destinationLabel,
            "startedAt" to FieldValue.serverTimestamp(),
            "deadlineAt" to trip.deadlineAt,
            "status" to trip.status,
        )
        return runCatching { col.add(data).await().id }.getOrDefault("")
    }

    suspend fun setStatus(tripId: String, status: String) {
        if (tripId.isBlank()) return
        runCatching {
            col.document(tripId).update(
                mapOf(
                    "status" to status,
                    "endedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }
}
