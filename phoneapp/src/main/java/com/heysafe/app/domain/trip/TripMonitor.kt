package com.heysafe.app.domain.trip

import android.content.Context
import android.util.Log
import com.heysafe.app.data.auth.AuthRepository
import com.heysafe.app.data.trips.Trip
import com.heysafe.app.data.trips.TripsRepository
import com.heysafe.app.domain.alert.AlertOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TripState(
    val tripId: String,
    val destinationLabel: String,
    val startedAt: Long,
    val etaMillis: Long,
    val deadlineMillis: Long,
    val now: Long = System.currentTimeMillis(),
)

class TripMonitor(
    private val context: Context,
    private val orchestrator: AlertOrchestrator,
    private val tripsRepo: TripsRepository,
    private val auth: AuthRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _trip = MutableStateFlow<TripState?>(null)
    val trip: StateFlow<TripState?> = _trip.asStateFlow()
    private var watcher: Job? = null

    fun start(destinationLabel: String, etaMillis: Long) {
        cancelInternal()
        scope.launch {
            val now = System.currentTimeMillis()
            val deadline = now + etaMillis
            val user = auth.currentUser()
            val tripId = tripsRepo.create(
                Trip(
                    userId = user?.uid ?: "anon",
                    userName = user?.email,
                    destinationLabel = destinationLabel,
                    startedAt = now,
                    deadlineAt = deadline,
                ),
            )
            _trip.value = TripState(
                tripId = tripId,
                destinationLabel = destinationLabel,
                startedAt = now,
                etaMillis = etaMillis,
                deadlineMillis = deadline,
            )
            // Schedule the durable alarm so the SOS fires even if the app process is reaped.
            TripAlarmReceiver.schedule(context, tripId, deadline)
            Log.i(TAG, "Trip started: $destinationLabel · eta ${etaMillis / 60_000} min · id=$tripId · alarm scheduled")

            watcher = launch {
                while (isActive) {
                    val current = _trip.value ?: break
                    val now2 = System.currentTimeMillis()
                    _trip.value = current.copy(now = now2)
                    if (now2 >= current.deadlineMillis) {
                        // Alarm receiver may also fire — both paths are idempotent enough
                        // (Firestore status flips, orchestrator alert is the visible thing).
                        // Clear local state; let the receiver handle the actual SOS so
                        // we don't double-fire.
                        _trip.value = null
                        break
                    }
                    delay(1_000)
                }
            }
        }
    }

    fun arrived() {
        val current = _trip.value
        Log.i(TAG, "Trip marked arrived")
        current?.let { TripAlarmReceiver.cancel(context, it.tripId) }
        scope.launch { runCatching { tripsRepo.setStatus(current?.tripId.orEmpty(), "arrived") } }
        cancelInternal()
    }

    private fun cancelInternal() {
        watcher?.cancel()
        watcher = null
        _trip.value = null
    }

    companion object {
        private const val TAG = "HeySafe.TripMonitor"
    }
}
