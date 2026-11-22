package com.game.maths

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay

private val SpaceInk = Color(0xFF253453)
private val SpaceNight = Color(0xFF17223F)
private val OrbitMint = Color(0xFF8BD9B4)
private val StarGold = Color(0xFFFFC65A)
private val DangerCoral = Color(0xFFF47C78)

@Composable
internal fun DefenseScreen(
    settings: GameSettings,
    audio: AudioDirector,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val initialDifficulty = remember { DefensePreferences.load(context) }
    var difficulty by rememberSaveable { mutableStateOf(initialDifficulty) }
    var deckNames by rememberSaveable {
        mutableStateOf((DefensePlant.entries.take(7) + DefensePlant.LILYPAD).joinToString(",") { it.name })
    }
    val deck = remember(deckNames) {
        deckNames.split(',').mapNotNull { name -> DefensePlant.entries.firstOrNull { it.name == name } }
    }
    val battleDeck = deck
    var started by rememberSaveable { mutableStateOf(false) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var guideOpen by rememberSaveable { mutableStateOf(false) }
    var guideZombieName by rememberSaveable { mutableStateOf<String?>(null) }
    var guidePlantName by rememberSaveable { mutableStateOf<String?>(null) }
    var statusMessage by rememberSaveable { mutableStateOf("先点左侧模块，再点空格部署") }
    var statusTone by remember { mutableStateOf(Color.White) }
    var focusedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var actionRevision by remember { mutableIntStateOf(0) }
    var hudRevision by remember { mutableIntStateOf(0) }
    val boardFrame = remember { mutableIntStateOf(0) }
    val engine = remember { DefenseEngine() }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    var boardOrigin by remember { mutableStateOf(Offset.Zero) }
    var boardExtent by remember { mutableStateOf(Offset.Zero) }
    var starTarget by remember { mutableStateOf(Offset.Zero) }

    BackHandler(enabled = guideOpen) { guideOpen = false; guideZombieName = null; guidePlantName = null }
    BackHandler(enabled = started && !guideOpen) {
        paused = !paused
        statusMessage = if (paused) "已暂停，可继续、查看图鉴或返回准备" else "战局继续"
    }
    LaunchedEffect(statusMessage) {
        if (statusMessage.isNotEmpty()) {
            delay(2_200)
            statusMessage = ""
        }
    }
    LaunchedEffect(started, difficulty, paused, guideOpen) {
        if (!started || paused || guideOpen) return@LaunchedEffect
        var lastFrame = 0L
        var lastHudFrame = 0L
        while (!engine.finished) {
            val frameTime = withFrameNanos { it }
            if (lastFrame != 0L) engine.step(((frameTime - lastFrame) / 1_000_000L).coerceIn(1L, 100L), difficulty)
            boardFrame.intValue++
            if (lastHudFrame == 0L || frameTime - lastHudFrame >= 250_000_000L) {
                hudRevision++
                lastHudFrame = frameTime
            }
            lastFrame = frameTime
        }
    }

    if (guideOpen) {
        DefenseGuidebook(onBack = { guideOpen = false; guideZombieName = null; guidePlantName = null },
            initialZombie = guideZombieName?.let { name -> ZombieKind.entries.firstOrNull { it.name == name } },
            initialPlant = guidePlantName?.let { name -> DefensePlant.entries.firstOrNull { it.name == name } })
        return
    }

    if (!started) {
        DefenseSetup(difficulty, deck, audio, onDeckChange = { deckNames = it.joinToString(",") { p -> p.name } }, onDifficulty = {
            difficulty = it
            DefensePreferences.save(context, it)
        },
            onBack = onBack, onGuide = { guideOpen = true }, onStart = {
                engine.start(); started = true; statusMessage = "战斗开始：先布置产能，再部署火力"; audio.tap()
            })
        return
    }

    @Suppress("UNUSED_VARIABLE") val actionFrame = actionRevision
    val cardElapsedMs = remember(hudRevision) { engine.elapsedMs }
    val question = engine.question
    Box(Modifier.fillMaxSize().background(SpaceNight).padding(4.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HudPill("✦ ${engine.sun} 星能", StarGold, modifier = Modifier.onGloballyPositioned {
                    val position = it.positionInRoot()
                    starTarget = Offset(position.x + it.size.width / 2f, position.y + it.size.height / 2f)
                })
                Spacer(Modifier.width(8.dp))
                HudPill("核心 ♥ ${engine.lives}/5", DangerCoral)
                if (engine.autoPickupCharges > 0) HudPill("自动收集 ×${engine.autoPickupCharges}", OrbitMint)
                Spacer(Modifier.weight(1f))
                HudPill("第 ${engine.elapsedMs / 45_000 + 1} 波 · ⏱ ${engine.elapsedMs / 60_000}:${(engine.elapsedMs / 1_000 % 60).toString().padStart(2, '0')}", Color(0xFF34486B), Color.White)
                Box(Modifier.width(82.dp).heightIn(min = 44.dp).clip(RoundedCornerShape(9.dp))
                    .background(if (engine.shovelMode) StarGold else Color(0xFFE8F2E9))
                    .clickable(enabled = question == null && !paused) {
                        engine.toggleShovel()
                        statusMessage = if (engine.shovelMode) "已选铲除：点击一个已有模块" else "已取消铲除"
                        statusTone = if (engine.shovelMode) StarGold else Color.White
                        audio.tap()
                        actionRevision++
                    }.padding(horizontal = 5.dp, vertical = 4.dp)) {
                    Text(if (engine.shovelMode) "✕ 取消铲除" else "⌑ 铲除模块",
                        color = SpaceInk, fontSize = 11.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.Center))
                }
                Button(onClick = { paused = true }, enabled = question == null,
                    modifier = Modifier.heightIn(min = 44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC65A),
                        disabledContainerColor = Color(0xFF68758B)), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 7.dp)) {
                    Text("Ⅱ 暂停", color = SpaceInk, fontWeight = FontWeight.Black)
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Column(Modifier.width(88.dp).fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xFF27385C)).padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("模块 · 花费", color = StarGold, fontSize = 9.sp, fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = GridCells.Fixed(1), modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                    items(battleDeck) { plant ->
                        val selected = engine.selected == plant
                        val cost = engine.cost(plant)
                        val remaining = ((engine.readyAt[plant] ?: 0L) - cardElapsedMs).coerceAtLeast(0L)
                        Box(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(9.dp))
                            .background(if (selected) StarGold else Color(0xFFE8F2E9))
                            .border(if (selected) 2.dp else 1.dp,
                                if (selected) StarGold else Color(0xFFD8E8D8),
                                RoundedCornerShape(12.dp))
                            .clickable(enabled = question == null && !paused) {
                                val wasSelected = engine.selected == plant
                                statusMessage = engine.select(plant)
                                statusTone = if (wasSelected || engine.selected == plant) { audio.tap(); OrbitMint } else { audio.retry(); DangerCoral }
                                actionRevision++
                            }
                            .padding(horizontal = 2.dp, vertical = 1.dp)) {
                            Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                PlantPortrait(plant, Modifier.size(25.dp))
                                Text(plant.title, color = SpaceInk, fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            if (remaining > 0) {
                                val fraction = (remaining.toFloat() / plant.rechargeMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                    .fillMaxHeight(fraction).background(Color(0xC8334159)))
                                Text("${(remaining + 999) / 1_000}s", color = Color.White, fontSize = 15.sp,
                                    fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center)
                                        .clip(RoundedCornerShape(6.dp)).background(Color(0xCC1B2944))
                                        .padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                            Text(cost?.let { "${it}✦" } ?: "复制",
                                color = if (remaining > 0) Color.White else Color(0xFF23314D), fontSize = 9.sp, maxLines = 1,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                    .background(if (remaining > 0) Color(0xC8334159) else if (selected) Color(0xFFFFE2A1) else Color(0xFFE5EAD8))
                                    .padding(horizontal = 1.dp, vertical = 2.dp), textAlign = TextAlign.Center)
                        }
                    }
                    }
                }
                Box(Modifier.weight(1f).fillMaxSize().onGloballyPositioned {
                    val position = it.positionInRoot()
                    boardOrigin = position
                    boardExtent = Offset(it.size.width.toFloat(), it.size.height.toFloat())
                }) {
                    DefenseBoard(engine, boardFrame, question == null && !paused, focusedCell,
                        onTapCell = { row, col ->
                            focusedCell = row to col
                            statusMessage = engine.tapCell(row, col)
                            statusTone = if (statusMessage.startsWith("已部署") || statusMessage.startsWith("已铲除")) {
                                audio.success(); OrbitMint
                            } else { audio.retry(); StarGold }
                            actionRevision++
                        },
                        onTapZombie = { type ->
                            guideZombieName = type.name
                            guidePlantName = null
                            paused = true
                            guideOpen = true
                        },
                        onTapPlant = { type ->
                            guidePlantName = type.name
                            guideZombieName = null
                            paused = true
                            guideOpen = true
                        },
                        onTapStar = { id ->
                            val tapped = engine.tapStar(id, settings)
                            if (tapped != null) { statusMessage = "星能题已开启：答对可得 $STAR_REWARD 点"; statusTone = StarGold; audio.tap() }
                            else { statusMessage = "暂时无法打开这颗星能题"; statusTone = StarGold }
                            actionRevision++
                        })
                    if (paused && question == null && !engine.finished) {
                        Column(Modifier.align(Alignment.Center).clip(RoundedCornerShape(23.dp))
                            .background(Color(0xFFF7F0D9)).border(3.dp, StarGold, RoundedCornerShape(23.dp)).padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Ⅱ 战局已暂停", color = SpaceInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Text("轨道、敌人和冷却均已停止", color = SpaceInk, fontSize = 13.sp)
                            Button(onClick = { paused = false; statusMessage = "战局继续" }, colors = ButtonDefaults.buttonColors(containerColor = StarGold)) {
                                Text("▶ 继续守护", color = SpaceInk)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { guideZombieName = null; guidePlantName = null; guideOpen = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34486B))) {
                                    Text("📖 打开图鉴", color = Color.White)
                                }
                                Button(onClick = { engine.reset(); paused = false; started = false; statusMessage = "先点左侧模块，再点空格部署" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A7891))) {
                                    Text("返回准备", color = Color.White)
                                }
                            }
                        }
                    }
                    if (question != null) {
                        DefenseQuestionPanel(question.problem, difficulty, settings.answerMode,
                            onAnswer = { answer ->
                                val correct = engine.answer(answer, difficulty)
                                if (correct) audio.success() else audio.retry()
                                val autoPickupEarned = correct && engine.lastAutoPickupAward > 0
                                statusMessage = if (autoPickupEarned) "答对5题！+$STAR_REWARD 星能，随机获得 ${engine.lastAutoPickupAward} 次自动收集"
                                    else if (correct) "答对了！+$STAR_REWARD 星能"
                                    else if (difficulty == DefenseDifficulty.NIGHTMARE) "答错了！扣除 25 星能，这颗星能已消散"
                                    else "答错了，这颗星能已消散；已有星能不受影响"
                                statusTone = if (correct) OrbitMint else DangerCoral
                                actionRevision++
                            }, onAbandon = { engine.abandonQuestion(); statusMessage = "已放弃，本颗星能消散"; statusTone = StarGold; actionRevision++ })
                    }
                    if (engine.finished) {
                        Column(Modifier.align(Alignment.Center).clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFF7F0D9)).border(3.dp, StarGold, RoundedCornerShape(24.dp))
                            .padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (engine.won) "轨道守住了！" else "星球核心失守", color = SpaceInk,
                                fontSize = 25.sp, fontWeight = FontWeight.Black)
                            Text("守护 ${(engine.elapsedMs / 60_000)} 分 ${(engine.elapsedMs / 1_000 % 60)} 秒  ·  到达第 ${engine.elapsedMs / 45_000 + 1} 波",
                                color = SpaceInk, fontSize = 13.sp)
                            Text("答对 ${engine.answeredCorrectly} 题  ·  答错 ${engine.answeredIncorrectly} 题  ·  收集 ${engine.collectedStars} 颗星能",
                                color = SpaceInk, fontSize = 12.sp)
                            Text(if (engine.won) "核心生命：${engine.lives}/5" else "核心已被突破，重新布防后再试试资源生产和减速组合。",
                                color = SpaceInk, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { engine.reset(); started = false; paused = false }, colors = ButtonDefaults.buttonColors(containerColor = StarGold)) {
                                    Text("再守一局", color = SpaceInk)
                                }
                                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34486B))) {
                                    Text("返回游戏选择", color = Color.White)
                                }
                            }
                        }
                    }
                    if (statusMessage.isNotEmpty()) {
                        Text(statusMessage, color = statusTone, fontSize = 11.sp, maxLines = 1,
                            modifier = Modifier.align(Alignment.BottomCenter).widthIn(max = 520.dp)
                                .clip(RoundedCornerShape(9.dp)).background(Color(0xDD1B2944))
                                .padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxSize().onGloballyPositioned { overlayOrigin = it.positionInRoot() }) {
            boardFrame.intValue
            if (boardExtent.x > 0f && starTarget.x > 0f && starTarget.y > 0f) {
                engine.starFlights.forEach { flight ->
                    drawStarFlight(flight, boardOrigin - overlayOrigin, boardExtent, starTarget - overlayOrigin)
                }
            }
        }
    }
}

@Composable
private fun DefenseSetup(
    difficulty: DefenseDifficulty, deck: List<DefensePlant>, audio: AudioDirector,
    onDeckChange: (List<DefensePlant>) -> Unit, onDifficulty: (DefenseDifficulty) -> Unit,
    onBack: () -> Unit, onGuide: () -> Unit, onStart: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(SpaceNight).padding(horizontal = 16.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("星球守护局", color = StarGold, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("开局 200 星能 · 选择模块部署", color = Color.White, fontSize = 12.sp)
            }
            Text("📖 单位图鉴", color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF34486B))
                    .clickable(onClick = onGuide).heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 9.dp))
            Spacer(Modifier.width(8.dp))
            Text("${deck.size}/8", color = StarGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.weight(1f).fillMaxWidth()
            .padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(DefensePlant.entries) { plant ->
                val selected = plant in deck
                Column(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(9.dp))
                    .border(if (selected) 2.dp else 1.dp, if (selected) StarGold else Color(0xFF485C7B), RoundedCornerShape(9.dp))
                    .background(if (selected) StarGold else Color(0xFF29395C))
                    .clickable {
                        when {
                            selected && deck.size > 1 -> { onDeckChange(deck - plant); audio.tap() }
                            !selected && deck.size < 8 -> { onDeckChange(deck + plant); audio.tap() }
                        }
                    }.padding(horizontal = 3.dp, vertical = 2.dp), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    PlantPortrait(plant, Modifier.size(28.dp))
                    Text(plant.title, color = if (selected) SpaceInk else Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1)
                    Text(if (plant == DefensePlant.IMITATER) "按复制对象收费" else "${plant.cost}✦",
                        color = if (selected) SpaceInk else Color(0xFFD5E3F4), fontSize = 8.sp, maxLines = 1)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("答题节奏", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            DefenseDifficulty.entries.forEach { option ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(if (option == difficulty) StarGold else Color(0xFF29395C))
                    .clickable {
                        if (option != difficulty) { onDifficulty(option); audio.tap() }
                    }.padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(option.title, color = if (option == difficulty) SpaceInk else Color.White,
                        fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(when (option) {
                        DefenseDifficulty.RELAXED -> "答题暂停"
                        DefenseDifficulty.HARD -> "继续计时"
                        DefenseDifficulty.NIGHTMARE -> "继续计时 · 错题 −25"
                    },
                        color = if (option == difficulty) SpaceInk else Color.White, fontSize = 10.sp)
                }
            }
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF425477))) {
                Text("返回")
            }
            Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = StarGold)) {
                Text("开始守护", color = SpaceInk, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HudPill(text: String, tint: Color, contentColor: Color = SpaceInk, modifier: Modifier = Modifier) {
    Text(text, color = contentColor,
        fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.clip(RoundedCornerShape(13.dp)).background(tint)
            .padding(horizontal = 9.dp, vertical = 6.dp))
}

@Composable
private fun DefenseBoard(
    engine: DefenseEngine, frameState: MutableIntState, enabled: Boolean, focusedCell: Pair<Int, Int>?,
    onTapCell: (Int, Int) -> Unit, onTapZombie: (ZombieKind) -> Unit,
    onTapPlant: (DefensePlant) -> Unit, onTapStar: (Long) -> Unit
) {
    Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp))
        .pointerInput(engine, enabled) {
            detectTapGestures { point ->
                val cellW = size.width / 9f
                val cellH = size.height / 5f
                val star = if (enabled) engine.stars.lastOrNull {
                    kotlin.math.abs(point.x - (it.col + .5f) * cellW) < cellW * .43f &&
                        kotlin.math.abs(point.y - ((it.row + .15f + it.progress.coerceIn(0f, 1f) * .35f) * cellH)) < cellH * .43f
                } else null
                val zombie = if (enabled && star == null && engine.selected == null && !engine.shovelMode) engine.zombies.firstOrNull {
                    kotlin.math.abs(point.x - it.x * cellW) < cellW * .3f &&
                        kotlin.math.abs(point.y - (it.row + .53f) * cellH) < cellH * .36f
                } else null
                val plant = if (enabled && star == null && zombie == null && engine.selected == null && !engine.shovelMode) engine.plants.lastOrNull {
                    kotlin.math.abs(point.x - (it.col + .5f) * cellW) < cellW * .36f &&
                        kotlin.math.abs(point.y - (it.row + .53f) * cellH) < cellH * .38f
                } else null
                when {
                    star != null -> onTapStar(star.id)
                    zombie != null -> onTapZombie(zombie.type)
                    plant != null -> onTapPlant(plant.type)
                    enabled -> onTapCell((point.y / cellH).toInt(), (point.x / cellW).toInt())
                }
            }
        }) {
        val frame = frameState.intValue
        val cellW = size.width / 9f
        val cellH = size.height / 5f
        drawRect(Color(0xFF8AB99D))
        for (r in 0..4) for (c in 0..8) {
            val left = c * cellW
            val top = r * cellH
            val terrain = if (r == 2) {
                if (c % 2 == 0) Color(0xFF62B1C7) else Color(0xFF55A5BF)
            } else if ((r + c) % 2 == 0) Color(0xFF9BCCA9) else Color(0xFF83B99B)
            drawRoundRect(terrain,
                Offset(left + 1, top + 1), androidx.compose.ui.geometry.Size(cellW - 2, cellH - 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f))
            if (focusedCell == (r to c)) drawRoundRect(StarGold.copy(alpha = .32f),
                Offset(left + 2, top + 2), androidx.compose.ui.geometry.Size(cellW - 4, cellH - 4),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f))
            if (r == 2) {
                drawLine(Color.White.copy(alpha = .25f), Offset(left + 6, top + cellH * .36f),
                    Offset(left + cellW - 8, top + cellH * .36f), strokeWidth = 2f)
                drawLine(Color.White.copy(alpha = .18f), Offset(left + 12, top + cellH * .72f),
                    Offset(left + cellW - 4, top + cellH * .72f), strokeWidth = 1.5f)
            }
            if (engine.selected != null && engine.plants.none { it.row == r && it.col == c }) {
                drawCircle(OrbitMint.copy(alpha = .22f), min(cellW, cellH) * .34f,
                    Offset(left + cellW / 2, top + cellH / 2))
            }
        }
        engine.plants.forEach { plant -> drawPlant(plant, cellW, cellH) }
        engine.shots.forEach { shot ->
            drawCircle(if (shot.slows) Color(0xFFB4EAFF) else StarGold, min(cellW, cellH) * .09f,
                Offset(shot.x * cellW, (shot.row + .5f) * cellH))
        }
        engine.zombies.forEach { drawZombie(it, cellW, cellH) }
        engine.stars.forEach { star ->
            val y = (star.row + .15f + star.progress.coerceIn(0f, 1f) * .35f) * cellH
            val x = (star.col + .5f) * cellW
            drawCircle(StarGold.copy(alpha = .22f + (frame % 12) * .002f), min(cellW, cellH) * .27f, Offset(x, y))
            drawCircle(StarGold, min(cellW, cellH) * .14f, Offset(x, y))
            drawCircle(Color(0xFFFFF2B3), min(cellW, cellH) * .05f, Offset(x, y))
        }
        engine.explosions.forEach { blast ->
            val progress = (1f - blast.remainingMs / blast.durationMs).coerceIn(0f, 1f)
            val grow = 1f - (1f - progress) * (1f - progress)
            val fade = 1f - progress * .72f
            val center = Offset(blast.x * cellW, (blast.row + .53f) * cellH)
            val radius = min(cellW, cellH) * blast.radiusCells * (.08f + .92f * grow)
            drawCircle(Color(0xFFE94732).copy(alpha = .34f * fade), radius, center)
            drawCircle(Color(0xFFFFA142).copy(alpha = .9f * fade), radius * .72f, center)
            drawCircle(Color(0xFFFFE89A).copy(alpha = fade), radius * .38f, center)
            drawCircle(Color.White.copy(alpha = fade), radius * .15f, center)
            drawCircle(Color(0xFFFFF0BD).copy(alpha = fade), radius,
                center, style = androidx.compose.ui.graphics.drawscope.Stroke(min(cellW, cellH) * .08f))
            repeat(8) { index ->
                val angle = index * (Math.PI / 4).toFloat()
                drawLine(Color(0xFFFFF2C6).copy(alpha = fade),
                    Offset(center.x + cos(angle) * radius * .68f, center.y + sin(angle) * radius * .68f),
                    Offset(center.x + cos(angle) * radius * 1.08f, center.y + sin(angle) * radius * 1.08f),
                    strokeWidth = min(cellW, cellH) * .07f)
            }
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(37, 52, 83)
            textAlign = Paint.Align.CENTER
            textSize = min(cellW, cellH) * .25f
            typeface = android.graphics.Typeface.create("sans-serif-rounded", android.graphics.Typeface.BOLD)
        }
        drawContext.canvas.nativeCanvas.drawText("← 核心方向", cellW * .6f, cellH * .22f, labelPaint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlant(
    plant: DefensePlantUnit, cellW: Float, cellH: Float
) {
    val center = Offset((plant.col + .5f) * cellW, (plant.row + .53f) * cellH)
    val pop = if (plant.type == DefensePlant.CHERRY)
        .42f + .78f * (plant.elapsedMs / CHERRY_FUSE_MS.toFloat()).coerceIn(0f, 1f) else 1f
    val radius = min(cellW, cellH) * .28f * pop
    val biteProgress = if (plant.type == DefensePlant.CHOMPER && plant.attackMs > 0)
        1f - plant.attackMs / CHOMPER_BITE_MS.toFloat() else 0f
    drawPlantGlyph(plant.type, center, radius, biteProgress)
    val hp = (plant.health.toFloat() / plant.type.health).coerceIn(0f, 1f)
    drawLine(Color(0xFF34415B), Offset(center.x - radius, center.y + radius * 1.2f),
        Offset(center.x + radius, center.y + radius * 1.2f), strokeWidth = 3f)
    drawLine(OrbitMint, Offset(center.x - radius, center.y + radius * 1.2f),
        Offset(center.x - radius + radius * 2 * hp, center.y + radius * 1.2f), strokeWidth = 3f)
    if (plant.type.kind == PlantKind.MINE && !plant.armed) {
        drawCircle(Color.White, radius * .18f, center)
        drawArc(Color(0xFFFFE08A), -90f, 360f * (plant.elapsedMs / MINE_ARMING_MS.toFloat()).coerceIn(0f, 1f),
            false, Offset(center.x - radius * .72f, center.y - radius * .72f),
            androidx.compose.ui.geometry.Size(radius * 1.44f, radius * 1.44f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(radius * .1f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawZombie(
    zombie: DefenseZombieUnit, cellW: Float, cellH: Float
) {
    val swallowed = if (zombie.swallowedMs > 0)
        (1f - zombie.swallowedMs / CHOMPER_BITE_MS.toFloat()).coerceIn(0f, 1f) else 0f
    val start = Offset(zombie.x * cellW, (zombie.row + .53f) * cellH)
    val target = Offset((zombie.swallowedByCol + .5f) * cellW, (zombie.row + .48f) * cellH)
    val center = Offset(start.x + (target.x - start.x) * swallowed, start.y + (target.y - start.y) * swallowed)
    val radius = min(cellW, cellH) * .25f * (1f - swallowed * .82f)
    drawZombieGlyph(zombie.type, center, radius,
        poleConsumed = zombie.type.behavior == ZombieBehavior.VAULT && zombie.jumped)
    if (zombie.slowedMs > 0 && zombie.swallowedMs <= 0) {
        drawCircle(Color(0x554FC8EE), radius * 1.42f, center)
        drawCircle(Color(0xFFD2F7FF), radius * 1.24f, center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(radius * .09f))
        drawCircle(Color.White, radius * .13f, Offset(center.x - radius * .92f, center.y - radius * .72f))
        drawCircle(Color(0xFF8DE3FF), radius * .12f, Offset(center.x + radius * .94f, center.y + radius * .56f))
    }
    if (zombie.swallowedMs <= 0) {
        val hp = (zombie.totalHealth.toFloat() / (zombie.type.health + zombie.type.armor)).coerceIn(0f, 1f)
        drawLine(DangerCoral, Offset(center.x - radius, center.y + radius * 1.25f),
            Offset(center.x - radius + radius * 2 * hp, center.y + radius * 1.25f), strokeWidth = 3f)
    }
}

private fun DrawScope.drawStarFlight(
    flight: StarFlight, boardOrigin: Offset, boardSize: Offset, target: Offset
) {
    val progress = ((flight.elapsedMs - flight.delayMs).toFloat() / STAR_FLIGHT_MS).coerceIn(0f, 1f)
    if (flight.elapsedMs < flight.delayMs) return
    val start = Offset(boardOrigin.x + (flight.col + .5f) * boardSize.x / 9f,
        boardOrigin.y + (flight.row + .15f + flight.dropProgress.coerceIn(0f, 1f) * .35f) * boardSize.y / 5f)
    val control = Offset((start.x + target.x) / 2f, min(start.y, target.y) - size.height * .04f)
    fun pointAt(t: Float): Offset {
        val inverse = 1f - t
        return Offset(start.x * inverse * inverse + 2f * control.x * inverse * t + target.x * t * t,
            start.y * inverse * inverse + 2f * control.y * inverse * t + target.y * t * t)
    }
    val point = pointAt(progress)
    val radius = min(size.width, size.height) * (.018f - .006f * progress)
    val tail = pointAt((progress - .12f).coerceAtLeast(0f))
    val inverse = 1f - progress
    drawLine(StarGold.copy(alpha = .55f * inverse), tail, point, radius * .55f)
    drawCircle(StarGold.copy(alpha = .24f * inverse), radius * 2.2f, point)
    drawCircle(StarGold, radius, point)
    drawCircle(Color.White, radius * .36f, Offset(point.x - radius * .18f, point.y - radius * .18f))
}

@Composable
private fun BoxScope.DefenseQuestionPanel(
    problem: MathProblem, difficulty: DefenseDifficulty, answerMode: AnswerMode,
    onAnswer: (Int) -> Unit, onAbandon: () -> Unit
) {
    Column(Modifier.align(Alignment.Center).fillMaxWidth(.72f)
        .clip(RoundedCornerShape(23.dp)).background(Color(0xFFF8F2E2))
        .border(3.dp, StarGold, RoundedCornerShape(23.dp)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("答对领取 $STAR_REWARD 星能", color = SpaceInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text(problem.expression, color = SpaceInk, fontSize = 29.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = 10.dp))
        if (answerMode == AnswerMode.MULTIPLE_CHOICE) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                problem.choices.forEach { choice ->
                    Button(onClick = { onAnswer(choice) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                        Text(choice.toString(), color = SpaceInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            var typed by remember(problem) { mutableStateOf("") }
            androidx.compose.material3.OutlinedTextField(
                value = typed,
                onValueChange = { typed = it.filter(Char::isDigit).take(7) },
                label = { Text("输入答案") }, singleLine = true
            )
            Button(onClick = { typed.toIntOrNull()?.let(onAnswer) }, enabled = typed.toIntOrNull() != null,
                colors = ButtonDefaults.buttonColors(containerColor = StarGold)) {
                Text("提交", color = SpaceInk)
            }
        }
        Text(when (difficulty) {
            DefenseDifficulty.RELAXED -> "战局已暂停"
            DefenseDifficulty.HARD -> "轨道仍在运行，尽快作答"
            DefenseDifficulty.NIGHTMARE -> "噩梦模式：轨道运行，答错扣 25 星能"
        },
            color = if (difficulty.keepsRunningWhileAnswering) DangerCoral else SpaceInk, fontSize = 12.sp)
        Button(onClick = onAbandon, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A7891))) {
            Text("放弃这颗星能", color = Color.White)
        }
    }
}
