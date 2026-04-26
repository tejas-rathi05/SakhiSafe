package com.heysafe.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class LocationProvider(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    /** Tries a single high-accuracy fix with a 5-second timeout. Falls back to last known location. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? = withTimeoutOrNull(5_000) {
        runCatching {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        }.getOrNull() ?: runCatching { client.lastLocation.await() }.getOrNull()
    }
}
