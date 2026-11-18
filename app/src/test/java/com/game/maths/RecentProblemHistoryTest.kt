package com.game.maths

import org.junit.Assert.*
import org.junit.Test

class RecentProblemHistoryTest {
    @Test fun retainsLatestFortyUniqueSkills() {
        var stored: String? = null
        for (i in 0..100) stored = RecentProblemHistory.add(stored, "ADD:" + i)
        val keys = RecentProblemHistory.read(stored)
        assertEquals(40, keys.size)
        assertEquals("ADD:61", keys.first())
        assertEquals("ADD:100", keys.last())
        stored = RecentProblemHistory.add(stored, "ADD:70")
        val next = RecentProblemHistory.read(stored)
        assertEquals(40, next.size)
        assertEquals(1, next.count { it == "ADD:70" })
        assertEquals("ADD:70", next.last())
    }

    @Test fun missingSavedHistoryIsAnEmptyList() {
        assertTrue(RecentProblemHistory.read(null).isEmpty())
        assertTrue(RecentProblemHistory.read("").isEmpty())
    }
}
