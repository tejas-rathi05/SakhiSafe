package com.heysafe.app.wear.detection

data class DetectorConfig(
    val hrSpikeBpm: Float = 30f,           // hr - baseline must exceed this
    val motionVarianceThreshold: Float = 3.0f,
    val sustainedSeconds: Int = 10,        // both conditions for at least this many seconds
)
