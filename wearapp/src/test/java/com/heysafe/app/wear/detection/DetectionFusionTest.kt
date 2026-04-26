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

    @Test fun `only ml returns ml`() {
        assertEquals("ml", DetectionFusion.fuse(false, 0.9f, 0.75f))
    }

    @Test fun `both returns both`() {
        assertEquals("both", DetectionFusion.fuse(true, 0.9f, 0.75f))
    }

    @Test fun `ml threshold is exclusive at the boundary`() {
        // mlScore = threshold should fire (>=)
        assertEquals("ml", DetectionFusion.fuse(false, 0.75f, 0.75f))
    }
}
