package com.game.maths

/**
 * Offline software PCM conversion: 16 kHz mono little-endian signed 16-bit to
 * 48 kHz interleaved stereo PCM16. Integer-rate 3x interpolation avoids depending
 * on the device's 16 kHz output resampler or any compressed-audio hardware codec.
 *
 * The final audio output still uses Android AudioTrack / the system audio mixer.
 */
object PcmResampler {
    const val OUTPUT_SAMPLE_RATE = 48_000
    const val OUTPUT_CHANNELS = 2
    const val INPUT_SAMPLE_RATE = 16_000
    private const val RATIO = OUTPUT_SAMPLE_RATE / INPUT_SAMPLE_RATE
    private const val BYTES_PER_OUTPUT_FRAME = OUTPUT_CHANNELS * 2

    fun toStereo48k(input: ByteArray, gain: Float = 0.72f): ByteArray {
        require(input.size % 2 == 0) { "PCM16 input must contain whole samples" }
        require(gain in 0f..1f) { "Gain must be between 0 and 1" }
        if (input.isEmpty()) return byteArrayOf()
        val inputFrames = input.size / 2
        require(inputFrames <= Int.MAX_VALUE / (RATIO * BYTES_PER_OUTPUT_FRAME))
        val outputFrames = inputFrames * RATIO
        val output = ByteArray(outputFrames * BYTES_PER_OUTPUT_FRAME)
        // Short whole-utterance fade avoids clicks if a sentence ends abruptly.
        val fadeFrames = minOf(240, outputFrames / 2)

        for (i in 0 until inputFrames) {
            val a = signedSample(input, i)
            val b = signedSample(input, minOf(i + 1, inputFrames - 1))
            for (phase in 0 until RATIO) {
                val frame = i * RATIO + phase
                val interpolated = (a * (RATIO - phase) + b * phase) / RATIO
                val fade = if (fadeFrames > 0) minOf(
                    1f,
                    frame.toFloat() / fadeFrames,
                    (outputFrames - 1 - frame).toFloat() / fadeFrames
                ) else 1f
                val scaled = (interpolated * gain * fade).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                val offset = frame * BYTES_PER_OUTPUT_FRAME
                val lo = (scaled and 0xff).toByte()
                val hi = ((scaled shr 8) and 0xff).toByte()
                output[offset] = lo
                output[offset + 1] = hi
                output[offset + 2] = lo
                output[offset + 3] = hi
            }
        }
        return output
    }

    private fun signedSample(input: ByteArray, index: Int): Int {
        val offset = index * 2
        val unsigned = (input[offset].toInt() and 0xff) or
            ((input[offset + 1].toInt() and 0xff) shl 8)
        return unsigned.toShort().toInt()
    }
}
