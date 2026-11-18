package com.game.maths

import org.junit.Assert.*
import org.junit.Test

class PcmResamplerTest {
    private fun encode(vararg values: Int): ByteArray {
        val result = ByteArray(values.size * 2)
        values.forEachIndexed { index, value ->
            result[index * 2] = (value and 0xff).toByte()
            result[index * 2 + 1] = (value shr 8 and 0xff).toByte()
        }
        return result
    }

    private fun decodeAt(bytes: ByteArray, offset: Int): Int {
        val value = (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8)
        return value.toShort().toInt()
    }

    @Test fun triplesFrameRateAndDuplicatesChannels() {
        val result = PcmResampler.toStereo48k(
            encode(0, 1000, -2000, 32767, -32768, 0)
        )
        assertEquals(6 * 3 * 4, result.size)
        for (offset in result.indices step 4) {
            assertEquals(decodeAt(result, offset), decodeAt(result, offset + 2))
        }
        assertEquals(48_000, PcmResampler.OUTPUT_SAMPLE_RATE)
        assertEquals(2, PcmResampler.OUTPUT_CHANNELS)
    }

    @Test fun interpolationDoesNotOverflowOrClipAndAppliesAttenuation() {
        val fullVolume = encode(*IntArray(1500) { if (it % 2 == 0) 32767 else -32768 })
        val output = PcmResampler.toStereo48k(fullVolume)
        var peak = 0
        for (offset in output.indices step 4) {
            peak = maxOf(peak, kotlin.math.abs(decodeAt(output, offset)))
        }
        assertTrue("should produce non-silent PCM", peak > 1000)
        assertTrue("0.72 gain should provide a safety margin", peak < 25000)
        assertEquals(0, decodeAt(output, 0))
        assertEquals(0, decodeAt(output, output.size - 4))
    }

    @Test fun emptyInputRemainsEmptyAndOddByteCountIsRejected() {
        assertArrayEquals(byteArrayOf(), PcmResampler.toStereo48k(byteArrayOf()))
        try {
            PcmResampler.toStereo48k(byteArrayOf(1, 2, 3))
            fail("Odd input length must be rejected")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun outputLengthMatchesTimeForOneSecondOfNarration() {
        val input = ByteArray(16_000 * 2)
        val output = PcmResampler.toStereo48k(input)
        assertEquals(48_000 * 2 * 2, output.size)
        assertTrue(output.all { it == 0.toByte() })
    }

    @Test fun playbackChoiceDefaultsToSoftwarePcmAndSurvivesPersistence() {
        assertEquals(VoicePlaybackMode.DIRECT_PCM, GameSettings().voicePlaybackMode)
        assertEquals(VoicePlaybackMode.DIRECT_PCM, VoicePlaybackMode.fromStored(null))
        assertEquals(VoicePlaybackMode.DIRECT_PCM, VoicePlaybackMode.fromStored("BAD_VALUE"))
        assertEquals(VoicePlaybackMode.SYSTEM_MEDIA, VoicePlaybackMode.fromStored("SYSTEM_MEDIA"))
    }
}
