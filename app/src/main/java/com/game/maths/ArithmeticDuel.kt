package com.game.maths

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class DuelPriority { SPEED, ACCURACY }

/** Players sit at opposite short ends; each half must face its own edge. */
internal object DuelSeatLayout {
    fun rotationFor(side: Int): Float = if (side == 0) 90f else -90f
}

/** Both edges show the shared question in speed mode; accuracy uses independent progress. */
internal object DuelProgress {
    fun topAndBottom(total: Int, speed: Boolean, sharedIndex: Int,
                     leftIndex: Int, rightIndex: Int): Pair<Int, Int> {
        val top = if (speed) sharedIndex else leftIndex
        val bottom = if (speed) sharedIndex else rightIndex
        return (top + 1).coerceIn(1, total) to (bottom + 1).coerceIn(1, total)
    }
}
internal data class DuelAnswerFeedback(val chosen: Int, val isCorrect: Boolean)
data class DuelOptions(
    val priority: DuelPriority = DuelPriority.SPEED,
    val vsComputer: Boolean = false,
    val computerLevel: Int = 0,
    val rounds: Int = 10
)

internal object DuelRules {
    /** The answer is only revealed after a round is completed or this player submitted. */
    fun displayedEquation(
        q: SkillQuestion, feedback: DuelAnswerFeedback?, revealCorrect: Boolean
    ): String {
        val number = when {
            revealCorrect -> q.answer.toString()
            feedback?.isCorrect == true -> q.answer.toString()
            feedback != null -> "${feedback.chosen} ✗"
            else -> "?"
        }
        return "${q.a} ${if (q.add) "+" else "−"} ${q.b} = $number"
    }

    fun feedbackText(q: SkillQuestion, feedback: DuelAnswerFeedback?, revealCorrect: Boolean): String {
        if (feedback != null) return if (feedback.isCorrect)
            "✅ 答对了！${displayedEquation(q, feedback, true)}"
        else if (revealCorrect) "❌ 你选了 ${feedback.chosen}，正确答案是 ${q.answer}"
        else "❌ ${feedback.chosen} 不正确，再想一想！"
        return if (revealCorrect) "本题正确答案是 ${q.answer}" else "认真计算，再选答案"
    }

    fun aiReactionMs(level: Int, random: Random = Random.Default): Long {
        // Random per-question reaction in milliseconds; half-second bounds are exact.
        val (min, max) = when (level.coerceIn(0, 3)) {
            0 -> 6_000L to 8_000L
            1 -> 4_000L to 6_000L
            2 -> 2_500L to 4_000L
            else -> 1_500L to 2_500L
        }
        return random.nextLong(min, max + 1)
    }

    /** Correct count always wins; only a tie is decided by time. */
    fun compareAccuracy(leftCorrect: Int, rightCorrect: Int, leftMs: Long, rightMs: Long): Int =
        when {
            leftCorrect > rightCorrect -> 1
            leftCorrect < rightCorrect -> -1
            leftMs < rightMs -> 1
            leftMs > rightMs -> -1
            else -> 0
        }

    fun choices(question: SkillQuestion, cap: Int): List<Int> {
        val available = linkedSetOf(question.answer)
        var distance = 1
        while (available.size < 4 && distance <= cap) {
            if (available.size < 4 && question.answer - distance >= 0)
                available.add(question.answer - distance)
            if (available.size < 4 && question.answer + distance <= cap)
                available.add(question.answer + distance)
            distance++
        }
        return available.shuffled(Random(question.learningKey.hashCode()))
    }
}

@Composable
fun ArithmeticDuelPicker(
    maxNumber: Int,
    onBack: () -> Unit, onStart: (DuelOptions) -> Unit
) {
    // The real-player option is always visible; OEM tablet-width reporting is unreliable.
    var selected by remember { mutableStateOf(DuelOptions(vsComputer = false)) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallPill("← 游戏选择", onBack)
            Spacer(Modifier.width(8.dp))
            PinyinText("⚔️ 计算对决", color = AdventureInk,
                fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
        PinyinText("题目遵守学习范围 0～$maxNumber；仅加减法，不与原竞技场混合。",
            color = AdventureInk, fontSize = 14.sp)
        PinyinText("对决对象", color = AdventureInk, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdventureChip("👫 真人双人", !selected.vsComputer) {
                selected = selected.copy(vsComputer = false)
            }
            AdventureChip("🤖 AI 对决", selected.vsComputer) {
                selected = selected.copy(vsComputer = true)
            }
        }
        PinyinText("获胜规则", color = AdventureInk, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdventureChip("⚡ 速度优先", selected.priority == DuelPriority.SPEED) {
                selected = selected.copy(priority = DuelPriority.SPEED)
            }
            AdventureChip("🎯 正确率优先", selected.priority == DuelPriority.ACCURACY) {
                selected = selected.copy(priority = DuelPriority.ACCURACY)
            }
        }
        PinyinText(
            if (selected.priority == DuelPriority.SPEED)
                "双方做同一道题，谁先答对谁得 1 分，当前题立即结束；答错不影响对手。"
            else "双方各自完成同一套题，答错也进入下一题；正确题数多者赢，同分时总用时短者赢。",
            color = AdventureInk, fontSize = 14.sp, lineHeight = 21.sp)
        if (selected.vsComputer) {
            PinyinText("🤖 AI 反应速度",
                color = AdventureInk, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("简单", "中等", "困难", "噩梦").forEachIndexed { index, title ->
                    AdventureChip(title, selected.computerLevel == index) {
                        selected = selected.copy(computerLevel = index)
                    }
                }
            }
            PinyinText("只影响 AI 作答快慢；算式数字遵守探险设置中的学习范围。",
                color = AdventureInk, fontSize = 12.sp)
        } else {
            PinyinText("真人模式已开放：进入后自动横屏，左右两端分别旋转 90°。建议使用平板，手机屏幕较小。",
                color = AdventureInk, fontSize = 13.sp)
        }
        PinyinText("对决题数", color = AdventureInk, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20).forEach { amount ->
                AdventureChip("$amount 题", selected.rounds == amount) {
                    selected = selected.copy(rounds = amount)
                }
            }
        }
        KidMainButton("🚀 开始对决", { onStart(selected) }, AdventureOrange, 58.dp)
    }
}

@Composable
fun ArithmeticDuelScreen(
    options: DuelOptions, maxNumber: Int,
    context: Context, audio: AudioDirector,
    onBack: () -> Unit, onAgain: () -> Unit
) {
    val activity = context as? Activity
    DisposableEffect(activity, options.vsComputer) {
        val previous = activity?.requestedOrientation
        if (!options.vsComputer) activity?.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { if (previous != null) activity.requestedOrientation = previous }
    }
    val questions = remember(options, maxNumber) {
        AdventureBank.makeRound(SkillTopic.MIXED, options.rounds, maxNumber = maxNumber)
    }
    var raceIndex by remember { mutableIntStateOf(0) }
    var leftIndex by remember { mutableIntStateOf(0) }
    var rightIndex by remember { mutableIntStateOf(0) }
    var raceWinner by remember { mutableIntStateOf(-1) }
    var speedDone by remember { mutableStateOf(false) }
    var leftCorrect by remember { mutableIntStateOf(0) }
    var rightCorrect by remember { mutableIntStateOf(0) }
    var leftMs by remember { mutableLongStateOf(0L) }
    var rightMs by remember { mutableLongStateOf(0L) }
    var leftStart by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var rightStart by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var wrongLeft by remember { mutableStateOf(false) }
    var wrongRight by remember { mutableStateOf(false) }
    var leftFeedback by remember { mutableStateOf<DuelAnswerFeedback?>(null) }
    var rightFeedback by remember { mutableStateOf<DuelAnswerFeedback?>(null) }
    val questionCap = maxNumber
    val speed = options.priority == DuelPriority.SPEED
    val ended = if (speed) speedDone else
        leftIndex == questions.size && rightIndex == questions.size
    val leftQ = questions.getOrNull(if (speed) raceIndex else leftIndex)
    val rightQ = questions.getOrNull(if (speed) raceIndex else rightIndex)

    fun record(side: Int, q: SkillQuestion, correct: Boolean, elapsed: Long) {
        if (side == 1 && options.vsComputer) return
        TrainingHistory.add(context, TrainingRecord(
            "计算对决·${if (side == 0) "左侧" else "右侧"}",
            q.expression, q.learningKey, correct, correct, elapsed))
    }

    fun answer(side: Int, number: Int) {
        if (ended || speed && raceWinner >= 0 ||
            !speed && (if (side == 0) leftFeedback != null else rightFeedback != null)) return
        val q = if (side == 0) leftQ else rightQ
        if (q == null) return
        if (speed && (if (side == 0) wrongLeft else wrongRight)) return
        val correct = number == q.answer
        val now = SystemClock.elapsedRealtime()
        val elapsed = (now - if (side == 0) leftStart else rightStart).coerceAtLeast(0L)
        if (speed) {
            if (!correct) {
                if (side == 0) {
                    wrongLeft = true
                    leftFeedback = DuelAnswerFeedback(number, false)
                } else {
                    wrongRight = true
                    rightFeedback = DuelAnswerFeedback(number, false)
                }
                if (side == 0) audio.retry()
                // A wrong answer never stops the other player or reveals the correct result.
                return
            }
            if (side == 0) {
                leftCorrect++; leftMs += elapsed
                leftFeedback = DuelAnswerFeedback(number, true)
            } else {
                rightCorrect++; rightMs += elapsed
                rightFeedback = DuelAnswerFeedback(number, true)
            }
            record(side, q, true, elapsed)
            raceWinner = side
            if (side == 0 || !options.vsComputer) audio.success()
        } else {
            if (side == 0) {
                if (correct) leftCorrect++
                leftMs += elapsed
                leftFeedback = DuelAnswerFeedback(number, correct)
            } else {
                if (correct) rightCorrect++
                rightMs += elapsed
                rightFeedback = DuelAnswerFeedback(number, correct)
            }
            record(side, q, correct, elapsed)
            if (side == 0) {
                if (correct) audio.success() else audio.retry()
            }
        }
    }

    LaunchedEffect(wrongLeft, raceIndex) {
        if (wrongLeft) {
            delay(850)
            wrongLeft = false
            if (raceWinner < 0) leftFeedback = null
        }
    }
    LaunchedEffect(wrongRight, raceIndex) {
        if (wrongRight) {
            delay(850)
            wrongRight = false
            if (raceWinner < 0) rightFeedback = null
        }
    }
    // Accuracy mode keeps the worked answer visible for a moment before independently advancing.
    LaunchedEffect(leftFeedback, leftIndex) {
        if (!speed && leftFeedback != null) {
            delay(1_450)
            if (leftIndex < questions.size) {
                leftIndex++
                leftStart = SystemClock.elapsedRealtime()
                leftFeedback = null
            }
        }
    }
    LaunchedEffect(rightFeedback, rightIndex) {
        if (!speed && rightFeedback != null) {
            delay(if (options.vsComputer) 1_200 else 1_450)
            if (rightIndex < questions.size) {
                rightIndex++
                rightStart = SystemClock.elapsedRealtime()
                rightFeedback = null
            }
        }
    }
    LaunchedEffect(raceWinner) {
        if (speed && raceWinner >= 0 && !speedDone) {
            delay(1_650)
            if (raceIndex + 1 >= questions.size) speedDone = true
            else {
                raceIndex++
                val now = SystemClock.elapsedRealtime()
                leftStart = now
                rightStart = now
                wrongLeft = false
                wrongRight = false
                leftFeedback = null
                rightFeedback = null
                raceWinner = -1
            }
        }
    }
    // AI never jumps instantly: each question gets a fresh random reaction delay.
    val aiQuestionIndex = if (speed) raceIndex else rightIndex
    LaunchedEffect(aiQuestionIndex, options.vsComputer, options.computerLevel,
        options.priority, raceWinner) {
        if (options.vsComputer && !ended && (speed.not() || raceWinner < 0)) {
            val delayMs = DuelRules.aiReactionMs(options.computerLevel)
            delay(delayMs)
            if (!ended && (!speed || raceWinner < 0) &&
                aiQuestionIndex == (if (speed) raceIndex else rightIndex)) {
                questions.getOrNull(aiQuestionIndex)?.let { answer(1, it.answer) }
            }
        }
    }

    if (ended) {
        val verdict = if (speed) leftCorrect.compareTo(rightCorrect)
            else DuelRules.compareAccuracy(leftCorrect, rightCorrect, leftMs, rightMs)
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
            .padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)) {
            PinyinText("🏆 计算对决结束", color = AdventureInk,
                fontSize = 25.sp, fontWeight = FontWeight.Black)
            PinyinText(when {
                verdict == 0 -> "🤝 平局！"
                verdict > 0 -> "🎉 左侧玩家获胜！"
                options.vsComputer -> "🤖 AI 获胜，这次再挑战吧！"
                else -> "🎉 右侧玩家获胜！"
            }, color = AdventureInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            PinyinText("🔵 左侧：$leftCorrect 道正确 · ${leftMs / 1000.0} 秒",
                color = AdventureInk, fontSize = 18.sp)
            PinyinText("${if (options.vsComputer) "🤖 AI" else "🟠 右侧"}：$rightCorrect 道正确 · ${rightMs / 1000.0} 秒",
                color = AdventureInk, fontSize = 18.sp)
            PinyinText(if (speed) "速度优先：先答对者获得该题的 1 分"
                else "正确率优先：正确题数优先，同分再比较用时",
                color = AdventureInk, fontSize = 13.sp)
            KidMainButton("🔁 再战一局", onAgain, AdventureOrange, 55.dp)
            SmallPill("返回对决设置", onBack)
        }
    } else if (options.vsComputer) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
            .padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SmallPill("← 退出对决", onBack)
                Spacer(Modifier.weight(1f))
                PinyinText("🔵 $leftCorrect  :  $rightCorrect 🤖",
                    color = AdventureInk, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            PinyinText(if (speed) "第 ${raceIndex + 1}/${questions.size} 题"
                else "你的进度 ${leftIndex.coerceAtMost(questions.size)}/${questions.size} · AI ${rightIndex.coerceAtMost(questions.size)}/${questions.size}",
                color = AdventureInk, fontSize = 14.sp)
            if (speed && raceWinner >= 0) PinyinText(if (raceWinner == 0) "✅ 你抢答成功！" else "🤖 AI 抢答成功",
                color = AdventureInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
            if (leftQ == null) PinyinText("✅ 你已完成，等待 AI 答完。", color = AdventureInk)
            else key(leftQ.learningKey, if (speed) raceIndex else leftIndex) {
                DuelQuestionBoard(leftQ, questionCap, "🧑 你的题目",
                    disabled = (speed && raceWinner >= 0) || wrongLeft ||
                        (!speed && leftFeedback != null),
                    feedback = leftFeedback,
                    revealCorrect = if (speed) raceWinner >= 0 else leftFeedback != null,
                    onAnswer = { answer(0, it) })
            }
            Column(Modifier.fillMaxWidth().background(AdventureCard,
                RoundedCornerShape(16.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                PinyinText("🤖 AI ${if (rightQ == null) "已完成" else if (rightFeedback != null) "已作答" else "正在解题"} · ${listOf("简单", "中等", "困难", "噩梦")[options.computerLevel.coerceIn(0, 3)]}",
                    color = AdventureInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (rightQ != null) {
                    // In accuracy mode do not expose an AI answer to a question
                    // the child is still working on; that would defeat the practice.
                    val revealed = rightFeedback != null && (
                        if (speed) raceWinner >= 0
                        else leftIndex > rightIndex ||
                            (leftIndex == rightIndex && leftFeedback != null)
                    )
                    PinyinText(DuelRules.displayedEquation(
                        rightQ, if (revealed) rightFeedback else null, revealed),
                        color = AdventureInk, fontWeight = FontWeight.Black, fontSize = 23.sp)
                    if (revealed)
                        PinyinText("正确答案 ${rightQ.answer} · AI 已提交",
                            color = AdventureInk, fontSize = 13.sp)
                    else if (rightFeedback != null)
                        PinyinText("AI 已作答，等你完成同一道题再揭晓答案",
                            color = AdventureInk, fontSize = 13.sp)
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            // Keep the exit action in the header. Progress lives at BOTH ends of
            // the center divider and the score is centered between the players.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SmallPill("← 退出对决", onBack)
            }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RotatedDuelSeat(
                    q = leftQ, cap = questionCap,
                    heading = "🔵 左侧玩家",
                    orientation = DuelSeatLayout.rotationFor(0), background = Color(0xFFE3F1FF),
                    disabled = (speed && raceWinner >= 0) || wrongLeft ||
                        (!speed && leftFeedback != null),
                    feedback = leftFeedback,
                    revealCorrect = if (speed) raceWinner >= 0 else leftFeedback != null,
                    status = when {
                        speed && raceWinner == 0 -> "✅ 你抢到了这一分！"
                        speed && raceWinner == 1 -> "对方先答对了"
                        wrongLeft && raceWinner < 0 -> "再想一想，别急。"
                        leftQ == null -> "✅ 已完成，等待对方"
                        else -> ""
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) { answer(0, it) }
                DuelCenterStatus(
                    total = questions.size,
                    speed = speed,
                    sharedIndex = raceIndex,
                    leftIndex = leftIndex,
                    rightIndex = rightIndex,
                    leftScore = leftCorrect,
                    rightScore = rightCorrect
                )
                RotatedDuelSeat(
                    q = rightQ, cap = questionCap,
                    heading = "🟠 右侧玩家",
                    orientation = DuelSeatLayout.rotationFor(1), background = Color(0xFFFFF0E0),
                    disabled = (speed && raceWinner >= 0) || wrongRight ||
                        (!speed && rightFeedback != null),
                    feedback = rightFeedback,
                    revealCorrect = if (speed) raceWinner >= 0 else rightFeedback != null,
                    status = when {
                        speed && raceWinner == 1 -> "✅ 你抢到了这一分！"
                        speed && raceWinner == 0 -> "对方先答对了"
                        wrongRight && raceWinner < 0 -> "再想一想，别急。"
                        rightQ == null -> "✅ 已完成，等待对方"
                        else -> ""
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) { answer(1, it) }
            }
        }
    }
}

/** Shared scoreboard on the boundary: top/bottom progress, score at the exact center. */
@Composable
private fun DuelCenterStatus(
    total: Int, speed: Boolean, sharedIndex: Int, leftIndex: Int, rightIndex: Int,
    leftScore: Int, rightScore: Int
) {
    val (topQuestion, bottomQuestion) = DuelProgress.topAndBottom(
        total, speed, sharedIndex, leftIndex, rightIndex
    )
    Column(
        Modifier.width(92.dp).fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = .83f))
            .padding(horizontal = 3.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        PinyinText("第 $topQuestion/$total 题",
            color = AdventureInk, fontWeight = FontWeight.Black,
            fontSize = 13.sp, textAlign = TextAlign.Center)
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            PinyinText("$leftScore : $rightScore",
                color = AdventureInk, fontWeight = FontWeight.Black,
                fontSize = 26.sp, textAlign = TextAlign.Center)
            PinyinText("🔵  VS  🟠", color = AdventureInk, fontSize = 12.sp,
                textAlign = TextAlign.Center)
            PinyinText("比分", color = AdventureInk.copy(alpha = .7f),
                fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        PinyinText("第 $bottomQuestion/$total 题",
            color = AdventureInk, fontWeight = FontWeight.Black,
            fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RotatedDuelSeat(
    q: SkillQuestion?, cap: Int, heading: String, orientation: Float,
    background: Color, disabled: Boolean, status: String,
    feedback: DuelAnswerFeedback?, revealCorrect: Boolean,
    modifier: Modifier, onAnswer: (Int) -> Unit
) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(15.dp)).background(background),
        contentAlignment = Alignment.Center) {
        // Swap width/height before rotating. The resulting visible and touchable
        // panel occupies precisely one physical half of the landscape tablet.
        val logicalWidth = maxHeight
        val logicalHeight = maxWidth
        Column(
            Modifier.requiredWidth(logicalWidth).requiredHeight(logicalHeight)
                .graphicsLayer { rotationZ = orientation }
                .padding(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            PinyinText(heading, color = AdventureInk, fontWeight = FontWeight.Black,
                fontSize = 16.sp, textAlign = TextAlign.Center)
            if (q == null) PinyinText("✅ 已完成！", fontSize = 22.sp, color = AdventureInk)
            else {
                PinyinText(DuelRules.displayedEquation(q, feedback, revealCorrect),
                    color = AdventureInk, fontSize = 30.sp,
                    fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                if (feedback != null || revealCorrect) {
                    PinyinText(DuelRules.feedbackText(q, feedback, revealCorrect),
                        color = AdventureInk, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
            PinyinText(status.ifBlank { "认真计算，再选答案" }, color = AdventureInk,
                fontSize = 13.sp, textAlign = TextAlign.Center)
            // Controls last = nearest each child's outer edge after rotation.
            if (q != null) DuelChoiceGrid(q, cap, disabled, onAnswer)
        }
    }
}

@Composable
private fun DuelQuestionBoard(
    q: SkillQuestion, cap: Int, heading: String,
    disabled: Boolean, feedback: DuelAnswerFeedback?, revealCorrect: Boolean,
    onAnswer: (Int) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().background(AdventureCard, RoundedCornerShape(22.dp))
            .padding(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PinyinText(heading, fontSize = 19.sp, color = AdventureInk,
            fontWeight = FontWeight.Black)
        PinyinText(DuelRules.displayedEquation(q, feedback, revealCorrect),
            fontSize = 35.sp, color = AdventureInk, fontWeight = FontWeight.Black)
        if (feedback != null || revealCorrect) {
            PinyinText(DuelRules.feedbackText(q, feedback, revealCorrect),
                color = AdventureInk, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
        DuelChoiceGrid(q, cap, disabled, onAnswer)
    }
}

@Composable
private fun DuelChoiceGrid(q: SkillQuestion, cap: Int,
    disabled: Boolean, onAnswer: (Int) -> Unit
) {
    val selections = remember(q, cap) { DuelRules.choices(q, cap) }
    selections.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { n ->
                Button(onClick = { onAnswer(n) }, enabled = !disabled,
                    modifier = Modifier.weight(1f).height(58.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = AdventureInk)
                ) {
                    PinyinText("$n", fontSize = 25.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
