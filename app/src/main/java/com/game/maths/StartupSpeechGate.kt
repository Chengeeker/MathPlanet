package com.game.maths

/**
 * A screen's first narration can be issued before the Activity reaches RESUMED.
 * Keep the newest request rather than losing it, then speak once the app is foreground.
 * Not thread-safe by itself: AudioDirector protects it with its existing lock.
 */
internal class StartupSpeechGate {
    private var pending: List<String>? = null

    fun submit(tokens: List<String>, foreground: Boolean): List<String>? {
        if (tokens.isEmpty()) return null
        return if (foreground) {
            pending = null
            tokens
        } else {
            pending = tokens.toList()
            null
        }
    }

    fun onForeground(): List<String>? {
        val request = pending
        pending = null
        return request
    }

    fun clear() {
        pending = null
    }

    internal fun hasPending(): Boolean = pending != null
}
