package com.game.maths

/** Landscape tablets must never require scrolling to reach the primary
 * action after answering. The question or explanation may scroll instead.
 * These rules do not alter portrait phone behavior.
 */
internal object LandscapeActionPolicy {
    fun pinClassicNext(wide: Boolean, resolved: Boolean): Boolean =
        wide && resolved

    fun pinPracticeNext(wide: Boolean, hasCurrentQuestion: Boolean,
        answeredOrRevealed: Boolean): Boolean =
        wide && hasCurrentQuestion && answeredOrRevealed

    fun pinSoloNext(wide: Boolean, resolved: Boolean, done: Boolean,
        hasCurrentQuestion: Boolean): Boolean =
        wide && resolved && !done && hasCurrentQuestion

    fun pinShopAction(wide: Boolean): Boolean = wide
}
