package com.game.maths

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun SoloAdventureScreen(
    game: SoloGame, rounds: Int, maxNumber: Int,
    shopRole: ShopRole, onShopRoleChange: (ShopRole) -> Unit,
    context: Context, audio: AudioDirector, review: List<SkillQuestion> = emptyList(),
    onBack: () -> Unit, onAgain: () -> Unit
) {
    val topic = if (game == SoloGame.SHOP) SkillTopic.MONEY else SkillTopic.MIXED
    val questions = remember(game, rounds, review, shopRole, maxNumber) {
        if (review.isNotEmpty()) review else AdventureBank.makeRound(
            topic, rounds, TrainingHistory.weakKeys(context),
            maxNumber = maxNumber, shopRole = shopRole)
    }
    var index by remember { mutableIntStateOf(0) }
    var resolved by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var firstTry by remember { mutableStateOf(true) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
     var started by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    val p = questions.getOrNull(index)
    val label = if (review.isNotEmpty()) "错题专项复习" else game.title

    fun record(ok: Boolean, method: Boolean) {
        val q = p ?: return
        TrainingHistory.add(context, TrainingRecord(label, q.expression, q.learningKey,
            ok, method, SystemClock.elapsedRealtime() - started))
    }
    fun submit(ok: Boolean, tip: String) {
        if (resolved || done) return
        if (!ok) {
            firstTry = false
            streak = 0
            message = tip
            audio.retry()
            if (p?.isMoney == true) audio.sayShopFeedback(false, tip.contains("多"))
            return
        }
        record(firstTry, firstTry)
        score += if (firstTry) 1 + streak.coerceAtMost(3) else 1
        streak++
        bestStreak = maxOf(bestStreak, streak)
        resolved = true
        message = if (firstTry) "🎉 方法正确！你成功啦！" else "🌈 已经找到正确方法，继续加油！"
        audio.success()
        if (p?.isMoney == true) audio.sayShopFeedback(true, false)
    }
    fun next() {
        audio.tap()
        audio.stopVoice()
        if (index + 1 >= questions.size) done = true
        else {
            index++
            resolved = false
            firstTry = true
            message = ""
             started = SystemClock.elapsedRealtime()
        }
    }
    // Shop and mistake-review modes are relaxed, untimed practice.
    LaunchedEffect(index, game) {
        val q = p ?: return@LaunchedEffect
        delay(150)
        if (q.isMoney) audio.sayShopNumbers(q.price, q.paid)
        else audio.sayQuestion(MathProblem(q.a, listOf(MathStep(
            if (q.add) Operation.ADD else Operation.SUBTRACT, q.b)), q.answer,
            listOf(q.answer)), index + 1)
    }

    // Every money question uses the compact layout (including shop-related mistake review).
    // Confirm becomes Next in-place, so answering never moves the navigation button.
    if (p?.isMoney == true && !done) {
        CompactShopScreen(
            q = p, questionIndex = index, questionCount = questions.size,
            score = score, streak = streak, title = label,
            allowRoleSwitch = game == SoloGame.SHOP, activeRole = p.role,
            resolved = resolved, message = message,
            onBack = onBack,
            onRoleChange = { role -> audio.tap(); onShopRoleChange(role) },
            onCheck = ::submit, onNext = ::next
        )
        return
    }

    val soloScroll=rememberScrollState()
    LaunchedEffect(index,game) {soloScroll.scrollTo(0)}
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val wide = maxWidth >= 650.dp && maxWidth > maxHeight
        val pinNext=LandscapeActionPolicy.pinSoloNext(
            wide,resolved,done,p!=null)
        Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(soloScroll)
            .padding(if (wide) 22.dp else 12.dp)
            .padding(bottom=if(pinNext) 112.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()) {
                SmallPill("← 游戏选择", onBack)
                Spacer(Modifier.weight(1f))
                PinyinText("学习范围 0～$maxNumber", color = AdventureInk,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            PinyinText(game.emoji + " " + label, fontWeight = FontWeight.Black,
                color = AdventureInk, fontSize = if (wide) 26.sp else 22.sp,
                lineHeight = 30.sp)
            if (game == SoloGame.SHOP) {
                PinyinText("选一个身份开始游戏，切换身份会重新开始本轮",
                    color = AdventureInk, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdventureChip("🧺 顾客 · 付款", shopRole == ShopRole.CUSTOMER) {
                        audio.tap(); onShopRoleChange(ShopRole.CUSTOMER)
                    }
                    AdventureChip("🏪 店长 · 找零", shopRole == ShopRole.CASHIER) {
                        audio.tap(); onShopRoleChange(ShopRole.CASHIER)
                    }
                }
            }
            if (done) {
                Column(
                    Modifier.fillMaxWidth().background(AdventureCard, RoundedCornerShape(26.dp))
                        .padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    PinyinText("🏆 冒险完成！", color = AdventureInk, fontSize = 30.sp,
                        fontWeight = FontWeight.Black)
                    PinyinText("总得分 $score · 最长连击 $bestStreak · 完成 ${questions.size} 题",
                        color = AdventureInk, fontSize = 18.sp)
                    PinyinText("成绩已保存到家长数据中心，错题自动收录。",
                        color = AdventureInk, textAlign = TextAlign.Center)
                    KidMainButton("🚀 再玩一轮", onAgain, AdventureOrange)
                    SmallPill("返回游戏选择", onBack)
                }
            } else if (p != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    PinyinText("第 ${index + 1}/${questions.size} 题", color = AdventureInk,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    PinyinText("⭐ $score  🔥 $streak", color = AdventureInk, fontWeight = FontWeight.Bold)
                    // No countdown in the remaining solo modes.
                }
                if (wide) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        QuestionSide(p, Modifier.weight(.72f), maxNumber)
                        Column(Modifier.weight(1.28f).background(AdventureCard, RoundedCornerShape(27.dp))
                            .padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                            key(index) {
                                SoloBoard(p, game, maxNumber, resolved,
                                    onCheck = ::submit)
                            }
                            MessageAndNext(message, resolved,
                                index == questions.lastIndex, ::next, showNext = !wide)
                        }
                    }
                } else {
                    QuestionSide(p, Modifier.fillMaxWidth(), maxNumber)
                    Column(Modifier.fillMaxWidth().background(AdventureCard, RoundedCornerShape(27.dp))
                        .padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        key(index) { SoloBoard(p, game, maxNumber, resolved, ::submit) }
                        MessageAndNext(message, resolved, index == questions.lastIndex, ::next)
                    }
                }
            }
        }
        if(pinNext) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color(0xFFF2F9FF).copy(alpha=.97f))
                .padding(horizontal=22.dp,vertical=8.dp)) {
                KidMainButton(
                    if(index==questions.lastIndex) "🏆 查看练习报告" else "➡️ 下一题",
                    ::next,AdventureOrange,55.dp)
            }
        }
        }
    }
}

@Composable
private fun QuestionSide(q: SkillQuestion, modifier: Modifier, maxNumber: Int) {
    Column(
        modifier.background(Color(0xFFFFF1CC), RoundedCornerShape(26.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PinyinText(if (q.isMoney) "🛍 小小商店" else "🦕 认真想一想",
            modifier = Modifier.fillMaxWidth(), color = AdventureInk,
            fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (q.isMoney) {
            PinyinText("商品价格", color = AdventureInk, fontSize = 14.sp,
                textAlign = TextAlign.Center)
            PinyinText("¥${q.price}", color = AdventureInk, fontWeight = FontWeight.Black,
                fontSize = if (q.price > 999) 26.sp else 34.sp,
                lineHeight = 39.sp, textAlign = TextAlign.Center)
            if (q.role == ShopRole.CASHIER) {
                PinyinText("顾客付给你", color = AdventureInk, fontSize = 14.sp)
                PinyinText("¥${q.paid}", color = AdventureInk, fontWeight = FontWeight.Black,
                    fontSize = if (q.paid > 999) 26.sp else 30.sp,
                    lineHeight = 36.sp, textAlign = TextAlign.Center)
                PinyinText("请找出正确的零钱", color = AdventureInk,
                    fontSize = 15.sp, textAlign = TextAlign.Center)
            } else {
                PinyinText("请凑出刚好的付款金额", color = AdventureInk,
                    fontSize = 15.sp, textAlign = TextAlign.Center)
            }
        } else {
            PinyinText(q.expression, color = AdventureInk, fontWeight = FontWeight.Black,
                fontSize = if (q.expression.length > 16) 23.sp else 30.sp,
                lineHeight = 36.sp, textAlign = TextAlign.Center)
        }
        PinyinText("本题计算数字不超过 $maxNumber", color = Color(0xFF667692),
            fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MessageAndNext(message: String, resolved: Boolean, last: Boolean,
    onNext: () -> Unit, showNext: Boolean = true) {
    if (message.isNotBlank()) PinyinText(message,
        modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBCD),
            RoundedCornerShape(14.dp)).padding(12.dp),
        color = AdventureInk, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
    if (resolved && showNext) KidMainButton(if (last) "🏆 查看练习报告" else "➡️ 下一题",
        onNext, AdventureOrange, 55.dp)
}

@Composable
internal fun SoloBoard(
    q: SkillQuestion, game: SoloGame, maxNumber: Int,
    resolved: Boolean, onCheck: (Boolean, String) -> Unit
) {
    when (game) {
        SoloGame.SHOP -> ShopBoard(q, resolved, onCheck)
        SoloGame.REVIEW -> when {
            q.isMoney -> ShopBoard(q, resolved, onCheck)
            else -> PracticeAnswerBoard(q, resolved, maxNumber, onCheck)
        }
    }
}

@Composable
internal fun ShopBoard(q: SkillQuestion, resolved: Boolean,
    onCheck: (Boolean, String) -> Unit
) {
    var notes by remember(q) { mutableStateOf<List<Int>>(emptyList()) }
    val sum = notes.sum()
    val grouped = notes.groupingBy { it }.eachCount().toSortedMap()
    PinyinText(if (q.role == ShopRole.CUSTOMER) "🧺 顾客模式 · 拿纸币付款"
        else "🏪 店长模式 · 找出零钱",
        color = AdventureInk, fontWeight = FontWeight.Black,
        fontSize = 18.sp, lineHeight = 24.sp)
    PinyinText("选择纸币（单位：元）", color = AdventureInk, fontSize = 14.sp)
    listOf(1, 5, 10, 20, 50, 100).chunked(3).forEach { moneyRow ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            moneyRow.forEach { value ->
                Button(onClick = { notes = notes + value }, enabled = !resolved,
                    modifier = Modifier.weight(1f).height(56.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD5EEE3), contentColor = AdventureInk),
                    shape = RoundedCornerShape(15.dp)) {
                    PinyinText("¥$value", fontWeight = FontWeight.Black, fontSize = 21.sp)
                }
            }
        }
    }
    PinyinText(if (notes.isEmpty()) "已选纸币：暂未选择"
        else "已选纸币：" + grouped.entries.joinToString("  ") { "¥${it.key} × ${it.value}" },
        color = AdventureInk, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 23.sp)
    PinyinText("已选总额：¥$sum", color = AdventureInk,
        fontWeight = FontWeight.Black, fontSize = 24.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallPill("↶ 撤回一张") { if (!resolved) notes = notes.dropLast(1) }
        SmallPill("清空重选") { if (!resolved) notes = emptyList() }
    }
    KidMainButton(
        if (q.role == ShopRole.CUSTOMER) "✅ 确认付款" else "✅ 确认找零",
        {
            val tip = if (sum > q.answer) {
                "选的钱多啦，可以撤回一张，再试试。"
            } else {
                if (q.role == ShopRole.CASHIER)
                    "还差一些零钱，想想收到的钱减去商品价格是多少。"
                else "还不够呢，看看离商品价格差多少。"
            }
            onCheck(sum == q.answer, tip)
        }, AdventureOrange, 53.dp
    )
}

@Composable
internal fun PracticeAnswerBoard(q: SkillQuestion, resolved: Boolean,
    maxNumber: Int, onCheck: (Boolean, String) -> Unit
) {
    var locked by remember { mutableStateOf(false) }
    LaunchedEffect(locked) { if (locked) { delay(900); locked = false } }
    val targets = remember(q) {
        val set = linkedSetOf(q.answer)
        val possible = ((q.answer - 10).coerceAtLeast(0)..(q.answer + 10).coerceAtMost(maxNumber))
            .filter { it != q.answer }
            .sortedBy { kotlin.math.abs(it - q.answer) }
        possible.take(6).shuffled().forEach { if (set.size < 4) set += it }
        set.shuffled()
    }
    PinyinText("✏️ 选出正确答案",
        color = AdventureInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    targets.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            pair.forEach { value ->
                Button(onClick = {
                    if (value != q.answer) locked = true
                    onCheck(value == q.answer, "再想一想这道题的计算结果。")
                }, enabled = !locked && !resolved, modifier = Modifier.weight(1f).height(83.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE0EBFF), contentColor = AdventureInk)) {
                    PinyinText("$value", fontWeight = FontWeight.Black, fontSize = 27.sp)
                }
            }
        }
    }
}
