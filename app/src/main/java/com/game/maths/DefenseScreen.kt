package com.game.maths

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.atan2
import kotlin.math.sin
import kotlinx.coroutines.delay

private val SpaceInk = Color(0xFF253453)
private val SpaceNight = Color(0xFF17223F)
private val OrbitMint = Color(0xFF8BD9B4)
private val StarGold = Color(0xFFFFC65A)
private val DangerCoral = Color(0xFFF47C78)

private fun DefenseBattlefield.roofSurfaceOffset(column: Float, cellH: Float) =
    if (roof) (5f - column).coerceIn(0f, 5f) * cellH * .048f else 0f

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
    var battlefield by rememberSaveable { mutableStateOf(DefenseBattlefield.LAWN_POOL) }
    var flowPage by rememberSaveable { mutableStateOf("MODES") }
    var campaignLevelId by rememberSaveable { mutableIntStateOf(0) }
    val deck = remember(deckNames) {
        deckNames.split(',').mapNotNull { name -> DefensePlant.entries.firstOrNull { it.name == name } }
    }
    val campaignLevel = remember(campaignLevelId) { DefenseCampaign.level(campaignLevelId) }
    val battleDeck = deck
    var started by rememberSaveable { mutableStateOf(false) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var guideOpen by rememberSaveable { mutableStateOf(false) }
    var guideZombieName by rememberSaveable { mutableStateOf<String?>(null) }
    var guidePlantName by rememberSaveable { mutableStateOf<String?>(null) }
    var statusMessage by rememberSaveable { mutableStateOf("先选左侧植物，再点空格种下") }
    var statusTone by remember { mutableStateOf(Color.White) }
    var focusedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var actionRevision by remember { mutableIntStateOf(0) }
    var hudRevision by remember { mutableIntStateOf(0) }
    val boardFrame = remember { mutableIntStateOf(0) }
    val activeBattlefield = campaignLevel?.battlefield ?: battlefield
    val engine = remember(activeBattlefield, campaignLevel?.id) {
        DefenseEngine(battlefield = activeBattlefield, campaignLevel = campaignLevel)
    }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    var boardOrigin by remember { mutableStateOf(Offset.Zero) }
    var boardExtent by remember { mutableStateOf(Offset.Zero) }
    var starTarget by remember { mutableStateOf(Offset.Zero) }

    BackHandler(enabled = guideOpen) { guideOpen = false; guideZombieName = null; guidePlantName = null }
    BackHandler(enabled = flowPage == "CAMPAIGN") { flowPage = "MODES" }
    BackHandler(enabled = flowPage == "SETUP" && !started && !guideOpen) {
        flowPage = if (campaignLevel != null) "CAMPAIGN" else "MODES"
    }
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
        var lastPlantHitSoundEvent = engine.plantHitSoundEvents
        var lastZombieBiteSoundEvent = engine.zombieBiteSoundEvents
        while (!engine.finished) {
            val frameTime = withFrameNanos { it }
            if (lastFrame != 0L) {
                engine.step(((frameTime - lastFrame) / 1_000_000L).coerceIn(1L, 100L), difficulty)
                if (engine.plantHitSoundEvents != lastPlantHitSoundEvent) audio.plantHit()
                if (engine.zombieBiteSoundEvents != lastZombieBiteSoundEvent) audio.zombieBite()
                lastPlantHitSoundEvent = engine.plantHitSoundEvents
                lastZombieBiteSoundEvent = engine.zombieBiteSoundEvents
                if (engine.finished) {
                    hudRevision++
                    if (engine.won) audio.success() else audio.retry()
                }
            }
            boardFrame.intValue++
            if (lastHudFrame == 0L || frameTime - lastHudFrame >= 250_000_000L) {
                hudRevision++
                lastHudFrame = frameTime
            }
            lastFrame = frameTime
        }
    }
    LaunchedEffect(campaignLevel?.id, engine.finished, engine.won) {
        if (campaignLevel != null && engine.finished && engine.won) {
            DefenseCampaignProgress.complete(context, campaignLevel.id)
        }
    }

    if (flowPage == "MODES") {
        DefenseModeMenu(
            audio = audio,
            onAdventure = { audio.tap(); flowPage = "CAMPAIGN" },
            onFreeBattle = {
                campaignLevelId = 0
                deckNames = (DefensePlant.entries.take(7) + DefensePlant.LILYPAD)
                    .joinToString(",") { it.name }
                flowPage = "SETUP"
            },
            onBack = onBack
        )
        return
    }
    if (flowPage == "CAMPAIGN") {
        DefenseCampaignMap(
            audio = audio,
            onBack = { flowPage = "MODES" },
            onStartLevel = { level ->
                campaignLevelId = level.id
                deckNames = DefenseCampaign.defaultDeck(level).joinToString(",") { it.name }
                flowPage = "SETUP"
            }
        )
        return
    }

    if (guideOpen) {
        DefenseGuidebook(onBack = { guideOpen = false; guideZombieName = null; guidePlantName = null },
            battlefield = activeBattlefield,
            initialZombie = guideZombieName?.let { name -> ZombieKind.entries.firstOrNull { it.name == name } },
            initialPlant = guidePlantName?.let { name -> DefensePlant.entries.firstOrNull { it.name == name } })
        return
    }

    if (!started) {
        DefenseSetup(difficulty, deck, activeBattlefield, audio, campaignLevel,
            onDeckChange = { updated ->
                val required = when {
                    activeBattlefield.hasPool -> DefensePlant.LILYPAD
                    activeBattlefield.roof -> DefensePlant.FLOWERPOT
                    else -> null
                }
                val validDeck = if (required == null) updated.take(8)
                    else (updated.filterNot { it == required }.take(7) + required)
                deckNames = validDeck.joinToString(",") { it.name }
            },
            onBattlefield = { battlefield = it }, onDifficulty = {
            difficulty = it
            DefensePreferences.save(context, it)
        },
            onBack = { flowPage = if (campaignLevel != null) "CAMPAIGN" else "MODES" },
            onGuide = { guideOpen = true }, onStart = {
                engine.start(); started = true; statusMessage = "战斗开始：先布置产能，再部署火力"; audio.tap()
            })
        return
    }

    @Suppress("UNUSED_VARIABLE") val actionFrame = actionRevision
    val cardElapsedMs = remember(hudRevision) { engine.elapsedMs }
    val question = engine.question
    Box(Modifier.fillMaxSize().background(SpaceNight).padding(4.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HudPill("☀ ${engine.sun}", StarGold, modifier = Modifier.onGloballyPositioned {
                    val position = it.positionInRoot()
                    starTarget = Offset(position.x + it.size.width / 2f, position.y + it.size.height / 2f)
                })
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    HudPill("♥ ${engine.lives}/5", DangerCoral)
                    if (engine.autoPickupCharges > 0) HudPill("自动收集 ×${engine.autoPickupCharges}", OrbitMint)
                    if (campaignLevel != null) HudPill("${campaignLevel.label} ${campaignLevel.title}", StarGold, SpaceInk)
                    engine.campaignWarning()?.let { HudPill(it, DangerCoral, Color.White) }
                    HudPill(engine.battlefield.title, Color(0xFF34486B), Color.White)
                    HudPill("波 ${engine.elapsedMs / 45_000 + 1} · ${engine.elapsedMs / 60_000}:${(engine.elapsedMs / 1_000 % 60).toString().padStart(2, '0')}", Color(0xFF34486B), Color.White)
                }
                Box(Modifier.width(64.dp).heightIn(min = 44.dp).clip(RoundedCornerShape(9.dp))
                    .background(if (engine.shovelMode) StarGold else Color(0xFFE8F2E9))
                    .clickable(enabled = question == null && !paused) {
                        engine.toggleShovel()
                        statusMessage = if (engine.shovelMode) "已选铲除：点击要移除的植物" else "已取消铲除"
                        statusTone = if (engine.shovelMode) StarGold else Color.White
                        audio.tap()
                        actionRevision++
                    }.padding(horizontal = 5.dp, vertical = 4.dp)) {
                    Text(if (engine.shovelMode) "取消" else "⌑ 铲除",
                        color = SpaceInk, fontSize = 11.sp, fontWeight = FontWeight.Black,
                        maxLines = 1, softWrap = false,
                        modifier = Modifier.align(Alignment.Center))
                }
                Button(onClick = { paused = true }, enabled = question == null,
                    modifier = Modifier.heightIn(min = 44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC65A),
                        disabledContainerColor = Color(0xFF68758B)), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 7.dp)) {
                    Text("暂停", color = SpaceInk, fontWeight = FontWeight.Black,
                        maxLines = 1, softWrap = false)
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Column(Modifier.width(88.dp).fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xFF27385C)).padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("植物 · 费用", color = StarGold, fontSize = 9.sp, fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = GridCells.Fixed(1), modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                    items(battleDeck) { plant ->
                        val selected = engine.selected == plant
                        val copyTarget = engine.selected == DefensePlant.IMITATER && engine.imitaterTarget == plant
                        val cost = engine.cost(plant)
                        val unaffordable = cost != null && engine.sun < cost
                        val remaining = ((engine.readyAt[plant] ?: 0L) - cardElapsedMs).coerceAtLeast(0L)
                        val cooldownFraction = if (remaining > 0) (remaining.toFloat() /
                            engine.rechargeMs(plant).coerceAtLeast(1L)).coerceIn(0f, 1f) else 0f
                        val cardShape = RoundedCornerShape(9.dp)
                        Box(Modifier.fillMaxWidth().height(62.dp).clip(cardShape)
                            .background(if (selected) StarGold else if (copyTarget) Color(0xFFC3E8D5) else Color(0xFFE8F2E9))
                            .border(if (selected || copyTarget) 2.dp else 1.dp,
                                if (selected) StarGold else if (copyTarget) OrbitMint else Color(0xFFD8E8D8), cardShape)
                            .clickable(enabled = question == null && !paused) {
                                val wasSelected = engine.selected == plant
                                val choosingCopy = engine.selected == DefensePlant.IMITATER && plant != DefensePlant.IMITATER
                                statusMessage = if (choosingCopy) engine.chooseImitaterTarget(plant) else engine.select(plant)
                                statusTone = if (wasSelected || engine.selected == plant ||
                                    choosingCopy && engine.imitaterTarget == plant) { audio.tap(); OrbitMint }
                                    else { audio.retry(); DangerCoral }
                                actionRevision++
                            }
                            .padding(horizontal = 2.dp, vertical = 1.dp)) {
                            Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                PlantPortrait(plant, Modifier.size(30.dp))
                                Text(plant.title, color = SpaceInk, fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            if (remaining > 0) {
                                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                    .fillMaxHeight(cooldownFraction)
                                    .clip(RoundedCornerShape(bottomStart = 9.dp, bottomEnd = 9.dp))
                                    .background(Color(0xC8334159)))
                                Text("${(remaining + 999) / 1_000}s", color = Color.White, fontSize = 15.sp,
                                    fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center)
                                        .clip(RoundedCornerShape(6.dp)).background(Color(0xCC1B2944))
                                        .padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                            Text(cost?.let { "☀ $it" } ?: "复制",
                                color = if (cooldownFraction > .2f) Color.White else Color(0xFF23314D), fontSize = 9.sp, maxLines = 1,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                    .background(if (remaining > 0) Color.Transparent else if (selected) Color(0xFFFFE2A1) else Color(0xFFE5EAD8))
                                    .padding(horizontal = 1.dp, vertical = 2.dp), textAlign = TextAlign.Center)
                            if (remaining == 0L && unaffordable) {
                                Box(Modifier.fillMaxSize().background(Color(0x80334159)))
                            }
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
                            val opened = engine.question
                            if (tapped != null && opened != null) {
                                statusMessage = if (opened.largeSun)
                                    "大阳光挑战（${opened.challengeHint}）：答对得 ${opened.value} 阳光"
                                else "阳光题已开启：答对可得 ${opened.value} 阳光"
                                statusTone = StarGold
                                audio.tap()
                            }
                            else {
                                statusMessage = if (engine.sunQuestion(id)?.largeSun == true)
                                    "无法生成更难的大阳光题，请降低范围或计算项数"
                                else "暂时无法打开这份阳光"
                                statusTone = StarGold
                            }
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
                                Button(onClick = { engine.reset(); paused = false; started = false; statusMessage = "先选左侧植物，再点空格种下" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A7891))) {
                                    Text("返回准备", color = Color.White)
                                }
                            }
                        }
                    }
                    if (question != null) {
                        DefenseQuestionPanel(question.problem, question.value, difficulty, settings.answerMode,
                            question.largeSun, question.challengeHint,
                            onAnswer = { answer ->
                                val correct = engine.answer(answer, difficulty)
                                if (correct) audio.success() else audio.retry()
                                val autoPickupEarned = correct && engine.lastAutoPickupAward > 0
                                statusMessage = when {
                                    correct && question.largeSun && autoPickupEarned -> "大阳光答对！+${question.value} 阳光；另获 2 次自动收集"
                                    correct && question.largeSun -> "大阳光答对！+${question.value} 阳光"
                                    autoPickupEarned -> "答对3题！+${question.value} 阳光，获得 2 次自动收集"
                                    correct -> "答对了！+${question.value} 阳光"
                                    difficulty == DefenseDifficulty.NIGHTMARE -> "答错了！扣除 25 阳光，这份阳光已消散"
                                    else -> if (question.largeSun) "大阳光题答错，这份阳光已消散"
                                        else "答错了，这份阳光已消散；已有阳光不受影响"
                                }
                                statusTone = if (correct) OrbitMint else DangerCoral
                                actionRevision++
                            }, onAbandon = { engine.abandonQuestion(); statusMessage =
                                if (question.largeSun) "已放弃，大阳光消散" else "已放弃，这份阳光消散"
                                statusTone = StarGold; actionRevision++ })
                    }
                    if (engine.finished) {
                        Column(Modifier.align(Alignment.Center).clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFF7F0D9)).border(3.dp, StarGold, RoundedCornerShape(24.dp))
                            .padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (engine.won) "轨道守住了！" else "星球核心失守", color = SpaceInk,
                                fontSize = 25.sp, fontWeight = FontWeight.Black)
                            Text("守护 ${(engine.elapsedMs / 60_000)} 分 ${(engine.elapsedMs / 1_000 % 60)} 秒  ·  到达第 ${engine.elapsedMs / 45_000 + 1} 波",
                                color = SpaceInk, fontSize = 13.sp)
                            Text("答对 ${engine.answeredCorrectly} 题  ·  答错 ${engine.answeredIncorrectly} 题  ·  收集 ${engine.collectedStars} 份阳光",
                                color = SpaceInk, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (engine.won) "核心生命：${engine.lives}/5" else "核心已被突破，重新布防后再试试资源生产和减速组合。",
                                color = SpaceInk, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            if (campaignLevel != null) {
                                if (engine.won && campaignLevel.id < 50) {
                                    Button(onClick = {
                                        val nextLevel = DefenseCampaign.level(campaignLevel.id + 1) ?: return@Button
                                        audio.tap()
                                        campaignLevelId = nextLevel.id
                                        deckNames = DefenseCampaign.defaultDeck(nextLevel).joinToString(",") { it.name }
                                        started = false
                                        paused = false
                                        flowPage = "SETUP"
                                    }, modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = StarGold)) {
                                        Text("下一关", color = SpaceInk, fontWeight = FontWeight.Black)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { engine.reset(); started = false; paused = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = StarGold)) {
                                        Text("再守一局", color = SpaceInk)
                                    }
                                    Button(onClick = {
                                        engine.reset(); started = false; paused = false
                                        flowPage = "CAMPAIGN"
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34486B))) {
                                        Text("返回关卡地图", color = Color.White)
                                    }
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { engine.reset(); started = false; paused = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = StarGold)) {
                                        Text("再守一局", color = SpaceInk)
                                    }
                                    Button(onClick = {
                                        engine.reset(); started = false; paused = false
                                        flowPage = "MODES"
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34486B))) {
                                        Text("返回模式选择", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    if (statusMessage.isNotEmpty()) {
                        Text(statusMessage, color = statusTone, fontSize = 11.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                    drawStarFlight(flight, boardOrigin - overlayOrigin, boardExtent,
                        starTarget - overlayOrigin, engine.battlefield)
                }
            }
        }
    }
}

@Composable
private fun DefenseSetup(
    difficulty: DefenseDifficulty, deck: List<DefensePlant>, battlefield: DefenseBattlefield,
    audio: AudioDirector, campaignLevel: DefenseCampaignLevel?, onDeckChange: (List<DefensePlant>) -> Unit,
    onBattlefield: (DefenseBattlefield) -> Unit, onDifficulty: (DefenseDifficulty) -> Unit,
    onBack: () -> Unit, onGuide: () -> Unit, onStart: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(SpaceNight).padding(horizontal = 16.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(campaignLevel?.let { "${it.label} · ${it.title}" } ?: "花园保卫战",
                    color = StarGold, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text(campaignLevel?.objective ?: "开局 200 阳光 · 选择植物种植",
                    color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            items(DefensePlant.entries.filter { campaignLevel == null || it in campaignLevel.allowedPlants }) { plant ->
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
                    PlantPortrait(plant, Modifier.size(31.dp))
                    Text(plant.title, color = if (selected) SpaceInk else Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1)
                    Text(if (plant == DefensePlant.IMITATER) "按复制对象收费" else "☀ ${plant.cost}",
                        color = if (selected) SpaceInk else Color(0xFFD5E3F4), fontSize = 8.sp, maxLines = 1)
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("场景", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            DefenseBattlefield.entries.filter { campaignLevel == null }.forEach { option ->
                Column(Modifier.width(132.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (option == battlefield) StarGold else Color(0xFF29395C))
                    .clickable { if (option != battlefield) { onBattlefield(option); audio.tap() } }
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(option.title, color = if (option == battlefield) SpaceInk else Color.White,
                        fontWeight = FontWeight.Black, fontSize = 11.sp)
                    Text(option.detail, color = if (option == battlefield) SpaceInk else Color.White,
                        fontSize = 8.sp, maxLines = 1)
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
        fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
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
                val cellH = size.height / engine.battlefield.rowCount.toFloat()
                val pointerCol = (point.x / cellW).toInt().coerceIn(0, 8)
                val pointerY = point.y - engine.battlefield.roofSurfaceOffset(pointerCol + .5f, cellH)
                val star = if (enabled) engine.stars.lastOrNull {
                    kotlin.math.abs(point.x - (it.col + .5f) * cellW) < cellW * .43f &&
                        kotlin.math.abs(point.y - ((it.row + .15f + it.progress.coerceIn(0f, 1f) * .35f) * cellH +
                            engine.battlefield.roofSurfaceOffset(it.col + .5f, cellH))) < cellH * .43f
                } else null
                val zombie = if (enabled && star == null && engine.selected == null && !engine.shovelMode) engine.zombies.firstOrNull {
                    kotlin.math.abs(point.x - it.x * cellW) < cellW * .3f &&
                        kotlin.math.abs(point.y - ((it.row + .53f) * cellH +
                            engine.battlefield.roofSurfaceOffset(it.x, cellH))) < cellH * .36f &&
                        !engine.isFogged(it.row, it.x.toInt().coerceIn(0, 8))
                } else null
                val plant = if (enabled && star == null && zombie == null && engine.selected == null && !engine.shovelMode) engine.plants.lastOrNull {
                    kotlin.math.abs(point.x - (it.col + .5f) * cellW) < cellW * .36f &&
                        kotlin.math.abs(point.y - ((it.row + .53f) * cellH +
                            engine.battlefield.roofSurfaceOffset(it.col + .5f, cellH))) < cellH * .38f
                } else null
                when {
                    star != null -> onTapStar(star.id)
                    zombie != null -> onTapZombie(zombie.type)
                    plant != null -> onTapPlant(plant.type)
                    enabled -> onTapCell((pointerY / cellH).toInt(), (point.x / cellW).toInt())
                }
            }
        }) {
        val frame = frameState.intValue
        val cellW = size.width / 9f
        val field = engine.battlefield
        val rowCount = field.rowCount
        val cellH = size.height / rowCount
        val base = when {
            field.roof && field.night -> Color(0xFF272B39)
            field.roof -> Color(0xFF6F4038)
            field.night -> Color(0xFF26344A)
            else -> Color(0xFF6F9E73)
        }
        drawRect(base)
        for (r in 0 until rowCount) for (c in 0..8) {
            val left = c * cellW
            val offset = field.roofSurfaceOffset(c + .5f, cellH)
            val top = r * cellH + offset
            val isWater = r in field.waterRows
            val terrain = when {
                isWater && field.night -> if ((r + c) % 2 == 0) Color(0xFF315A70) else Color(0xFF294D65)
                isWater -> if ((r + c) % 2 == 0) Color(0xFF4FA6BD) else Color(0xFF4299B1)
                field.roof && field.night -> if ((r + c) % 2 == 0) Color(0xFF604D55) else Color(0xFF55434D)
                field.roof -> if ((r + c) % 2 == 0) Color(0xFFB65E43) else Color(0xFFA9513B)
                field.night -> if ((r + c) % 2 == 0) Color(0xFF536548) else Color(0xFF46583F)
                (r + c) % 2 == 0 -> Color(0xFF95C887)
                else -> Color(0xFF7EB779)
            }
            drawRoundRect(terrain,
                Offset(left + 1, top + 1), androidx.compose.ui.geometry.Size(cellW - 2, cellH - 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(if (field.roof) 4f else 7f))
            if (field.roof) {
                drawLine(Color(0x4A4B2524), Offset(left + 3, top + cellH * .28f),
                    Offset(left + cellW - 3, top + cellH * .28f), strokeWidth = 1.5f)
                drawLine(Color(0x55F0A077), Offset(left + 3, top + cellH * .34f),
                    Offset(left + cellW - 3, top + cellH * .34f), strokeWidth = 1f)
                drawLine(Color(0x563E292A), Offset(left + cellW * .7f, top + cellH * .1f),
                    Offset(left + cellW * .7f, top + cellH * .9f), strokeWidth = 1f)
            } else if (isWater) {
                val rippleX = left + cellW * (.18f + (c % 3) * .19f)
                val rippleY = top + cellH * (.3f + (r % 2) * .34f)
                drawArc(Color(0x66E7FFFF), 190f, 155f, false,
                    Offset(rippleX, rippleY), androidx.compose.ui.geometry.Size(cellW * .42f, cellH * .17f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(cellH * .025f))
                drawArc(Color(0x4498E6F2), 10f, 155f, false,
                    Offset(left + cellW * .52f, top + cellH * .61f),
                    androidx.compose.ui.geometry.Size(cellW * .32f, cellH * .12f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(cellH * .02f))
            } else {
                val bladeX = left + cellW * (.22f + (c % 3) * .2f)
                val bladeY = top + cellH * .77f
                drawLine(Color(0x4487E08C), Offset(bladeX, bladeY),
                    Offset(bladeX + cellW * .08f, bladeY - cellH * .11f), strokeWidth = 1.2f)
                if ((r + c) % 4 == 0) drawLine(Color(0x3385D884),
                    Offset(left + cellW * .76f, top + cellH * .7f),
                    Offset(left + cellW * .81f, top + cellH * .59f), strokeWidth = 1f)
            }
            if ((r to c) in engine.craters) {
                drawRoundRect(Color(0xB34E3D45), Offset(left + 1, top + 1),
                    androidx.compose.ui.geometry.Size(cellW - 2, cellH - 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f))
                drawLine(Color(0xFF322B32), Offset(left + cellW * .25f, top + cellH * .35f),
                    Offset(left + cellW * .68f, top + cellH * .66f), strokeWidth = 2f)
            }
            if (focusedCell == (r to c)) drawRoundRect(StarGold.copy(alpha = .32f),
                Offset(left + 2, top + 2), androidx.compose.ui.geometry.Size(cellW - 4, cellH - 4),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f))
            if ((r to c) in engine.iceTrails) {
                drawRoundRect(Color(0xAA9BE6F3), Offset(left + 2, top + cellH * .62f),
                    androidx.compose.ui.geometry.Size(cellW - 4, cellH * .25f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellH * .12f))
                drawLine(Color(0xFFE6FAFF), Offset(left + cellW * .18f, top + cellH * .74f),
                    Offset(left + cellW * .78f, top + cellH * .69f), strokeWidth = 2f)
            }
            if ((r to c) in engine.gravestones) drawGravestone(Offset(left + cellW * .5f, top + cellH * .63f),
                min(cellW, cellH) * .3f)
            if (engine.selected != null && engine.plants.none { it.row == r && it.col == c }) {
                drawCircle(OrbitMint.copy(alpha = .22f), min(cellW, cellH) * .34f,
                    Offset(left + cellW / 2, top + cellH / 2))
            }
        }
        if (field.roof) {
            val pitchStart = Offset(0f, field.roofSurfaceOffset(0f, cellH))
            val ridgeStart = Offset(cellW * 5f, 0f)
            val pitchEnd = Offset(0f, size.height + field.roofSurfaceOffset(0f, cellH))
            val ridgeEnd = Offset(cellW * 5f, size.height)
            drawLine(Color(0xFF6B342E), pitchStart, ridgeStart, cellH * .075f)
            drawLine(Color(0xFFD78C63), Offset(0f, pitchStart.y - cellH * .035f),
                Offset(ridgeStart.x, ridgeStart.y - cellH * .035f), cellH * .022f)
            drawLine(Color(0xFF6B342E), pitchEnd, ridgeEnd, cellH * .075f)
            drawLine(Color(0xFFD78C63), Offset(0f, pitchEnd.y + cellH * .035f),
                Offset(ridgeEnd.x, ridgeEnd.y + cellH * .035f), cellH * .022f)
            drawLine(Color(0xFF5B332F), ridgeStart, ridgeEnd, cellW * .035f)
            drawLine(Color(0xFFE5A175), Offset(ridgeStart.x + cellW * .035f, 0f),
                Offset(ridgeEnd.x + cellW * .035f, size.height), cellW * .018f)
            drawLine(Color(0xFF8B4135), Offset(cellW * 5f, cellH * .1f),
                Offset(size.width, cellH * .1f), cellH * .02f)
            drawLine(Color(0xFF8B4135), Offset(cellW * 5f, size.height - cellH * .1f),
                Offset(size.width, size.height - cellH * .1f), cellH * .02f)
            val chimney = Offset(cellW * .32f, cellH * .48f)
            drawRoundRect(Color(0xFF743D34), Offset(chimney.x - cellW * .14f, chimney.y - cellH * .12f),
                androidx.compose.ui.geometry.Size(cellW * .28f, cellH * .24f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellW * .035f))
            drawRoundRect(Color(0xFF332D35), Offset(chimney.x - cellW * .08f, chimney.y - cellH * .08f),
                androidx.compose.ui.geometry.Size(cellW * .16f, cellH * .13f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellW * .04f))
            val dish = Offset(size.width - cellW * .32f, cellH * .35f)
            drawArc(Color(0xFF7B8180), 205f, 130f, false,
                Offset(dish.x - cellW * .16f, dish.y - cellH * .12f),
                androidx.compose.ui.geometry.Size(cellW * .32f, cellH * .22f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(cellW * .035f))
            drawLine(Color(0xFF717777), dish, Offset(dish.x, dish.y + cellH * .13f), cellW * .025f)
        }
        if (field.hasPool) {
            val waterTop = field.waterRows.min() * cellH
            val waterBottom = (field.waterRows.max() + 1) * cellH
            drawRect(Color(0xFFB9A988), Offset(0f, waterTop - cellH * .055f),
                androidx.compose.ui.geometry.Size(size.width, cellH * .09f))
            drawLine(Color(0xFFE8D5AE), Offset(0f, waterTop - cellH * .035f),
                Offset(size.width, waterTop - cellH * .035f), cellH * .025f)
            drawRect(Color(0xFF9F927A), Offset(0f, waterBottom - cellH * .035f),
                androidx.compose.ui.geometry.Size(size.width, cellH * .09f))
            drawLine(Color(0xFFE8D5AE), Offset(0f, waterBottom + cellH * .015f),
                Offset(size.width, waterBottom + cellH * .015f), cellH * .025f)
        }
        if (field.night) {
            val moon = Offset(size.width * .84f, cellH * .32f)
            drawCircle(Color(0x223D64A1), cellW * .34f, moon)
            drawCircle(Color(0xFFFFE8A2), cellW * .17f, moon)
            repeat(8) { index ->
                val x = (index * 37f % 83f + 7f) / 100f * size.width
                val y = (index * 19f % 45f + 8f) / 100f * cellH
                drawCircle(Color(0x99E1E9FF), min(cellW, cellH) * .018f,
                    Offset(x, y), alpha = .75f + (frame % 12) * .01f)
            }
        }
        engine.campaignLevel?.activeRows?.let { activeRows ->
            for (r in 0 until rowCount) if (r !in activeRows) {
                drawRect(Color(0xB5182030), Offset(0f, r * cellH),
                    androidx.compose.ui.geometry.Size(size.width, cellH))
            }
        }
        engine.mowerRows.forEach { row ->
            drawLawnMower(Offset(cellW * .1f, (row + .84f) * cellH), cellW * .28f, cellH * .22f)
        }
        engine.plants.sortedBy { if (it.type == DefensePlant.FLOWERPOT) 0 else 1 }
            .forEach { plant -> drawPlant(plant, cellW, cellH, frame,
                field.roofSurfaceOffset(plant.col + .5f, cellH)) }
        engine.shots.forEach { shot ->
            val shotColor = when {
                shot.butter -> Color(0xFFFFE7A1)
                shot.burning -> Color(0xFFFF4D2E)
                shot.slows -> Color(0xFF76D8FF)
                shot.sourceType == DefensePlant.KERNEL -> Color(0xFFFFC93D)
                shot.targetKind == PlantKind.LOB -> if (shot.sourceType == DefensePlant.MELON) Color(0xFF82C94D) else Color(0xFFE7C94B)
                shot.targetKind == PlantKind.LOB_SLOW -> Color(0xFF70D9F2)
                shot.targetKind == PlantKind.COB -> Color(0xFFFF9D42)
                shot.targetKind == PlantKind.PIERCE -> Color(0xFFB6E878)
                shot.targetKind == PlantKind.STAR_SHOT -> StarGold
                shot.targetKind == PlantKind.CATTAIL -> Color(0xFF65D9D0)
                else -> Color(0xFF72C94A)
            }
            val point = Offset(shot.x * cellW, (shot.lane + .5f - shot.arcOffsetCells) * cellH +
                field.roofSurfaceOffset(shot.x, cellH))
            if (shot.homing || shot.sourceType == DefensePlant.CACTUS) {
                val angle = atan2((shot.targetLane - shot.lane) * cellH, (shot.targetX - shot.x) * cellW)
                val length = min(cellW, cellH) * if (shot.homing) .27f else .2f
                val halfWidth = length * .35f
                val dx = cos(angle) * length
                val dy = sin(angle) * length
                val px = -sin(angle) * halfWidth
                val py = cos(angle) * halfWidth
                drawCircle(shotColor.copy(alpha = .24f), length * .9f, point)
                val spike = Path().apply {
                    moveTo(point.x + dx, point.y + dy)
                    lineTo(point.x - dx * .55f + px, point.y - dy * .55f + py)
                    lineTo(point.x - dx * .55f - px, point.y - dy * .55f - py)
                    close()
                }
                drawPath(spike, shotColor)
            } else if (shot.vectorShot) {
                val outer = min(cellW, cellH) * .19f
                val inner = outer * .44f
                val star = Path().apply {
                    for (index in 0 until 10) {
                        val angle = -Math.PI.toFloat() / 2f + index * Math.PI.toFloat() / 5f
                        val radius = if (index % 2 == 0) outer else inner
                        val x = point.x + cos(angle) * radius
                        val y = point.y + sin(angle) * radius
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(star, shotColor)
            } else {
                val radius = min(cellW, cellH) * when (shot.sourceType) {
                    DefensePlant.CABBAGE, DefensePlant.KERNEL -> .13f
                    DefensePlant.MELON, DefensePlant.WINTERMELON -> .19f
                    DefensePlant.COB -> .25f
                    else -> .09f
                }
                drawCircle(shotColor.copy(alpha = .32f), radius * 1.65f, point)
                drawCircle(shotColor, radius, point)
                if (shot.targetKind == PlantKind.PIERCE) {
                    drawCircle(shotColor.copy(alpha = .55f), radius * .52f,
                        Offset(point.x - radius * 1.5f, point.y + radius * .3f))
                }
            }
        }
        engine.zombies.forEach { drawZombie(it, cellW, cellH, frame, field) }
        engine.zombieShots.forEach { shot ->
            val progress = 1f - shot.remainingMs / shot.durationMs.toFloat()
            val x = shot.col + .5f + (shot.targetCol - shot.col) * progress
            val row = shot.row + (shot.targetRow - shot.row) * progress
            val y = (row + .45f - .7f * 4f * progress * (1f - progress)) * cellH +
                field.roofSurfaceOffset(x, cellH)
            drawCircle(Color(0xFF3D3440), min(cellW, cellH) * .15f, Offset(x * cellW, y))
            drawCircle(Color(0xFFCF7440), min(cellW, cellH) * .12f, Offset(x * cellW, y))
            drawLine(Color.White.copy(alpha = .8f), Offset(x * cellW - cellW * .04f, y),
                Offset(x * cellW + cellW * .04f, y), strokeWidth = 1.5f)
        }
        engine.stars.forEach { star ->
            val y = (star.row + .15f + star.progress.coerceIn(0f, 1f) * .35f) * cellH +
                field.roofSurfaceOffset(star.col + .5f, cellH)
            val x = (star.col + .5f) * cellW
            val radius = min(cellW, cellH) * when {
                star.largeSun -> .2f
                star.coin -> .13f
                else -> .14f
            }
            if (star.largeSun) {
                val center = Offset(x, y)
                drawCircle(Color(0x55FFF0A2), radius * 2.2f, center)
                repeat(8) { ray ->
                    val angle = ray * Math.PI.toFloat() / 4f
                    val inner = radius * 1.12f
                    val outer = radius * 1.68f
                    drawLine(Color(0xFFFFD75B),
                        Offset(x + cos(angle) * inner, y + sin(angle) * inner),
                        Offset(x + cos(angle) * outer, y + sin(angle) * outer),
                        strokeWidth = radius * .2f)
                }
                drawCircle(Color(0xFFFFB82E), radius * 1.12f, center)
                drawCircle(Color(0xFFFFE879), radius, center)
                drawCircle(Color(0xFFFFF8CE), radius * .34f, Offset(x - radius * .18f, y - radius * .2f))
            } else {
                drawCircle(StarGold.copy(alpha = .22f + (frame % 12) * .002f), radius * 1.9f, Offset(x, y))
                drawCircle(if (star.coin) Color(0xFFFFD34F) else StarGold, radius, Offset(x, y))
            }
            if (star.coin) drawCircle(Color(0xFFB8862C), radius * .62f, Offset(x, y),
                style = androidx.compose.ui.graphics.drawscope.Stroke(radius * .12f))
            else if (!star.largeSun) drawCircle(Color(0xFFFFF2B3), radius * .36f, Offset(x, y))
        }
        if (field.fog) for (r in 0 until rowCount) for (c in 0..8) if (engine.isFogged(r, c)) {
            val offset = field.roofSurfaceOffset(c + .5f, cellH)
            drawRoundRect(Color(0x77AFB9C3), Offset(c * cellW, r * cellH + offset),
                androidx.compose.ui.geometry.Size(cellW, cellH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f))
            drawCircle(Color(0x40FFFFFF), min(cellW, cellH) * .28f,
                Offset((c + .25f + (frame % 20) * .02f) * cellW,
                    (r + .38f) * cellH + offset))
            drawCircle(Color(0x337FE0E4), min(cellW, cellH) * .22f,
                Offset((c + .76f - (frame % 24) * .012f) * cellW,
                    (r + .7f) * cellH + offset))
        }
        engine.explosions.forEach { blast ->
            val progress = (1f - blast.remainingMs / blast.durationMs).coerceIn(0f, 1f)
            val grow = 1f - (1f - progress) * (1f - progress)
            val fade = 1f - progress * .72f
            val center = Offset(blast.x * cellW, (blast.row + .53f) * cellH +
                field.roofSurfaceOffset(blast.x, cellH))
            val radius = min(cellW, cellH) * blast.radiusCells * (.08f + .92f * grow)
            when (blast.kind) {
                DefenseEffectKind.FLAME_ROW -> {
                    val y = (blast.row + .53f) * cellH
                    drawLine(Color(0xFFFF4C2F).copy(alpha = fade), Offset(0f, y),
                        Offset(size.width, y), strokeWidth = cellH * (.12f + .22f * grow))
                    drawLine(Color(0xFFFFD86F).copy(alpha = fade), Offset(0f, y),
                        Offset(size.width, y), strokeWidth = cellH * .06f)
                }
                DefenseEffectKind.FREEZE -> {
                    drawRect(Color(0x5577DEFF).copy(alpha = fade), size = size)
                    drawCircle(Color(0x5577DEFF).copy(alpha = fade), radius, center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(cellH * .07f))
                }
                DefenseEffectKind.WIND -> repeat(10) { index ->
                    val y = (index / 2f) * cellH / 2f
                    val x = ((frame % 18) / 18f) * size.width
                    drawLine(Color(0xFFDAF6FF).copy(alpha = fade), Offset(x, y),
                        Offset(x + cellW * 1.5f, y - cellH * .08f), strokeWidth = cellH * .025f)
                }
                DefenseEffectKind.MAGNET -> drawLine(Color(0xFFFFC85B).copy(alpha = fade), center,
                    Offset(blast.endX * cellW, (blast.endRow + .53f) * cellH),
                    strokeWidth = cellH * (.03f + .05f * (1f - progress)))
                DefenseEffectKind.SHIELD -> {
                    drawCircle(Color(0xFF75D9EF).copy(alpha = .25f * fade), radius, center)
                    drawCircle(Color(0xFFB9F6FF).copy(alpha = fade), radius,
                        center, style = androidx.compose.ui.graphics.drawscope.Stroke(cellH * .08f))
                }
                DefenseEffectKind.ROOF_IMPACT -> {
                    drawCircle(Color(0xB54E3432).copy(alpha = fade), radius, center)
                    drawCircle(Color(0xFFEAB176).copy(alpha = fade), radius * .58f, center)
                    drawLine(Color(0xFFFFE0A3).copy(alpha = fade),
                        Offset(center.x - radius * .8f, center.y),
                        Offset(center.x + radius * .8f, center.y), cellH * .025f)
                }
                DefenseEffectKind.MOWER -> {
                    val mowerX = (-.12f + 9.25f * progress) * cellW
                    val mowerY = (blast.row + .84f) * cellH
                    drawLine(Color(0x9979D35B).copy(alpha = fade), Offset(0f, mowerY + cellH * .1f),
                        Offset(mowerX, mowerY + cellH * .12f), strokeWidth = cellH * .3f)
                    drawLawnMower(Offset(mowerX, mowerY), cellW * .35f, cellH * .25f)
                }
                else -> {
                    val tint = when (blast.kind) {
                        DefenseEffectKind.CHARM -> Color(0xFFBD7EFF)
                        DefenseEffectKind.WAKE -> Color(0xFFFFD76E)
                        DefenseEffectKind.TRANSFORM -> Color(0xFF8FE8B7)
                        DefenseEffectKind.SQUASH -> Color(0xFFFFC85B)
                        DefenseEffectKind.KELP -> Color(0xFF55CBD0)
                        else -> Color(0xFFE94732)
                    }
                    drawCircle(tint.copy(alpha = .34f * fade), radius, center)
                    drawCircle(tint.copy(alpha = .9f * fade), radius * .72f, center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(cellH * .06f))
                    repeat(8) { index ->
                        val angle = index * (Math.PI / 4).toFloat() + progress * Math.PI.toFloat()
                        drawLine(tint.copy(alpha = fade),
                            Offset(center.x + cos(angle) * radius * .68f, center.y + sin(angle) * radius * .68f),
                            Offset(center.x + cos(angle) * radius * 1.08f, center.y + sin(angle) * radius * 1.08f),
                            strokeWidth = min(cellW, cellH) * .06f)
                    }
                }
            }
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(37, 52, 83)
            textAlign = Paint.Align.CENTER
            textSize = min(cellW, cellH) * .25f
            typeface = android.graphics.Typeface.create("sans-serif-rounded", android.graphics.Typeface.BOLD)
        }
        drawContext.canvas.nativeCanvas.drawText("← 核心方向", cellW * .6f,
            cellH * .22f + field.roofSurfaceOffset(.5f, cellH), labelPaint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLawnMower(
    center: Offset, width: Float, height: Float
) {
    drawLine(Color(0xFF454C49), Offset(center.x - width * .24f, center.y - height * .17f),
        Offset(center.x - width * .43f, center.y - height * .72f), strokeWidth = width * .035f)
    drawCircle(Color(0xFF283348), height * .16f, Offset(center.x - width * .25f, center.y + height * .28f))
    drawCircle(Color(0xFF283348), height * .16f, Offset(center.x + width * .25f, center.y + height * .28f))
    drawCircle(Color(0xFF939AA1), height * .065f, Offset(center.x - width * .25f, center.y + height * .28f))
    drawCircle(Color(0xFF939AA1), height * .065f, Offset(center.x + width * .25f, center.y + height * .28f))
    drawRoundRect(Color(0xFFCB3F39), Offset(center.x - width * .38f, center.y - height * .27f),
        androidx.compose.ui.geometry.Size(width * .76f, height * .5f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(height * .09f))
    drawRoundRect(Color(0xFFFFC94F), Offset(center.x - width * .04f, center.y - height * .39f),
        androidx.compose.ui.geometry.Size(width * .21f, height * .2f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(height * .05f))
    drawCircle(Color(0xFFFFEBA5), height * .045f, Offset(center.x + width * .23f, center.y - height * .05f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlant(
    plant: DefensePlantUnit, cellW: Float, cellH: Float, frame: Int, surfaceYOffset: Float = 0f
) {
    val pulse = if (plant.attackMs > 0) .06f * sin(frame * .8f) else 0f
    val jump = if (plant.type == DefensePlant.SQUASH && plant.activated)
        .22f * (plant.attackMs / 420f).coerceIn(0f, 1f) else 0f
    val center = Offset((plant.col + .5f) * cellW, (plant.row + .53f - jump) * cellH + surfaceYOffset)
    val pop = if (plant.type == DefensePlant.CHERRY)
        .42f + .78f * (plant.elapsedMs / CHERRY_FUSE_MS.toFloat()).coerceIn(0f, 1f) else 1f
    val growth = if (plant.type == DefensePlant.SUNSHROOM) {
        if (plant.ageMs >= 120_000L) 1.16f else .8f
    } else 1f
    val baseRadius = min(cellW, cellH) * .31f * pop * growth * (1f + pulse)
    if (plant.frightened) drawCircle(Color(0xFF665078), baseRadius * .72f, center)
    val radius = baseRadius * if (plant.frightened) .36f else 1f
    val biteProgress = if (plant.type == DefensePlant.CHOMPER && plant.attackMs > 0)
        1f - plant.attackMs / CHOMPER_BITE_MS.toFloat() else 0f
    val actionProgress = if (plant.attackMs > 0) .5f + .5f * sin(frame * .9f) else 0f
    drawPlantGlyph(plant.type, center, radius, biteProgress, actionProgress, frame)
    if (plant.transformingMs > 0L) {
        val progress = (1f - plant.transformingMs / 800f).coerceIn(0f, 1f)
        val reveal = radius * (.16f + .72f * progress)
        drawArc(Color(0xFFFFE89A).copy(alpha = .9f - progress * .25f), frame * 7f, 245f,
            false, Offset(center.x - radius * 1.18f, center.y - radius * 1.18f),
            androidx.compose.ui.geometry.Size(radius * 2.36f, radius * 2.36f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(radius * .08f))
        plant.disguiseType?.let { drawPlantGlyph(it, center, reveal, actionProgress = progress, phase = frame) }
    }
    if (!plant.awake) {
        drawCircle(Color(0xAA78859A), radius * .32f, Offset(center.x + radius * .62f, center.y - radius * .72f))
        drawCircle(Color(0xFFDDE8F3), radius * .13f, Offset(center.x + radius * .62f, center.y - radius * .72f))
    }
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
    if (plant.type == DefensePlant.COB) {
        val load = (plant.elapsedMs / plant.type.intervalMs.toFloat()).coerceIn(0f, 1f)
        drawArc(Color(0xFFFFE08A), -90f, 360f * load, false,
            Offset(center.x - radius * .8f, center.y - radius * .8f),
            androidx.compose.ui.geometry.Size(radius * 1.6f, radius * 1.6f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(radius * .1f))
    }
    if (plant.laddered) drawLadder(center, radius)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGravestone(center: Offset, r: Float) {
    drawRoundRect(Color(0xFF3E4656), Offset(center.x - r * .72f, center.y - r * .9f),
        androidx.compose.ui.geometry.Size(r * 1.44f, r * 1.7f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * .3f))
    drawRoundRect(Color(0xFF828B94), Offset(center.x - r * .57f, center.y - r * .8f),
        androidx.compose.ui.geometry.Size(r * 1.14f, r * 1.47f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * .27f))
    drawLine(Color(0xFF58636E), Offset(center.x - r * .32f, center.y - r * .3f),
        Offset(center.x + r * .3f, center.y - r * .3f), r * .09f)
    drawLine(Color(0xFF58636E), Offset(center.x, center.y - r * .58f),
        Offset(center.x, center.y + r * .06f), r * .09f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLadder(center: Offset, r: Float) {
    drawLine(Color(0xFFC49B64), Offset(center.x + r * .52f, center.y - r * .65f),
        Offset(center.x + r * .7f, center.y + r * .7f), r * .09f)
    drawLine(Color(0xFFC49B64), Offset(center.x + r * .86f, center.y - r * .65f),
        Offset(center.x + r * 1.04f, center.y + r * .7f), r * .09f)
    repeat(4) { index ->
        val y = center.y - r * .48f + index * r * .32f
        drawLine(Color(0xFFE0BD83), Offset(center.x + r * .58f, y),
            Offset(center.x + r * .89f, y + r * .04f), r * .07f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawZombie(
    zombie: DefenseZombieUnit, cellW: Float, cellH: Float, frame: Int, field: DefenseBattlefield
) {
    val surfaceYOffset = field.roofSurfaceOffset(zombie.x, cellH)
    val swallowed = if (zombie.swallowedMs > 0)
        (1f - zombie.swallowedMs / CHOMPER_BITE_MS.toFloat()).coerceIn(0f, 1f) else 0f
    if (zombie.underground) {
        drawCircle(Color(0xFF795E48), min(cellW, cellH) * .15f,
            Offset(zombie.x * cellW, (zombie.row + .77f) * cellH + surfaceYOffset))
        drawCircle(Color(0xFFA17B57), min(cellW, cellH) * .09f,
            Offset(zombie.x * cellW, (zombie.row + .75f) * cellH + surfaceYOffset))
        return
    }
    val jump = if (zombie.jumpingMs > 0) sin((1f - zombie.jumpingMs / 800f).coerceIn(0f, 1f) * Math.PI).toFloat() else 0f
    val bob = if (zombie.type.behavior == ZombieBehavior.AIRBORNE || zombie.type.behavior == ZombieBehavior.BUNGEE)
        sin(frame * .15f) * .12f else 0f
    val start = Offset(zombie.x * cellW, (zombie.row + .53f - jump * .45f - bob) * cellH + surfaceYOffset)
    val target = Offset((zombie.swallowedByCol + .5f) * cellW,
        (zombie.row + .48f) * cellH + field.roofSurfaceOffset(zombie.swallowedByCol + .5f, cellH))
    val center = Offset(start.x + (target.x - start.x) * swallowed, start.y + (target.y - start.y) * swallowed)
    val sizeScale = when (zombie.type) {
        ZombieKind.GIANT, ZombieKind.BOSS -> .35f
        ZombieKind.IMP -> .2f
        else -> .28f
    }
    val radius = min(cellW, cellH) * sizeScale * (1f - swallowed * .82f)
    drawZombieGlyph(zombie.type, center, radius,
        poleConsumed = zombie.type.behavior == ZombieBehavior.VAULT && zombie.jumped,
        armorRemaining = zombie.armor,
        gearRemoved = zombie.equipmentRemoved,
        angry = zombie.angry,
        jumped = zombie.jumped, actionMs = zombie.actionMs, jumpingMs = zombie.jumpingMs,
        retreating = zombie.retreating, friendly = zombie.friendly, submerged = zombie.diveUnderwater,
        gearLostMs = zombie.gearLostMs,
        targetX = if (zombie.targetCol >= 0) zombie.targetCol + .5f - zombie.x else -1f,
        targetRow = if (zombie.targetRow in 0 until field.rowCount) zombie.targetRow - zombie.row else -1, phase = frame)
    zombie.capturedPlantType?.let { carried ->
        val carryBob = sin(frame * .24f) * radius * .08f
        drawPlantGlyph(carried, Offset(center.x + radius * .15f, center.y + radius * 1.08f + carryBob),
            radius * .42f, phase = frame)
    }
    if (zombie.slowedMs > 0 && zombie.swallowedMs <= 0) {
        drawCircle(Color(0x554FC8EE), radius * 1.42f, center)
        drawCircle(Color(0xFFD2F7FF), radius * 1.24f, center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(radius * .09f))
        drawCircle(Color.White, radius * .13f, Offset(center.x - radius * .92f, center.y - radius * .72f))
        drawCircle(Color(0xFF8DE3FF), radius * .12f, Offset(center.x + radius * .94f, center.y + radius * .56f))
    }
    if (zombie.stunnedMs > 0 && zombie.swallowedMs <= 0) {
        drawRoundRect(Color(0xFFFFE7A1), Offset(center.x - radius * .5f, center.y - radius * .9f),
            androidx.compose.ui.geometry.Size(radius, radius * .2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * .05f))
    }
    if (zombie.swallowedMs <= 0) {
        val hp = (zombie.totalHealth.toFloat() / (zombie.type.health + zombie.type.armor)).coerceIn(0f, 1f)
        drawLine(DangerCoral, Offset(center.x - radius, center.y + radius * 1.25f),
            Offset(center.x - radius + radius * 2 * hp, center.y + radius * 1.25f), strokeWidth = 3f)
    }
}

private fun DrawScope.drawStarFlight(
    flight: StarFlight, boardOrigin: Offset, boardSize: Offset, target: Offset, field: DefenseBattlefield
) {
    val progress = ((flight.elapsedMs - flight.delayMs).toFloat() / STAR_FLIGHT_MS).coerceIn(0f, 1f)
    if (flight.elapsedMs < flight.delayMs) return
    val cellH = boardSize.y / field.rowCount
    val start = Offset(boardOrigin.x + (flight.col + .5f) * boardSize.x / 9f,
        boardOrigin.y + (flight.row + .15f + flight.dropProgress.coerceIn(0f, 1f) * .35f) * cellH +
            field.roofSurfaceOffset(flight.col + .5f, cellH))
    val control = Offset((start.x + target.x) / 2f, min(start.y, target.y) - size.height * .04f)
    fun pointAt(t: Float): Offset {
        val inverse = 1f - t
        return Offset(start.x * inverse * inverse + 2f * control.x * inverse * t + target.x * t * t,
            start.y * inverse * inverse + 2f * control.y * inverse * t + target.y * t * t)
    }
    val point = pointAt(progress)
    val radius = min(size.width, size.height) * (if (flight.largeSun) .025f else .018f) * (1f - .33f * progress)
    val color = if (flight.largeSun) Color(0xFFFFD24A) else StarGold
    val tail = pointAt((progress - .12f).coerceAtLeast(0f))
    val inverse = 1f - progress
    drawLine(color.copy(alpha = .55f * inverse), tail, point, radius * .55f)
    drawCircle(color.copy(alpha = .24f * inverse), radius * if (flight.largeSun) 3f else 2.2f, point)
    drawCircle(color, radius, point)
    drawCircle(Color.White, radius * .36f, Offset(point.x - radius * .18f, point.y - radius * .18f))
}

@Composable
private fun BoxScope.DefenseQuestionPanel(
    problem: MathProblem, reward: Int, difficulty: DefenseDifficulty, answerMode: AnswerMode,
    largeSun: Boolean, challengeHint: String?, onAnswer: (Int) -> Unit, onAbandon: () -> Unit
) {
    Column(Modifier.align(Alignment.Center).fillMaxWidth(.72f)
        .clip(RoundedCornerShape(23.dp)).background(Color(0xFFF8F2E2))
        .border(3.dp, StarGold, RoundedCornerShape(23.dp)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("答对领取 $reward 阳光", color = SpaceInk, fontSize = 17.sp, fontWeight = FontWeight.Black,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (largeSun) Text("☀ 大阳光挑战 · ${challengeHint.orEmpty()}", color = Color(0xFF9C6500),
            fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            DefenseDifficulty.NIGHTMARE -> "噩梦模式：轨道运行，答错扣 25 阳光"
        },
            color = if (difficulty.keepsRunningWhileAnswering) DangerCoral else SpaceInk, fontSize = 12.sp)
        Button(onClick = onAbandon, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A7891))) {
            Text("放弃这份阳光", color = Color.White)
        }
    }
}
