package com.game.maths

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class AdventureEngineTest {
    @Test fun carryQuestionsAlwaysCrossTenAndAreWithinTwenty() {
        for (seed in 0..300) {
            val q = AdventureBank.generate(SkillTopic.CARRY, Random(seed))
            assertTrue(q.add)
            assertTrue(q.a in 1..9 && q.b in 2..9)
            assertTrue(q.answer in 11..20)
            assertEquals(q.answer, 10 + (q.b - q.strategyStep))
            assertTrue(q.strategyStep in 1..q.b)
        }
    }

    @Test fun borrowQuestionsRequireBreakingTen() {
        for (seed in 0..300) {
            val q = AdventureBank.generate(SkillTopic.BORROW, Random(seed))
            assertFalse(q.add)
            assertTrue(q.a in 11..19)
            assertTrue(q.b > q.a % 10)
            assertTrue(q.b <= 9)
            assertEquals(q.answer, q.resultBreak)
        }
    }

    @Test fun moneyQuestionsHaveValidPaymentsAndChanges() {
        for (seed in 0..300) {
            val q = AdventureBank.generate(SkillTopic.MONEY, Random(seed))
            assertTrue(q.isMoney)
            assertTrue(q.price in 1..20)
            assertTrue(q.answer in 1..20)
            if (q.role == ShopRole.CASHIER) {
                assertTrue(q.paid > q.price)
                assertEquals(q.paid, q.price + q.answer)
            } else assertEquals(q.price, q.answer)
        }
    }

    @Test fun plansPreferDifferentConsecutiveQuestions() {
        for (seed in 0..40) {
            val round = AdventureBank.makeRound(SkillTopic.MIXED, 20,
                random = Random(seed))
            assertEquals(20, round.size)
            round.zipWithNext().forEach { (a,b) ->
                assertNotEquals(a.learningKey, b.learningKey)
            }
        }
    }

    @Test fun shopRoleCanBeExplicitlySelectedForWholeRound() {
        for (role in ShopRole.entries) {
            val round = AdventureBank.makeRound(SkillTopic.MONEY, 20,
                random = Random(17), maxNumber = 100, shopRole = role)
            assertEquals(20, round.size)
            assertTrue(round.all { it.role == role })
        }
    }

    @Test fun globalLearningRangeCapsAllGeneratedQuestionValues() {
        for (max in listOf(3, 5, 10, 20, 50, 100, 1000)) {
            for (topic in SkillTopic.entries) {
                for (seed in 0..80) {
                    val q = AdventureBank.generate(topic,
                        random = Random(seed), maxNumber = max)
                    assertTrue("cap=$max topic=$topic: $q", q.answer in 0..max)
                    assertTrue(q.a in 0..max && q.b in 0..max)
                    if (q.isMoney) {
                        assertTrue(q.price in 1..max)
                        assertTrue(q.paid in 0..max)
                    }
                }
            }
        }
    }

    @Test fun smallRangeCashierCanStillMakeChange() {
        val questions = AdventureBank.makeRound(SkillTopic.MONEY, 10,
            random = Random(7), maxNumber = 10, shopRole = ShopRole.CASHIER)
        assertTrue(questions.all { it.paid in 1..10 && it.price in 1..10 })
        assertTrue(questions.all { it.paid > it.price && it.answer in 0..10 })
    }

    @Test fun classicMistakesCanBeReplayed() {
        val subtract = parseRecordedQuestion("12|SUBTRACT:4")!!
        assertEquals(8, subtract.answer)
        assertEquals("12|SUBTRACT:4", subtract.learningKey)
        val compound = parseRecordedQuestion("2|ADD:5|MULTIPLY:3")!!
        assertEquals(21, compound.answer)
        assertTrue(compound.expression.contains("×"))
    }

    @Test fun parentDashboardExcludesRetiredGamesWithoutDeletingTheirRecords() {
        val titles = listOf("自由算术探险", "小小商店", "错题专项复习",
            "⚡ 闪电抢答赛·左侧", "🧠 巧思挑战赛·右侧",
            "凑十传送带", "破十小仓库", "炮台打数字")
        val records = titles.mapIndexed { index, title ->
            TrainingRecord(title, "1 + 1", "ADD:1:1", index % 2 == 0, false, 10L)
        }
        val current = TrainingHistory.currentGameRecords(records)
        // Both historical arena records stay on disk but are not visible in
        // today's identical phone/tablet game catalogue or dashboard filters.
        assertEquals(listOf("算数探险", "小小商店", "错题专项复习"),
            current.map { it.game })
        assertEquals(8, records.size) // Original storage remains untouched.
        assertEquals("自由算术探险", records.first().game)
        val legacyDuel = TrainingRecord("基础加减法对决·左侧", "2+2", "ADD:2:2",
            true, true, 100L)
        assertEquals("计算对决·左侧",
            TrainingHistory.currentGameRecords(listOf(legacyDuel)).single().game)
        assertEquals("基础加减法对决·左侧", legacyDuel.game)
    }

    @Test fun explanationCannotBeShownBeforeTenSeconds() {
        assertEquals(10L, ExplanationGate.secondsRemaining(100L, 100L))
        assertFalse(ExplanationGate.canShow(100L, 10_099L))
        assertEquals(1L, ExplanationGate.secondsRemaining(100L, 10_099L))
        assertTrue(ExplanationGate.canShow(100L, 10_100L))
        assertEquals(0L, ExplanationGate.secondsRemaining(100L, 11_000L))
        assertFalse(ExplanationGate.canShow(20_000L, 20_999L))
    }

    @Test fun aiReactionTimesStayWithinEachLevelInterval() {
        val limits = listOf(
            6_000L to 8_000L, // 简单
            4_000L to 6_000L, // 中等
            2_500L to 4_000L, // 困难
            1_500L to 2_500L  // 噩梦
        )
        for ((level, range) in limits.withIndex()) {
            for (seed in 0..300) {
                assertTrue(DuelRules.aiReactionMs(level, Random(seed)) in range.first..range.second)
            }
        }
    }

    @Test fun learningRangeIsTheOnlyArithmeticCeiling() {
        val questions = (0..350).map {
            AdventureBank.generate(SkillTopic.MIXED, random = Random(it), maxNumber = 1000)
        }
        assertTrue(questions.any { maxOf(it.a, it.b, it.answer) > 100 })
        assertTrue(questions.all { it.a in 0..1000 && it.b in 0..1000 &&
            it.answer in 0..1000 })
    }

    @Test fun duelShowsSubmittedResultAfterEqualsSign() {
        val question = SkillQuestion(3, 4)
        assertEquals("3 + 4 = ?", DuelRules.displayedEquation(question, null, false))
        assertEquals("3 + 4 = 7", DuelRules.displayedEquation(question,
            DuelAnswerFeedback(7, true), false))
        assertEquals("3 + 4 = 6 ✗", DuelRules.displayedEquation(question,
            DuelAnswerFeedback(6, false), false))
        assertEquals("3 + 4 = 7", DuelRules.displayedEquation(question,
            DuelAnswerFeedback(6, false), true))
        assertTrue(DuelRules.feedbackText(question, DuelAnswerFeedback(6, false), true)
            .contains("正确答案是 7"))
    }

    @Test fun duelPlayersFaceTheirOwnOuterEdges() {
        // Regression guard: the old signs put both sets of controls near the center.
        assertEquals(90f, DuelSeatLayout.rotationFor(0))
        assertEquals(-90f, DuelSeatLayout.rotationFor(1))
    }

    @Test fun duelAccuracyComparesCorrectCountBeforeSpeed() {
        assertEquals(1, DuelRules.compareAccuracy(8, 7, 50000, 10000))
        assertEquals(-1, DuelRules.compareAccuracy(7, 8, 10000, 50000))
        assertEquals(1, DuelRules.compareAccuracy(8, 8, 10000, 12000))
        assertEquals(-1, DuelRules.compareAccuracy(8, 8, 12000, 10000))
        assertEquals(0, DuelRules.compareAccuracy(8, 8, 10000, 10000))
    }

    @Test fun duelAnswerChoicesAreBoundedAndIncludeCorrectAnswer() {
        for (cap in listOf(3, 5, 10, 20, 100)) {
            for (seed in 0..20) {
                val q = AdventureBank.generate(SkillTopic.MIXED, random = Random(seed),
                    maxNumber = cap)
                val choices = DuelRules.choices(q, cap)
                assertEquals(4, choices.size)
                assertEquals(4, choices.distinct().size)
                assertTrue(choices.all { it in 0..cap })
                assertTrue(choices.contains(q.answer))
            }
        }
    }

    @Test fun historicalKeysAreReversible() {
        val q = SkillQuestion(12, 4, false)
        assertEquals(q, parseRecordedQuestion(q.learningKey))
        val shop = SkillQuestion(0, 0, price = 8, paid = 10, role = ShopRole.CASHIER)
        assertEquals(shop, parseRecordedQuestion(shop.learningKey))
        assertNull(parseRecordedQuestion("invalid"))
    }
}
