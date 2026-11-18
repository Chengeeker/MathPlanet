package com.game.maths

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class MathEngineTest {
    @Test fun negativeRangeUsesSymmetricMaxAndKeepsCorrectArithmetic() {
        for (maximum in listOf(10, 50, 100, 1000, 135)) {
            val settings = GameSettings(maxNumber = maximum, allowNegative = true).normalized()
            assertEquals(-maximum, settings.minNumber)
            assertEquals(maximum, settings.maxNumber)
            val problems = RoundProblemPlanner.plan(settings, Random(maximum))
            assertEquals(10, problems.size)
            assertTrue(problems.all { p ->
                p.start in -maximum..maximum &&
                    p.answer in -maximum..maximum &&
                    p.choices.size == 4 &&
                    p.choices.all { it in -maximum..maximum } &&
                    p.answer == MathEngine.evaluate(p.start, p.steps)
            })
        }
    }

    @Test fun signedMultiplicationAndDivisionRemainExact() {
        for (op in listOf(Operation.MULTIPLY, Operation.DIVIDE)) {
            val settings = GameSettings(maxNumber = 100, allowNegative = true,
                operations = setOf(op))
            val problems = RoundProblemPlanner.plan(settings, Random(24))
            assertEquals(10, problems.size)
            for (p in problems) {
                assertEquals(p.answer, MathEngine.evaluate(p.start, p.steps))
                assertTrue(p.answer in -100..100)
                assertTrue(p.steps.all { it.operand != 0 })
            }
        }
    }

    @Test fun defaultsAreTenOrLess() {
        val random = Random(42)
        repeat(2000) {
            val problem = MathEngine.generate(GameSettings(), random)
            assertNotNull(problem)
            val p = problem!!
            assertTrue(p.answer in 0..10)
            assertTrue(p.start in 0..10)
            assertEquals(1, p.steps.size)
            assertEquals(p.answer, MathEngine.evaluate(p.start, p.steps))
            assertTrue(p.choices.contains(p.answer))
            assertEquals(4, p.choices.distinct().size)
        }
    }
    @Test fun multiStepAndMixedOperationsAreValid() {
        val random = Random(5)
        val settings = GameSettings(maxNumber = 1000, terms = 5, operations = Operation.entries.toSet())
        repeat(2000) {
            val p = MathEngine.generate(settings, random)!!
            assertEquals(4, p.steps.size)
            assertEquals(p.answer, MathEngine.evaluate(p.start, p.steps))
            assertTrue(p.answer in 0..1000)
            assertTrue(p.steps.all { it.operand in 0..1000 })
            assertTrue(p.steps.filter { it.operator == Operation.DIVIDE }.all { it.operand != 0 })
        }
    }
    @Test fun divisionProducesOnlyWholeNumbers() {
        val s = GameSettings(maxNumber = 100, terms = 3, operations = setOf(Operation.DIVIDE))
        repeat(200) {
            val p = MathEngine.generate(s)!!
            assertEquals(2, p.steps.size)
            assertEquals(p.answer, MathEngine.evaluate(p.start, p.steps))
            assertTrue(p.expression.contains("÷"))
        }
    }
    @Test fun customRangesAreRespected() {
        val s = GameSettings(minNumber = 15, maxNumber = 100, terms = 3,
            operations = setOf(Operation.ADD, Operation.SUBTRACT))
        repeat(200) {
            val p = MathEngine.generate(s)!!
            assertTrue(p.start in 15..100)
            assertTrue(p.answer in 15..100)
            assertTrue(p.choices.all { it in 15..100 })
            assertTrue(p.steps.all { it.operand in 15..100 })
        }
    }
    @Test fun invalidSettingsCannotHang() {
        val s = GameSettings(minNumber = 999996, maxNumber = 1000000, terms = 5,
            operations = setOf(Operation.MULTIPLY))
        // No valid 5-term product in this window.
        assertNull(MathEngine.generate(s, Random(8)))
    }
}
