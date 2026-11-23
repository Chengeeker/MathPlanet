package com.game.maths

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SettingInk = Color(0xFF294369)
private val SettingGray = Color(0xFF6B809D)
private val SeaBlue = Color(0xFF609AEE)
private val SunGold = Color(0xFFFFB64C)

@Composable
fun SettingsScreen(
    initial: GameSettings,
    audio: AudioDirector,
    onBack: () -> Unit,
    onSave: (GameSettings) -> Unit,
    onPreviewAudio: () -> Unit,
    onAudioPreviewSettings: (GameSettings) -> Unit
) {
    var edit by remember(initial) { mutableStateOf(initial) }
    var maxText by remember(initial) { mutableStateOf(initial.maxNumber.toString()) }
    var customRange by remember(initial) {
        mutableStateOf(initial.maxNumber !in listOf(10, 50, 100, 1000))
    }
    var showError by remember { mutableStateOf(false) }
    val max = maxText.toIntOrNull()
    val valid = max != null && max in 3..1_000_000

    fun update(updated: GameSettings) {
        edit = updated
        onAudioPreviewSettings(updated)
    }
    fun save() {
        if (!valid) {
            audio.retry()
            showError = true
            return
        }
        onSave(edit.copy(
            minNumber = 0,
            maxNumber = max!!,
            allowNegative = false
        ))
    }

    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val wide = maxWidth >= 600.dp && maxWidth > maxHeight
        val padding = if (wide) 20.dp else 15.dp
        Column(Modifier.fillMaxSize().padding(padding)) {
            if(PinyinLayoutPolicy.useStackedPageHeading(LocalPinyinEnabled.current,this@BoxWithConstraints.maxWidth.value)) {
                Column(Modifier.fillMaxWidth()) {
                    SmallPill("← 返回",onBack)
                    PinyinText("⚙️ 探险设置",color=SettingInk,
                        fontSize=20.sp,fontWeight=FontWeight.Black)
                }
            } else {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    SmallPill("← 返回",onBack)
                    Spacer(Modifier.width(12.dp))
                    PinyinText("⚙️ 探险设置",color=SettingInk,
                        fontSize=PinyinLayoutPolicy.pageTitleSp(25f,LocalPinyinEnabled.current).sp,
                        fontWeight=FontWeight.Black,modifier=Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        AnswerModeSection(edit.answerMode, audio) { mode ->
                            edit = edit.copy(answerMode = mode)
                        }
                        Spacer(Modifier.height(12.dp))
                        RangeSection(
                            edit = edit,
                            maxText = maxText,
                            customRange = customRange,
                            onMax = { maxText = it; showError = false },
                            onPreset = { n ->
                                audio.tap()
                                maxText = n.toString()
                                customRange = false
                                showError = false
                                edit = edit.copy(
                                    minNumber = if (edit.allowNegative) -n else 0,
                                    maxNumber = n
                                )
                            },
                            onCustom = { audio.tap(); customRange = true },
                            onNegative = { enabled ->
                                audio.tap()
                                edit = edit.copy(
                                    allowNegative = enabled,
                                    minNumber = if (enabled) -(maxText.toIntOrNull() ?: edit.maxNumber) else 0
                                )
                            },
                            invalid = showError && !valid
                        )
                        Spacer(Modifier.height(12.dp))
                        OperationSection(edit.operations, audio) { ops ->
                            edit = edit.copy(operations = ops)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        ComplexitySection(edit, audio, { n -> edit = edit.copy(terms = n) }, { n -> edit = edit.copy(roundLength = n) })
                        Spacer(Modifier.height(12.dp))
                        AudioSection(edit, audio, { update(it) }, onPreviewAudio)
                        Spacer(Modifier.height(10.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                KidMainButton("💾 保存设置，继续冒险", { audio.tap(); save() }, SunGold, 58.dp)
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    AnswerModeSection(edit.answerMode, audio) { mode ->
                        edit = edit.copy(answerMode = mode)
                    }
                    Spacer(Modifier.height(13.dp))
                    RangeSection(
                            edit = edit,
                            maxText = maxText,
                            customRange = customRange,
                            onMax = { maxText = it; showError = false },
                            onPreset = { n ->
                                audio.tap()
                                maxText = n.toString()
                                customRange = false
                                showError = false
                                edit = edit.copy(
                                    minNumber = if (edit.allowNegative) -n else 0,
                                    maxNumber = n
                                )
                            },
                            onCustom = { audio.tap(); customRange = true },
                            onNegative = { enabled ->
                                audio.tap()
                                edit = edit.copy(
                                    allowNegative = enabled,
                                    minNumber = if (enabled) -(maxText.toIntOrNull() ?: edit.maxNumber) else 0
                                )
                            },
                            invalid = showError && !valid
                        )
                    Spacer(Modifier.height(13.dp))
                    OperationSection(edit.operations, audio) { ops ->
                        edit = edit.copy(operations = ops)
                    }
                    Spacer(Modifier.height(13.dp))
                    ComplexitySection(edit, audio, { n -> edit = edit.copy(terms = n) }, { n -> edit = edit.copy(roundLength = n) })
                    Spacer(Modifier.height(13.dp))
                    AudioSection(edit, audio, { update(it) }, onPreviewAudio)
                    Spacer(Modifier.height(13.dp))
                }
                Spacer(Modifier.height(11.dp))
                KidMainButton("💾 保存设置", { audio.tap(); save() }, SunGold, 62.dp)
            }
        }
    }
}

@Composable
private fun AnswerModeSection(
    selected: AnswerMode,
    audio: AudioDirector,
    onChange: (AnswerMode) -> Unit
) {
    SettingCard("✏️ 怎样回答？", "孩子练习的作答方式") {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            AnswerMode.entries.forEach { mode ->
                val chosen = mode == selected
                val title = if (mode == AnswerMode.MULTIPLE_CHOICE) "🎯 四选一" else "✍️ 数字填空"
                Box(
                    Modifier.weight(1f)
                        .background(if (chosen) SeaBlue.copy(alpha = .19f) else Color(0xFFF3F5FA),
                            RoundedCornerShape(14.dp))
                        .border(2.dp, if (chosen) SeaBlue else Color.Transparent,
                            RoundedCornerShape(14.dp))
                        .clickable {
                            audio.tap()
                            onChange(mode)
                        }
                        .padding(vertical = 15.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PinyinText(title, color = SettingInk, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        PinyinText(
            if (selected == AnswerMode.NUMERIC_INPUT)
                "不显示候选答案，由孩子通过应用内数字键盘独立填写。"
            else "从四个数字中选择答案，适合刚开始接触数学的孩子。",
            color = SettingGray, fontSize = 12.sp
        )
    }
}

@Composable
private fun SettingCard(
    title: String, description: String,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth()
        .background(Color.White.copy(alpha = .94f), RoundedCornerShape(24.dp))
        .border(2.dp, Color.White, RoundedCornerShape(24.dp))
        .padding(15.dp)) {
        PinyinText(title, color = SettingInk, fontWeight = FontWeight.Black, fontSize = 20.sp)
        PinyinText(description, color = SettingGray, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun RangeSection(
    edit: GameSettings,
    maxText: String,
    customRange: Boolean,
    onMax: (String) -> Unit,
    onPreset: (Int) -> Unit,
    onCustom: () -> Unit,
    onNegative: (Boolean) -> Unit,
    invalid: Boolean
) {
    SettingCard("🔢 学习范围设置", "全部数学游戏、对战和商店共用这个范围；出题数字和计算结果都不得超过上限") {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(10, 50, 100, 1000).forEach { n ->
                val checked = !customRange && maxText == n.toString()
                Box(
                    Modifier.weight(1f)
                        .background(if (checked) SeaBlue else Color(0xFFEFF4FD),
                            RoundedCornerShape(11.dp))
                        .clickable { onPreset(n) }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PinyinText(n.toString(), color = if (checked) Color.White else SettingInk,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Box(
                Modifier.weight(1f)
                    .background(if (customRange) SeaBlue else Color(0xFFEFF4FD),
                        RoundedCornerShape(11.dp))
                    .clickable(onClick = onCustom)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                PinyinText("自定义", color = if (customRange) Color.White else SettingInk,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        if (customRange) {
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(
                value = maxText,
                onValueChange = {
                    if (it.length <= 7 && it.all(Char::isDigit)) onMax(it)
                },
                singleLine = true,
                label = { PinyinText("最大数字（3～1,000,000）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(7.dp))
        PinyinText(
            if (invalid) "⚠️ 最大数字应为 3～1,000,000。"
            else {
                val upper = maxText.toIntOrNull()
                if (upper == null) "请输入最大数字"
                else "当前全游戏学习范围：0 ～ $upper"
            },
            color = if (invalid) Color(0xFFE15D5C) else SettingInk,
            fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 5.dp)
        )
        PinyinText("自由算术、商店和对战的算式数字、商品价格、实付金额及答案，都在这个范围内；纸币按钮仍提供 1～100 元。",
            color = SettingGray, fontSize = 12.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun OperationSection(
    selected: Set<Operation>,
    audio: AudioDirector,
    onChange: (Set<Operation>) -> Unit
) {
    SettingCard("🧮 运算魔法", "选择一种或多种运算") {
        val all = Operation.entries
        all.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { op ->
                    val active = op in selected
                    val color = when(op) {
                        Operation.ADD -> Color(0xFFFF86A1)
                        Operation.SUBTRACT -> SeaBlue
                        Operation.MULTIPLY -> SunGold
                        Operation.DIVIDE -> Color(0xFF58D3B2)
                    }
                    Box(
                        Modifier.weight(1f)
                            .background(if(active) color.copy(alpha=.23f)
                                else Color(0xFFF1F2F8), RoundedCornerShape(15.dp))
                            .border(2.dp, if(active) color else Color.Transparent,
                                RoundedCornerShape(15.dp))
                            .clickable {
                                audio.tap()
                                val ops = if (active) selected - op else selected + op
                                if(ops.isNotEmpty()) onChange(ops)
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PinyinText(op.symbol + " " + when(op){
                            Operation.ADD -> "加法"
                            Operation.SUBTRACT -> "减法"
                            Operation.MULTIPLY -> "乘法"
                            Operation.DIVIDE -> "除法"
                        }, color = SettingInk, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        PinyinText("仅控制自由算术探险；对决固定使用加减法，所有玩法均不产生负数答案。", color = SettingGray, fontSize = 12.sp)
    }
}

@Composable
private fun ComplexitySection(
    edit: GameSettings, audio: AudioDirector, onTerms: (Int) -> Unit, onRounds: (Int) -> Unit
) {
    SettingCard("🧩 题目复杂度", "每道题可以有几个数字？") {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            (2..5).forEach { n ->
                val checked = n == edit.terms
                Box(Modifier.weight(1f)
                    .background(if(checked) Color(0xFFA58CED) else Color(0xFFEDEAF6),
                        RoundedCornerShape(15.dp))
                    .clickable { audio.tap(); onTerms(n) }
                    .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    PinyinText(n.toString(), fontWeight = FontWeight.Black, fontSize = 23.sp,
                        color = if(checked) Color.White else SettingInk)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        PinyinText(
            if(edit.terms == 2) "例子：1 + 1"
            else "例子：" + List(edit.terms) { "1" }.joinToString(" + "),
            color = SettingGray, fontSize = 13.sp
        )
        Spacer(Modifier.height(9.dp))
        PinyinText("单人游戏每局 " + edit.roundLength + " 道题（自由算术、小小商店共用）",
            color = SettingInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Slider(
            value = edit.roundLength.toFloat(),
            onValueChange = {
                val rounds = ((it / 5).toInt() * 5).coerceIn(5,20)
                if(rounds != edit.roundLength) onRounds(rounds)
            },
            valueRange = 5f..20f, steps = 2,
            onValueChangeFinished = { audio.tap() }
        )
    }
}

@Composable
private fun AudioSection(
    edit: GameSettings,
    audio: AudioDirector,
    onChange: (GameSettings) -> Unit,
    onPreviewAudio: () -> Unit
) {
    SettingCard("🎵 声音与节奏", "像儿童动画一样，有音乐也有小恐龙的鼓励") {
        SettingSwitch("🔤 汉字拼音辅助", "开启后为界面与题目中的汉字标注拼音；默认关闭",
            edit.pinyinEnabled, audio) { onChange(edit.copy(pinyinEnabled=it)) }
        Spacer(Modifier.height(7.dp))
        SettingSwitch("🗣️ 中文语音", "内置离线中文读题和鼓励",
            edit.voiceEnabled, audio) { onChange(edit.copy(voiceEnabled=it)) }
        Spacer(Modifier.height(4.dp))
        PinyinText(audio.voiceStatus, color = Color(0xFF328B75), fontSize = 12.sp)
        PinyinText(audio.playbackStatus, color = SettingInk, fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 5.dp))
        PinyinText("如果试听没声音，请检查平板的媒体音量，而不是铃声音量。",
            color = SettingGray, fontSize = 11.sp)
        Spacer(Modifier.height(5.dp))
        SmallPill("🔊 试听小恐龙配音", { audio.tap(); onPreviewAudio() })
        Spacer(Modifier.height(12.dp))
        PinyinText("🧪 配音播放方式", color = SettingInk,
            fontSize = 15.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // A phonetic annotation consumes vertical space and requires a
            // legible full-width label. Two 50%-width chips on a phone used to
            // split the final "出" of "PCM 软件直出" onto another line.
            val stacked = PinyinLayoutPolicy.stackVoiceModeOptions(
                LocalPinyinEnabled.current, maxWidth.value)
            if (stacked) {
                Column(Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoicePlaybackMode.entries.forEach { mode ->
                        VoiceModeChoice(mode, edit.voicePlaybackMode,
                            Modifier.fillMaxWidth(), audio) {
                            onChange(edit.copy(voicePlaybackMode = mode))
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoicePlaybackMode.entries.forEach { mode ->
                        VoiceModeChoice(mode, edit.voicePlaybackMode,
                            Modifier.weight(1f), audio) {
                            onChange(edit.copy(voicePlaybackMode = mode))
                        }
                    }
                }
            }
        }
        PinyinText(
            if (edit.voicePlaybackMode == VoicePlaybackMode.DIRECT_PCM)
                "推荐尝试：应用内把音频重采样为 48 kHz，整句预加载播放，不使用媒体解码器。"
            else "旧版对照：系统 MediaPlayer 播放本地 WAV。切换后可点击上方试听。",
            color = SettingGray, fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
        )
        Spacer(Modifier.height(5.dp))
        SettingSwitch("🎼 轻快背景音乐", "语音播报期间暂停音乐，朗读结束后恢复",
            edit.musicEnabled, audio) { onChange(edit.copy(musicEnabled=it)) }
        if (edit.musicEnabled) {
            PinyinText("音乐音量 " + (edit.musicVolume*100).toInt() + "%",
                color = SettingGray, fontSize = 12.sp)
            Slider(value = edit.musicVolume, onValueChange = {
                onChange(edit.copy(musicVolume=it))
            })
        }
        SettingSwitch("🎶 互动音效", "按键、答对与鼓励使用不同音效",
            edit.soundEnabled, audio) { onChange(edit.copy(soundEnabled=it)) }
        SettingSwitch("📳 触摸震动", "轻触、答对、答错都有不同手感",
            edit.hapticsEnabled, audio) { onChange(edit.copy(hapticsEnabled=it)) }
        PinyinText("全部声音均已内置，不需要联网。", fontSize = 12.sp, color = SettingGray)
    }
}

/** Audio output choices remain single-line even with phonetic ruby enabled.
 * On narrow annotated screens the two choices stack vertically for legibility.
 */
@Composable
private fun VoiceModeChoice(
    mode: VoicePlaybackMode,
    selected: VoicePlaybackMode,
    modifier: Modifier,
    audio: AudioDirector,
    onSelect: () -> Unit
) {
    val active = mode == selected
    val annotated = LocalPinyinEnabled.current
    val caption = if (mode == VoicePlaybackMode.DIRECT_PCM)
        "PCM 软件直出" else "系统媒体播放"
    Box(
        modifier
            .heightIn(min = if (annotated) 62.dp else 44.dp)
            .background(if (active) Color(0xFFD9F6EA)
                else Color(0xFFF1F3F9), RoundedCornerShape(15.dp))
            .border(2.dp, if (active) Color(0xFF53C8A8)
                else Color.Transparent, RoundedCornerShape(15.dp))
            .clickable {
                audio.tap()
                onSelect()
            }
            .padding(vertical = 12.dp, horizontal = if (annotated) 9.dp else 3.dp),
        contentAlignment = Alignment.Center
    ) {
        PinyinText(caption,
            modifier = Modifier.fillMaxWidth(),
            color = SettingInk, fontWeight = FontWeight.Bold,
            fontSize = 13.sp, textAlign = TextAlign.Center,
            maxLines = 1, softWrap = false)
    }
}

@Composable
private fun SettingSwitch(
    label: String, note: String, checked: Boolean,
    audio: AudioDirector, onChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            PinyinText(label, color = SettingInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            PinyinText(note, color = SettingGray, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = {
            audio.tap()
            onChange(it)
        })
    }
}
