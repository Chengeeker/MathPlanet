package com.game.maths

import org.junit.Assert.*
import org.junit.Test

class AudioMixPolicyTest {
    @Test fun denseKeypadClicksDoNotGenerateOverlappingSoundEffects() {
        val gate = UiSoundGate(180)
        assertTrue(gate.shouldPlay(1000, narrating = false))
        assertFalse(gate.shouldPlay(1025, narrating = false))
        assertFalse(gate.shouldPlay(1140, narrating = false))
        assertTrue(gate.shouldPlay(1180, narrating = false))
        assertFalse(gate.shouldPlay(1181, narrating = false))
        assertTrue(gate.shouldPlay(1360, narrating = false))
    }

    @Test fun speechSuppressesClickWithoutConsumingItsGate() {
        val gate = UiSoundGate(180)
        assertFalse(gate.shouldPlay(1000, narrating = true))
        assertFalse(gate.shouldPlay(1100, narrating = true))
        assertTrue(gate.shouldPlay(1101, narrating = false))
        assertFalse(gate.shouldPlay(1102, narrating = false))
    }

    @Test fun gateAllowsClickAgainAfterClockReset() {
        val gate = UiSoundGate(180)
        assertTrue(gate.shouldPlay(100000, narrating = false))
        assertTrue(gate.shouldPlay(50, narrating = false))
    }

    @Test fun backgroundMusicDucksRatherThanStoppingDuringNarration() {
        assertEquals(0.25f, AudioMixPolicy.musicTarget(.25f, false), .0001f)
        assertEquals(.03f, AudioMixPolicy.musicTarget(.25f, true), .0001f)
        assertEquals(0f, AudioMixPolicy.musicTarget(0f, true), .0001f)
        assertEquals(1f, AudioMixPolicy.musicTarget(4f, false), .0001f)
    }

    @Test fun answerEffectsDoNotMixWithSpokenAnswer() {
        assertFalse(AudioMixPolicy.shouldPlayEffect(soundEnabled = true, voiceEnabled = true))
        assertTrue(AudioMixPolicy.shouldPlayEffect(soundEnabled = true, voiceEnabled = false))
        assertFalse(AudioMixPolicy.shouldPlayEffect(soundEnabled = false, voiceEnabled = false))
        assertFalse(AudioMixPolicy.shouldPlayEffect(soundEnabled = false, voiceEnabled = true))
    }

    @Test fun tapGateDoesNotDependOnChoiceOrInputMode() {
        // Keeping the gate independent of the answer mode ensures other
        // screens cannot spawn a burst of SoundPool streams either.
        for (mode in AnswerMode.entries) {
            val gate = UiSoundGate(180)
            assertTrue(mode.name, gate.shouldPlay(2000, false))
            assertFalse(mode.name, gate.shouldPlay(2010, false))
        }
    }
}
