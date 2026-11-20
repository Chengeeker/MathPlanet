package com.game.maths

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class PracticeEngineTest {
    @Test fun preschoolNeverGetsHighGradeTopics() {
        val allowed=PracticeGenerator.topics(PracticeStage.PRESCHOOL)
        assertEquals(setOf(PracticeTopic.STORY,PracticeTopic.PICTURE,
            PracticeTopic.SHAPES,PracticeTopic.PATTERN),allowed.toSet())
        assertFalse(PracticeTopic.CLOCK in allowed)
        assertFalse(PracticeTopic.CHART in allowed)
        assertFalse(PracticeTopic.MIXED in allowed)
    }
    @Test fun everyTopicCreatesUniqueFourChoiceQuestions() {
        for(stage in PracticeStage.entries) {
            for(topic in listOf(PracticeTopic.MIXED)+PracticeGenerator.topics(stage)) {
                repeat(60) { seed ->
                    val q=PracticeGenerator.make(stage,topic,Random(seed))
                    assertTrue("$stage / $topic",q.answer>=0)
                    assertTrue("$stage / $topic",q.options.contains(q.answer))
                    assertEquals("$stage / $topic",4,q.options.toSet().size)
                    assertTrue("$stage / $topic",q.prompt.isNotBlank())
                    assertTrue("$stage / $topic",q.explanation.isNotBlank())
                    assertTrue("$stage / $topic",q.topic in PracticeGenerator.topics(stage))
                    if(stage==PracticeStage.PRESCHOOL) {
                        assertTrue(q.topic !in listOf(PracticeTopic.CHART,PracticeTopic.CLOCK))
                        assertTrue("$q",q.answer<=14)
                    }
                }
            }
        }
    }
    @Test fun everyCategoryBuildsEightDistinctPrompts() {
        for(stage in PracticeStage.entries) {
            for(topic in listOf(PracticeTopic.MIXED)+PracticeGenerator.topics(stage)) {
                val questions=PracticeGenerator.round(stage,topic,8,Random(2026))
                assertEquals("$stage / $topic",8,questions.size)
                assertEquals("$stage / $topic",8,questions.map{
                    it.prompt+"|"+it.visual+"|"+it.a+"|"+it.b+"|"+it.c
                }.toSet().size)
            }
        }
    }
    @Test fun firstGradersNeverNeedMultiplicationInWordProblems() {
        repeat(250) { seed ->
            val q=PracticeGenerator.make(PracticeStage.FIRST,PracticeTopic.STORY,Random(seed))
            assertFalse(q.explanation.contains("×"))
            assertFalse(q.explanation.contains("÷"))
        }
    }
    @Test fun perimeterAndAreaUseCorrectFormulas() {
        repeat(150) { seed ->
            val a=PracticeGenerator.make(PracticeStage.THIRD,PracticeTopic.SHAPES,Random(seed))
            assertEquals(2*(a.a+a.b),a.answer)
            assertEquals("厘米",a.unit)
            val b=PracticeGenerator.make(PracticeStage.FOURTH,PracticeTopic.SHAPES,Random(seed))
            assertEquals(if(b.c==1) b.a*b.b else 2*(b.a+b.b),b.answer)
        }
    }
    @Test fun timeOptionsOnlyUseWholeHoursForFirstGrade() {
        repeat(40) {seed ->
            val q=PracticeGenerator.make(PracticeStage.FIRST,PracticeTopic.CLOCK,Random(seed))
            assertTrue(q.options.all {it%2==0})
            assertTrue(q.answer in 2..22)
        }
    }
    @Test fun pictureQuantitiesAgreeWithTheRenderedDots() {
        repeat(150) {seed ->
            val q=PracticeGenerator.make(PracticeStage.PRESCHOOL,PracticeTopic.PICTURE,Random(seed))
            if(q.visual==PracticeVisual.DOTS) assertEquals(q.a,q.answer)
            if(q.visual==PracticeVisual.GROUPS) assertEquals(q.a*q.b,q.answer)
        }
    }
    @Test fun pinyinDisabledByDefaultDoesNotChangeExistingSettings() {
        assertFalse(GameSettings().pinyinEnabled)
        assertFalse(GameSettings(maxNumber=10).pinyinEnabled)
        assertTrue(GameSettings(pinyinEnabled=true).normalized().pinyinEnabled)
    }
}
