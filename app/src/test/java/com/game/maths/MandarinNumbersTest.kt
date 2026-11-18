package com.game.maths

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class MandarinNumbersTest {
    private fun say(number: Int) = MandarinNumbers.tokens(number).joinToString(" ") {
        when (it) {
            "unit_100" -> "百"
            "unit_1000" -> "千"
            "unit_10000" -> "万"
            "unit_zero" -> "零"
            else -> (it.removePrefix("n_").toIntOrNull() ?: -1).let { value ->
                val digits = listOf("零","一","二","三","四","五","六","七","八","九")
                when {
                    value in 0..9 -> digits[value]
                    value in 10..19 -> "十" + (if (value > 10) digits[value % 10] else "")
                    value in 20..99 -> digits[value / 10] + "十" +
                        (if (value % 10 != 0) digits[value % 10] else "")
                    else -> "BAD"
                }
            }
        }
    }.replace(" ", "")

    @Test fun negativeValuesHaveExplicitNegativeVoiceTokens() {
        assertEquals(listOf("negative", "n_3"), MandarinNumbers.tokens(-3))
        assertEquals(listOf("negative", "n_10"), MandarinNumbers.tokens(-10))
        assertEquals(
            listOf("negative", "n_3", "q_plus", "n_2"),
            MandarinNumbers.tokenizeExpression("-3 + 2 = ?")
        )
        assertEquals(
            listOf("n_5", "q_minus", "negative", "n_2"),
            MandarinNumbers.tokenizeExpression("5 − -2 = ?")
        )
    }

    @Test fun importantNumbersAreSpokenCompletely() {
        assertEquals("零", say(0))
        assertEquals("十", say(10))
        assertEquals("十一", say(11))
        assertEquals("一百一十", say(110))
        assertEquals("一百一十一", say(111))
        assertEquals("一千零一十", say(1010))
        assertEquals("一万零一", say(10001))
        assertEquals("一万零一十", say(10010))
        assertEquals("十万零一十", say(100010))
        assertEquals("一百万", say(1_000_000))
    }

    @Test fun randomNumbersOnlyUseAvailableAudioClips() {
        val vocabulary = (0..99).map { "n_$it" }.toSet() +
            setOf("unit_100", "unit_1000", "unit_10000", "unit_zero")
        val rng = Random(42)
        repeat(5000) {
            val n = rng.nextInt(1_000_001)
            val pieces = MandarinNumbers.tokens(n)
            assertTrue("$n: $pieces", pieces.isNotEmpty())
            assertTrue("$n: $pieces", pieces.all { it in vocabulary })
        }
    }

    @Test fun spokenExpressionRetainsExplicitParentheses() {
        val p = MathProblem(2, listOf(
            MathStep(Operation.ADD, 3),
            MathStep(Operation.MULTIPLY, 2)),
            10, listOf(7,8,9,10))
        val tokens = MandarinNumbers.tokenizeExpression(p.expression)
        assertEquals(listOf("q_lparen","n_2","q_plus","n_3","q_rparen",
            "q_multiply","n_2"), tokens)
    }
}
