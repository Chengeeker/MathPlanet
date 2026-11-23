package com.game.maths

import org.junit.Assert.*
import org.junit.Test

class LandscapeActionPolicyTest {
    @Test fun classicNextPinsOnlyAfterAnswerOnTablet() {
        assertTrue(LandscapeActionPolicy.pinClassicNext(true,true))
        assertFalse(LandscapeActionPolicy.pinClassicNext(true,false))
        assertFalse(LandscapeActionPolicy.pinClassicNext(false,true))
    }
    @Test fun practiceNextPinsAfterAnswerOrShowSteps() {
        assertTrue(LandscapeActionPolicy.pinPracticeNext(true,true,true))
        assertFalse(LandscapeActionPolicy.pinPracticeNext(true,true,false))
        assertFalse(LandscapeActionPolicy.pinPracticeNext(true,false,true))
        assertFalse(LandscapeActionPolicy.pinPracticeNext(false,true,true))
    }
    @Test fun soloActionDoesNotShowOnFinishedOrPortrait() {
        assertTrue(LandscapeActionPolicy.pinSoloNext(true,true,false,true))
        assertFalse(LandscapeActionPolicy.pinSoloNext(true,false,false,true))
        assertFalse(LandscapeActionPolicy.pinSoloNext(true,true,true,true))
        assertFalse(LandscapeActionPolicy.pinSoloNext(true,true,false,false))
        assertFalse(LandscapeActionPolicy.pinSoloNext(false,true,false,true))
    }
    @Test fun moneyGameConfirmationAndNextRemainInSamePinnedPosition() {
        assertTrue(LandscapeActionPolicy.pinShopAction(true))
        assertFalse(LandscapeActionPolicy.pinShopAction(false))
    }
}
