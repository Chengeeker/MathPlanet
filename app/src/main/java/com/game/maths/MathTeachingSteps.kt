package com.game.maths

/**
 * Explains *how* to calculate, rather than merely restating the question and answer.
 * Every arithmetic identity is derived from the actual operands of the question.
 *
 * This is kept independent of Android/UI so the children's worked examples can be
 * tested exhaustively, including edge cases near each ten and at large ranges.
 */
object MathTeachingSteps {
    fun explain(problem: MathProblem): String {
        var current = problem.start
        return problem.steps.mapIndexed { index, step ->
            val result = MathEngine.evaluate(current, listOf(step))
            val description = explainSingle(current, step.operator, step.operand, result)
            current = result
            if (problem.steps.size == 1) description
            else "第${index + 1}步：${description}"
        }.joinToString("\n\n")
    }

    fun explainSingle(a: Int, operator: Operation, b: Int, result: Int): String =
        when (operator) {
            Operation.ADD -> "${addition(a, b, result)}\n所以 $a + $b = $result。"
            Operation.SUBTRACT -> "${subtraction(a, b, result)}\n所以 $a − $b = $result。"
            Operation.MULTIPLY -> "${multiplication(a, b, result)}\n所以 $a × $b = $result。"
            Operation.DIVIDE -> "${division(a, b, result)}\n所以 $a ÷ $b = $result。"
        }

    private fun addition(a: Int, b: Int, answer: Int): String {
        if (a < 0 || b < 0) return "先根据正负号决定增加或减少的数量，算出 $answer。"
        if (a == 0 || b == 0) return "加上 0 不会改变数量，留下原来的 ${maxOf(a, b)} 个。"

        // Prefer the larger addend when teaching a young child to count on.
        val big = maxOf(a, b)
        val little = minOf(a, b)
        if (big < 10 && answer >= 10) {
            val part = 10 - big
            val leftover = little - part
            return if (leftover > 0)
                "用凑十法：把 $little 拆成 $part 和 $leftover。\n" +
                    "先算 $big + $part = 10，再算 10 + $leftover = $answer。"
            else "用凑十法：$big 距离 10 还差 $part，刚好添上 $little 就凑成 10。"
        }
        if (big <= 10 && little <= 5) {
            val counted = (1..little).map { big + it }.joinToString(" → ")
            return "用接着数的方法：先记住 $big，再往后数 $little 次：\n$counted。"
        }
        if (big < 10 && answer <= 10) {
            val counted = (1..little).map { big + it }.joinToString(" → ")
            return "把 $big 个和 $little 个合起来，从 $big 往后数：\n$counted。"
        }

        // Crossing the next ten is more helpful than a bare one-line sum.
        if (b in 1..9 && a >= 10 && a % 10 + b >= 10 && a % 10 != 0) {
            val makeTen = 10 - a % 10
            val extra = b - makeTen
            val rounded = a + makeTen
            return if (extra == 0)
                "用凑整十法：$a 的个位是 ${a % 10}，再加 $makeTen 就凑成 $rounded。"
            else "用凑整十法：把 $b 拆成 $makeTen 和 $extra。\n" +
                "先算 $a + $makeTen = $rounded，再算 $rounded + $extra = $answer。"
        }
        if (a in 1..9 && b >= 10) {
            return "加法交换顺序，先从较大的 $b 开始：\n" +
                addition(b, a, answer)
        }
        if (b >= 10) {
            val tens = b / 10 * 10
            val units = b % 10
            val intermediate = a + tens
            return if (units == 0)
                "把 $b 看成 ${b / 10} 个十，给 $a 添上这些十，得到 $answer。"
            else "用拆分法：把 $b 拆成 $tens 和 $units。\n" +
                "先算 $a + $tens = $intermediate，再算 $intermediate + $units = $answer。"
        }
        // No carry: add to the ones place, leaving the tens unchanged.
        val tens = a - a % 10
        val units = a % 10
        return if (units == 0)
            "$a 是整十数，再添 $b 个一，得到 $answer。"
        else "用数位拆分法：把 $a 看成 $tens 和 $units。\n" +
            "个位先算 $units + $b = ${units + b}，再加回 $tens，得到 $answer。"
    }

    private fun subtraction(a: Int, b: Int, answer: Int): String {
        if (a < 0 || b < 0) return "先根据正负号判断减少的方向，算出 $answer。"
        if (b > a) return "减去的数量比原有的数量多，会得到负数 $answer。"
        if (b == 0) return "什么也不拿走，数量还是 $a。"
        if (a == b) return "一共有 $a 个，全部拿走 $b 个，剩下 0 个。"
        if (a <= 10 && b <= 10) {
            val counted = (1..b).map { a - it }.joinToString(" → ")
            return "用倒着数的方法：从 $a 开始往回数 $b 次：\n$counted。"
        }
        if (b in 1..9) {
            val units = a % 10
            val tens = a - units
            return if (units >= b) {
                "个位够减：把 $a 拆成 $tens 和 $units。\n" +
                    "先算 $units − $b = ${units - b}，再加上 $tens，得到 $answer。"
            } else if (units > 0) {
                val remaining = b - units
                "用退位凑整十法：把 $b 拆成 $units 和 $remaining。\n" +
                    "先算 $a − $units = $tens，再算 $tens − $remaining = $answer。"
            } else {
                val leftoverTens = a - 10
                val ones = 10 - b
                "用破十法：把 $a 拆成 $leftoverTens 和 10。\n" +
                    "先算 10 − $b = $ones，再算 $leftoverTens + $ones = $answer。"
            }
        }
        val tens = b / 10 * 10
        val units = b % 10
        val intermediate = a - tens
        return if (units == 0)
            "把要减去的 $b 看成 ${b / 10} 个十。\n" +
                "从 $a 中去掉这些十，就剩下 $answer。"
        else "用拆分法：把 $b 拆成 $tens 和 $units。\n" +
            "先算 $a − $tens = $intermediate，再算 $intermediate − $units = $answer。"
    }

    private fun multiplication(a: Int, b: Int, answer: Int): String {
        if (a == 0 || b == 0) return "只要有一个乘数是 0，就没有任何组里的数量，得到 0。"
        if (a == 1 || b == 1) return "乘以 1 代表只拿一份，数量仍是 $answer。"
        val repeats = minOf(a, b)
        val amount = maxOf(a, b)
        if (repeats in 2..6) {
            val expanded = List(repeats) { amount.toString() }.joinToString(" + ")
            return "用相同数量连加：每份 $amount 个，共 $repeats 份。\n" +
                "$expanded = $answer。"
        }
        if (b >= 10 && b % 10 != 0) {
            val tens = b / 10 * 10
            val units = b % 10
            val first = a.toLong() * tens
            val second = a.toLong() * units
            return "用拆分法：把 $b 拆成 $tens 和 $units。\n" +
                "$a × $tens = $first，$a × $units = $second；\n" +
                "最后 $first + $second = $answer。"
        }
        return "把 $a 看作每组的数量，$b 是组数。\n" +
            "也可以倒过来算 $b × $a，结果一样是 $answer。"
    }

    private fun division(a: Int, b: Int, answer: Int): String {
        if (b == 0) return "不能除以 0。"
        if (a == 0) return "没有东西可分，分成 $b 份，每份仍然是 0。"
        if (answer in 1..8 && b > 0) {
            val counted = (1..answer).map { it.toLong() * b }.joinToString(" → ")
            return "用分组法：每 $b 个分成一组，依次分到\n$counted。\n" +
                "共分成 $answer 组，正好用完 $a 个。"
        }
        return "用乘法检验除法：想一想，$b 乘几等于 $a？\n" +
            "$b × $answer = $a，所以每组是 $answer。"
    }
}
