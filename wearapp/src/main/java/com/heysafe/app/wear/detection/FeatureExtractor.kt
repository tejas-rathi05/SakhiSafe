package com.heysafe.app.wear.detection

import kotlin.math.sqrt

object FeatureExtractor {
    /**
     * Returns the same 8 features used in WESAD training, or null if either window
     * has fewer than 5 samples.
     *
     * Feature order: [hr_mean, hr_std, hr_range, hr_slope, motion_mean, motion_std, motion_max, peak_count]
     */
    fun extract(hr: FloatArray, motion: FloatArray): FloatArray? {
        if (hr.size < 5 || motion.size < 5) return null

        val hrMean = hr.average().toFloat()
        val hrStd = sqrt(hr.map { (it - hrMean) * (it - hrMean) }.average()).toFloat()
        val hrRange = (hr.maxOrNull() ?: 0f) - (hr.minOrNull() ?: 0f)

        // Linear regression slope of HR vs index
        val n = hr.size
        val xMean = (n - 1) / 2f
        var num = 0.0
        var den = 0.0
        for (i in 0 until n) {
            val dx = i - xMean
            num += dx * (hr[i] - hrMean)
            den += dx * dx
        }
        val hrSlope = if (den == 0.0) 0f else (num / den).toFloat()

        val moMean = motion.average().toFloat()
        val moStd = sqrt(motion.map { (it - moMean) * (it - moMean) }.average()).toFloat()
        val moMax = motion.maxOrNull() ?: 0f
        val threshold = moMean + moStd
        val peakCount = motion.count { it > threshold }.toFloat()

        return floatArrayOf(hrMean, hrStd, hrRange, hrSlope, moMean, moStd, moMax, peakCount)
    }
}
