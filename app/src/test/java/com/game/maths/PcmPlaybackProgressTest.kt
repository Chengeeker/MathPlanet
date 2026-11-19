package com.game.maths

import org.junit.Assert.*
import org.junit.Test

class PcmPlaybackProgressTest {
    @Test fun tracksAWholeLongUtteranceInSmallHardwareSteps() {
        val monitor = PcmPlaybackProgress(0)
        // Simulate 15s of audio at 48kHz while writing far beyond a 1.8s buffer.
        repeat(750) { index ->
            assertTrue(monitor.observe((index + 1L) * 960L))
        }
        assertEquals(720_000L, monitor.playedFrames)
        assertEquals(0, monitor.counterResets)
    }

    @Test fun vendorCounterResetDoesNotMarkSentenceAsCompleted() {
        val monitor = PcmPlaybackProgress(150_000)
        assertTrue(monitor.observe(150_960))
        assertFalse(monitor.observe(0))
        assertEquals(960L, monitor.playedFrames)
        assertEquals(1, monitor.counterResets)
        assertTrue(monitor.observe(960))
        assertEquals(1920L, monitor.playedFrames)
    }

    @Test fun normalUnsigned32BitWrapIsStillForwardProgress() {
        val monitor = PcmPlaybackProgress(0xfffffe00L)
        assertTrue(monitor.observe(0x00000100L))
        assertEquals(768L, monitor.playedFrames)
        assertEquals(0, monitor.counterResets)
    }

    @Test fun stationaryOutputNeverAdvancesAndCounterJumpIsIgnored() {
        val monitor = PcmPlaybackProgress(400L)
        repeat(200) { assertFalse(monitor.observe(400L)) }
        assertFalse(monitor.observe(120_000L))
        assertEquals(0L, monitor.playedFrames)
        assertEquals(1, monitor.counterResets)
    }
}
