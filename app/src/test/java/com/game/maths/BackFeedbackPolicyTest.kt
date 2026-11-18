package com.game.maths

import org.junit.Assert.*
import org.junit.Test

class BackFeedbackPolicyTest {
    @Test fun systemBackDoesNotAddSecondPulse() {
        assertFalse(BackFeedbackPolicy.playAppTap(fromSystemBack = true))
    }
    @Test fun visibleBackButtonGetsExactlyOneAppPulse() {
        assertTrue(BackFeedbackPolicy.playAppTap(fromSystemBack = false))
    }
}
