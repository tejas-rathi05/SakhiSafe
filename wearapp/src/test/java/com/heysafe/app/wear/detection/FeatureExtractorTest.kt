package com.heysafe.app.wear.detection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeatureExtractorTest {
    @Test fun `produces 8 features for valid input`() {
        val hr = FloatArray(60) { 70f + it }
        val mo = FloatArray(60) { 1f }
        val f = FeatureExtractor.extract(hr, mo)
        assertEquals(8, f?.size)
    }

    @Test fun `returns null for too-short windows`() {
        assertNull(FeatureExtractor.extract(FloatArray(3), FloatArray(3)))
        assertNull(FeatureExtractor.extract(FloatArray(60), FloatArray(3)))
        assertNull(FeatureExtractor.extract(FloatArray(3), FloatArray(60)))
    }

    @Test fun `hr_mean equals arithmetic mean`() {
        val hr = floatArrayOf(60f, 70f, 80f, 90f, 100f)
        val f = FeatureExtractor.extract(hr, FloatArray(5) { 1f })!!
        assertEquals(80f, f[0], absoluteTolerance = 1e-3f)
    }

    @Test fun `hr_range equals max minus min`() {
        val hr = floatArrayOf(60f, 70f, 80f, 90f, 100f)
        val f = FeatureExtractor.extract(hr, FloatArray(5) { 1f })!!
        assertEquals(40f, f[2], absoluteTolerance = 1e-3f)
    }

    @Test fun `hr_slope is positive when HR rises linearly`() {
        val hr = FloatArray(20) { it.toFloat() } // 0,1,2,...,19 — slope = 1
        val f = FeatureExtractor.extract(hr, FloatArray(20) { 1f })!!
        assertEquals(1f, f[3], absoluteTolerance = 1e-2f)
    }

    @Test fun `motion_max returns highest motion sample`() {
        val mo = floatArrayOf(1f, 2f, 7f, 3f, 1f)
        val f = FeatureExtractor.extract(FloatArray(5) { 70f }, mo)!!
        assertEquals(7f, f[6], absoluteTolerance = 1e-3f)
    }

    @Test fun `peak_count counts samples above mean+std`() {
        // mean=3, std~2.53 → threshold ~5.53 → only the 7 counts = 1 peak
        val mo = floatArrayOf(1f, 1f, 5f, 1f, 7f)
        val f = FeatureExtractor.extract(FloatArray(5) { 70f }, mo)!!
        assertEquals(1f, f[7], absoluteTolerance = 1e-3f)
    }
}
