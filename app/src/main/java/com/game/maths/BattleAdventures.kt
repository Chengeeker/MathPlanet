package com.game.maths

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

data class ArenaOptions(
    val game: BattleGame = BattleGame.LIGHTNING,
    val rounds: Int = 10,
    val vsComputer: Boolean = false,
    val computerLevel: Int = 1,
    val pool: SkillTopic = SkillTopic.MIXED
)

@Composable
fun BattlePicker(
    maxNumber: Int, onBack: () -> Unit,
    onStart: (ArenaOptions) -> Unit
) {
    var selection by remember { mutableStateOf(ArenaOptions()) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
        .padding(19.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallPill("← 游戏选择", onBack)
            Spacer(Modifier.width(12.dp))
            Text("🏆 对战竞技场", color = AdventureInk, fontWeight = FontWeight.Black, fontSize = 24.sp)
        }
        Text("平板将自动调整至竖屏。两侧独立操作，中间隔离防误触。",
            color = AdventureInk, fontSize = 14.sp)
        Text("选择玩法", color = AdventureInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BattleGame.entries.forEach { game ->
                AdventureChip(game.title, selection.game == game) {
                    selection = selection.copy(game = game,
                        rounds = if (game == BattleGame.STRATEGY) 10 else 20,
                        pool = SkillTopic.MIXED)
                }
            }
        }
        Text("对手类型", color = AdventureInk, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdventureChip("🧑‍🤝‍🧑 本地双人", !selection.vsComputer) {
                selection = selection.copy(vsComputer = false)
            }
            AdventureChip("🤖 AI 电脑", selection.vsComputer) {
                selection = selection.copy(vsComputer = true)
            }
        }
        if (selection.vsComputer) {
            Text("AI 难度", color = AdventureInk, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("慢", "普通", "高手").forEachIndexed { i, level ->
                    AdventureChip(level, selection.computerLevel == i) {
                        selection = selection.copy(computerLevel = i)
                    }
                }
            }
        }
        Text("每局题量", color = AdventureInk, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (if (selection.game == BattleGame.LIGHTNING) listOf(10, 20, 30)
             else listOf(10, 20)).forEach { count ->
                AdventureChip("$count 题", selection.rounds == count) {
                    selection = selection.copy(rounds = count)
                }
            }
        }
        if (selection.game == BattleGame.LIGHTNING) {
            Text("抢答赛题库", color = AdventureInk, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdventureChip("🔢 0～$maxNumber 加减", selection.pool != SkillTopic.MONEY) {
                    selection = selection.copy(pool = SkillTopic.MIXED)
                }
                AdventureChip("🪙 人民币找零", selection.pool == SkillTopic.MONEY) {
                    selection = selection.copy(pool = SkillTopic.MONEY)
                }
            }
        } else Text("巧思挑战：仅使用进位加法 / 退位减法，无倒计时。",
            color = AdventureInk)
        KidMainButton("🚀 开始对战", { onStart(selection) }, AdventureOrange)
    }
}

@Composable
fun BattleAdventureScreen(
    options: ArenaOptions, maxNumber: Int, context: Context,
    audio: AudioDirector, onBack: () -> Unit, onAgain: () -> Unit
) {
    val activity = context as? Activity
    DisposableEffect(activity) {
        val oldOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            if (oldOrientation != null) activity.requestedOrientation = oldOrientation
        }
    }
    val questions = remember(options, maxNumber) {
        AdventureBank.makeRound(
            if (options.game == BattleGame.STRATEGY) SkillTopic.MIXED else options.pool,
            options.rounds, TrainingHistory.weakKeys(context),
            maxNumber = maxNumber,
            shopRole = if (options.pool == SkillTopic.MONEY) ShopRole.CASHIER else null
        )
    }
    var index by remember { mutableIntStateOf(0) }
    var scoreLeft by remember { mutableStateOf(0f) }
    var scoreRight by remember { mutableStateOf(0f) }
    var leftResult by remember { mutableStateOf<Int?>(null) }
    var rightResult by remember { mutableStateOf<Int?>(null) }
    var leftAt by remember { mutableLongStateOf(0L) }
    var rightAt by remember { mutableLongStateOf(0L) }
    var lockedLeft by remember { mutableStateOf(false) }
    var lockedRight by remember { mutableStateOf(false) }
    var leftMethodCount by remember { mutableIntStateOf(0) }
    var rightMethodCount by remember { mutableIntStateOf(0) }
    var leftHits by remember { mutableIntStateOf(0) }
    var rightHits by remember { mutableIntStateOf(0) }
    var leftStreak by remember { mutableIntStateOf(0) }
    var rightStreak by remember { mutableIntStateOf(0) }
    var leftBest by remember { mutableIntStateOf(0) }
    var rightBest by remember { mutableIntStateOf(0) }
    var leftWrongThisQuestion by remember { mutableStateOf(false) }
    var rightWrongThisQuestion by remember { mutableStateOf(false) }
    var leftTotalAnswerMs by remember { mutableLongStateOf(0L) }
    var rightTotalAnswerMs by remember { mutableLongStateOf(0L) }
    var questionStartedAt by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var ended by remember { mutableStateOf(false) }
    var started by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val q = questions.getOrNull(index)

    fun record(side: Int, correct: Boolean, method: Boolean) {
        if (side == 1 && options.vsComputer) return
        q?.let {
            TrainingHistory.add(context, TrainingRecord(
                "${options.game.title}·${if (side == 0) "左侧" else "右侧"}",
                it.expression, it.learningKey, correct, method,
                SystemClock.elapsedRealtime() - started))
        }
    }
    fun mark(side: Int, correct: Boolean, method: Boolean) {
        if (q == null || ended) return
        if (side == 0 && leftResult != null || side == 1 && rightResult != null) return
        if (options.game == BattleGame.LIGHTNING && !correct) {
            if (side == 0) { lockedLeft = true; leftWrongThisQuestion = true }
            else { lockedRight = true; rightWrongThisQuestion = true }
            audio.retry()
            return
        }
        val points = if (!correct) 0 else if (options.game == BattleGame.STRATEGY && method) 2 else 1
        if (side == 0) {
            if (correct) leftTotalAnswerMs += SystemClock.elapsedRealtime() - questionStartedAt
            leftResult = points
            leftAt = SystemClock.elapsedRealtime()
            if (correct) {
                leftHits++; leftStreak++; leftBest = maxOf(leftBest, leftStreak)
                if (method) leftMethodCount++
            } else leftStreak = 0
        } else {
            if (correct) rightTotalAnswerMs += SystemClock.elapsedRealtime() - questionStartedAt
            rightResult = points
            rightAt = SystemClock.elapsedRealtime()
            if (correct) {
                rightHits++; rightStreak++; rightBest = maxOf(rightBest, rightStreak)
                if (method) rightMethodCount++
            } else rightStreak = 0
        }
        if (correct) audio.success()
        val clean = if (side == 0) !leftWrongThisQuestion else !rightWrongThisQuestion
        record(side, correct && clean, method && correct && clean)
    }
    LaunchedEffect(lockedLeft) {
        if (lockedLeft) { delay(1000); lockedLeft = false }
    }
    LaunchedEffect(lockedRight) {
        if (lockedRight) { delay(1000); lockedRight = false }
    }
    LaunchedEffect(index, options.vsComputer, options.computerLevel) {
        if (options.vsComputer && !ended) {
            val reaction = listOf(4600L, 2800L, 1300L)[options.computerLevel.coerceIn(0,2)]
            delay(reaction)
            if (rightResult == null && !ended) {
                // Experts prefer proper strategies; novices are deliberately less reliable.
                val accurate = Random.nextInt(100) < listOf(67, 84, 96)[options.computerLevel.coerceIn(0,2)]
                val method = options.game == BattleGame.STRATEGY &&
                    Random.nextInt(100) < listOf(45, 72, 94)[options.computerLevel.coerceIn(0,2)]
                mark(1, accurate, method)
            }
        }
    }
    // A short fairness window lets two practically simultaneous correct touches both score.
    LaunchedEffect(index, leftResult, rightResult) {
        if (options.game == BattleGame.LIGHTNING && (leftResult != null || rightResult != null) && !ended) {
            delay(240)
            val both = leftResult != null && rightResult != null && abs(leftAt - rightAt) < 240
            if (both && leftResult!! > 0 && rightResult!! > 0) {
                scoreLeft += .5f; scoreRight += .5f
            } else if (leftResult != null && leftResult!! > 0 &&
                (rightResult == null || rightAt > leftAt)) scoreLeft += 1f
            else if (rightResult != null && rightResult!! > 0 &&
                (leftResult == null || leftAt > rightAt)) scoreRight += 1f
            delay(500)
            if (index == questions.lastIndex) ended = true
            else {
                index++; leftResult = null; rightResult = null
                leftAt = 0; rightAt = 0; lockedLeft = false; lockedRight = false
                leftWrongThisQuestion = false; rightWrongThisQuestion = false
                questionStartedAt = SystemClock.elapsedRealtime()
            }
        }
        if (options.game == BattleGame.STRATEGY && leftResult != null && rightResult != null && !ended) {
            scoreLeft += leftResult!!.toFloat()
            scoreRight += rightResult!!.toFloat()
            delay(750)
            if (index == questions.lastIndex) ended = true
            else {
                index++; leftResult = null; rightResult = null
                leftAt = 0; rightAt = 0
                leftWrongThisQuestion = false; rightWrongThisQuestion = false
                questionStartedAt = SystemClock.elapsedRealtime()
            }
        }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallPill("← 退出对战", onBack)
            Spacer(Modifier.weight(1f))
            Text("${index + 1}/${questions.size} 题",
                color = AdventureInk, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("⏱ ${(SystemClock.elapsedRealtime() - started) / 1000}秒",
                color = AdventureInk)
        }
        if (ended) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .background(AdventureCard, RoundedCornerShape(22.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏆 对战结算", color = AdventureInk,
                    fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text(if (scoreLeft == scoreRight) {
                    if (leftTotalAnswerMs == rightTotalAnswerMs) "平局！得分、用时相同"
                    else if (leftTotalAnswerMs < rightTotalAnswerMs) "🎉 左侧用时更短，获胜！"
                    else "🎉 右侧用时更短，获胜！"
                } else if (scoreLeft > scoreRight) "🎉 左侧获胜！" else "🎉 右侧获胜！",
                    color = AdventureInk, fontSize = 23.sp)
                Text("左侧 $scoreLeft 分   VS   右侧 $scoreRight 分",
                    color = AdventureInk, fontSize = 22.sp)
                Text("左侧正确率 ${leftHits * 100 / questions.size}% · 连击 $leftBest · 方法正确 $leftMethodCount",
                    color = AdventureInk)
                Text("右侧正确率 ${rightHits * 100 / questions.size}% · 连击 $rightBest · 方法正确 $rightMethodCount",
                    color = AdventureInk)
                Text("总用时 ${(SystemClock.elapsedRealtime() - started) / 1000} 秒 · 左侧答题累计 ${leftTotalAnswerMs / 1000} 秒 · 右侧 ${rightTotalAnswerMs / 1000} 秒",
                    color = AdventureInk)
                KidMainButton("🔁 再战一局", onAgain, AdventureOrange)
                SmallPill("返回游戏选择", onBack)
            }
        } else if (q != null) {
            Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = .75f),
                RoundedCornerShape(18.dp)).padding(10.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("🔵 $scoreLeft 分", color = AdventureInk,
                    fontWeight = FontWeight.Black, fontSize = 19.sp)
                Spacer(Modifier.width(16.dp))
                Text("VS", color = Color(0xFFB75C3D), fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(16.dp))
                Text("🟠 $scoreRight 分", color = AdventureInk,
                    fontWeight = FontWeight.Black, fontSize = 19.sp)
            }
            Text(q.expression, modifier = Modifier.fillMaxWidth(),
                color = AdventureInk, fontSize = 29.sp, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.weight(1f).fillMaxSize().background(Color(0xFFE8F3FF),
                    RoundedCornerShape(20.dp)).padding(7.dp)) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔵 左侧玩家", color = AdventureInk, fontWeight = FontWeight.Black)
                        key(index) {
                            BattleAnswerSide(q, options, maxNumber, leftResult != null || lockedLeft,
                                leftResult) { correct, method -> mark(0, correct, method) }
                        }
                    }
                }
                Box(Modifier.width(7.dp).fillMaxHeight()
                    .background(Color(0xFFB6C5D6), RoundedCornerShape(10.dp)))
                Box(Modifier.weight(1f).fillMaxSize().background(Color(0xFFFFF0E2),
                    RoundedCornerShape(20.dp)).padding(7.dp)) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (options.vsComputer) "🤖 AI 电脑" else "🟠 右侧玩家",
                            color = AdventureInk, fontWeight = FontWeight.Black)
                        if (options.vsComputer) Text("电脑正在思考…", color = AdventureInk)
                        else key(index) {
                            BattleAnswerSide(q, options, maxNumber, rightResult != null || lockedRight,
                                rightResult) { correct, method -> mark(1, correct, method) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleAnswerSide(
    q: SkillQuestion, options: ArenaOptions, maxNumber: Int,
    disabled: Boolean, result: Int?, onAnswer: (Boolean, Boolean) -> Unit
) {
    if (result != null) {
        Text(if (result > 0) "✅ 完成，得 $result 分" else "🌈 再接再厉",
            color = AdventureInk, fontWeight = FontWeight.Black)
        return
    }
    if (options.game == BattleGame.STRATEGY) {
        StrategyStepBoard(q, disabled, onAnswer)
        Text("也可以直接选答案（正确仅 +1 分）",
            color = AdventureInk, fontSize = 12.sp)
    }
    val optionsNumbers = remember(q, maxNumber) {
        val list = linkedSetOf(q.answer)
        for (distance in 1..maxNumber.coerceAtLeast(3)) {
            for (value in listOf(q.answer - distance, q.answer + distance).shuffled()) {
                if (value in 0..maxNumber && list.size < 4) list += value
            }
            if (list.size >= 4) break
        }
        list.shuffled()
    }
    optionsNumbers.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEach { n ->
                androidx.compose.material3.Button(onClick = { onAnswer(n == q.answer, false) },
                    enabled = !disabled, modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = AdventureInk)) {
                    Text("$n", fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}


/** A generic two-step method that works even when the global range is 0..10. */
@Composable
private fun StrategyStepBoard(q: SkillQuestion, disabled: Boolean, onAnswer: (Boolean, Boolean) -> Unit) {
    val preferred = remember(q) {
        val toBoundary = if (q.add) 10 - (q.a % 10) else q.a % 10
        if (toBoundary in 1 until q.b) toBoundary else (q.b / 2).coerceAtLeast(1)
    }
    val candidates = remember(q) {
        (listOf(preferred, 1, (q.b - 1).coerceAtLeast(1), (q.b / 2).coerceAtLeast(1))
            .distinct() + (1..minOf(q.b, 9))).distinct().take(4)
    }
    var split by remember(q) { mutableIntStateOf(0) }
    var hint by remember(q) { mutableStateOf("") }
    Text("🧠 方法题：把 ${q.b} 拆成两部分，分两步计算。",
        color = AdventureInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    if (split == 0) {
        Text("第一步先算多少？", color = AdventureInk, fontSize = 13.sp)
        candidates.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { value ->
                    AdventureChip("先算 $value", false) {
                        if (!disabled) {
                            if (value == preferred) {
                                split = value
                                hint = ""
                            } else hint = "想想能不能先凑到整十，或把数字平均拆开。"
                        }
                    }
                }
            }
        }
    } else {
        val middle = if (q.add) q.a + split else q.a - split
        Text("第一步：${q.a} ${if (q.add) "+" else "−"} $split = $middle",
            color = AdventureInk, fontSize = 14.sp)
        Text("第二步：剩下 ${q.b - split}，再接着计算",
            color = AdventureInk, fontSize = 14.sp)
        KidMainButton("✅ 完成两步方法", { if (!disabled) onAnswer(true, true) },
            AdventureOrange, 47.dp)
    }
    if (hint.isNotEmpty()) Text(hint, color = Color(0xFF9A6540), fontSize = 12.sp)
}
