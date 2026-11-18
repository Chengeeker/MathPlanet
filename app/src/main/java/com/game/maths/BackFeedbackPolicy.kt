package com.game.maths

/** Android's system back gesture already supplies feedback; never stack another app pulse. */
object BackFeedbackPolicy {
    fun playAppTap(fromSystemBack: Boolean): Boolean = !fromSystemBack
}
