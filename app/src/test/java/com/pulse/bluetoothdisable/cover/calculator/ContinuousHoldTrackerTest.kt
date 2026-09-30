package com.pulse.bluetoothdisable.cover.calculator

import com.pulse.bluetoothdisable.cover.ContinuousHoldTracker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuousHoldTrackerTest {
    @Test fun shortHoldsNeverTrigger() {
        for (duration in listOf(1_000L, 5_000L, 6_900L, 6_999L)) {
            val hold = ContinuousHoldTracker()
            hold.start(100)
            assertFalse(hold.advance(100 + duration))
            hold.cancel()
            assertFalse(hold.advance(10_000))
        }
    }

    @Test fun sevenSecondsTriggersExactlyOnceUntilRelease() {
        val hold = ContinuousHoldTracker()
        hold.start(100)
        assertTrue(hold.advance(7_100))
        assertFalse(hold.advance(10_100))
        assertFalse(hold.advance(15_100))
        hold.cancel()
        hold.start(20_000)
        assertTrue(hold.advance(27_000))
    }

    @Test fun separateFourSecondHoldsDoNotAccumulate() {
        val hold = ContinuousHoldTracker()
        hold.start(0)
        assertFalse(hold.advance(4_000))
        hold.cancel()
        hold.start(5_000)
        assertFalse(hold.advance(9_000))
    }
}
