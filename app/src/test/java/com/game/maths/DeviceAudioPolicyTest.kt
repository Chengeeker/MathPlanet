package com.game.maths

import org.junit.Assert.*
import org.junit.Test

class DeviceAudioPolicyTest {
    @Test fun tabletsUsePcmForInitialAudioPreferenceButPhonesKeepTheirPreference() {
        assertTrue(DeviceAudioPolicy.isTablet(800, 4))
        assertTrue(DeviceAudioPolicy.isTablet(590, 3))
        assertFalse(DeviceAudioPolicy.isTablet(392, 2))
        assertTrue(DeviceAudioPolicy.usePcmOnUpgrade(tablet = true, migrated = false))
        assertFalse(DeviceAudioPolicy.usePcmOnUpgrade(tablet = true, migrated = true))
        assertFalse(DeviceAudioPolicy.usePcmOnUpgrade(tablet = false, migrated = false))
    }

    @Test fun tabletPcmPrebufferIsLargeEnoughForStableSoftwarePlayback() {
        val target = DeviceAudioPolicy.streamBufferBytes(16384)
        assertTrue(target >= 48000 * 4 * 3 / 4)
        assertEquals(0, target % 4)
        val start = DeviceAudioPolicy.initialBytesForPrebuffer(target, 600000)
        assertTrue(start > 100000)
        assertTrue(start < target)
        assertEquals(0, start % 4)
        assertEquals(0, DeviceAudioPolicy.initialBytesForPrebuffer(target, 0))
        assertEquals(2048, DeviceAudioPolicy.initialBytesForPrebuffer(target, 2048))
        assertEquals(0, DeviceAudioPolicy.streamBufferBytes(1) % 4)
    }
}
