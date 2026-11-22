package com.game.maths

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val AdventureInk = Color(0xFF294369)
internal val AdventureCard = Color(0xFFFFFDF7)
internal val AdventureBlue = Color(0xFF5E99F2)
internal val AdventureOrange = Color(0xFFFFB64C)

@Composable
internal fun AdventureChip(text: String, active: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) AdventureOrange else Color.White,
            contentColor = AdventureInk),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) { PinyinText(text, fontWeight = if (active) FontWeight.Black else FontWeight.Medium, fontSize = 13.sp) }
}

@Composable
internal fun AdventureCardBlock(title: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(23.dp)).background(AdventureCard)
            .border(2.dp, Color.White, RoundedCornerShape(23.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        PinyinText(title, color = AdventureInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
        PinyinText(description, color = Color(0xFF6B809D), fontSize = 13.sp, lineHeight = 19.sp)
        PinyinText("点这里开始 →", color = Color(0xFFBC6730), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun AdventureHub(
    rounds: Int, maxNumber: Int,
    onBack: () -> Unit, onSolo: (SoloGame) -> Unit,
    onClassic: () -> Unit, onDuel: () -> Unit,
    onPractice: () -> Unit, onSettings: () -> Unit, onParents: () -> Unit,
    onDefense: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val wide = maxWidth > 700.dp && maxWidth > maxHeight
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide) 28.dp else 15.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if(PinyinLayoutPolicy.useStackedPageHeading(LocalPinyinEnabled.current,this@BoxWithConstraints.maxWidth.value)) {
                Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Row {SmallPill("← 星球首页",onBack)}
                    PinyinText("🪐 选择数学冒险",color=AdventureInk,fontSize=20.sp,
                        fontWeight=FontWeight.Black,modifier=Modifier.fillMaxWidth())
                }
            } else {
                Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                    SmallPill("← 星球首页", onBack)
                    Spacer(Modifier.width(10.dp))
                    PinyinText("🪐 选择数学冒险", color = AdventureInk,
                        fontSize = PinyinLayoutPolicy.pageTitleSp(24f,LocalPinyinEnabled.current).sp,
                        fontWeight = FontWeight.Black,modifier=Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth()
                .background(Color.White.copy(alpha = .68f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                PinyinText("📚 学习范围 0～$maxNumber · 单人 $rounds 题/局",
                    color = AdventureInk, fontSize = 13.sp, modifier = Modifier.weight(1f))
                SmallPill("⚙️ 调整", onSettings)
            }
            PinyinText("🎒 单人训练场", color = AdventureInk, fontSize = 22.sp, fontWeight = FontWeight.Black)
            AdventureCardBlock("🔢 算数探险",
                "加减乘除、四选一与数字填空，遵守统一学习范围",
                onClassic, Modifier.fillMaxWidth())
            AdventureCardBlock("📚 综合数学练习",
                "新增独立题型 · 默认幼小衔接 · 应用题、看图、图形与规律",
                onPractice, Modifier.fillMaxWidth())
            AdventureCardBlock("🛒 小小商店",
                "顾客付款或店长找零 · 1、5、10、20、50、100 元",
                { onSolo(SoloGame.SHOP) }, Modifier.fillMaxWidth())
            AdventureCardBlock("🌌 星球守护局",
                "轨道布防与波次防守 · 答题收集 50 星能",
                onDefense, Modifier.fillMaxWidth())
            AdventureCardBlock("⚔️ 计算对决",
                "真人左右面对面对决 / AI 四档速度 · 速度或正确率优先",
                onDuel, Modifier.fillMaxWidth())
            AdventureCardBlock("👨‍👩‍👧 家长数据中心",
                "本地练习报告、错题本、正确率和成长曲线",
                onParents, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ParentDashboard(context: Context, maxNumber: Int,
    onBack: () -> Unit, onPractice: (List<SkillQuestion>) -> Unit) {
    var version by remember { mutableIntStateOf(0) }
    val allRecords = remember(version) { TrainingHistory.read(context) }
    val records = remember(allRecords) { TrainingHistory.currentGameRecords(allRecords) }
    var filter by remember { mutableStateOf("全部") }
    var confirm by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(TrainingHistory.enabled(context)) }
    val gameNames = listOf("全部") + records.map { it.game }.distinct().sorted()
    val shown = if (filter == "全部" || filter !in gameNames) records else records.filter { it.game == filter }
    val errors = remember(shown) { TrainingHistory.mistakes(shown) }
    val correct = shown.count { it.correct }
    val rate = if (shown.isEmpty()) 0 else correct * 100 / shown.size
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if(PinyinLayoutPolicy.useStackedPageHeading(
                    LocalPinyinEnabled.current,
                    androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat())) {
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Row {SmallPill("← 返回",onBack)}
                    PinyinText("📈 家长数据中心",modifier=Modifier.fillMaxWidth(),
                        color=AdventureInk,fontSize=21.sp,fontWeight=FontWeight.Black,
                        maxLines=1,softWrap=false)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallPill("← 返回", onBack)
                    Spacer(Modifier.width(12.dp))
                    PinyinText("📈 家长数据中心", color = AdventureInk,
                        fontSize = 23.sp, fontWeight = FontWeight.Black,
                        maxLines=1,softWrap=false)
                }
            }
            PinyinText("记录仅保存在这台设备，可随时清除", color = AdventureInk, fontSize = 13.sp)
            AdventureChip(if (recording) "🔒 本地统计：已开启" else "🔕 本地统计：已关闭",
                recording) {
                recording = !recording
                TrainingHistory.setEnabled(context, recording)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                AdventureChip("全部", filter == "全部") { filter = "全部" }
                // Keep filter controls scrollable when many game modes have data.
            }
            gameNames.drop(1).chunked(3).forEach { part ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    part.forEach { name -> AdventureChip(name, filter == name) { filter = name } }
                }
            }
            Column(
                Modifier.fillMaxWidth().background(AdventureCard, RoundedCornerShape(24.dp))
                    .padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                // Never concatenate a numeric metric to a long ruby-annotated
                // sentence. Each label gets its own line, each number an
                // independent single-line text component, on phone + tablet.
                val metrics=listOf(
                    "累计练习" to shown.size.toString(),
                    "正确率" to "$rate%",
                    "练习时长（分钟）" to (TrainingHistory.duration(shown)/60000).toString(),
                    "活跃错题" to errors.size.toString()
                )
                metrics.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        row.forEach { (label,value) ->
                            Column(Modifier.weight(1f)
                                .background(Color(0xFFF0F5FF),RoundedCornerShape(14.dp))
                                .padding(horizontal=7.dp,vertical=12.dp),
                                horizontalAlignment=Alignment.CenterHorizontally,
                                verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                PinyinText(label,modifier=Modifier.fillMaxWidth(),
                                    color=AdventureInk,fontSize=12.sp,
                                    textAlign=TextAlign.Center,maxLines=1,softWrap=false)
                                // Numeric-only path intentionally avoids ruby.
                                PinyinText(value,modifier=Modifier.fillMaxWidth(),
                                    color=AdventureInk,fontSize=24.sp,
                                    fontWeight=FontWeight.Black,
                                    textAlign=TextAlign.Center,maxLines=1,softWrap=false)
                            }
                        }
                    }
                }
                PinyinText("正确率变化（按最近 10 组练习）", color = AdventureInk, fontWeight = FontWeight.Bold)
                val trend = shown.chunked(10).takeLast(10).map {
                    it.count { q -> q.correct }.toFloat() / it.size.coerceAtLeast(1)
                }
                Canvas(Modifier.fillMaxWidth().height(100.dp)) {
                    val lineColor = Color(0xFF5E99F2)
                    if (trend.size < 2) {
                        drawLine(lineColor, Offset(0f, size.height * .5f),
                            Offset(size.width, size.height * .5f), 4.dp.toPx())
                    } else {
                        val points = trend.mapIndexed { i, value ->
                            Offset(i * size.width / (trend.size - 1),
                                size.height - (value * (size.height - 8.dp.toPx())) - 4.dp.toPx())
                        }
                        points.zipWithNext().forEach { (a,b) -> drawLine(lineColor,a,b,4.dp.toPx(),cap=StrokeCap.Round) }
                        points.forEach { drawCircle(AdventureOrange, 5.dp.toPx(), it) }
                    }
                }
            }
            val grouped = shown.groupBy { it.game }.toList().sortedByDescending { it.second.size }
            grouped.forEach { (game, rows) ->
                val score = rows.count { it.correct } * 100 / rows.size
                Row(Modifier.fillMaxWidth()
                    .background(Color.White.copy(alpha=.58f),RoundedCornerShape(12.dp))
                    .padding(horizontal=10.dp,vertical=10.dp),
                    horizontalArrangement=Arrangement.spacedBy(8.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    PinyinText(game,modifier=Modifier.weight(1f),
                        color=AdventureInk,fontSize=15.sp,maxLines=2)
                    Column(horizontalAlignment=Alignment.End,
                        verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        PinyinText("${rows.size} 题",color=AdventureInk,fontSize=14.sp,
                            maxLines=1,softWrap=false)
                        PinyinText("正确率 $score%",color=AdventureInk,fontSize=13.sp,
                            maxLines=1,softWrap=false)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                PinyinText("🧩 高频薄弱题",fontSize=19.sp,color=AdventureInk,
                    fontWeight=FontWeight.Black,maxLines=1,softWrap=false)
                PinyinText("Top 10",fontSize=17.sp,color=AdventureInk,
                    fontWeight=FontWeight.Black,maxLines=1,softWrap=false)
            }
            val weak = shown.filter { !it.correct || !it.methodCorrect }.groupingBy { it.key }.eachCount()
            weak.entries.sortedByDescending { it.value }.take(10).forEachIndexed { i, entry ->
                val label = shown.lastOrNull { it.key == entry.key }?.question ?: entry.key
                // A long question and its count must never share a text run:
                // e.g. "错误 1 次" previously orphaned "次" on the next line.
                Row(Modifier.fillMaxWidth()
                    .background(Color.White.copy(alpha=.78f),RoundedCornerShape(14.dp))
                    .padding(horizontal=12.dp,vertical=10.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    PinyinText("${i + 1}. $label",modifier=Modifier.weight(1f),
                        color=AdventureInk,fontSize=15.sp,maxLines=2)
                    Column(Modifier.background(Color(0xFFEAF2FF),RoundedCornerShape(10.dp))
                        .padding(horizontal=10.dp,vertical=6.dp),
                        horizontalAlignment=Alignment.CenterHorizontally) {
                        PinyinText("错误",color=AdventureInk,fontSize=12.sp,
                            maxLines=1,softWrap=false)
                        // Keep number + unit inside one TextView: mixed
                        // Compose / Android baselines otherwise drift.
                        PinyinText("${entry.value} 次",color=AdventureInk,
                            fontWeight=FontWeight.Black,fontSize=16.sp,
                            maxLines=1,softWrap=false,
                            textAlign=TextAlign.Center)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                PinyinText("📒 错题本",fontSize=19.sp,
                    color=AdventureInk,fontWeight=FontWeight.Black,
                    maxLines=1,softWrap=false)
                // Keep count beside the heading, never on a new line.
                PinyinText("（${errors.size}）",fontSize=18.sp,
                    color=AdventureInk,fontWeight=FontWeight.Black,
                    maxLines=1,softWrap=false)
            }
            errors.take(30).forEach { r ->
                PinyinText("• ${r.game} · ${r.question}", color = AdventureInk)
            }
            KidMainButton("🎯 一键练习当前错题池", {
                val questions = errors.mapNotNull { parseRecordedQuestion(it.key) }
                    .filter { q -> q.answer in 0..maxNumber && q.a in 0..maxNumber &&
                        q.b in 0..maxNumber && (!q.isMoney ||
                        (q.price in 1..maxNumber && q.paid in 0..maxNumber)) }
                if (questions.isNotEmpty()) onPractice(questions)
            }, AdventureOrange, 53.dp)
            if (confirm.isEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallPill("清空错题库", { confirm = "errors" })
                    SmallPill("清空全部记录", { confirm = "all" })
                }
            } else {
                PinyinText(if (confirm == "all") "确定要删除所有练习记录吗？"
                else "确定清空错题库吗？", color = Color(0xFFB64A4A), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SmallPill("取消", { confirm = "" })
                    SmallPill("确认清空", {
                        if (confirm == "all") TrainingHistory.clear(context)
                        else TrainingHistory.clearMistakes(context)
                        confirm = ""
                        version++
                    })
                }
            }
        }
    }
}

internal fun parseRecordedQuestion(key: String): SkillQuestion? = runCatching {
    val parts = key.split(":")
    when (parts.firstOrNull()) {
        "ADD" -> SkillQuestion(parts[1].toInt(), parts[2].toInt())
        "SUB" -> SkillQuestion(parts[1].toInt(), parts[2].toInt(), add = false)
        "MONEY" -> SkillQuestion(0, 0, price=parts[2].toInt(), paid=parts[3].toInt(),
            role=ShopRole.valueOf(parts[1]))
        else -> {
            val split = key.split("|")
            val start = split.first().toInt()
            val steps = split.drop(1).map { token ->
                val terms = token.split(":")
                MathStep(Operation.valueOf(terms[0]), terms[1].toInt())
            }
            if (steps.size == 1 &&
                steps[0].operator in listOf(Operation.ADD, Operation.SUBTRACT)) {
                SkillQuestion(start, steps[0].operand,
                    add = steps[0].operator == Operation.ADD, sourceKey = key)
            } else {
                val expr = MathProblem(start, steps, MathEngine.evaluate(start, steps),
                    emptyList()).expression
                SkillQuestion(0, 0, rawExpression = expr,
                    rawAnswer = MathEngine.evaluate(start, steps), sourceKey = key)
            }
        }
    }
}.getOrNull()
