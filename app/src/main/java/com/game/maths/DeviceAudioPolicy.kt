package com.game.maths

/**
 * Device size affects audio-backend reliability and initial preference only.
 * Game menu entries must never depend on any device/tablet classification.
 */
internal object DeviceAudioPolicy {
    // Android Configuration.SCREENLAYOUT_SIZE_LARGE == 3, X-LARGE == 4.
    fun isTablet(smallestWidthDp: Int, screenLayoutSize: Int): Boolean =
        smallestWidthDp >= 600 || screenLayoutSize >= 3

    /** One-time preference migration, without overriding future manual changes. */
    fun usePcmOnUpgrade(tablet: Boolean, migrated: Boolean): Boolean =
        tablet && !migrated

    const val PCM_SAMPLE_RATE = 48_000
    const val BYTES_PER_FRAME = 4 // Stereo PCM16
    // Leave extra audio queued for vendor mixer scheduling jitter on tablets.
    const val PREBUFFER_MS = 1_800L

    /** Never replay an already-started narration from its beginning. */
    fun canFallbackAfterFailure(alreadyStarted: Boolean, requestStillCurrent: Boolean): Boolean =
        !alreadyStarted && requestStillCurrent

    fun streamBufferBytes(platformMinBytes: Int): Int =
        maxOf(platformMinBytes.coerceAtLeast(BYTES_PER_FRAME),
            (PCM_SAMPLE_RATE * BYTES_PER_FRAME * PREBUFFER_MS / 1000L).toInt())
            .let { it + (BYTES_PER_FRAME - it % BYTES_PER_FRAME) % BYTES_PER_FRAME }

    fun initialBytesForPrebuffer(bufferBytes: Int, totalBytes: Int): Int =
        minOf(totalBytes, bufferBytes * 3 / 4).let { it - it % BYTES_PER_FRAME }
}
