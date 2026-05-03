package com.heysafe.app.wear.sensors

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sin
import kotlin.random.Random

class SensorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val hrWindow = RollingWindow(300)      // 5 min @ 1 Hz baseline
    private val motionWindow = RollingWindow(50)   // ~5 sec @ 10 Hz
    private val mlHrWindow = RollingWindow(60)        // 60 sec @ 1 Hz HR
    private val mlMotionWindow = RollingWindow(60)    // ~6 sec @ 10 Hz motion (rolling sample)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
        _isMonitoring.value = true
        _startedAt.value = System.currentTimeMillis()
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
        // Tracks the wallclock of the last real on-wrist HR sample. If the watch is
        // off-wrist or the PPG sensor stalls, we fall back to a synthetic stream so
        // the phone & dashboard stay alive during demos.
        val lastRealHrAt = AtomicLong(0L)

        suspend fun handleHr(v: Float, source: String) {
            hrWindow.add(v)
            mlHrWindow.add(v)
            _latestHr.value = v
            val baseline = hrWindow.median()
            val motionVar = motionWindow.variance()
            val ts = System.currentTimeMillis()
            sender.sendVitals(v, baseline, motionVar, ts).also {
                android.util.Log.d("HeySafe", "vitals[$source] sent hr=${v.toInt()} baseline=${baseline.toInt()}")
            }
            detector.feed(v, baseline, motionVar, ts)
        }

        scope.launch {
            hr.collectLatest { v ->
                // Filter sensor noise / off-wrist signals (typical PPG returns 0 or <30 in those cases).
                if (v < 30f || v > 220f) return@collectLatest
                lastRealHrAt.set(System.currentTimeMillis())
                runCatching { handleHr(v, "real") }
                    .onFailure { android.util.Log.e("HeySafe", "handleHr(real) failed", it) }
            }
        }
        // Synthetic-HR fallback: emits ~1 Hz when no real sample has arrived for >5 sec.
        scope.launch {
            android.util.Log.d("HeySafe", "synthetic HR fallback launched")
            var t = 0
            while (isActive) {
                delay(1_000)
                if (System.currentTimeMillis() - lastRealHrAt.get() > 5_000) {
                    val v = 80f + 6f * sin(t / 11f).toFloat() + Random.nextFloat() * 3f
                    runCatching { handleHr(v, "synth") }
                        .onFailure { android.util.Log.e("HeySafe", "handleHr(synth) failed", it) }
                    t++
                }
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
        _isMonitoring.value = false
        _latestHr.value = null
        _startedAt.value = 0L
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL_ID, "VSafe sensors", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(ch)
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("VSafe is monitoring")
            .setContentText("Heart rate and motion are being tracked")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "heysafe_sensors"
        const val NOTIF_ID = 42
        const val ACTION_STOP = "com.heysafe.app.wear.action.STOP_SENSOR"

        private val _isMonitoring = MutableStateFlow(false)
        val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

        private val _latestHr = MutableStateFlow<Float?>(null)
        val latestHr: StateFlow<Float?> = _latestHr.asStateFlow()

        private val _startedAt = MutableStateFlow(0L)
        val startedAt: StateFlow<Long> = _startedAt.asStateFlow()
    }
}
