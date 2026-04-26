package com.heysafe.app.wear.sensors

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.heysafe.app.wear.transport.DataLayerSender
import com.heysafe.app.wear.util.RollingWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SensorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val hrWindow = RollingWindow(300)      // 5 min @ 1 Hz baseline
    private val motionWindow = RollingWindow(50)   // ~5 sec @ 10 Hz

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        val hr = HeartRateCollector(this).samples()
        val motion = MotionCollector(this).samples()
        val sender = DataLayerSender(this)

        scope.launch {
            motion.collectLatest { m -> motionWindow.add(m) }
        }
        scope.launch {
            hr.collectLatest { v ->
                hrWindow.add(v)
                val baseline = hrWindow.median()
                val motionVar = motionWindow.variance()
                runCatching {
                    sender.sendVitals(v, baseline, motionVar, System.currentTimeMillis())
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL_ID, "HeySafe sensors", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(ch)
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("HeySafe is monitoring")
            .setContentText("Heart rate and motion are being tracked")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "heysafe_sensors"
        const val NOTIF_ID = 42
    }
}
