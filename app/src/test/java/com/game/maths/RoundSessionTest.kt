package com.game.maths

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class RoundSessionTest {
    @Test fun wrongAnswerAllowsRetryAndThenReveals() {
        val round = RoundSession(GameSettings(), Random(124))
        val p = round.problem!!
        val wrongs = p.choices.filter { it != p.answer }
        assertEquals(AttemptOutcome.RETRY, round.choose(wrongs[0]))
        assertFalse(round.resolved)
        assertEquals(0, round.stars)
        assertEquals(AttemptOutcome.IGNORED, round.choose(wrongs[0]))
        assertEquals(AttemptOutcome.REVEAL, round.choose(wrongs[1]))
        assertTrue(round.resolved)
        assertEquals(0, round.stars)
    }
    @Test fun cannotSkipOrDoubleCount() {
        val round = RoundSession(GameSettings(), Random(5))
        assertFalse(round.next())
        val answer = round.problem!!.answer
        assertEquals(AttemptOutcome.CORRECT, round.choose(answer))
        assertEquals(AttemptOutcome.IGNORED, round.choose(answer))
        assertEquals(1, round.stars)
        assertTrue(round.next())
        assertEquals(2, round.questionNumber)
    }
    @Test fun retryCanStillEarnASingleStar() {
        val round = RoundSession(GameSettings(), Random(30))
        val p = round.problem!!
        val wrong = p.choices.first { it != p.answer }
        assertEquals(AttemptOutcome.RETRY, round.choose(wrong))
        assertEquals(AttemptOutcome.CORRECT_AFTER_RETRY, round.choose(p.answer))
        assertEquals(1, round.stars)
    }
    @Test fun finishingCompletesExactlyOneRound() {
        val s = GameSettings(roundLength = 5)
        val round = RoundSession(s, Random(777))
        repeat(s.roundLength) {
            assertEquals(AttemptOutcome.CORRECT, round.choose(round.problem!!.answer))
            assertTrue(round.next())
        }
        assertTrue(round.finished)
        assertEquals(5, round.stars)
        assertFalse(round.next())
        assertEquals(AttemptOutcome.IGNORED, round.choose(1))
    }
    @Test fun firstWrongThenSkipAlwaysAdvances() {
        val round = RoundSession(GameSettings(roundLength = 5), Random(222))
        val p = round.problem!!
        val wrong = p.choices.first { it != p.answer }
        assertEquals(AttemptOutcome.RETRY, round.choose(wrong))
        assertFalse(round.resolved)
        assertTrue(round.skip())
        assertTrue(round.resolved)
        assertEquals(AttemptOutcome.REVEAL, round.lastOutcome)
        assertEquals(0, round.stars)
        assertTrue(round.next())
        assertEquals(2, round.questionNumber)
        assertFalse(round.resolved)
        assertEquals(AttemptOutcome.IGNORED, round.lastOutcome)
    }

    @Test fun skipWholeRoundCannotGetStuck() {
        val round = RoundSession(GameSettings(roundLength = 5), Random(94))
        repeat(5) { n ->
            assertEquals(n + 1, round.questionNumber)
            assertTrue(round.skip())
            assertFalse(round.skip())
            assertTrue(round.next())
        }
        assertTrue(round.finished)
        assertEquals(0, round.stars)
        assertFalse(round.next())
    }

    @Test fun composeObserverReceivesAnswerTransition() {
        val round = RoundSession(GameSettings(), Random(12))
        val observer = SnapshotStateObserver { runnable -> runnable() }
        var observedChanges = 0
        observer.start()
        try {
            observer.observeReads("quiz", { observedChanges++ }) {
                round.resolved
                round.lastOutcome
            }
            round.choose(round.problem!!.answer)
            Snapshot.sendApplyNotifications()
            assertTrue("Compose must see the newly resolved question", observedChanges > 0)
        } finally {
            observer.stop()
        }
    }

    @Test fun fillInModeAcceptsAnyValidNumberRatherThanFourChoicesOnly() {
        val s = GameSettings(answerMode = AnswerMode.NUMERIC_INPUT)
        val round = RoundSession(s, Random(17))
        val problem = round.problem!!
        assertEquals(AttemptOutcome.IGNORED, round.choose(problem.answer))
        // The first wrong answer can be any number, even one not listed among the four choices.
        val wrong = (0..s.maxNumber).first { it != problem.answer && it !in problem.choices }
        wrong.toString().forEach { round.enterDigit(it.digitToInt()) }
        assertEquals(AttemptOutcome.RETRY, round.submitNumericAnswer())
        assertEquals("", round.numericDigits)
        assertFalse(round.resolved)
        problem.answer.toString().forEach { round.enterDigit(it.digitToInt()) }
        assertEquals(AttemptOutcome.CORRECT_AFTER_RETRY, round.submitNumericAnswer())
        assertTrue(round.resolved)
        assertEquals(1, round.stars)
        assertTrue(round.next())
        assertEquals("", round.numericDigits)
    }

    @Test fun fillInModeDoesNotGradeUntilChildExplicitlySubmits() {
        val round = RoundSession(GameSettings(answerMode = AnswerMode.NUMERIC_INPUT), Random(4))
        val answer = round.problem!!.answer
        answer.toString().forEach { round.enterDigit(it.digitToInt()) }
        assertEquals(answer.toString(), round.numericDigits)
        assertFalse(round.resolved)
        assertEquals(0, round.stars)
        assertEquals(AttemptOutcome.CORRECT, round.submitNumericAnswer())
        assertEquals(1, round.stars)
        round.enterDigit(9)
        round.deleteDigit()
        assertEquals(answer.toString(), round.numericDigits)
    }

    @Test fun fillInModeShowsAnswerAfterSecondMistake() {
        val s = GameSettings(answerMode = AnswerMode.NUMERIC_INPUT)
        val round = RoundSession(s, Random(13))
        val answer = round.problem!!.answer
        val wrongs = (0..10).filter { it != answer }.take(2)
        round.enterDigit(wrongs[0])
        assertEquals(AttemptOutcome.RETRY, round.submitNumericAnswer())
        assertEquals("", round.numericDigits)
        round.enterDigit(wrongs[1])
        assertEquals(AttemptOutcome.REVEAL, round.submitNumericAnswer())
        assertTrue(round.resolved)
        assertEquals(0, round.stars)
        assertTrue(round.next())
        assertEquals("", round.numericDigits)
    }

    @Test fun fillInModeHandlesZeroAndMaxSevenDigitAnswers() {
        assertEquals("0", DigitComposer.append("", 0, 10))
        assertEquals("6", DigitComposer.append("0", 6, 10))
        assertEquals("1", DigitComposer.append("", 1, 10))
        assertEquals("1", DigitComposer.append("1", 1, 10))
        assertEquals("10", DigitComposer.append("1", 0, 10))
        assertEquals("1", DigitComposer.erase("10"))
        assertNull(DigitComposer.parse("", 10))
        assertNull(DigitComposer.parse("11", 10))
        assertEquals(0, DigitComposer.parse("0", 10))
        val max = listOf(1, 0, 0, 0, 0, 0, 0).fold("") { digits, digit ->
            DigitComposer.append(digits, digit, 1_000_000)
        }
        assertEquals("1000000", max)
        assertEquals(1_000_000, DigitComposer.parse(max, 1_000_000))
        assertEquals(max, DigitComposer.append(max, 1, 1_000_000))
    }

    @Test fun negativeFillInCanTypeMinusAndSubmit() {
        val settings = GameSettings(
            maxNumber = 10,
            allowNegative = true,
            answerMode = AnswerMode.NUMERIC_INPUT
        ).normalized()
        val round = RoundSession(settings, Random(28))
        assertEquals(-10, round.settings.minNumber)
        round.toggleNumericSign()
        assertEquals("-", round.numericDigits)
        assertEquals(AttemptOutcome.IGNORED, round.submitNumericAnswer())
        round.enterDigit(3)
        assertEquals("-3", round.numericDigits)
        round.deleteDigit()
        assertEquals("-", round.numericDigits)
        round.toggleNumericSign()
        assertEquals("", round.numericDigits)
        assertEquals("-10", DigitComposer.append("-1", 0, 10, true))
        assertEquals(-10, DigitComposer.parse("-10", 10, true))
        assertNull(DigitComposer.parse("-10", 10, false))
        assertNull(DigitComposer.parse("-11", 10, true))
    }

    @Test fun negativeFillInGradesWithoutFourOptions() {
        val settings = GameSettings(maxNumber = 10, allowNegative = true,
            answerMode = AnswerMode.NUMERIC_INPUT)
        val rng = Random(9)
        val round = RoundSession(settings, rng)
        val answer = round.problem!!.answer
        if (answer < 0) round.toggleNumericSign()
        answer.toString().removePrefix("-").forEach { round.enterDigit(it.digitToInt()) }
        assertEquals(AttemptOutcome.CORRECT, round.submitNumericAnswer())
        assertEquals(1, round.stars)
    }

    @Test fun savedModeDefaultsToExistingMultipleChoice() {
        assertEquals(AnswerMode.MULTIPLE_CHOICE, GameSettings().answerMode)
        assertEquals(AnswerMode.MULTIPLE_CHOICE, AnswerMode.fromStored(null))
        assertEquals(AnswerMode.MULTIPLE_CHOICE, AnswerMode.fromStored("UNKNOWN"))
        assertEquals(AnswerMode.NUMERIC_INPUT, AnswerMode.fromStored("NUMERIC_INPUT"))
    }

    @Test fun explanationReflectsSequentialArithmetic() {
        val p = MathProblem(2, listOf(MathStep(Operation.ADD, 3),
            MathStep(Operation.MULTIPLY, 2)), 10, listOf(7, 10, 9, 11))
        assertEquals(10, MathEngine.evaluate(p.start, p.steps))
        assertTrue(p.expression.contains("(2 + 3) × 2"))
        assertTrue(p.explanation.contains("2 + 3 = 5"))
        assertTrue(p.explanation.contains("5 × 2 = 10"))
    }
}
