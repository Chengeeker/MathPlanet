package com.game.maths

import org.junit.Assert.*
import org.junit.Test

class StartupSpeechGateTest {
    @Test fun initialNarrationIsDeferredUntilResume() {
        val gate = StartupSpeechGate()
        assertNull(gate.submit(listOf("welcome"), foreground = false))
        assertTrue(gate.hasPending())
        assertEquals(listOf("welcome"), gate.onForeground())
        assertFalse(gate.hasPending())
    }

    @Test fun earlyPlayRequestReplacesWelcomeWithRelevantQuestion() {
        val gate = StartupSpeechGate()
        gate.submit(listOf("welcome"), foreground = false)
        gate.submit(listOf("q_begin", "n_1", "q_equal"), foreground = false)
        assertEquals(listOf("q_begin", "n_1", "q_equal"), gate.onForeground())
    }

    @Test fun resumingConsumesRequestExactlyOnce() {
        val gate = StartupSpeechGate()
        gate.submit(listOf("test"), foreground = false)
        assertEquals(listOf("test"), gate.onForeground())
        assertNull(gate.onForeground())
    }

    @Test fun foregroundNarrationDoesNotLeaveOldDeferredRequest() {
        val gate = StartupSpeechGate()
        gate.submit(listOf("welcome"), foreground = false)
        assertEquals(listOf("good"), gate.submit(listOf("good"), foreground = true))
        assertNull(gate.onForeground())
    }

    @Test fun disablingOrLeavingClearsPendingSpeech() {
        val gate = StartupSpeechGate()
        gate.submit(listOf("q_begin"), foreground = false)
        gate.clear()
        assertNull(gate.onForeground())
        assertFalse(gate.hasPending())
    }

    @Test fun emptyRequestDoesNotReplacePreviousSpeech() {
        val gate = StartupSpeechGate()
        gate.submit(listOf("q_begin"), foreground = false)
        assertNull(gate.submit(emptyList(), foreground = false))
        assertEquals(listOf("q_begin"), gate.onForeground())
    }
}
