package com.game.maths

import kotlin.math.abs
import kotlin.random.Random

/**
 * A complete round is planned *before* play:
 *  - requested operations receive balanced slots (5 addition + 5 subtraction for default 10);
 *  - meaningful arithmetic only (no +0, -0 or 0+N);
 *  - repeated skills / answers / near-identical operands are discouraged;
 *  - questions tend from easier to harder without sudden extreme jumps.
 *
 * A scarce or mathematically impossible custom configuration is handled gracefully:
 * redistribute slots to feasible operations, then reuse the least repetitive task only
 * if a requested round is longer than the available distinct problems.
 */
object RoundProblemPlanner {
    fun plan(
        settings: GameSettings,
        random: Random = Random.Default,
        recentSkills: Set<String> = emptySet()
    ): List<MathProblem> {
        val s = settings.normalized()
        val ops = s.operations.sortedBy { it.ordinal }
        if (ops.isEmpty()) return emptyList()

        val quota = operationSchedule(ops, s.roundLength, random)
        val poolCache = mutableMapOf<Operation, List<MathProblem>>()
        fun poolFor(op: Operation): List<MathProblem> =
            poolCache.getOrPut(op) {
                if (s.terms == 2 && op in listOf(Operation.ADD, Operation.SUBTRACT) &&
                    s.maxNumber - s.minNumber <= 60
                ) enumerateMeaningful(s, op)
                else sampleProblems(s, op, random)
            }

        val planned = mutableListOf<MathProblem>()
        for ((index, wanted) in quota.withIndex()) {
            val candidates = poolFor(wanted).ifEmpty {
                // An impossible operation does not sabotage the entire round.
                ops.filter { it != wanted }.flatMap(::poolFor)
            }
            if (candidates.isEmpty()) {
                // No feasible candidate at all. Present the existing adjust-difficulty UI.
                break
            }
            val choice = pickCandidate(candidates, planned, s, index, random, recentSkills)
            planned += choice.copy(
                choices = MathEngine.choices(choice.answer, s.minNumber, s.maxNumber, random)
            )
        }
        return planned
    }

    /** Fair rotating order: when only + and - are selected, 10 problems = exactly 5 each. */
    internal fun operationSchedule(
        operations: List<Operation>,
        total: Int,
        random: Random
    ): List<Operation> {
        if (total <= 0 || operations.isEmpty()) return emptyList()
        val ordered = operations.distinct().shuffled(random)
        val counts = ordered.associateWith { total / ordered.size }.toMutableMap()
        repeat(total % ordered.size) { counts[ordered[it]] = counts.getValue(ordered[it]) + 1 }
        val result = mutableListOf<Operation>()
        repeat(total) {
            val possible = ordered.filter { counts.getValue(it) > 0 }
            val withoutLast = possible.filter { it != result.lastOrNull() }
            val options = withoutLast.ifEmpty { possible }
            val mostRemaining = options.maxOf { counts.getValue(it) }
            val chosen = options.filter { counts.getValue(it) == mostRemaining }.random(random)
            result += chosen
            counts[chosen] = counts.getValue(chosen) - 1
        }
        return result
    }

    /**
     * For the starter course enumerate the full elementary curriculum instead of repeatedly
     * rolling an independent random expression. Equivalent 2+3 and 3+2 share a learning key.
     */
    private fun enumerateMeaningful(s: GameSettings, op: Operation): List<MathProblem> {
        val result = ArrayList<MathProblem>()
        for (start in s.minNumber..s.maxNumber) {
            if (start == 0) continue
            for (operand in 1..s.maxNumber) {
                val answer = when (op) {
                    Operation.ADD -> start + operand
                    Operation.SUBTRACT -> start - operand
                    else -> continue
                }
                if (answer in s.minNumber..s.maxNumber) {
                    result += MathProblem(start, listOf(MathStep(op, operand)), answer, emptyList())
                }
            }
        }
        return result
    }

    /** For larger ranges or multi-step arithmetic, sample a bounded pool for each first operator. */
    private fun sampleProblems(
        s: GameSettings, firstOperation: Operation, random: Random
    ): List<MathProblem> {
        val result = mutableListOf<MathProblem>()
        val keys = HashSet<String>()
        var failed = 0
        repeat(48) {
            val candidate = MathEngine.generate(s, random, firstOperation)
            if (candidate == null) {
                failed++
                // Do not spend seconds retrying an impossible custom configuration.
                if (failed >= 2) return result
            } else {
                val key = learningKey(candidate)
                if (keys.add(key)) result += candidate
            }
        }
        return result
    }

    /**
     * Prefer new skills and answers; vary the starting number/operand; introduce bigger
     * values gradually. Small random jitter preserves variety across rounds.
     */
    private fun pickCandidate(
        candidates: List<MathProblem>,
        previous: List<MathProblem>,
        settings: GameSettings,
        position: Int,
        random: Random,
        recentSkills: Set<String>
    ): MathProblem {
        val counts = previous.groupingBy(::learningKey).eachCount()
        val fresh = candidates.filter { learningKey(it) !in counts }
        // Prioritize skills not shown in the preceding rounds, when enough exist.
        val unseenRecently = fresh.filter { learningKey(it) !in recentSkills }
        val eligible = unseenRecently.ifEmpty { fresh.ifEmpty { candidates } }
        val last = previous.takeLast(2)
        val targetDifficulty = .30 + .48 * position / (settings.roundLength - 1).coerceAtLeast(1)
        val range = (settings.maxNumber - settings.minNumber).coerceAtLeast(1).toDouble()
        return eligible.maxBy { candidate ->
            val step = candidate.steps.first()
            val difficultyValue = if (step.operator == Operation.SUBTRACT) candidate.start
                else candidate.answer
            val difficulty = (difficultyValue - settings.minNumber).toDouble() / range
            val matches = previous.count { it.answer == candidate.answer }
            val recentMatches = last.count { it.answer == candidate.answer }
            val startMatches = previous.count { it.start == candidate.start }
            val operandMatches = previous.count {
                it.steps.firstOrNull()?.operand == step.operand
            }
            val immediateInverse = last.any {
                it.steps.size == 1 && step.operator == Operation.SUBTRACT &&
                    it.steps[0].operator == Operation.ADD &&
                    it.answer == candidate.start &&
                    it.start == candidate.answer
            }
            // 0 is a valid answer to e.g. 4-4, but should rarely dominate a round.
            val zeroPenalty = if (candidate.answer == 0) 5.0 else 0.0
            random.nextDouble(0.0, 2.5) +
                -8.0 * counts.getOrDefault(learningKey(candidate), 0) +
                -6.0 * matches - 7.0 * recentMatches -
                1.7 * startMatches - .75 * operandMatches -
                2.5 * abs(difficulty - targetDifficulty) -
                (if (immediateInverse) 5.0 else 0.0) - zeroPenalty
        }
    }

    /** Addition and multiplication are commutative; reversed operand order is not a new skill. */
    internal fun learningKey(problem: MathProblem): String {
        if (problem.steps.size == 1) {
            val op = problem.steps.first().operator
            val operand = problem.steps.first().operand
            if (op == Operation.ADD || op == Operation.MULTIPLY) {
                return op.name + ":" + minOf(problem.start, operand) + ":" +
                    maxOf(problem.start, operand)
            }
        }
        return problem.start.toString() + problem.steps.joinToString("") {
            "|" + it.operator.name + ":" + it.operand
        }
    }
}
