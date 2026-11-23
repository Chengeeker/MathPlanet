package com.game.maths

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val CampaignInk = Color(0xFF253453)
private val CampaignGold = Color(0xFFFFC65A)
private val CampaignCream = Color(0xFFFFF4D8)

@Composable
internal fun DefenseModeMenu(
    audio: AudioDirector,
    onAdventure: () -> Unit,
    onFreeBattle: () -> Unit,
    onBack: () -> Unit
) {
    var comingSoon by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFF66C5E7), Color(0xFFB8E6D0), Color(0xFF446848))))) {
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0..34) {
                val x = (i * 173f + 43f) % size.width
                val y = (i * 97f + 31f) % (size.height * .72f)
                drawCircle(Color.White.copy(alpha = if (i % 3 == 0) .52f else .24f),
                    if (i % 4 == 0) 2.2f else 1.4f, Offset(x, y))
            }
            drawCircle(Color(0xFFFFD475), size.minDimension * .09f,
                Offset(size.width * .16f, size.height * .26f))
            drawCircle(Color(0xFF78AE83), size.width * .47f,
                Offset(size.width * .49f, size.height * 1.14f))
            drawCircle(Color(0xFF4E805F), size.width * .35f,
                Offset(size.width * .84f, size.height * 1.13f))
        }
        Row(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text("花园保卫战", color = CampaignCream, fontSize = 34.sp,
                    fontWeight = FontWeight.Black)
                Text("选择任务模式", color = CampaignInk, fontSize = 19.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                Spacer(Modifier.height(15.dp))
                Text("穿过五片战场，逐步解锁 50 个关卡。",
                    color = CampaignInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text("返回游戏大厅", color = CampaignCream, fontSize = 14.sp,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Color(0xA9243453)).clickable { audio.tap(); onBack() }
                        .heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp))
            }
            Column(Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                ModeStoneButton("冒险模式", "50 个逐步解锁的关卡", CampaignGold, onAdventure)
                Spacer(Modifier.height(4.dp))
                ModeStoneButton("迷你游戏", "短局挑战", Color(0xFFF3D49B)) { audio.tap(); comingSoon = "迷你游戏" }
                Spacer(Modifier.height(4.dp))
                ModeStoneButton("益智模式", "策略与解谜", Color(0xFFE8D2A8)) { audio.tap(); comingSoon = "益智模式" }
                Spacer(Modifier.height(4.dp))
                ModeStoneButton("生存模式", "持续防守", Color(0xFFE9C990)) { audio.tap(); comingSoon = "生存模式" }
                Spacer(Modifier.height(6.dp))
                Button(onClick = { audio.tap(); onFreeBattle() },
                    modifier = Modifier.fillMaxWidth(.92f).heightIn(min = 48.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344B68))) {
                    Text("自由战斗", color = Color.White, fontWeight = FontWeight.Bold,
                        maxLines = 1, softWrap = false)
                }
            }
        }
    }
    comingSoon?.let { mode ->
        Dialog(onDismissRequest = { comingSoon = null }, properties = DialogProperties()) {
            Column(Modifier.fillMaxWidth().padding(8.dp).shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(CampaignCream, Color(0xFFFFE7C9))))
                .border(2.dp, Color.White, RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌱 花园保卫战", color = CampaignInk, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Text(mode, color = CampaignInk, fontSize = 24.sp, fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("正在开发，敬请期待。", color = CampaignInk.copy(alpha = .82f),
                    fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp))
                    .background(CampaignGold).clickable { audio.tap(); comingSoon = null }
                    .padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("知道了", color = CampaignInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ModeStoneButton(title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(.92f).heightIn(min = 48.dp).clip(RoundedCornerShape(15.dp))
        .background(Brush.verticalGradient(listOf(tint, tint.copy(red = (tint.red * .82f).coerceAtLeast(0f))))
        ).border(2.dp, Color(0xFF755F46), RoundedCornerShape(15.dp))
        .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = CampaignInk, fontWeight = FontWeight.Black, fontSize = 18.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = CampaignInk.copy(alpha = .82f), fontSize = 9.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = CampaignInk, fontSize = 28.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun DefenseCampaignMap(
    audio: AudioDirector,
    onBack: () -> Unit,
    onStartLevel: (DefenseCampaignLevel) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val unlocked = remember { mutableIntStateOf(DefenseCampaignProgress.unlockedThrough(context)) }
    var chapter by remember { mutableIntStateOf(((unlocked.intValue - 1) / 10).coerceIn(0, 4)) }
    var previewId by remember { mutableIntStateOf(unlocked.intValue) }
    val levels = DefenseCampaign.levels.filter { it.chapter == chapter + 1 }
    val preview = DefenseCampaign.level(previewId)?.takeIf { it.chapter == chapter + 1 }
        ?: levels.first()
    Box(Modifier.fillMaxSize().background(Color(0xFF17223F)).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("花园保卫战 · 冒险模式", color = CampaignGold, fontSize = 20.sp,
                        fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("已解锁 ${unlocked.intValue.coerceAtMost(50)} / 50 关 · 每关 150 秒",
                        color = Color.White, fontSize = 10.sp, maxLines = 1)
                }
                Box(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFF425477)).clickable { audio.tap(); onBack() }
                    .padding(horizontal = 14.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
                    Text("‹ 返回", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                DefenseCampaign.themes.forEachIndexed { index, theme ->
                    val selected = index == chapter
                    val firstId = index * 10 + 1
                    val open = firstId <= unlocked.intValue
                    Column(Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (selected) CampaignGold else Color(0xFF2B3D5D))
                        .clickable { audio.tap(); chapter = index; previewId = firstId.coerceAtMost(unlocked.intValue) }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("${if (open) "✦" else "🔒"} ${theme.title}",
                            color = if (selected) CampaignInk else Color.White,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (open) "第 ${firstId}–${firstId + 9} 关" else "待解锁",
                            color = if (selected) CampaignInk else Color(0xFFD5E1EF), fontSize = 8.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Text("${chapter + 1} 区 · ${DefenseCampaign.themes[chapter].title}  |  路线、关卡与敌种逐步增加",
                color = Color(0xFFD4E0EE), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(levels) { level ->
                    val open = level.id <= unlocked.intValue
                    val selected = level.id == preview.id
                    Column(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (!open) Color(0xFF25334B) else if (selected) CampaignGold else Color(0xFFEAF0DF))
                        .border(if (selected) 2.dp else 1.dp,
                            if (selected) Color.White else Color(0xFF8B9B9E), RoundedCornerShape(12.dp))
                        .clickable(enabled = open) { audio.tap(); previewId = level.id }
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(if (open) level.label else "🔒",
                            color = if (!open) Color(0xFF9EACC0) else CampaignInk,
                            fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text(if (open) level.title else "未解锁", color = if (!open) Color(0xFF9EACC0) else CampaignInk,
                            fontSize = 9.sp, maxLines = 1, textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF283A59)).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("${preview.label} · ${preview.title}   ${"★".repeat(preview.difficulty)}",
                        color = CampaignGold, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(preview.objective, color = Color.White, fontSize = 10.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    if (preview.battlefield.hasPool) Text("6×9 棋盘：中间两行为水路，其余为草地。",
                        color = Color(0xFFB9DDF0), fontSize = 9.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                Button(onClick = { audio.tap(); onStartLevel(preview) }, enabled = preview.id <= unlocked.intValue,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CampaignGold,
                        disabledContainerColor = Color(0xFF68758B))) {
                    Text(if (preview.id <= unlocked.intValue) "开始关卡" else "尚未解锁",
                        color = if (preview.id <= unlocked.intValue) CampaignInk else Color.White,
                        fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
