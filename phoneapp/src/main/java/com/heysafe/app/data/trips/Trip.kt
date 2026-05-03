package com.heysafe.app.data.trips

data class Trip(
    val id: String = "",
    val userId: String,
    val userName: String?,
    val destinationLabel: String,
    val startedAt: Long,
    val deadlineAt: Long,
    val status: String = "active", // active | arrived | sos-fired
)
