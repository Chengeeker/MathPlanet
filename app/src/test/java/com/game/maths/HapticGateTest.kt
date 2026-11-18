package com.game.maths

import org.junit.Assert.*
import org.junit.Test

class HapticGateTest {
    @Test fun firstTouchVibratesOnlyOnce() {
        val gate = HapticGate(120L)
        assertTrue(gate.tryAcquire(1000L))
        assertFalse(gate.tryAcquire(1000L))
        assertFalse(gate.tryAcquire(1050L))
        assertFalse(gate.tryAcquire(1119L))
        assertTrue(gate.tryAcquire(1120L))
    }

    @Test fun longIntervalAndClockResetAllowFeedback() {
        val gate = HapticGate(120L)
        assertTrue(gate.tryAcquire(100L))
        assertTrue(gate.tryAcquire(300L))
        // Uptime / clock transitions cannot permanently prevent a new vibration.
        assertTrue(gate.tryAcquire(0L))
        assertFalse(gate.tryAcquire(0L))
    }

    @Test fun disabledIntervalDoesNotThrottle() {
        val gate = HapticGate(0L)
        assertTrue(gate.tryAcquire(4L))
        assertTrue(gate.tryAcquire(4L))
    }
}
