package com.game.maths

/** A monotonic-clock gate: no answer explanation in the first ten seconds of a new question. */
object ExplanationGate {
    const val WAIT_MS = 10_000L
    fun remainingMs(startedAt: Long, now: Long): Long =
        (WAIT_MS - (now - startedAt).coerceAtLeast(0L)).coerceAtLeast(0L)
    fun canShow(startedAt: Long, now: Long): Boolean = remainingMs(startedAt, now) == 0L
    fun secondsRemaining(startedAt: Long, now: Long): Long =
        (remainingMs(startedAt, now) + 999L) / 1000L
}
