package com.game.maths

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class MathTeachingStepsTest {
    private fun problem(a: Int, operator: Operation, b: Int, answer: Int) =
        MathProblem(a, listOf(MathStep(operator, b)), answer, listOf(answer))

    @Test fun tinyAdditionExplainsCountingInsteadOfRepeatingTheAnswer() {
        val explanation = problem(1, Operation.ADD, 1, 2).explanation
        assertTrue(explanation, explanation.contains("接着数"))
        assertTrue(explanation, explanation.contains("往后数 1 次"))
        assertTrue(explanation, explanation.contains("2"))
        assertFalse(explanation, explanation.startsWith("先算 1 + 1 = 2"))
    }

    @Test fun nearTenAdditionExplainsExactlyHowToMakeTen() {
        val eightPlusFive = problem(8, Operation.ADD, 5, 13).explanation
        assertTrue(eightPlusFive, eightPlusFive.contains("凑十法"))
        assertTrue(eightPlusFive, eightPlusFive.contains("8 + 2 = 10"))
        assertTrue(eightPlusFive, eightPlusFive.contains("10 + 3 = 13"))

        val ninePlusOne = problem(9, Operation.ADD, 1, 10).explanation
        assertTrue(ninePlusOne, ninePlusOne.contains("凑十法"))
        assertTrue(ninePlusOne, ninePlusOne.contains("还差 1"))
    }

    @Test fun borrowAndBreakTenGiveRealIntermediateSteps() {
        val twelveMinusFive = problem(12, Operation.SUBTRACT, 5, 7).explanation
        assertTrue(twelveMinusFive, twelveMinusFive.contains("退位"))
        assertTrue(twelveMinusFive, twelveMinusFive.contains("12 − 2 = 10"))
        assertTrue(twelveMinusFive, twelveMinusFive.contains("10 − 3 = 7"))

        val twentyMinusSeven = problem(20, Operation.SUBTRACT, 7, 13).explanation
        assertTrue(twentyMinusSeven, twentyMinusSeven.contains("破十法"))
        assertTrue(twentyMinusSeven, twentyMinusSeven.contains("10 − 7 = 3"))
        assertTrue(twentyMinusSeven, twentyMinusSeven.contains("10 + 3 = 13"))

        val eightMinusThree = problem(8, Operation.SUBTRACT, 3, 5).explanation
        assertTrue(eightMinusThree, eightMinusThree.contains("倒着数"))
    }

    @Test fun twoDigitAdditionAndSubtractionShowPlaceValueSplits() {
        val plus = problem(38, Operation.ADD, 24, 62).explanation
        assertTrue(plus, plus.contains("38 + 20 = 58"))
        assertTrue(plus, plus.contains("58 + 4 = 62"))
        val minus = problem(52, Operation.SUBTRACT, 24, 28).explanation
        assertTrue(minus, minus.contains("52 − 20 = 32"))
        assertTrue(minus, minus.contains("32 − 4 = 28"))
    }

    @Test fun multiplicationDivisionAndMultipleStepsAreExplained() {
        val multiplication = problem(3, Operation.MULTIPLY, 4, 12).explanation
        assertTrue(multiplication, multiplication.contains("连加"))
        assertTrue(multiplication, multiplication.contains("4 + 4 + 4 = 12"))

        val division = problem(12, Operation.DIVIDE, 3, 4).explanation
        assertTrue(division, division.contains("分组法"))
        assertTrue(division, division.contains("3 → 6 → 9 → 12"))
        assertTrue(division, division.contains("共分成 4 组"))

        val chained = MathProblem(8,
            listOf(MathStep(Operation.ADD, 5), MathStep(Operation.SUBTRACT, 4)),
            9, listOf(9)).explanation
        assertTrue(chained, chained.contains("第1步"))
        assertTrue(chained, chained.contains("第2步"))
        assertTrue(chained, chained.contains("8 + 2 = 10"))
        assertTrue(chained, chained.contains("13 − 3 = 10"))
        assertTrue(chained, chained.contains("10 − 1 = 9"))
    }

    @Test fun generatedIntermediateEquationsAreMathematicallyCorrect() {
        val equation = Regex("""([0-9]+) *([+−×÷]) *([0-9]+) *= *([0-9]+)""")
        val repeatedAddition = Regex("""([0-9]+(?: +[+] +[0-9]+){2,}) *= *([0-9]+)""")
        val symbols = mapOf("+" to Operation.ADD, "−" to Operation.SUBTRACT,
            "×" to Operation.MULTIPLY, "÷" to Operation.DIVIDE)

        fun validate(problem: MathProblem) {
            val text = problem.explanation
            assertTrue("Missing conclusion: $text",
                text.contains("= ${problem.answer}。"))
            // A chain like 3 + 3 + 3 = 9 is not the binary equation 3 + 3 = 9.
            val multiTerm = repeatedAddition.findAll(text).toList()
            multiTerm.forEach { match ->
                val terms = match.groupValues[1].split(" + ").map(String::toInt)
                assertEquals("Incorrect repeated addition in: $text",
                    match.groupValues[2].toInt(), terms.sum())
            }
            equation.findAll(text).forEach { match ->
                if (multiTerm.any { match.range.first in it.range }) return@forEach
                val left = match.groupValues[1].toInt()
                val operation = symbols.getValue(match.groupValues[2])
                val right = match.groupValues[3].toInt()
                val claimed = match.groupValues[4].toInt()
                assertEquals("Incorrect intermediate equation in: $text",
                    claimed, MathEngine.evaluate(left, listOf(MathStep(operation, right))))
            }
        }

        for (a in 1..65) {
            for (b in 0..65) {
                validate(problem(a, Operation.ADD, b, a + b))
                if (b <= a) validate(problem(a, Operation.SUBTRACT, b, a - b))
            }
        }
        for (a in 1..28) for (b in 1..28)
            validate(problem(a, Operation.MULTIPLY, b, a * b))
        for (a in 1..24) for (b in 1..24)
            validate(problem(a * b, Operation.DIVIDE, b, a))
        for (seed in 0..100) {
            val random = Random(seed)
            val s = GameSettings(maxNumber = 1000, terms = 3,
                operations = setOf(Operation.ADD, Operation.SUBTRACT,
                    Operation.MULTIPLY, Operation.DIVIDE))
            MathEngine.generate(s, random)?.let(::validate)
        }
    }
}
