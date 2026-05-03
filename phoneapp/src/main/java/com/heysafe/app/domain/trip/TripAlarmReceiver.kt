package com.heysafe.app.domain.trip

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.heysafe.app.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires when a trip's deadline passes. Updates the Firestore trip doc to
 * "sos-fired" and triggers the auto-SOS pipeline. Works even if the app
 * process was reaped between trip start and deadline (within typical
 * `setAndAllowWhileIdle` drift).
 */
class TripAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tripId = intent.getStringExtra(EXTRA_TRIP_ID).orEmpty()
        Log.w(TAG, "Trip alarm fired tripId=$tripId — auto-SOS")

        // Make sure the service locator is initialized in case the receiver
        // booted the process from a cold start.
        runCatching { ServiceLocator.init(context.applicationContext) }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            runCatching { ServiceLocator.tripsRepository.setStatus(tripId, "sos-fired") }
            runCatching { ServiceLocator.alertOrchestrator.onAutoAlert("trip-overdue") }
                .onFailure { Log.e(TAG, "Auto-alert from receiver failed", it) }
        }
    }

    companion object {
        private const val TAG = "HeySafe.TripAlarm"
        const val ACTION_FIRE = "com.heysafe.app.action.TRIP_ALARM_FIRE"
        const val EXTRA_TRIP_ID = "tripId"
        private const val REQUEST_CODE = 7301

        fun schedule(context: Context, tripId: String, atMillis: Long) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending(context, tripId))
        }

        fun cancel(context: Context, tripId: String) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pending(context, tripId))
        }

        private fun pending(context: Context, tripId: String): PendingIntent {
            val intent = Intent(context, TripAlarmReceiver::class.java).apply {
                action = ACTION_FIRE
                putExtra(EXTRA_TRIP_ID, tripId)
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
