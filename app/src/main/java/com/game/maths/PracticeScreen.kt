package com.game.maths

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Handler
import android.os.Looper
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.BoxWithConstraints
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import java.util.Locale

private val PracticeInk=Color(0xFF294369)
private val PracticeBlue=Color(0xFF5E99F2)
private val PracticeMint=Color(0xFF58BA91)

/** Uses the platform Mandarin reader for arbitrary word problems, not the numeric clip vocabulary. */
private class PracticeReader(context: Context, private val audio: AudioDirector) {
    private val main=Handler(Looper.getMainLooper())
    private var ready=false
    private var closed=false
    private var speechNumber=0
    private var currentUtterance:String?=null
    private val tts=TextToSpeech(context.applicationContext) { status ->
        main.post {
            ready=status==TextToSpeech.SUCCESS
        }
    }
    init {
        tts.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
            override fun onStart(utteranceId:String?) {}
            override fun onDone(utteranceId:String?) { main.post {finish(utteranceId)} }
            @Deprecated("Use onError with code")
            override fun onError(utteranceId:String?) { main.post {finish(utteranceId)} }
            override fun onError(utteranceId:String?, errorCode:Int) {
                main.post {finish(utteranceId)}
            }
        })
    }
    private fun finish(utteranceId:String?) {
        if(!closed && utteranceId==currentUtterance) {
            currentUtterance=null
            audio.setExternalNarrating(false)
        }
    }
    fun speak(text:String) {
        if(closed || !ready) return
        audio.stopVoice()
        audio.setExternalNarrating(true)
        tts.language=Locale.SIMPLIFIED_CHINESE
        tts.setSpeechRate(.85f)
        val id="math-practice-${++speechNumber}"
        currentUtterance=id
        if(tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,id) != TextToSpeech.SUCCESS)
            finish(id)
    }
    fun stop() {
        currentUtterance=null
        runCatching {tts.stop()}
        audio.setExternalNarrating(false)
    }
    fun close() {closed=true;stop();tts.shutdown()}
}

@Composable
internal fun PracticeScreen(audio: AudioDirector, onBack: () -> Unit) {
    val context=LocalContext.current
    val prefs=remember {context.getSharedPreferences("math_planet_practice",Context.MODE_PRIVATE)}
    var stage by remember { mutableStateOf(PracticeStage.entries.firstOrNull {
        it.name==prefs.getString("stage",PracticeStage.PRESCHOOL.name)
    } ?: PracticeStage.PRESCHOOL) }
    var topic by remember { mutableStateOf<PracticeTopic?>(null) }
    var questions by remember { mutableStateOf<List<PracticeQuestion>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var revealed by remember { mutableStateOf(false) }
    var hintOpened by remember { mutableStateOf(false) }
    var correct by remember { mutableIntStateOf(0) }
    val reader=remember {PracticeReader(context,audio)}
    DisposableEffect(reader) { onDispose {reader.close()} }

    fun back() {
        reader.stop()
        if(questions.isNotEmpty()) {
            questions=emptyList();selected=null;revealed=false;hintOpened=false
        } else onBack()
    }
    BackHandler {back()}
    fun start(next:PracticeTopic) {
        reader.stop()
        audio.tap()
        questions=PracticeGenerator.round(stage,next,8,Random(System.nanoTime()))
        topic=next;index=0;correct=0;selected=null;revealed=false;hintOpened=false
    }

    val practiceScroll=rememberScrollState()
    LaunchedEffect(index,questions) {
        // After Next, always show the new question from its beginning, even
        // when the previous explanation was scrolled to the bottom.
        practiceScroll.scrollTo(0)
    }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val wide=maxWidth>=720.dp && maxWidth>maxHeight
        val annotated=LocalPinyinEnabled.current
        val pendingNext=LandscapeActionPolicy.pinPracticeNext(
            wide, index in questions.indices, selected!=null || revealed)
        Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(practiceScroll)
            .padding(horizontal=if(wide) 28.dp else 14.dp,vertical=12.dp)
            .padding(bottom=if(pendingNext) 112.dp else 0.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if (annotated && !wide) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    SmallPill(if(questions.isEmpty()) "← 冒险大厅" else "← 换个练习",
                        {audio.tap();back()})
                }
                PinyinText("📚 综合数学练习",modifier=Modifier.fillMaxWidth(),
                    textAlign=TextAlign.Center,maxLines=1,
                    color=PracticeInk,fontSize=21.sp,fontWeight=FontWeight.Black)
            } else {
                Row(verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    SmallPill(if(questions.isEmpty()) "← 冒险大厅" else "← 换个练习",
                        {audio.tap();back()})
                    PinyinText("📚 综合数学练习",color=PracticeInk,
                        fontSize=22.sp,fontWeight=FontWeight.Black,maxLines=1)
                }
            }
            if(questions.isEmpty()) {
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp))
                    .padding(16.dp), verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    PinyinText("先选学习阶段",fontWeight=FontWeight.Bold,color=PracticeInk,fontSize=18.sp)
                    PinyinText("默认幼小衔接，原来的算数探险、小小商店和计算对决完全不变。",
                        color=PracticeInk,fontSize=13.sp)
                    PracticeStage.entries.chunked(if(wide) 5 else 2).forEach { set ->
                        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                            set.forEach { entry ->
                                PracticePill(entry.title,stage==entry,Modifier.weight(1f)) {
                                    stage=entry;prefs.edit().putString("stage",entry.name).apply()
                                    audio.tap()
                                }
                            }
                        }
                    }
                }
                PinyinText("选择练习主题",color=PracticeInk,fontWeight=FontWeight.Black,fontSize=20.sp)
                val available=listOf(PracticeTopic.MIXED)+PracticeGenerator.topics(stage)
                available.chunked(if(wide) 3 else 2).forEach { row ->
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        row.forEach { item ->
                            Column(Modifier.weight(1f)
                                .background(Color.White.copy(alpha=.93f),RoundedCornerShape(21.dp))
                                .border(2.dp,Color.White,RoundedCornerShape(21.dp))
                                .clickable {start(item)}.padding(17.dp),
                                verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                PinyinText(when(item) {
                                    PracticeTopic.MIXED -> "🌈"
                                    PracticeTopic.STORY -> "🛒"
                                    PracticeTopic.PICTURE -> "🍎"
                                    PracticeTopic.SHAPES -> "🔷"
                                    PracticeTopic.PATTERN -> "🧩"
                                    PracticeTopic.CLOCK -> "🕒"
                                    PracticeTopic.CHART -> "📊"
                                },fontSize=29.sp)
                                PinyinText(item.title,fontSize=17.sp,color=PracticeInk,fontWeight=FontWeight.Bold)
                                PinyinText("8 道题 · 可看解题步骤",fontSize=11.sp,color=PracticeInk)
                            }
                        }
                        if(row.size < (if(wide) 3 else 2)) Spacer(Modifier.weight(1f))
                    }
                }
                PinyinText("认识图形、数物品适合大班；乘除法、周长、面积会随年级逐步开放。",
                    color=PracticeInk,fontSize=12.sp)
            } else if(index>=questions.size) {
                Column(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(25.dp))
                    .padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(15.dp)) {
                    PinyinText("🎉 本轮完成！",fontSize=27.sp,color=PracticeInk,fontWeight=FontWeight.Black)
                    PinyinText("答对 $correct / ${questions.size} 题",fontSize=23.sp,color=PracticeInk)
                    PinyinText("继续练习会生成新的题目，不影响原有关卡星星。",
                        color=PracticeInk,textAlign=TextAlign.Center)
                    KidMainButton("🔁 再练一次",{start(topic ?: PracticeTopic.MIXED)},PracticeMint)
                    KidMainButton("📚 返回题型选择",{audio.tap();back()},PracticeBlue)
                }
            } else {
                val q=questions[index]
                if (annotated) {
                    // Stage/topic and question progress have independent layout
                    // constraints: the last '题' may never be orphaned.
                    PinyinText("${stage.title} · ${topic?.title ?: "练习"}",
                        modifier=Modifier.fillMaxWidth(),
                        color=PracticeInk,fontSize=15.sp,fontWeight=FontWeight.Bold,
                        textAlign=TextAlign.Center,maxLines=2)
                    PinyinText("第 ${index+1} / ${questions.size} 题",
                        modifier=Modifier.fillMaxWidth(),
                        color=PracticeInk,fontSize=15.sp,fontWeight=FontWeight.Bold,
                        textAlign=TextAlign.Center,maxLines=1)
                } else {
                    PinyinText("${stage.title} · ${topic?.title ?: "练习"} · 第 ${index+1} / ${questions.size} 题",
                        color=PracticeInk,fontSize=15.sp,fontWeight=FontWeight.Bold)
                }
                Column(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(26.dp))
                    .padding(if(wide) 23.dp else 14.dp),
                    verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        PinyinText(q.topic.title,color=PracticeBlue,fontSize=16.sp,fontWeight=FontWeight.Bold,
                            modifier=Modifier.weight(1f))
                        SmallPill("🔊 读题",{
                            audio.tap()
                            reader.speak(q.prompt)
                        })
                    }
                    PinyinText(q.prompt,modifier=Modifier.fillMaxWidth(),
                        color=PracticeInk,fontWeight=FontWeight.Bold,
                        fontSize=if(annotated) { if(wide) 22.sp else 18.sp }
                            else if(wide) 24.sp else 20.sp,
                        lineHeight=if(annotated) {if(wide) 35.sp else 31.sp}
                            else if(wide) 34.sp else 29.sp)
                    if(q.visual!=PracticeVisual.NONE) {
                        PracticeDrawing(q,Modifier.fillMaxWidth().height(if(wide) 190.dp else 168.dp))
                    }
                    PinyinText("选出正确答案",
                        modifier=if(annotated)Modifier.fillMaxWidth() else Modifier,
                        textAlign=if(annotated)TextAlign.Center else null,
                        color=PracticeInk,fontSize=14.sp,maxLines=1)
                    q.options.chunked(2).forEach { pair ->
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            pair.forEach { option ->
                                val answered=selected!=null || revealed
                                val active=answered && option==q.answer
                                val wrong=selected==option && option!=q.answer
                                val bg=when {
                                    active -> Color(0xFFD7F5DE)
                                    wrong -> Color(0xFFFFDFDF)
                                    else -> Color(0xFFEAF2FF)
                                }
                                Box(Modifier.weight(1f)
                                    .heightIn(min=if(annotated) 78.dp else 60.dp)
                                    .background(bg,RoundedCornerShape(17.dp))
                                    .border(2.dp,if(active) PracticeMint else Color.Transparent,
                                        RoundedCornerShape(17.dp))
                                    .clickable(enabled=!answered) {
                                        // No generic tap: it consumed the only haptic pulse,
                                        // and classic success()/retry() mute sound in voice mode.
                                        reader.stop()
                                        selected=option
                                        if(option==q.answer) correct++
                                        audio.practiceAnswer(option==q.answer)
                                    }
                                    .padding(vertical=15.dp),contentAlignment=Alignment.Center) {
                                    val label=if(q.visual==PracticeVisual.CLOCK) {
                                        val hours=option/2
                                        if(option%2==0) "${hours}点整" else "${hours}点半"
                                    } else option.toString()+if(q.unit.isEmpty()) "" else " ${q.unit}"
                                    if(annotated && q.visual!=PracticeVisual.CLOCK && q.unit.isNotEmpty()) {
                                        // A number and its measure word are a
                                        // single choice, never two stacked lines.
                                        Row(verticalAlignment=Alignment.CenterVertically,
                                            horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                            PinyinText(option.toString(),color=PracticeInk,
                                                fontSize=24.sp,fontWeight=FontWeight.Black,
                                                maxLines=1,softWrap=false)
                                            PinyinText(q.unit,color=PracticeInk,
                                                fontSize=17.sp,fontWeight=FontWeight.Bold,
                                                maxLines=1,softWrap=false)
                                        }
                                    } else {
                                        PinyinText(label,color=PracticeInk,fontSize=20.sp,
                                            fontWeight=FontWeight.Black,maxLines=1,
                                            softWrap=false,textAlign=TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                    if(selected==null && !revealed) {
                        if(hintOpened && q.hint.isNotBlank())
                            PinyinText("💡 ${q.hint}",color=PracticeInk,fontSize=15.sp)
                        if(annotated && !wide) {
                            Column(Modifier.fillMaxWidth(),
                                horizontalAlignment=Alignment.CenterHorizontally,
                                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                if(!hintOpened) SmallPill("💡 获取提示",{audio.tap();hintOpened=true})
                                SmallPill("👀 看解题步骤",{
                                    audio.tap();reader.stop();revealed=true
                                })
                            }
                        } else {
                            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                                if(!hintOpened) SmallPill("💡 获取提示",{audio.tap();hintOpened=true})
                                SmallPill("👀 看解题步骤",{
                                    audio.tap();reader.stop();revealed=true
                                })
                            }
                        }
                    } else {
                        PinyinText(if(selected==q.answer) "🌟 回答正确！"
                            else "🌱 这次再学一学，答案是：${if(q.visual==PracticeVisual.CLOCK) "${q.answer/2}点${if(q.answer%2==0)"整" else "半"}" else q.answer.toString()+q.unit}",
                            color=PracticeInk,fontWeight=FontWeight.Black,fontSize=19.sp)
                        PinyinText(q.explanation,color=PracticeInk,fontSize=16.sp,lineHeight=24.sp)
                        if(q.hint.isNotEmpty()) PinyinText("小提示：${q.hint}",color=PracticeInk,fontSize=13.sp)
                        if(!wide) {
                            KidMainButton(if(index==questions.lastIndex)
                                "🏆 查看本轮成绩" else "➡️ 下一题",{
                                reader.stop();index++;selected=null;revealed=false;hintOpened=false;audio.tap()
                            },PracticeMint)
                        }
                    }
                }
            }
        }
        if(pendingNext) {
            // Wide tablets keep the continuation action in the viewport.
            // Only the question/explanation content can scroll behind it.
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color(0xFFF1F9FF).copy(alpha=.97f))
                .padding(horizontal=28.dp,vertical=8.dp)) {
                KidMainButton(
                    if(index==questions.lastIndex) "🏆 查看本轮成绩" else "➡️ 下一题",
                    {
                        reader.stop();index++;selected=null;revealed=false;hintOpened=false;audio.tap()
                    },PracticeMint
                )
            }
        }
        }
    }
}

@Composable
private fun PracticePill(label:String,active:Boolean,modifier:Modifier=Modifier,onClick:()->Unit) {
    Box(modifier.background(if(active)Color(0xFFD9F6EA) else Color(0xFFEFF3FB),
        RoundedCornerShape(13.dp)).border(2.dp,if(active)PracticeMint else Color.Transparent,
        RoundedCornerShape(13.dp)).clickable(onClick=onClick).padding(vertical=10.dp,horizontal=7.dp),
        contentAlignment=Alignment.Center) {
        PinyinText(label,fontSize=13.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,color=PracticeInk)
    }
}

@Composable
private fun PracticeDrawing(q:PracticeQuestion, modifier:Modifier=Modifier) {
    Canvas(modifier.background(Color(0xFFF7FAFF),RoundedCornerShape(18.dp))) {
        val w=size.width;val h=size.height
        val stroke=3.dp.toPx()
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=android.graphics.Color.rgb(40,72,102)
            textSize=16.sp.toPx();textAlign=Paint.Align.CENTER
        }
        when(q.visual) {
            PracticeVisual.DOTS,PracticeVisual.GROUPS -> {
                val count=if(q.visual==PracticeVisual.DOTS)q.a else q.a*q.b
                val groups=if(q.visual==PracticeVisual.DOTS)1 else q.a
                val perGroup=if(q.visual==PracticeVisual.DOTS)count else q.b
                val columns=if(groups==1) minOf(perGroup,5) else minOf(groups,4)
                val rows=if(groups==1) (count+columns-1)/columns else (groups+columns-1)/columns
                val stepX=w/(columns+1);val stepY=h/(rows+1)
                if(groups==1) repeat(count) { i ->
                    drawCircle(Color(0xFFFFB64C),radius=minOf(15.dp.toPx(),stepX*.31f,stepY*.33f),
                        center=Offset((i%columns+1)*stepX,(i/columns+1)*stepY))
                } else repeat(groups) { group ->
                    val left=(group%columns+.5f)*stepX
                    val top=(group/columns+1)*stepY
                    repeat(perGroup) { j ->
                        val offset=(j-(perGroup-1)/2f)*minOf(25.dp.toPx(),stepX/(perGroup+1))
                        drawCircle(Color(0xFFFFB64C),radius=8.dp.toPx(),
                            center=Offset(left+offset,top))
                    }
                }
            }
            PracticeVisual.SHAPES -> {
                val count=q.a
                val columns=minOf(count,5);val dx=w/(columns+1)
                val rows=(count+columns-1)/columns;val dy=h/(rows+1)
                repeat(count) { i ->
                    val x=(i%columns+1)*dx;val y=(i/columns+1)*dy
                    val r=minOf(25.dp.toPx(),dx*.29f,dy*.35f)
                    when(q.b) {
                        0 -> drawCircle(PracticeBlue,r,Offset(x,y))
                        1 -> drawRect(PracticeMint,Offset(x-r,y-r),androidx.compose.ui.geometry.Size(2*r,2*r))
                        else -> {
                            val p=Path().apply {
                                moveTo(x,y-r);lineTo(x-r,y+r);lineTo(x+r,y+r);close()
                            }
                            drawPath(p,Color(0xFFFFB64C))
                        }
                    }
                }
            }
            PracticeVisual.RECTANGLE -> {
                val left=w*.17f;val top=h*.20f
                val rw=w*.60f;val rh=h*.63f
                drawRect(Color(0xFFD7F3D8),Offset(left,top),
                    androidx.compose.ui.geometry.Size(rw,rh))
                drawRect(PracticeMint,Offset(left,top),androidx.compose.ui.geometry.Size(rw,rh),
                    style=Stroke(stroke))
                drawContext.canvas.nativeCanvas.drawText("${q.a} 厘米",left+rw/2,top-8.dp.toPx(),paint)
                drawContext.canvas.nativeCanvas.drawText("${q.b}厘米",left+rw+37.dp.toPx(),top+rh/2,paint)
            }
            PracticeVisual.BARS -> {
                val bars=listOf(q.a,q.b,q.c)
                val names=listOf("香蕉","苹果","橘子")
                val max=bars.max().coerceAtLeast(1)
                bars.forEachIndexed { i, n ->
                    val x=w*(i+1)/4f
                    val bh=h*.65f*n/max
                    drawRect(listOf(PracticeBlue,PracticeMint,Color(0xFFFFB64C))[i],
                        Offset(x-18.dp.toPx(),h*.77f-bh),
                        androidx.compose.ui.geometry.Size(36.dp.toPx(),bh))
                    drawContext.canvas.nativeCanvas.drawText("$n",x,h*.77f-bh-7.dp.toPx(),paint)
                    drawContext.canvas.nativeCanvas.drawText(names[i],x,h*.92f,paint)
                }
            }
            PracticeVisual.CLOCK -> {
                val center=Offset(w*.5f,h*.49f)
                val radius=minOf(w,h)*.38f
                drawCircle(Color.White,radius,center)
                drawCircle(PracticeBlue,radius,center,style=Stroke(stroke))
                repeat(12) { i ->
                    val number=i+1
                    val theta=(number.toDouble()/6.0*Math.PI-Math.PI/2).toFloat()
                    val a=Offset(center.x+cos(theta)*radius*.84f,
                        center.y+sin(theta)*radius*.84f)
                    val b=Offset(center.x+cos(theta)*radius*.94f,
                        center.y+sin(theta)*radius*.94f)
                    drawLine(PracticeInk,a,b,strokeWidth=2.dp.toPx())
                    val labelX=center.x+cos(theta)*radius*.69f
                    val labelY=center.y+sin(theta)*radius*.69f+paint.textSize*.34f
                    drawContext.canvas.nativeCanvas.drawText(number.toString(),labelX,labelY,paint)
                }
                val hoursAngle=((q.a%12)+q.b/60f)/12f*2*Math.PI-Math.PI/2
                val minutesAngle=(q.b/60f)*2*Math.PI-Math.PI/2
                drawLine(PracticeInk,center,Offset(center.x+cos(hoursAngle).toFloat()*radius*.55f,
                    center.y+sin(hoursAngle).toFloat()*radius*.55f),5.dp.toPx())
                drawLine(PracticeBlue,center,Offset(center.x+cos(minutesAngle).toFloat()*radius*.77f,
                    center.y+sin(minutesAngle).toFloat()*radius*.77f),3.dp.toPx())
                drawCircle(PracticeInk,5.dp.toPx(),center)
            }
            else -> Unit
        }
    }
}
