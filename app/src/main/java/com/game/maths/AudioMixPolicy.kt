package com.game.maths

/**
 * Keep short UI sound effects from stacking up while a child rapidly enters digits.
 * The haptic path is independent, so suppressing a click never suppresses feedback.
 */
class UiSoundGate(private val minimumIntervalMillis: Long = 180L) {
    private var lastPlayedAt = -1L

    fun shouldPlay(nowMillis: Long, narrating: Boolean): Boolean {
        if (narrating) return false
        if (lastPlayedAt >= 0L && nowMillis >= lastPlayedAt &&
            nowMillis - lastPlayedAt < minimumIntervalMillis) return false
        lastPlayedAt = nowMillis
        return true
    }
}

object AudioMixPolicy {
    const val DUCK_FACTOR = 0.12f

    fun musicTarget(musicVolume: Float, narrating: Boolean): Float =
        musicVolume.coerceIn(0f, 1f) * (if (narrating) DUCK_FACTOR else 1f)

    fun shouldPlayEffect(soundEnabled: Boolean, voiceEnabled: Boolean): Boolean =
        soundEnabled && !voiceEnabled
}
