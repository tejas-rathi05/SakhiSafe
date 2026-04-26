package com.heysafe.app.wear.sensors

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.heysafe.app.wear.detection.DetectionFusion
import com.heysafe.app.wear.detection.DetectorConfig
import com.heysafe.app.wear.detection.FeatureExtractor
import com.heysafe.app.wear.detection.HeuristicDetector
import com.heysafe.app.wear.detection.MlDetector
import com.heysafe.app.wear.presentation.SosActivity
import com.heysafe.app.wear.transport.DataLayerSender
import com.heysafe.app.wear.util.RollingWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SensorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val hrWindow = RollingWindow(300)      // 5 min @ 1 Hz baseline
    private val motionWindow = RollingWindow(50)   // ~5 sec @ 10 Hz
    private val mlHrWindow = RollingWindow(60)        // 60 sec @ 1 Hz HR
    private val mlMotionWindow = RollingWindow(60)    // ~6 sec @ 10 Hz motion (rolling sample)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        val hr = HeartRateCollector(this).samples()
        val motion = MotionCollector(this).samples()
        val sender = DataLayerSender(this)
        val detector = HeuristicDetector(DetectorConfig())
        val mlDetector = MlDetector(this)

        scope.launch {
            motion.collectLatest { m ->
                motionWindow.add(m)
                mlMotionWindow.add(m)
            }
        }
        scope.launch {
            hr.collectLatest { v ->
                hrWindow.add(v)
                mlHrWindow.add(v)
                val baseline = hrWindow.median()
                val motionVar = motionWindow.variance()
                val ts = System.currentTimeMillis()
                runCatching {
                    sender.sendVitals(v, baseline, motionVar, ts)
                }
                detector.feed(v, baseline, motionVar, ts)
                // SOS launch now happens in the fusion loop below — do NOT launch from here.
            }
        }
        scope.launch {
            while (isActive) {
                delay(5_000)
                val hrArr = mlHrWindow.snapshot().toFloatArray()
                val moArr = mlMotionWindow.snapshot().toFloatArray()
                val features = FeatureExtractor.extract(hrArr, moArr)
                val mlScore = if (features != null) {
                    runCatching { mlDetector.predict(features) }.getOrDefault(0f)
                } else 0f
                val source = DetectionFusion.fuse(heuristic = detector.fired, mlScore = mlScore)
                if (source != null) {
                    android.util.Log.i("HeySafe", "Fusion fired: source=$source mlScore=$mlScore")
                    detector.reset()
                    val intent = Intent(this@SensorService, SosActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("triggerSource", source)
                        .putExtra("hrWindow", hrArr)
                        .putExtra("motionWindow", moArr)
                    startActivity(intent)
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
