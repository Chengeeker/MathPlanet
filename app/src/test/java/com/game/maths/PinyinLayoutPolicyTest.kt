package com.game.maths

import org.junit.Assert.*
import org.junit.Test

class PinyinLayoutPolicyTest {
    @Test fun regularControlsDoNotChangeWithPinyinOff() {
        for (height in listOf(47f, 55f, 65f, 96f)) {
            assertEquals(height, PinyinLayoutPolicy.buttonHeightDp(height, false), 0.001f)
        }
        assertFalse(PinyinLayoutPolicy.useStackedProgressHeader(false, false))
        assertFalse(PinyinLayoutPolicy.useStackedAnswerHeading(false, false))
    }

    @Test fun pinyinResultButtonFitsOnBothCompactTabletAndPhone() {
        // Tablet question card: 47dp; phone question card: 55dp.
        assertEquals(82f, PinyinLayoutPolicy.buttonHeightDp(47f, true), 0.001f)
        assertEquals(82f, PinyinLayoutPolicy.buttonHeightDp(55f, true), 0.001f)
        assertEquals(82f, PinyinLayoutPolicy.buttonHeightDp(65f, true), 0.001f)
        assertEquals(96f, PinyinLayoutPolicy.buttonHeightDp(96f, true), 0.001f)
    }

    @Test fun longAnnotatedActionsHaveTwoLinesAndEnoughHeight() {
        assertEquals(104f,PinyinLayoutPolicy.mainButtonHeightDp(55f,true,12),.001f)
        assertEquals(104f,PinyinLayoutPolicy.mainButtonHeightDp(65f,true,9),.001f)
        assertEquals(82f,PinyinLayoutPolicy.mainButtonHeightDp(65f,true,6),.001f)
        assertEquals(65f,PinyinLayoutPolicy.mainButtonHeightDp(65f,false,15),.001f)
        assertEquals(2,PinyinLayoutPolicy.mainButtonLines(true,10))
        assertEquals(1,PinyinLayoutPolicy.mainButtonLines(true,6))
        assertEquals(1,PinyinLayoutPolicy.mainButtonLines(false,14))
    }

    @Test fun phonePageTitlesStackOnlyForPinyinAndNarrowWidth() {
        assertTrue(PinyinLayoutPolicy.useStackedPageHeading(true,360f))
        assertTrue(PinyinLayoutPolicy.useStackedPageHeading(true,430f))
        assertFalse(PinyinLayoutPolicy.useStackedPageHeading(true,460f))
        assertFalse(PinyinLayoutPolicy.useStackedPageHeading(false,360f))
        assertEquals(21f,PinyinLayoutPolicy.pageTitleSp(25f,true),.001f)
        assertEquals(25f,PinyinLayoutPolicy.pageTitleSp(25f,false),.001f)
    }

    @Test fun narrowPhoneAndPortraitTabletUseNonCrampedHeaders() {
        assertTrue(PinyinLayoutPolicy.useStackedProgressHeader(true, false))
        assertFalse(PinyinLayoutPolicy.useStackedProgressHeader(true, true))
        assertTrue(PinyinLayoutPolicy.useStackedAnswerHeading(true, false))
        assertFalse(PinyinLayoutPolicy.useStackedAnswerHeading(true, true))
    }

    @Test fun pinyinAudioChoiceFitsPhoneButKeepsTabletTwoColumns() {
        assertTrue(PinyinLayoutPolicy.stackVoiceModeOptions(true, 310f))
        assertTrue(PinyinLayoutPolicy.stackVoiceModeOptions(true, 390f))
        assertTrue(PinyinLayoutPolicy.stackVoiceModeOptions(true, 439f))
        assertFalse(PinyinLayoutPolicy.stackVoiceModeOptions(true, 440f))
        assertFalse(PinyinLayoutPolicy.stackVoiceModeOptions(true, 820f))
        assertFalse(PinyinLayoutPolicy.stackVoiceModeOptions(false, 310f))
    }
}
