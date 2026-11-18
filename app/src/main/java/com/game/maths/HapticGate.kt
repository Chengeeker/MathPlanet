package com.game.maths

/**
 * Ensures one haptic response per touch event rather than stacking short pulses when
 * an action passes through multiple handlers, or the child taps very quickly.
 *
 * This is intentionally separate from quiz logic: answering still works when haptics
 * are suppressed.
 */
class HapticGate(
    private val minimumIntervalMillis: Long = 120L
) {
    init { require(minimumIntervalMillis >= 0L) }
    private var lastPulseAt: Long? = null

    @Synchronized
    fun tryAcquire(nowMillis: Long): Boolean {
        val previous = lastPulseAt
        if (previous != null && nowMillis >= previous &&
            nowMillis - previous < minimumIntervalMillis) return false
        lastPulseAt = nowMillis
        return true
    }
}
