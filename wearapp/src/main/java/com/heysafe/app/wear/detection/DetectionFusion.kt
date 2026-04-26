package com.heysafe.app.wear.detection

object DetectionFusion {
    /** Returns trigger source label, or null if neither path fired. */
    fun fuse(heuristic: Boolean, mlScore: Float, mlThreshold: Float = 0.75f): String? {
        val ml = mlScore >= mlThreshold
        return when {
            heuristic && ml -> "both"
            heuristic -> "heuristic"
            ml -> "ml"
            else -> null
        }
    }
}
