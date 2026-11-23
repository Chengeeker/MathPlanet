package com.game.maths

import android.os.SystemClock
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val PlanetBackground: Brush = Brush.verticalGradient(
    listOf(Color(0xFF81D8FF), Color(0xFFDBF4FF), Color(0xFFFFF5DB))
)
private val Deep = Color(0xFF294369)
private val Muted = Color(0xFF627897)
private val Orange = Color(0xFFFFB64C)
private val Rose = Color(0xFFFF7799)
private val Azure = Color(0xFF5E99F2)
private val Mint = Color(0xFF53C8A8)
private val Purple = Color(0xFFA288E4)
private val Surface = Color(0xFFFFFDF7)

@Composable
private fun WideScreen(maxWidth: Dp, maxHeight: Dp): Boolean =
    maxWidth >= 600.dp && maxWidth > maxHeight

@Composable
fun HomeScreen(
    settings: GameSettings,
    totalStars: Int,
    completedRounds: Int,
    audio: AudioDirector,
    onStart: () -> Unit,
    onSettings: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val wide = WideScreen(maxWidth, maxHeight)
        if (wide) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(25.dp)
            ) {
                MascotShowcase(
                    modifier = Modifier.weight(0.95f).fillMaxHeight(),
                    photoSize = minOf(230.dp, this@BoxWithConstraints.maxHeight * .47f)
                )
                Column(
                    Modifier.weight(1.05f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    HeaderLine(onSettings)
                    Spacer(Modifier.height(14.dp))
                    PinyinText("和小恐龙一起", fontSize = 25.sp, color = Deep, fontWeight = FontWeight.Bold)
                    PinyinText("玩转数学魔法！", fontSize = 34.sp, lineHeight = 42.sp,
                        color = Deep, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(7.dp))
                    PinyinText("点一点，算一算，星星奖励等你拿！ 🌈",
                        color = Muted, fontSize = 16.sp)
                    Spacer(Modifier.height(22.dp))
                    KidMainButton("🚀 开始数学冒险", onStart, Orange)
                    Spacer(Modifier.height(14.dp))
                    InfoRow(settings, totalStars, completedRounds)
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeaderLine(onSettings)
                Spacer(Modifier.height(17.dp))
                if (LocalPinyinEnabled.current) {
                    // Keep the hero heading readable rather than letting a
                    // native TextView choose awkward word-by-word breaks.
                    PinyinText("和小恐龙一起", modifier=Modifier.fillMaxWidth(),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = Deep, textAlign = TextAlign.Center, maxLines=1)
                    Spacer(Modifier.height(3.dp))
                    PinyinText("玩转数学魔法！", modifier=Modifier.fillMaxWidth(),
                        fontSize = 26.sp, fontWeight = FontWeight.Black,
                        color = Deep, textAlign = TextAlign.Center, maxLines=1)
                    Spacer(Modifier.height(6.dp))
                    PinyinText("点一点，算一算，",modifier=Modifier.fillMaxWidth(),
                        fontSize=14.sp,color=Muted,textAlign=TextAlign.Center,maxLines=1)
                    PinyinText("星星奖励等你拿！ 🌈",modifier=Modifier.fillMaxWidth(),
                        fontSize=14.sp,color=Muted,textAlign=TextAlign.Center,maxLines=1)
                } else {
                    PinyinText("和小恐龙一起，玩转数学魔法！", fontSize = 27.sp,
                        fontWeight = FontWeight.Black, color = Deep, textAlign = TextAlign.Center)
                    PinyinText("点一点，算一算，星星奖励等你拿！ 🌈",
                        fontSize = 14.sp, color = Muted, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(13.dp))
                MascotShowcase(Modifier.fillMaxWidth(), 175.dp)
                Spacer(Modifier.height(18.dp))
                KidMainButton("🚀 开始数学冒险", onStart, Orange)
                Spacer(Modifier.height(15.dp))
                InfoRow(settings, totalStars, completedRounds)
                Spacer(Modifier.height(15.dp))
                PinyinText("🎵 开启声音，跟着节奏快乐学数学",
                    fontSize = 13.sp, color = Muted)
            }
        }
    }
}

@Composable
private fun HeaderLine(onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PinyinText("🌟 数学星球", fontWeight = FontWeight.Black, color = Deep,
            fontSize = if(LocalPinyinEnabled.current) 19.sp else 23.sp,
            modifier = Modifier.weight(1f),maxLines=if(LocalPinyinEnabled.current) 2 else 1)
        SmallPill("⚙️ 设置", onSettings)
    }
}

@Composable
private fun MascotShowcase(modifier: Modifier, photoSize: Dp) {
    val bobbing = rememberInfiniteTransition(label = "dino")
    val offset by bobbing.animateFloat(
        0f, -6f,
        animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Reverse),
        label = "floating"
    )
    Box(
        modifier.shadow(10.dp, RoundedCornerShape(34.dp))
            .clip(RoundedCornerShape(34.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFF4B4), Color(0xFFFFD8AF))))
            .border(3.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(34.dp))
            .padding(15.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PinyinText("✨ 欢迎来到快乐数字王国 ✨",
                modifier=Modifier.fillMaxWidth(),fontWeight = FontWeight.Bold,
                color = Color(0xFFA65C28), fontSize = 15.sp,
                textAlign = TextAlign.Center, maxLines=2)
            Spacer(Modifier.height(9.dp))
            Image(
                painter = painterResource(R.drawable.mascot),
                contentDescription = "开心的小恐龙举着数学黑板",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(photoSize).offset(y = offset.dp)
                    .clip(RoundedCornerShape(26.dp))
            )
            Spacer(Modifier.height(8.dp))
            PinyinText("🦕 嗨！我是你的数学搭档",
                modifier=Modifier.fillMaxWidth(), fontSize = 15.sp,
                fontWeight = FontWeight.Black, color = Deep, textAlign=TextAlign.Center)
        }
    }
}

@Composable
private fun InfoRow(settings: GameSettings, stars: Int, rounds: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat("🔢", settings.minNumber.toString() + "～" + settings.maxNumber,
            "数字范围", Modifier.weight(1.2f))
        MiniStat("⭐", stars.toString(), "累计星星", Modifier.weight(.8f))
        MiniStat("🏆", rounds.toString(), "完成关卡", Modifier.weight(.8f))
    }
}

@Composable
private fun MiniStat(icon: String, value: String, name: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(19.dp)).background(Color.White.copy(alpha = .93f))
            .padding(vertical = 12.dp, horizontal = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PinyinText(icon, fontSize = 22.sp)
        PinyinText(value, fontWeight = FontWeight.Black, fontSize = if(value.length > 9) 12.sp else 17.sp,
            color = Deep, maxLines = 1, overflow = TextOverflow.Ellipsis)
        PinyinText(name, modifier=Modifier.fillMaxWidth(),
            fontSize = if (LocalPinyinEnabled.current) 12.sp else 11.sp,
            color = Muted, textAlign=TextAlign.Center, maxLines=1,
            overflow=TextOverflow.Ellipsis)
    }
}

@Composable
fun PlayScreen(
    round: RoundSession,
    revision: Int,
    audio: AudioDirector,
    onBack: () -> Unit,
    onChoose: (Int) -> Unit,
    onEnterDigit: (Int) -> Unit,
    onDeleteDigit: () -> Unit,
    onToggleSign: () -> Unit,
    onSubmitNumeric: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onAdjust: () -> Unit
) {
    @Suppress("UNUSED_VARIABLE") val observedRevision = revision
    val p = round.problem
    val startedAt = remember(round.questionNumber, p) { SystemClock.elapsedRealtime() }
    var explanationClock by remember(round.questionNumber, p) {
        mutableStateOf(SystemClock.elapsedRealtime())
    }
    var showSteps by remember(round.questionNumber, p) { mutableStateOf(false) }
    var earlyStepTap by remember(round.questionNumber, p) { mutableStateOf(false) }
    LaunchedEffect(round.questionNumber, p) {
        while (!ExplanationGate.canShow(startedAt, SystemClock.elapsedRealtime())) {
            explanationClock = SystemClock.elapsedRealtime()
            delay(200L)
        }
        explanationClock = SystemClock.elapsedRealtime()
    }
    LaunchedEffect(round.lastOutcome, round.questionNumber) {
        if (round.lastOutcome == AttemptOutcome.REVEAL &&
            ExplanationGate.canShow(startedAt, SystemClock.elapsedRealtime())) {
            showSteps = true
        }
    }
    val waitSeconds = ExplanationGate.secondsRemaining(startedAt, explanationClock)
    val toggleSteps: () -> Unit = {
        audio.tap()
        if (!showSteps && !ExplanationGate.canShow(startedAt, SystemClock.elapsedRealtime())) {
            earlyStepTap = true
        } else {
            earlyStepTap = false
            showSteps = !showSteps
            if (showSteps) p?.let(audio::sayExplanation)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val wide = WideScreen(maxWidth, maxHeight)
        val shortPhone = !wide && (maxHeight < 780.dp || maxWidth < 385.dp)
        val tinyPhone = !wide && maxHeight < 650.dp
        val numeric = round.settings.answerMode == AnswerMode.NUMERIC_INPUT
        Column(
            Modifier.fillMaxSize().padding(
                horizontal = if (wide) 20.dp else 12.dp,
                vertical = if (wide) 8.dp else 6.dp
            )
        ) {
            // Pinyin increases each word's intrinsic width. On a portrait
            // phone, keep the progress label on a separate full-width line
            // rather than squeezing "第 4/10 题" into two vertical fragments.
            if (PinyinLayoutPolicy.useStackedProgressHeader(LocalPinyinEnabled.current, wide)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SmallPill("← 首页", onBack)
                    Spacer(Modifier.weight(1f))
                    SmallPill("🔊", {
                        audio.tap()
                        p?.let { audio.sayQuestion(it, round.questionNumber, repeat = true) }
                    })
                    Spacer(Modifier.width(9.dp))
                    PinyinText("⭐" + round.stars, color = Deep, fontSize = 17.sp,
                        fontWeight = FontWeight.Black, maxLines = 1)
                }
                PinyinText("第 " + round.questionNumber + "/" + round.total + " 题",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center, maxLines = 1,
                    color = Deep, fontSize = 19.sp, fontWeight = FontWeight.Black)
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SmallPill("← 首页", onBack)
                    Spacer(Modifier.weight(1f))
                    PinyinText("第 " + round.questionNumber + "/" + round.total + " 题",
                        color = Deep, fontSize = if (shortPhone) 16.sp else 19.sp,
                        fontWeight = FontWeight.Black, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    SmallPill(if (shortPhone) "🔊" else "🔊 再听", {
                        audio.tap()
                        p?.let { audio.sayQuestion(it, round.questionNumber, repeat = true) }
                    })
                    Spacer(Modifier.width(5.dp))
                    PinyinText("⭐" + round.stars, color = Deep, fontSize = 17.sp,
                        fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
            Spacer(Modifier.height(6.dp))
            BarProgress(round.answeredCount.toFloat() / round.total.coerceAtLeast(1))
            Spacer(Modifier.height(if (wide) 12.dp else 9.dp))
            if (wide) {
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // The child must not scroll just to find Next. The
                    // content is scrollable while the primary action is
                    // pinned below it on every landscape tablet height.
                    Column(Modifier.weight(.9f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuestionPanel(
                            p, round.streak, Modifier.fillMaxWidth().weight(1f),
                            compact = true, minimal = false, innerScroll = true,
                            showNextAction = false,
                            showSteps = showSteps, onShowSteps = toggleSteps,
                            explanationWait = waitSeconds, earlyStepTap = earlyStepTap,
                            round = round, onSkip = onSkip, onNext = onNext, onAdjust = onAdjust
                        )
                        if (LandscapeActionPolicy.pinClassicNext(wide,round.resolved)) {
                            KidMainButton(
                                if (round.questionNumber >= round.total)
                                    "🏆 查看闯关成绩" else "➡️ 下一道题",
                                onNext, Orange, height = 55.dp
                            )
                        }
                    }
                    // The answer area is a full-height illustrated card, not a top-aligned
                    // 2x2 grid floating over a giant blank tablet column.
                    BoxWithConstraints(Modifier.weight(1.1f).fillMaxHeight()) {
                        val answerShape = RoundedCornerShape(28.dp)
                        AnswersPanel(
                            p, round, compact = false,
                            modifier = Modifier.fillMaxWidth().heightIn(min = maxHeight)
                                .verticalScroll(rememberScrollState())
                                .clip(answerShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFF0F9FF), Color(0xFFFFF1E4))
                                    )
                                )
                                .border(2.dp, Color.White.copy(alpha = .85f), answerShape)
                                .padding(horizontal = 22.dp, vertical = 18.dp),
                            onChoose = onChoose, onEnterDigit = onEnterDigit,
                            onDeleteDigit = onDeleteDigit, onToggleSign = onToggleSign,
                            onSubmitNumeric = onSubmitNumeric,
                            spacious = true
                        )
                    }
                }
            } else if (numeric && p != null && !round.resolved) {
                // The child's keypad never scrolls away; only the question area may scroll.
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    QuestionPanel(
                        p, round.streak, Modifier.fillMaxWidth().weight(1f),
                        compact = true, minimal = tinyPhone, innerScroll = true,
                        showSteps = showSteps, onShowSteps = toggleSteps,
                        explanationWait = waitSeconds, earlyStepTap = earlyStepTap,
                        round = round, onSkip = onSkip, onNext = onNext, onAdjust = onAdjust
                    )
                    Spacer(Modifier.height(6.dp))
                    AnswersPanel(
                        p, round, compact = shortPhone, modifier = Modifier.fillMaxWidth(),
                        onChoose = onChoose, onEnterDigit = onEnterDigit,
                        onDeleteDigit = onDeleteDigit, onToggleSign = onToggleSign,
                        onSubmitNumeric = onSubmitNumeric
                    )
                }
            } else {
                // Use the whole phone viewport: spread the question and options evenly
                // instead of squeezing both at the top and leaving a blank bottom.
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val minimumQuestionHeight = maxHeight * if (tinyPhone) .39f else .49f
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuestionPanel(
                            p, round.streak,
                            Modifier.fillMaxWidth().heightIn(min = minimumQuestionHeight),
                            compact = shortPhone, minimal = tinyPhone,
                            showSteps = showSteps, onShowSteps = toggleSteps,
                            explanationWait = waitSeconds, earlyStepTap = earlyStepTap,
                            round = round, onSkip = onSkip, onNext = onNext, onAdjust = onAdjust
                        )
                        AnswersPanel(
                            p, round, compact = shortPhone, modifier = Modifier.fillMaxWidth(),
                            onChoose = onChoose, onEnterDigit = onEnterDigit,
                            onDeleteDigit = onDeleteDigit, onToggleSign = onToggleSign,
                        onSubmitNumeric = onSubmitNumeric
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarProgress(progress: Float) {
    Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)
        .background(Color.White.copy(alpha = 0.86f))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight()
            .clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(Orange, Rose))))
    }
}

@Composable
private fun QuestionPanel(
    p: MathProblem?,
    streak: Int,
    modifier: Modifier,
    compact: Boolean,
    minimal: Boolean = false,
    innerScroll: Boolean = false,
    showNextAction: Boolean = true,
    showSteps: Boolean,
    onShowSteps: () -> Unit,
    explanationWait: Long, earlyStepTap: Boolean,
    round: RoundSession,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onAdjust: () -> Unit
) {
    Column(
        modifier.shadow(7.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF8D6), Color(0xFFFFEAD3))))
            .border(2.dp, Color.White, RoundedCornerShape(28.dp))
            .then(if (innerScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = if (compact) 13.dp else 17.dp,
                vertical = if (compact) 8.dp else 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PinyinText("🦕", fontSize = 36.sp)
                Spacer(Modifier.width(7.dp))
                PinyinText("来算一算吧！", fontSize = 21.sp,
                    fontWeight = FontWeight.Bold, color = Deep)
            }
        }
        Spacer(Modifier.height(if (compact) 3.dp else 18.dp))
        if (p == null) {
            PinyinText("这个组合暂时出不了题。\n可以调整范围或运算类型！", color = Deep,
                fontSize = 19.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            KidMainButton("⚙️ 调整学习范围", onAdjust, Purple, height = 48.dp)
            return@Column
        }
        val charCount = p.expression.length
        PinyinText(
            p.expression,
            fontSize = when {
                charCount >= 35 -> 21.sp
                charCount >= 24 -> 25.sp
                charCount >= 16 -> 31.sp
                else -> if(compact) 33.sp else 41.sp
            },
            lineHeight = if (compact) 39.sp else 47.sp,
            color = Deep, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(if (compact) 3.dp else 10.dp))
        if (!minimal || showSteps) CountingDots(p)
        if (streak >= 2) {
            Spacer(Modifier.height(8.dp))
            PinyinText("🔥 连对 " + streak + " 题，太棒啦！",
                color = Color(0xFFD86B28), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(if (compact) 3.dp else 11.dp))
        SmallPill(if (showSteps) "收起计算步骤 ↑" else "💡 查看过程", onShowSteps)
        if (earlyStepTap && explanationWait > 0L) {
            PinyinText("请先自己思考一下，$explanationWait 秒后再查看过程。",
                color = Color(0xFF9B6133), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(if (compact) 8.dp else 13.dp))
        // Keep this action directly beneath the explanation toggle, in BOTH answer modes.
        if (round.resolved) {
            if (showNextAction) {
                KidMainButton(
                    if (round.questionNumber >= round.total) "🏆 查看闯关成绩" else "➡️ 下一道题",
                    onNext, Orange, height = if (compact) 47.dp else 55.dp
                )
            }
        } else {
            SmallPill("⏭ 不会做？跳过本题", onSkip)
        }
        if (showSteps) {
            Spacer(Modifier.height(8.dp))
            PinyinText("🧩 一步一步来\n\n" + p.explanation, color = Deep,
                fontSize = if(compact) 14.sp else 16.sp,
                lineHeight = 25.sp, textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                    .background(Color.White.copy(alpha = .80f)).padding(10.dp))
        }
    }
}

@Composable
private fun CountingDots(p: MathProblem) {
    val step = p.steps.singleOrNull() ?: return
    if (p.start !in 0..10 || step.operand !in 0..10 || p.answer !in 0..10) return
    if (step.operator != Operation.ADD && step.operator != Operation.SUBTRACT) return
    val total = if (step.operator == Operation.ADD) p.answer else p.start
    if (total <= 0) return
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically) {
            repeat(total) { i ->
                val shade = when {
                    step.operator == Operation.ADD && i >= p.start -> Mint
                    step.operator == Operation.SUBTRACT && i >= p.answer -> Color(0xFFC6CAD4)
                    else -> Rose
                }
                Box(Modifier.size(17.dp).background(shade, CircleShape)
                    .border(1.dp, Color.White, CircleShape))
            }
        }
        Spacer(Modifier.height(4.dp))
        PinyinText(
            if (step.operator == Operation.ADD) "粉色与绿色圆点合起来有几个？"
            else "灰色圆点代表减去的数量",
            color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AnswersPanel(
    p: MathProblem?,
    round: RoundSession,
    compact: Boolean,
    modifier: Modifier,
    onChoose: (Int) -> Unit,
    onEnterDigit: (Int) -> Unit,
    onDeleteDigit: () -> Unit,
    onToggleSign: () -> Unit,
    onSubmitNumeric: () -> Unit,
    spacious: Boolean = false
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (spacious) Arrangement.SpaceEvenly else Arrangement.Top
    ) {
        if (p == null) {
            PinyinText("暂时没有符合设置的练习题，请调整学习范围或运算类型。",
                color = Deep, fontWeight = FontWeight.Bold)
            return@Column
        }
        val fillIn = round.settings.answerMode == AnswerMode.NUMERIC_INPUT
        if (fillIn) {
            NumericAnswerPanel(round, compact, onEnterDigit, onDeleteDigit, onToggleSign, onSubmitNumeric)
        } else {
            if (PinyinLayoutPolicy.useStackedAnswerHeading(LocalPinyinEnabled.current, spacious)) {
                // Pinyin makes two side-by-side labels too wide on portrait
                // phones and tablets. Put them on separate centered rows.
                PinyinText("👇 选出正确答案", modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center, maxLines = 1,
                    color = Deep, fontWeight = FontWeight.Black,
                    fontSize = if (compact) 16.sp else 20.sp)
                PinyinText("四选一", modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center, maxLines = 1,
                    color = Muted, fontSize = 13.sp)
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PinyinText("👇 选出正确答案", color = Deep, fontWeight = FontWeight.Black,
                        fontSize = if (compact) 16.sp else 20.sp, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    PinyinText("四选一", color = Muted, fontSize = 13.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.height(if (compact) 5.dp else 10.dp))
            val colors = listOf(Rose, Azure, Orange, Mint)
            p.choices.chunked(2).forEachIndexed { rowNumber, choices ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    choices.forEachIndexed { offset, answer ->
                        AnswerTile(
                            value = answer, tint = colors[rowNumber * 2 + offset],
                            answer = p.answer, resolved = round.resolved,
                            alreadyWrong = answer in round.wrongAnswers,
                            compact = compact, modifier = Modifier.weight(1f),
                            onClick = { onChoose(answer) }
                        )
                    }
                }
                Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
            }
        }
        when (round.lastOutcome) {
            AttemptOutcome.RETRY -> MessageBanner(
                if (fillIn) "🧡 还不对，再算一算，输入新答案吧！"
                else "🧡 再试一次！灰色答案已经排除啦。",
                Color(0xFFFFEADB)
            )
            AttemptOutcome.CORRECT, AttemptOutcome.CORRECT_AFTER_RETRY ->
                MessageBanner("🎉 答对啦！获得一颗星星！", Color(0xFFD7F9D8))
            AttemptOutcome.REVEAL ->
                MessageBanner("💛 正确答案是 " + p.answer + "，一起看看计算步骤吧。",
                    Color(0xFFEDE7FF))
            else -> PinyinText(
                if (fillIn) "🌈 自己算出答案，再点击提交！" else "🌈 认真想一想，选出答案！",
                fontSize = if (compact) 12.sp else 14.sp, color = Muted
            )
        }
    }
}

@Composable
private fun NumericAnswerPanel(
    round: RoundSession,
    compact: Boolean,
    onEnterDigit: (Int) -> Unit,
    onDeleteDigit: () -> Unit,
    onToggleSign: () -> Unit,
    onSubmitNumeric: () -> Unit
) {
    val keyHeight = if (compact) 43.dp else 55.dp
    val gap = if (compact) 4.dp else 7.dp
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PinyinText("✏️ 把答案填进来", fontSize = if (compact) 15.sp else 18.sp,
            fontWeight = FontWeight.Black, color = Deep)
        Spacer(Modifier.weight(1f))
        PinyinText("不提供选项", fontSize = 12.sp, color = Muted)
    }
    Spacer(Modifier.height(if (compact) 3.dp else 8.dp))
    Box(
        Modifier.fillMaxWidth().height(if (compact) 45.dp else 53.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Color.White.copy(alpha = .94f))
            .border(2.dp, Azure.copy(alpha = .7f), RoundedCornerShape(17.dp)),
        contentAlignment = Alignment.Center
    ) {
        PinyinText(
            when {
                round.numericDigits.isNotEmpty() -> round.numericDigits
                round.resolved && round.lastOutcome == AttemptOutcome.REVEAL ->
                    "答案是 " + (round.problem?.answer ?: "")
                else -> "？"
            },
            fontSize = if (compact) 28.sp else 32.sp, fontWeight = FontWeight.Black,
            color = Deep, maxLines = 1
        )
    }
    if (round.resolved) return
    Spacer(Modifier.height(if (compact) 4.dp else 9.dp))
    val keys = listOf(
        listOf("1", "2", "3"), listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        if (round.settings.allowNegative) listOf("±", "0", "⌫", "提交")
        else listOf("⌫", "0", "提交")
    )
    keys.forEachIndexed { rowNumber, row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            row.forEach { label ->
                val submit = label == "提交"
                val enabled = !round.resolved && !round.finished &&
                    (!submit || DigitComposer.parse(
                        round.numericDigits, round.settings.maxNumber,
                        round.settings.allowNegative
                    ) != null)
                Button(
                    onClick = {
                        when (label) {
                            "⌫" -> onDeleteDigit()
                            "±" -> onToggleSign()
                            "提交" -> onSubmitNumeric()
                            else -> onEnterDigit(label.toInt())
                        }
                    },
                    enabled = enabled,
                    modifier = Modifier.weight(1f).height(keyHeight),
                    shape = RoundedCornerShape(if (compact) 13.dp else 17.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (submit) Orange else
                            if (label == "⌫") Color(0xFFE5EFFC) else Color.White
                    )
                ) {
                    PinyinText(label, fontSize = if (compact) 22.sp else 25.sp,
                        color = Deep, fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
        }
        if (rowNumber < keys.lastIndex) Spacer(Modifier.height(gap))
    }
    Spacer(Modifier.height(if (compact) 4.dp else 9.dp))
}

@Composable
private fun AnswerTile(
    value: Int, tint: Color, answer: Int, resolved: Boolean,
    alreadyWrong: Boolean, compact: Boolean,
    modifier: Modifier, onClick: () -> Unit
) {
    val correct = resolved && value == answer
    // Solid pastel tiles: no transparent layer, white backing rectangle or elevated shadow.
    val baseColor = when (tint) {
        Rose -> Color(0xFFFFE4EA)
        Azure -> Color(0xFFE3EDFF)
        Orange -> Color(0xFFFFECD4)
        else -> Color(0xFFDCF4E9)
    }
    val background by animateColorAsState(
        targetValue = when {
            correct -> Color(0xFFCFFADC)
            alreadyWrong -> Color(0xFFECECF0)
            else -> baseColor
        }, label = "answerColor"
    )
    val shape = RoundedCornerShape(if (compact) 20.dp else 24.dp)
    val enabled = !resolved && !alreadyWrong
    Box(
        modifier.height(if (compact) 74.dp else 96.dp)
            .clip(shape)
            .background(background)
            .border(2.dp, if (correct) Mint else if (alreadyWrong) Color(0xFFD4D9E0)
                else tint, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PinyinText((if (correct) "✓ " else if (alreadyWrong) "✕ " else "") + value,
            color = if(alreadyWrong) Muted else Deep,
            fontSize = if (value >= 100000) 22.sp else if(value > 9999) 26.sp else 34.sp,
            fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MessageBanner(text: String, background: Color) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(background).padding(horizontal = 13.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        PinyinText(text, textAlign = TextAlign.Center, fontSize = 15.sp, lineHeight = 20.sp,
            fontWeight = FontWeight.Bold, color = Deep)
    }
}

@Composable
fun ResultScreen(
    round: RoundSession?,
    allStars: Int,
    audio: AudioDirector,
    onAgain: () -> Unit,
    onHome: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val wide = WideScreen(maxWidth, maxHeight)
        if (wide) {
            Row(Modifier.fillMaxSize().padding(25.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically) {
                MascotShowcase(Modifier.weight(1f).fillMaxHeight(), minOf(255.dp, this@BoxWithConstraints.maxHeight * .55f))
                ResultInfo(round, allStars,
                    Modifier.weight(1.1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    onAgain, onHome)
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PinyinText("🎊 冒险成功！ 🎊", fontSize = 29.sp, fontWeight = FontWeight.Black, color = Deep)
                Spacer(Modifier.height(14.dp))
                Image(painterResource(R.drawable.mascot), contentDescription = "庆祝的小恐龙",
                    modifier = Modifier.size(176.dp).clip(RoundedCornerShape(30.dp)),
                    contentScale = ContentScale.Crop)
                Spacer(Modifier.height(14.dp))
                ResultInfo(round, allStars, Modifier.fillMaxWidth(), onAgain, onHome)
            }
        }
    }
}

@Composable
private fun ResultInfo(
    round: RoundSession?,
    allStars: Int, modifier: Modifier,
    onAgain: () -> Unit,
    onHome: () -> Unit
) {
    Column(modifier,
        horizontalAlignment = Alignment.CenterHorizontally) {
        PinyinText("🏆 闯关小英雄", fontSize = 27.sp, fontWeight = FontWeight.Black, color = Deep)
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(25.dp))
                .clip(RoundedCornerShape(25.dp)).background(Surface).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PinyinText("这次获得", fontSize = 18.sp, color = Deep)
            PinyinText("⭐ " + (round?.stars ?: 0) + " / " + (round?.total ?: 0),
                fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color(0xFFEA9637))
            PinyinText("🔥 最长连续答对 " + (round?.bestStreak ?: 0) + " 题",
                fontSize = 15.sp, color = Muted)
            Spacer(Modifier.height(9.dp))
            PinyinText("你已经累计收集 " + allStars + " 颗星星！",
                fontSize = 17.sp, color = Deep, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        KidMainButton("🚀 再玩一关", onAgain, Orange)
        Spacer(Modifier.height(10.dp))
        SmallPill("🏠 返回星球首页", onHome)
    }
}

@Composable
fun KidMainButton(
    label: String, onClick: () -> Unit, color: Color,
    height: Dp = 65.dp
) {
    val annotated = LocalPinyinEnabled.current && PinyinReadings.containsHan(label)
    // The original 47/55dp result buttons cannot hold ruby + Han text.
    // Increase height only in pinyin mode; keep all old-game sizes untouched
    // when the setting is disabled. Works in the shared phone/tablet panel.
    val buttonHeight = PinyinLayoutPolicy.mainButtonHeightDp(height.value, annotated,label.length).dp
    val lineLimit = PinyinLayoutPolicy.mainButtonLines(annotated,label.length)
    Button(onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(buttonHeight)
            .shadow(7.dp, RoundedCornerShape(23.dp)),
        contentPadding = if (annotated)
            androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 3.dp)
            else ButtonDefaults.ContentPadding,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(23.dp)
    ) {
        if (annotated) {
            // Explicit, full-size centered container prevents the native
            // TextView's ruby line box from drifting to the button bottom.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PinyinText(label, modifier = Modifier.fillMaxWidth(),
                    color = Deep, fontSize = 19.sp, fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center, maxLines = lineLimit,
                    overflow = TextOverflow.Ellipsis)
            }
        } else {
            // Exact pre-pinyin button layout.
            PinyinText(label, color = Deep, fontSize = 20.sp,
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SmallPill(label: String, onClick: () -> Unit) {
    val annotated = LocalPinyinEnabled.current && PinyinReadings.containsHan(label)
    if (annotated) {
        TextButton(onClick = onClick,
            modifier = Modifier.heightIn(min = 58.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 9.dp, vertical = 6.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.textButtonColors(containerColor = Color.White.copy(alpha = .92f))
        ) {
            PinyinText(label, fontWeight = FontWeight.Bold, color = Deep,
                fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    } else {
        // No visual regression in the original (pinyin off) pill layout.
        TextButton(onClick = onClick, shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.textButtonColors(containerColor = Color.White.copy(alpha = .92f))
        ) {
            PinyinText(label, fontWeight = FontWeight.Bold, color = Deep,
                fontSize = 14.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}
