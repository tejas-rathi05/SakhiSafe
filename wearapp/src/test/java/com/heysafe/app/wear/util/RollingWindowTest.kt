package com.heysafe.app.wear.util

import kotlin.test.Test
import kotlin.test.assertEquals

class RollingWindowTest {
    @Test fun `add stores up to capacity then drops oldest`() {
        val w = RollingWindow(capacity = 3)
        w.add(1f); w.add(2f); w.add(3f); w.add(4f)
        assertEquals(listOf(2f, 3f, 4f), w.snapshot())
    }

    @Test fun `median of empty returns 0`() {
        assertEquals(0f, RollingWindow(3).median())
    }

    @Test fun `median odd`() {
        val w = RollingWindow(5).apply { add(3f); add(1f); add(2f) }
        assertEquals(2f, w.median())
    }

    @Test fun `median even averages middle two`() {
        val w = RollingWindow(4).apply { add(1f); add(2f); add(3f); add(4f) }
        assertEquals(2.5f, w.median())
    }

    @Test fun `mean`() {
        val w = RollingWindow(4).apply { add(1f); add(2f); add(3f); add(4f) }
        assertEquals(2.5f, w.mean())
    }

    @Test fun `variance`() {
        val w = RollingWindow(4).apply { add(1f); add(2f); add(3f); add(4f) }
        // mean=2.5, variance = ((1.5)^2 + (0.5)^2 + (0.5)^2 + (1.5)^2)/4 = 1.25
        assertEquals(1.25f, w.variance(), absoluteTolerance = 1e-4f)
    }

    @Test fun `min and max`() {
        val w = RollingWindow(4).apply { add(3f); add(1f); add(4f); add(2f) }
        assertEquals(1f, w.min())
        assertEquals(4f, w.max())
    }

    @Test fun `size reports current element count`() {
        val w = RollingWindow(5)
        assertEquals(0, w.size())
        w.add(1f); w.add(2f)
        assertEquals(2, w.size())
        w.add(3f); w.add(4f); w.add(5f); w.add(6f)
        assertEquals(5, w.size())
    }
}
