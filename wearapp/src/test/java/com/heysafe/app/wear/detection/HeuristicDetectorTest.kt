package com.heysafe.app.wear.detection

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeuristicDetectorTest {
    private val cfg = DetectorConfig(hrSpikeBpm = 30f, motionVarianceThreshold = 3f, sustainedSeconds = 5)

    @Test fun `does not fire when only hr spikes`() {
        val d = HeuristicDetector(cfg)
        repeat(10) { d.feed(hr = 110f, baseline = 70f, motion = 0.5f, ts = it * 1000L) }
        assertFalse(d.fired)
    }

    @Test fun `does not fire when only motion is high`() {
        val d = HeuristicDetector(cfg)
        repeat(10) { d.feed(hr = 75f, baseline = 70f, motion = 5f, ts = it * 1000L) }
        assertFalse(d.fired)
    }

    @Test fun `fires when both conditions hold for sustainedSeconds`() {
        val d = HeuristicDetector(cfg)
        repeat(6) { d.feed(hr = 110f, baseline = 70f, motion = 5f, ts = it * 1000L) }
        assertTrue(d.fired)
    }

    @Test fun `resets if either condition drops`() {
        val d = HeuristicDetector(cfg)
        repeat(3) { d.feed(110f, 70f, 5f, it * 1000L) }
        d.feed(110f, 70f, 0.5f, 4000L) // motion drops
        repeat(3) { d.feed(110f, 70f, 5f, (5 + it) * 1000L) }
        assertFalse(d.fired) // < 5s sustained after reset
    }

    @Test fun `reset clears fired and streak`() {
        val d = HeuristicDetector(cfg)
        repeat(6) { d.feed(110f, 70f, 5f, it * 1000L) }
        assertTrue(d.fired)
        d.reset()
        assertFalse(d.fired)
    }
}
