package com.heysafe.app.data.alerts

data class GeoPoint(val lat: Double, val lng: Double, val accuracy: Float)

data class Alert(
    val id: String = "",
    val userId: String,
    val userName: String,
    val triggerSource: String,
    val status: String = "active",                  // "active" | "resolved"
    val createdAtMs: Long = System.currentTimeMillis(),
    val location: GeoPoint? = null,
    val hrWindow: List<Float> = emptyList(),
    val motionWindow: List<Float> = emptyList(),
    val audioBase64: String? = null,                // populated after the 30-sec recording finishes
    val contactsNotified: List<String> = emptyList(),
)
