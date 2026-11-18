package com.game.maths

/**
 * Music starts at 25% for new players. Older releases initialized the volume at 45%.
 * Only migrate that exact legacy default once, preserving other user-selected volumes.
 */
object MusicVolumePolicy {
    const val defaultVolume = .25f
    const val oldDefaultVolume = .45f

    fun resolve(persisted: Float?, migrationCompleted: Boolean): Float {
        if (persisted == null) return defaultVolume
        return if (!migrationCompleted && persisted == oldDefaultVolume) defaultVolume
            else persisted.coerceIn(0f, 1f)
    }
}
