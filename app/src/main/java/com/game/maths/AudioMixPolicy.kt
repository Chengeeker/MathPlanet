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

/** Practice uses on-demand device TTS, not classic PCM answer narration.
 * Its success/error sounds follow the sound switch, independently of the
 * global voice-enabled flag, but do not overlay a currently speaking voice.
 */
object PracticeFeedbackPolicy {
    fun shouldPlayResultSound(
        soundEnabled: Boolean, pcmNarrating: Boolean, ttsNarrating: Boolean
    ): Boolean = soundEnabled && !pcmNarrating && !ttsNarrating
}

object AudioMixPolicy {
    // PA2373 / Android 15: keeping a MediaPlayer stream at even 12% volume
    // stretches a 3-second PCM narration to ~4.2 seconds. Voice must have the
    // audio mixer to itself; only resume the BGM between spoken utterances.
    fun musicTarget(musicVolume: Float, narrating: Boolean): Float =
        if (narrating) 0f else musicVolume.coerceIn(0f, 1f)

    fun shouldPlayEffect(soundEnabled: Boolean, voiceEnabled: Boolean): Boolean =
        soundEnabled && !voiceEnabled
}
