package com.heysafe.app.wear.detection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DetectionFusionTest {
    @Test fun `neither fired returns null`() {
        assertNull(DetectionFusion.fuse(heuristic = false, mlScore = 0.5f, mlThreshold = 0.75f))
    }

    @Test fun `only heuristic returns heuristic`() {
        assertEquals("heuristic", DetectionFusion.fuse(true, 0.1f, 0.75f))
    }

    @Test fun `ml alone does not fire`() {
        // ML is a corroborating signal only — never fires SOS without the heuristic.
        assertNull(DetectionFusion.fuse(false, 0.9f, 0.75f))
    }

    @Test fun `both returns both`() {
        assertEquals("both", DetectionFusion.fuse(true, 0.9f, 0.75f))
    }

    @Test fun `ml at threshold without heuristic still does not fire`() {
        assertNull(DetectionFusion.fuse(false, 0.75f, 0.75f))
    }

    @Test fun `ml at threshold with heuristic upgrades to both`() {
        assertEquals("both", DetectionFusion.fuse(true, 0.75f, 0.75f))
    }
}
