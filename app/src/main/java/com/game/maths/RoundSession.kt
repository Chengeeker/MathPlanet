package com.game.maths

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

/**
 * Child-friendly round state, observable directly by Compose.
 * No plain mutable fields that could be skipped by Compose's strong skipping.
 */
enum class AttemptOutcome { IGNORED, RETRY, CORRECT, CORRECT_AFTER_RETRY, REVEAL }

class RoundSession(
    val settings: GameSettings,
    private val random: Random = Random.Default,
    recentSkills: Set<String> = emptySet()
) {
    // Pre-plan the whole round: operation balance and exercise uniqueness are global.
    private val plannedProblems = RoundProblemPlanner.plan(settings, random, recentSkills)

    var questionIndex by mutableIntStateOf(0)
        private set
    var problem by mutableStateOf(plannedProblems.firstOrNull())
        private set
    var wrongAnswers by mutableStateOf<Set<Int>>(emptySet())
        private set
    var numericDigits by mutableStateOf("")
        private set
    var resolved by mutableStateOf(false)
        private set
    var finished by mutableStateOf(false)
        private set
    var stars by mutableIntStateOf(0)
        private set
    var streak by mutableIntStateOf(0)
        private set
    var bestStreak by mutableIntStateOf(0)
        private set
    var lastOutcome by mutableStateOf(AttemptOutcome.IGNORED)
        private set

    val questionNumber: Int get() = questionIndex + 1
    val total: Int get() = settings.roundLength
    val answeredCount: Int get() = questionIndex + if (resolved) 1 else 0

    fun choose(choice: Int): AttemptOutcome {
        val current = problem ?: return AttemptOutcome.IGNORED
        if (settings.answerMode != AnswerMode.MULTIPLE_CHOICE || choice !in current.choices) {
            return AttemptOutcome.IGNORED
        }
        return evaluateAttempt(choice)
    }

    fun enterDigit(digit: Int) {
        if (settings.answerMode == AnswerMode.NUMERIC_INPUT && !resolved && !finished) {
            numericDigits = DigitComposer.append(numericDigits, digit, settings.maxNumber, settings.allowNegative)
        }
    }

    fun deleteDigit() {
        if (settings.answerMode == AnswerMode.NUMERIC_INPUT && !resolved && !finished) {
            numericDigits = DigitComposer.erase(numericDigits)
        }
    }

    fun toggleNumericSign() {
        if (settings.allowNegative && settings.answerMode == AnswerMode.NUMERIC_INPUT &&
            !resolved && !finished) numericDigits = DigitComposer.toggleSign(numericDigits)
    }

    fun submitNumericAnswer(): AttemptOutcome {
        if (settings.answerMode != AnswerMode.NUMERIC_INPUT) return AttemptOutcome.IGNORED
        val value = DigitComposer.parse(numericDigits, settings.maxNumber, settings.allowNegative)
            ?: return AttemptOutcome.IGNORED
        val outcome = evaluateAttempt(value)
        if (outcome == AttemptOutcome.RETRY || (outcome == AttemptOutcome.IGNORED && value in wrongAnswers)) {
            numericDigits = ""
        }
        return outcome
    }

    private fun evaluateAttempt(choice: Int): AttemptOutcome {
        val current = problem ?: return AttemptOutcome.IGNORED
        if (resolved || finished || choice in wrongAnswers) return AttemptOutcome.IGNORED
        if (choice == current.answer) {
            resolved = true
            stars += 1
            streak += 1
            bestStreak = maxOf(bestStreak, streak)
            lastOutcome = if (wrongAnswers.isEmpty()) AttemptOutcome.CORRECT
                else AttemptOutcome.CORRECT_AFTER_RETRY
        } else {
            wrongAnswers = wrongAnswers + choice
            streak = 0
            lastOutcome = if (wrongAnswers.size >= 2) {
                resolved = true
                AttemptOutcome.REVEAL
            } else AttemptOutcome.RETRY
        }
        return lastOutcome
    }

    /**
     * Skipping is always allowed while a question is unanswered. A skip awards no star,
     * reveals the current answer, and allows a normal Next click.
     */
    fun skip(): Boolean {
        if (problem == null || resolved || finished) return false
        resolved = true
        streak = 0
        lastOutcome = AttemptOutcome.REVEAL
        return true
    }

    /** Advance only after the answer is resolved or the child chooses to skip. */
    fun next(): Boolean {
        if (!resolved || finished) return false
        if (questionNumber >= total) {
            finished = true
            return true
        }
        questionIndex++
        problem = plannedProblems.getOrNull(questionIndex)
        numericDigits = ""
        wrongAnswers = emptySet()
        resolved = false
        lastOutcome = AttemptOutcome.IGNORED
        return true
    }
}

/** Child-friendly number entry, including an optional minus sign without system IME. */
object DigitComposer {
    const val MAX_DIGITS = 7

    fun append(current: String, digit: Int, maxAnswer: Int, allowNegative: Boolean = false): String {
        if (digit !in 0..9) return current
        val prefix = if (allowNegative && current.startsWith("-")) "-" else ""
        val digits = current.removePrefix("-")
        val nextDigits = if (digits == "0") digit.toString() else digits + digit
        if (nextDigits.length > MAX_DIGITS) return current
        val value = nextDigits.toIntOrNull() ?: return current
        if (value > maxAnswer) return current
        return if (nextDigits == "0") "0" else prefix + nextDigits
    }

    fun toggleSign(current: String): String = when {
        current.startsWith("-") -> current.removePrefix("-")
        current == "0" -> "0"
        else -> "-" + current
    }

    fun erase(current: String): String =
        if (current.isEmpty()) "" else current.dropLast(1)

    fun parse(current: String, maxAnswer: Int, allowNegative: Boolean = false): Int? {
        if (current.isEmpty() || current == "-" || (!allowNegative && current.startsWith("-")))
            return null
        val digits = current.removePrefix("-")
        if (digits.length > MAX_DIGITS) return null
        return current.toIntOrNull()?.takeIf {
            it in (if (allowNegative) -maxAnswer else 0)..maxAnswer
        }
    }
}
