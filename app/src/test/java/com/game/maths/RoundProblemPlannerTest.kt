package com.game.maths

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class RoundProblemPlannerTest {
    @Test fun defaultTenHasExactlyFiveAddAndFiveSubtract() {
        repeat(120) { seed ->
            val questions = RoundProblemPlanner.plan(GameSettings(), Random(seed))
            assertEquals("round seed " + seed, 10, questions.size)
            val operations = questions.map { it.steps.single().operator }
            assertEquals("add balance seed " + seed, 5,
                operations.count { it == Operation.ADD })
            assertEquals("subtract balance seed " + seed, 5,
                operations.count { it == Operation.SUBTRACT })
            assertTrue("no clumped operations seed " + seed,
                operations.zipWithNext().all { (a, b) -> a != b })
        }
    }

    @Test fun defaultRoundNeverGivesTrivialZeroOperandQuestions() {
        repeat(120) { seed ->
            RoundProblemPlanner.plan(GameSettings(), Random(seed)).forEach { p ->
                assertTrue("zero start in " + p.expression, p.start > 0)
                assertEquals(1, p.steps.size)
                assertTrue("zero operand in " + p.expression, p.steps.first().operand > 0)
                assertEquals(p.answer, MathEngine.evaluate(p.start, p.steps))
                assertTrue(p.answer in 0..10)
                assertEquals(4, p.choices.distinct().size)
                assertTrue(p.answer in p.choices)
            }
        }
    }

    @Test fun defaultRoundAvoidsSwappedAdditionAndOtherRepeatedSkills() {
        repeat(120) { seed ->
            val questions = RoundProblemPlanner.plan(GameSettings(), Random(seed))
            val keys = questions.map(RoundProblemPlanner::learningKey)
            assertEquals("repeated skill seed " + seed, keys.size, keys.distinct().size)
            assertTrue("answers too repetitive seed " + seed,
                questions.map { it.answer }.distinct().size >= 5)
            assertTrue("too many zero answers seed " + seed,
                questions.count { it.answer == 0 } <= 1)
        }
        val a = MathProblem(2, listOf(MathStep(Operation.ADD, 5)), 7, emptyList())
        val b = MathProblem(5, listOf(MathStep(Operation.ADD, 2)), 7, emptyList())
        assertEquals(RoundProblemPlanner.learningKey(a), RoundProblemPlanner.learningKey(b))
    }

    @Test fun shortAndLongRoundsHaveBalancedOperations() {
        for (length in listOf(5, 10, 15, 20)) {
            repeat(30) { seed ->
                val questions = RoundProblemPlanner.plan(
                    GameSettings(roundLength = length), Random(seed)
                )
                assertEquals(length, questions.size)
                val counts = questions.groupingBy { it.steps.first().operator }.eachCount()
                assertTrue(abs(counts.getOrDefault(Operation.ADD, 0) -
                    counts.getOrDefault(Operation.SUBTRACT, 0)) <= 1)
            }
        }
    }

    @Test fun singleOperationSelectionsRemainSupported() {
        val subtractOnly = GameSettings(operations = setOf(Operation.SUBTRACT))
        val addOnly = GameSettings(operations = setOf(Operation.ADD))
        repeat(30) { seed ->
            for (settings in listOf(subtractOnly, addOnly)) {
                val problems = RoundProblemPlanner.plan(settings, Random(seed))
                assertEquals(10, problems.size)
                assertTrue(problems.all { it.steps.first().operator in settings.operations })
                assertTrue(problems.all { it.start > 0 && it.steps.first().operand > 0 })
            }
        }
    }

    @Test fun scarceElementaryRangeStillGeneratesUsableQuiz() {
        val s = GameSettings(maxNumber = 3)
        val questions = RoundProblemPlanner.plan(s, Random(19))
        assertEquals(10, questions.size)
        assertTrue(questions.all { it.steps.first().operand > 0 })
        assertTrue(questions.all { it.answer in 0..3 })
        assertEquals(5, questions.count { it.steps.first().operator == Operation.ADD })
        assertEquals(5, questions.count { it.steps.first().operator == Operation.SUBTRACT })
    }

    @Test fun customMultiStepRangesStayValidAndRespectFirstOperationBalance() {
        val s = GameSettings(minNumber = 15, maxNumber = 100,
            terms = 3, operations = setOf(Operation.ADD, Operation.SUBTRACT))
        repeat(15) { seed ->
            val questions = RoundProblemPlanner.plan(s, Random(seed))
            assertEquals(10, questions.size)
            assertEquals(5, questions.count { it.steps.first().operator == Operation.ADD })
            assertEquals(5, questions.count { it.steps.first().operator == Operation.SUBTRACT })
            questions.forEach { p ->
                assertEquals(2, p.steps.size)
                assertEquals(p.answer, MathEngine.evaluate(p.start, p.steps))
                assertTrue(p.answer in 15..100)
                assertTrue(p.choices.all { it in 15..100 })
                assertTrue(p.steps.all { it.operand in 15..100 })
            }
        }
    }

    @Test fun allFourOperationsCanBeScheduledAcrossTenQuestions() {
        val s = GameSettings(maxNumber = 100,
            operations = Operation.entries.toSet())
        repeat(8) { seed ->
            val questions = RoundProblemPlanner.plan(s, Random(seed))
            assertEquals(10, questions.size)
            val tally = questions.groupingBy { it.steps.first().operator }.eachCount()
            assertEquals(4, tally.size)
            assertTrue(tally.values.max() - tally.values.min() <= 1)
            questions.forEach { p ->
                assertEquals(p.answer, MathEngine.evaluate(p.start, p.steps))
            }
        }
    }

    @Test fun impossibleCustomConfigurationHasNoFakeMath() {
        val s = GameSettings(minNumber = 999996, maxNumber = 1000000,
            terms = 5, operations = setOf(Operation.MULTIPLY))
        assertTrue(RoundProblemPlanner.plan(s, Random(42)).isEmpty())
    }

    @Test fun newlyStartedRoundsAvoidRecentlyShownExercises() {
        val s = GameSettings()
        repeat(30) { seed ->
            val first = RoundProblemPlanner.plan(s, Random(seed))
            val seen = first.map(RoundProblemPlanner::learningKey).toSet()
            val second = RoundProblemPlanner.plan(s, Random(seed + 400), seen)
            val newer = second.map(RoundProblemPlanner::learningKey).toSet()
            assertTrue("too few fresh exercises in next round: " + seed,
                seen.intersect(newer).isEmpty())
            assertEquals(5, second.count { it.steps.first().operator == Operation.ADD })
            assertEquals(5, second.count { it.steps.first().operator == Operation.SUBTRACT })
        }
    }

    @Test fun planningIsRepeatableUnderASpecifiedSeed() {
        val s = GameSettings()
        val a = RoundProblemPlanner.plan(s, Random(72))
        val b = RoundProblemPlanner.plan(s, Random(72))
        assertEquals(a, b)
    }
}
