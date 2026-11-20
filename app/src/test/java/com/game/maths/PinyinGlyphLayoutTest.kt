package com.game.maths

import org.junit.Assert.*
import org.junit.Test

class PinyinGlyphLayoutTest {
    @Test fun readingNeverWidenNeighboringHanCharacters() {
        val examples=listOf(
            35f to 16f, 24f to 20f, 46f to 23f, 12f to 14f, 15f to 15f
        )
        for((reading,han) in examples) {
            val scale=PinyinGlyphLayout.scaleFor(reading,han)
            assertTrue("reading=$reading Han=$han",reading*scale<=han+0.001f)
            assertTrue(scale>0f && scale<=1f)
        }
    }
    @Test fun smallCaptionsAndLargeHeadingsUseDistinctRubySizes() {
        assertEquals(6f,PinyinGlyphLayout.annotationSizePx(11f,6f,11f),.001f)
        assertEquals(8.4f,PinyinGlyphLayout.annotationSizePx(20f,6f,11f),.001f)
        assertEquals(11f,PinyinGlyphLayout.annotationSizePx(36f,6f,11f),.001f)
    }
    @Test fun noStretchNeededForShortReadings() {
        assertEquals(1f,PinyinGlyphLayout.scaleFor(12f,23f),.001f)
    }
    @Test fun longReadingsLeaveVisualSpaceBetweenAdjacentCells() {
        val scale=PinyinGlyphLayout.scaleFor(26f,20f)
        assertEquals(18f,26f*scale,.001f)
    }
}
