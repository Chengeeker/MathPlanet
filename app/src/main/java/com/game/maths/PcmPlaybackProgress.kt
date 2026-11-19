package com.game.maths

/**
 * Tracks played frames for a reusable AudioTrack. Some vendor audio HALs reset
 * the unsigned 32-bit playback-head counter across stop/play. Treat an
 * implausibly large jump as a counter reset, not a completed utterance.
 */
internal class PcmPlaybackProgress(startPosition: Long) {
    private var previous = startPosition and MASK
    var playedFrames: Long = 0L
        private set
    var counterResets: Int = 0
        private set

    /** True only when the hardware head moved forward by a plausible amount. */
    fun observe(position: Long): Boolean {
        val now = position and MASK
        val delta = (now - previous) and MASK
        previous = now
        return when {
            delta == 0L -> false
            delta <= MAX_POLL_FRAMES -> {
                playedFrames += delta
                true
            }
            else -> {
                counterResets++
                false
            }
        }
    }

    companion object {
        private const val MASK = 0xffffffffL
        // Polling every 10ms; tolerate pauses up to 2s, but not a counter reset.
        private const val MAX_POLL_FRAMES = 96_000L
    }
}
