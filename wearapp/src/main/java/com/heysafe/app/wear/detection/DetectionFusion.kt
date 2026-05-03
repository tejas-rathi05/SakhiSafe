package com.heysafe.app.wear.detection

object DetectionFusion {
    /**
     * Returns trigger source label, or null if SOS should not fire.
     *
     * ML alone never fires SOS — it acts as a corroborating signal that upgrades
     * a heuristic trigger to "both" (highest confidence). The heuristic is the
     * sole safety-net trigger because it has well-tuned thresholds + a 10s
     * sustained window, while the on-device ML feature pipeline currently has a
     * mismatch with the training feature pipeline (motion window is 6s of 10Hz
     * raw samples vs. 60s of 1Hz averages in training), which biases scores
     * upward and would cause spurious alerts if treated as a standalone trigger.
     */
    fun fuse(heuristic: Boolean, mlScore: Float, mlThreshold: Float = 0.75f): String? {
        val ml = mlScore >= mlThreshold
        return when {
            heuristic && ml -> "both"
            heuristic -> "heuristic"
            else -> null
        }
    }
}
