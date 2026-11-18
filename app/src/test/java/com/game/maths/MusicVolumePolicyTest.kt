package com.game.maths

import org.junit.Assert.*
import org.junit.Test

class MusicVolumePolicyTest {
    @Test fun freshInstallDefaultsTo25Percent() {
        assertEquals(.25f, GameSettings().musicVolume, 0f)
        assertEquals(.25f, MusicVolumePolicy.resolve(null, false), 0f)
    }

    @Test fun legacyDefaultMigratesOnceAndRemainsStable() {
        assertEquals(.25f, MusicVolumePolicy.resolve(.45f, false), 0f)
        assertEquals(.45f, MusicVolumePolicy.resolve(.45f, true), 0f)
    }

    @Test fun userCustomizedVolumeIsNotOverwritten() {
        assertEquals(.12f, MusicVolumePolicy.resolve(.12f, false), 0f)
        assertEquals(.78f, MusicVolumePolicy.resolve(.78f, false), 0f)
        assertEquals(1f, MusicVolumePolicy.resolve(3f, false), 0f)
    }
}
