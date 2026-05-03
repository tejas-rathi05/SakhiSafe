package com.heysafe.app.wear.detection

class HeuristicDetector(private val cfg: DetectorConfig) {
    private var streakStartTs: Long = -1L
    var fired: Boolean = false
        private set

    fun feed(hr: Float, baseline: Float, motion: Float, ts: Long) {
        val hrTriggered = (hr - baseline) > cfg.hrSpikeBpm
        val motionTriggered = motion > cfg.motionVarianceThreshold
        if (hrTriggered && motionTriggered) {
            if (streakStartTs < 0) streakStartTs = ts
            if (!fired && ts - streakStartTs >= cfg.sustainedSeconds * 1000L) fired = true
        } else {
            streakStartTs = -1L
        }
    }

    fun reset() {
        streakStartTs = -1L
        fired = false
    }
}
