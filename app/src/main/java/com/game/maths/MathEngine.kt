package com.game.maths

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class Operation(val symbol: String, val spoken: String) {
    ADD("+", "加"),
    SUBTRACT("−", "减"),
    MULTIPLY("×", "乘"),
    DIVIDE("÷", "除以")
}

enum class AnswerMode(val label: String) {
    MULTIPLE_CHOICE("四选一"), NUMERIC_INPUT("数字填空");

    companion object {
        fun fromStored(raw: String?): AnswerMode = entries.firstOrNull { it.name == raw } ?: MULTIPLE_CHOICE
    }
}

data class GameSettings(
    val minNumber: Int = 0,
    val maxNumber: Int = 10,
    val terms: Int = 2,
    val operations: Set<Operation> = setOf(Operation.ADD, Operation.SUBTRACT),
    val voiceEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val musicVolume: Float = MusicVolumePolicy.defaultVolume,
    val hapticsEnabled: Boolean = true,
    val roundLength: Int = 10,
    val answerMode: AnswerMode = AnswerMode.MULTIPLE_CHOICE,
    val allowNegative: Boolean = false,
    val voicePlaybackMode: VoicePlaybackMode = VoicePlaybackMode.DIRECT_PCM,
    val pinyinEnabled: Boolean = false
) {
    fun normalized(): GameSettings {
        val maximum = maxNumber.coerceIn(3, 1_000_000)
        val minimum = if (allowNegative) -maximum else minNumber.coerceIn(0, maximum - 3)
        return copy(
            minNumber = minimum,
            maxNumber = maximum,
            terms = terms.coerceIn(2, 5),
            operations = operations.ifEmpty { setOf(Operation.ADD) },
            musicVolume = musicVolume.coerceIn(0f, 1f),
            roundLength = roundLength.coerceIn(5, 20)
        )
    }
}

data class MathStep(val operator: Operation, val operand: Int)

data class MathProblem(
    val start: Int,
    val steps: List<MathStep>,
    val answer: Int,
    val choices: List<Int>
) {
    val expression: String
        get() {
            var result = start.toString()
            for ((index, step) in steps.withIndex()) {
                if (index > 0 && (step.operator == Operation.MULTIPLY || step.operator == Operation.DIVIDE)) {
                    result = "($result)"
                }
                result += " " + step.operator.symbol + " " + step.operand
            }
            return result + " = ?"
        }

    val explanation: String
        get() = MathTeachingSteps.explain(this)

    val spoken: String
        get() = expression.replace("+", " 加 ")
            .replace("−", " 减 ")
            .replace("×", " 乘以 ")
            .replace("÷", " 除以 ")
            .replace("(", "左括号").replace(")", "右括号")
            .replace("=", "等于多少")
            .replace("?", "？")
}

object MathEngine {
    fun generate(
        settings: GameSettings,
        random: Random = Random.Default,
        preferredFirstOperation: Operation? = null
    ): MathProblem? {
        val s = settings.normalized()
        // Bounded retries prevent an impossible narrow range from hanging the app.
        repeat(450) {
            // Avoid 0 + N or 0 × N in standard arithmetic practice.
            val start = randomInt(random, s.minNumber, s.maxNumber)
                .let { if (it == 0) 1 else it }
            var current = start
            val steps = mutableListOf<MathStep>()
            for (index in 1 until s.terms) {
                val operations = if (index == 1 && preferredFirstOperation != null) {
                    if (preferredFirstOperation !in s.operations) return null
                    listOf(preferredFirstOperation)
                } else s.operations.shuffled(random)
                val candidates = operations.mapNotNull {
                    op -> makeStep(op, current, s.minNumber, s.maxNumber, random)
                }
                if (candidates.isEmpty()) break
                val (step, newValue) = candidates[random.nextInt(candidates.size)]
                steps += step
                current = newValue
            }
            if (steps.size != s.terms - 1) return@repeat
            val choices = choices(current, s.minNumber, s.maxNumber, random)
            if (choices.size < 4) return@repeat
            return MathProblem(start, steps, current, choices)
        }
        return null
    }

    fun evaluate(start: Int, steps: List<MathStep>): Int {
        var result = start.toLong()
        for (step in steps) {
            val n = step.operand.toLong()
            result = when (step.operator) {
                Operation.ADD -> result + n
                Operation.SUBTRACT -> result - n
                Operation.MULTIPLY -> result * n
                Operation.DIVIDE -> {
                    require(n != 0L && result % n == 0L) { "Non-integer division" }
                    result / n
                }
            }
        }
        require(result in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
        return result.toInt()
    }

    private fun randomInt(random: Random, low: Int, high: Int): Int {
        if (high <= low) return low
        return random.nextInt(low, high + 1)
    }

    private fun makeStep(
        op: Operation, current: Int, low: Int, high: Int, random: Random
    ): Pair<MathStep, Int>? {
        val operand: Int
        val result: Int
        when (op) {
            Operation.ADD -> {
                val positiveMinimum = maxOf(1, low)
                if (positiveMinimum > high - current) return null
                operand = randomInt(random, positiveMinimum, min(high, high - current))
                result = current + operand
            }
            Operation.SUBTRACT -> {
                val positiveMinimum = maxOf(1, low)
                if (positiveMinimum > current - low) return null
                operand = randomInt(random, positiveMinimum, min(high, current - low))
                result = current - operand
            }
            Operation.MULTIPLY -> {
                if (current == 0) return null
                val maximum = min(high, high / abs(current))
                val minimum = maxOf(1, low)
                if (maximum < minimum) return null
                // Multiplying by 0/1 teaches little. Prefer 2+ when feasible.
                val preferredMinimum = if (maximum >= maxOf(2, minimum)) maxOf(2, minimum) else minimum
                operand = randomInt(random, preferredMinimum, maximum)
                result = (current.toLong() * operand).toInt()
            }
            Operation.DIVIDE -> {
                // Division is exact. Avoid dividing by zero or producing decimals.
                if (current == 0) return null
                val lower = 1
                val upper = high
                if (upper < lower) return null
                val divisors = mutableListOf<Int>()
                // For large ranges, test candidate divisors up to sqrt and their complements.
                val magnitude = abs(current)
                var d = 1
                while (d.toLong() * d <= magnitude) {
                    if (magnitude % d == 0) {
                        val other = magnitude / d
                        if (d in lower..upper && current / d in low..high) divisors += d
                        if (other != d && other in lower..upper && current / other in low..high) divisors += other
                    }
                    d++
                }
                if (divisors.isEmpty()) return null
                val meaningful = divisors.filter { it > 1 }
                operand = (if (meaningful.isNotEmpty() && random.nextInt(5) != 0) meaningful else divisors).random(random)
                result = current / operand
            }
        }
        if (result !in low..high) return null
        return MathStep(op, operand) to result
    }

    private fun divisorCandidatesForZero(low: Int, high: Int, random: Random): List<Int> =
        if (high < low) emptyList() else List(min(12, high - low + 1)) { randomInt(random, low, high) }

    internal fun choices(answer: Int, low: Int, high: Int, random: Random): List<Int> {
        val result = linkedSetOf(answer)
        var distance = 1
        while (result.size < 4 && distance <= high - low) {
            val offsets = listOf(-distance, distance).shuffled(random)
            for (offset in offsets) {
                val candidate = answer.toLong() + offset
                if (candidate in low.toLong()..high.toLong()) result += candidate.toInt()
                if (result.size == 4) break
            }
            distance++
        }
        return result.shuffled(random)
    }
}
