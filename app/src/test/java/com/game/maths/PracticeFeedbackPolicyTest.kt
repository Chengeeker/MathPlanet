package com.game.maths

import org.junit.Assert.*
import org.junit.Test

class PracticeFeedbackPolicyTest {
    @Test fun practiceResultSoundStillPlaysWithDefaultVoiceEnabled() {
        val settings=GameSettings()
        assertTrue(settings.voiceEnabled)
        assertTrue(settings.soundEnabled)
        assertTrue(settings.hapticsEnabled)
        assertTrue(PracticeFeedbackPolicy.shouldPlayResultSound(
            settings.soundEnabled,pcmNarrating=false,ttsNarrating=false))
        // Unlike classic AudioMixPolicy.shouldPlayEffect, this rule is
        // independent of voiceEnabled: practice does not read out a result.
        assertFalse(AudioMixPolicy.shouldPlayEffect(settings.soundEnabled,settings.voiceEnabled))
    }

    @Test fun practiceAnswerRespectsSoundSwitchAndCurrentNarration() {
        assertFalse(PracticeFeedbackPolicy.shouldPlayResultSound(false,false,false))
        assertFalse(PracticeFeedbackPolicy.shouldPlayResultSound(true,true,false))
        assertFalse(PracticeFeedbackPolicy.shouldPlayResultSound(true,false,true))
        assertFalse(PracticeFeedbackPolicy.shouldPlayResultSound(true,true,true))
        assertTrue(PracticeFeedbackPolicy.shouldPlayResultSound(true,false,false))
    }
}
