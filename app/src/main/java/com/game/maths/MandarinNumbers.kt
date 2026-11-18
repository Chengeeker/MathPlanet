package com.game.maths

/**
 * Offline narrator number vocabulary. Each token corresponds to a prepacked PCM clip.
 * Supports all question/answer values from 0 through 1,000,000.
 */
object MandarinNumbers {
    fun tokens(number: Int): List<String> {
        require(number in -1_000_000..1_000_000) { "Out of supported number range" }
        if (number < 0) return listOf("negative") + tokens(-number)
        if (number == 0) return listOf("n_0")
        val result = mutableListOf<String>()
        if (number >= 10_000) {
            underTenThousand(number / 10_000, result)
            result += "unit_10000"
            val remainder = number % 10_000
            if (remainder in 1..999) result += "unit_zero"
            if (remainder in 10..19) result += "n_1"
            if (remainder > 0) underTenThousand(remainder, result)
        } else {
            underTenThousand(number, result)
        }
        return result
    }

    private fun underTenThousand(input: Int, parts: MutableList<String>) {
        var rest = input
        if (rest >= 1000) {
            parts += "n_" + rest / 1000
            parts += "unit_1000"
            rest %= 1000
            if (rest in 1..99) parts += "unit_zero"
        }
        if (rest >= 100) {
            parts += "n_" + rest / 100
            parts += "unit_100"
            rest %= 100
            if (rest in 1..9) parts += "unit_zero"
        }
        // n_10 is pronounced 十, so "一百一十" needs an extra 一.
        if (rest in 10..19 && input >= 100) parts += "n_1"
        if (rest > 0) parts += "n_$rest"
    }

    fun tokenizeExpression(expression: String): List<String> {
        val parts = mutableListOf<String>()
        Regex("""-?\d+|[+−×÷()]""").findAll(expression).forEach { match ->
            val item = match.value
            when {
                item.toIntOrNull() != null -> parts += tokens(item.toInt())
                item == "+" -> parts += "q_plus"
                item == "−" -> parts += "q_minus"
                item == "×" -> parts += "q_multiply"
                item == "÷" -> parts += "q_divide"
                item == "(" -> parts += "q_lparen"
                item == ")" -> parts += "q_rparen"
            }
        }
        return parts
    }
}
