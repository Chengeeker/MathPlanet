package com.game.maths

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Small-shop play area: the question and the money controls appear at the top,
 * and submitting the answer turns the SAME button into Next, without moving it.
 * Most portrait and landscape tablet viewports require no vertical scrolling.
 */
@Composable
internal fun CompactShopScreen(
    q: SkillQuestion, questionIndex: Int, questionCount: Int,
    score: Int, streak: Int, title: String, allowRoleSwitch: Boolean,
    activeRole: ShopRole, resolved: Boolean, message: String,
    onBack: () -> Unit, onRoleChange: (ShopRole) -> Unit,
    onCheck: (Boolean, String) -> Unit, onNext: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val landscape = maxWidth >= 650.dp && maxWidth > maxHeight
        val compact = maxHeight < 640.dp
        val gap = if (compact) 6.dp else 9.dp
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = if (landscape) 1100.dp else 720.dp)
                    .fillMaxWidth()
                    .then(if (landscape) Modifier.fillMaxHeight()
                        else Modifier.verticalScroll(rememberScrollState()))
                    .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                if(PinyinLayoutPolicy.useStackedPageHeading(
                        LocalPinyinEnabled.current,this@BoxWithConstraints.maxWidth.value)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        SmallPill("← 返回",onBack)
                        Spacer(Modifier.weight(1f))
                        PinyinText("${questionIndex + 1}/$questionCount · ⭐$score",
                            color=AdventureInk,fontWeight=FontWeight.Bold,
                            fontSize=12.sp,maxLines=1)
                    }
                    PinyinText(if(allowRoleSwitch) "🛒 $title" else "📒 $title",
                        color=AdventureInk,fontSize=18.sp,
                        fontWeight=FontWeight.Black,maxLines=2,
                        modifier=Modifier.fillMaxWidth())
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        SmallPill("← 返回", onBack)
                        PinyinText(if (allowRoleSwitch) "🛒 $title" else "📒 $title",
                            color = AdventureInk, fontSize = if (compact) 18.sp else 20.sp,
                            fontWeight = FontWeight.Black, maxLines = 1,
                            modifier = Modifier.weight(1f))
                        PinyinText("${questionIndex + 1}/$questionCount · ⭐$score",
                            color = AdventureInk, fontWeight = FontWeight.Bold,
                            fontSize = 12.sp, maxLines = 1)
                    }
                }
                if (allowRoleSwitch) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        AdventureChip("🧺 顾客付款", activeRole == ShopRole.CUSTOMER) {
                            if (activeRole != ShopRole.CUSTOMER) onRoleChange(ShopRole.CUSTOMER)
                        }
                        AdventureChip("🏪 店长找零", activeRole == ShopRole.CASHIER) {
                            if (activeRole != ShopRole.CASHIER) onRoleChange(ShopRole.CASHIER)
                        }
                        Spacer(Modifier.weight(1f))
                        if (streak > 0) PinyinText("🔥$streak", color = AdventureInk, fontSize = 13.sp)
                    }
                } else {
                    PinyinText(if (q.role == ShopRole.CASHIER) "🏪 店长找零" else "🧺 顾客付款",
                        fontSize = 14.sp, color = AdventureInk, fontWeight = FontWeight.Bold)
                }
                if (landscape) {
                    Row(Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top) {
                        CompactShopQuestion(q, Modifier.weight(.75f))
                        // Only the banknote summary may scroll. Confirmation
                        // becomes Next at the exact same pinned location.
                        CompactShopBoard(q, resolved, message, compact, questionIndex == questionCount - 1,
                            onCheck, onNext, Modifier.weight(1.25f).fillMaxHeight(),
                            pinAction = LandscapeActionPolicy.pinShopAction(landscape))
                    }
                } else {
                    CompactShopQuestion(q, Modifier.fillMaxWidth())
                    CompactShopBoard(q, resolved, message, compact, questionIndex == questionCount - 1,
                        onCheck, onNext, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun CompactShopQuestion(q: SkillQuestion, modifier: Modifier) {
    Row(
        modifier.background(Color(0xFFFFF1CC), RoundedCornerShape(20.dp))
            .padding(horizontal = 15.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PinyinText("🏷 商品价格", fontSize = 13.sp, color = AdventureInk)
            PinyinText("¥${q.price}", fontSize = if (q.price >= 10000) 24.sp else 30.sp,
                fontWeight = FontWeight.Black, color = AdventureInk, maxLines = 1)
        }
        Box(Modifier.width(1.dp).height(48.dp).background(Color(0xFFD8C89F)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PinyinText(if (q.role == ShopRole.CASHIER) "💵 顾客付款" else "🧺 你的任务",
                fontSize = 13.sp, color = AdventureInk)
            if (q.role == ShopRole.CASHIER) {
                PinyinText("¥${q.paid}", fontSize = if (q.paid >= 10000) 24.sp else 30.sp,
                    fontWeight = FontWeight.Black, color = AdventureInk, maxLines = 1)
            } else {
                PinyinText("凑齐货款", fontSize = 20.sp, fontWeight = FontWeight.Black,
                    color = AdventureInk, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CompactShopBoard(
    q: SkillQuestion, resolved: Boolean, message: String, compact: Boolean,
    last: Boolean, onCheck: (Boolean, String) -> Unit, onNext: () -> Unit,
    modifier: Modifier,
    pinAction: Boolean = false
) {
    var notes by remember(q) { mutableStateOf<List<Int>>(emptyList()) }
    val sum = notes.sum()
    val noteGroups = notes.groupingBy { it }.eachCount().toSortedMap()
    val denominations = listOf(1, 5, 10, 20, 50, 100)
    val buttonHeight = if (compact) 44.dp else 52.dp
    Column(
        modifier.background(AdventureCard, RoundedCornerShape(20.dp))
            .padding(if (compact) 10.dp else 13.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp)
    ) {
        Column(
            Modifier.then(if (pinAction)
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                else Modifier),
            verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp)
        ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PinyinText("💴 选择纸币", color = AdventureInk, fontWeight = FontWeight.Black,
                fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            PinyinText("已选 ¥$sum", color = AdventureInk, fontWeight = FontWeight.Black,
                fontSize = 19.sp, maxLines = 1)
        }
        // Reserving the feedback line avoids any layout jump after an answer.
        Box(Modifier.fillMaxWidth().heightIn(min=if(LocalPinyinEnabled.current) 68.dp else 36.dp),
            contentAlignment = Alignment.CenterStart) {
            val hint = if (message.isNotBlank()) message else
                if (q.role == ShopRole.CASHIER) "收到的钱减去商品价格，找出零钱。" else "选择纸币，刚好付清商品价格。"
            PinyinText(hint, fontSize = 13.sp,
                lineHeight = if(LocalPinyinEnabled.current) 25.sp else 17.sp,
                color = AdventureInk,
                maxLines = if(LocalPinyinEnabled.current) 3 else 2,
                overflow = TextOverflow.Ellipsis)
        }
        denominations.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { amount ->
                    Button(
                        onClick = {
                            if (sum <= Int.MAX_VALUE - amount) notes = notes + amount
                        },
                        enabled = !resolved,
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD5EEE3), contentColor = AdventureInk)
                    ) {
                        PinyinText("¥$amount", fontWeight = FontWeight.Black,
                            fontSize = if (compact) 18.sp else 20.sp)
                    }
                }
            }
        }
        PinyinText(
            if (notes.isEmpty()) "已选纸币：暂无" else
                "已选纸币：" + noteGroups.entries.joinToString("  ") { "¥${it.key}×${it.value}" },
            color = AdventureInk, fontSize = 12.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Button(onClick = { notes = notes.dropLast(1) },
                enabled = notes.isNotEmpty() && !resolved,
                modifier = Modifier.weight(.85f).height(if(LocalPinyinEnabled.current) 82.dp else 48.dp),
                contentPadding = PaddingValues(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2EAF5),
                    contentColor = AdventureInk),
                shape = RoundedCornerShape(13.dp)) {
                PinyinText("↶ 撤回", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    maxLines = if(LocalPinyinEnabled.current) 2 else 1)
            }
            Button(onClick = { notes = emptyList() },
                enabled = notes.isNotEmpty() && !resolved,
                modifier = Modifier.weight(.75f).height(if(LocalPinyinEnabled.current) 82.dp else 48.dp),
                contentPadding = PaddingValues(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2EAF5),
                    contentColor = AdventureInk),
                shape = RoundedCornerShape(13.dp)) {
                PinyinText("清空", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    maxLines = if(LocalPinyinEnabled.current) 2 else 1)
            }
            Button(
                onClick = {
                    if (resolved) onNext()
                    else {
                        val hint = if (sum > q.answer) "选的钱多啦，撤回一张再试试。"
                        else if (q.role == ShopRole.CASHIER)
                            "还差一些零钱，想想收到的钱减去商品价格。"
                        else "还不够呢，再选一些纸币。"
                        onCheck(sum == q.answer, hint)
                    }
                },
                modifier = Modifier.weight(1.55f).height(if(LocalPinyinEnabled.current) 82.dp else 48.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdventureOrange,
                    contentColor = AdventureInk),
                shape = RoundedCornerShape(13.dp)
            ) {
                PinyinText(if (resolved) {
                    if (last) "🏆 看报告" else "➡ 下一题"
                } else if (q.role == ShopRole.CASHIER) "✅ 确认找零" else "✅ 确认付款",
                    fontSize = 14.sp, fontWeight = FontWeight.Black,
                    maxLines = if(LocalPinyinEnabled.current) 2 else 1)
            }
        }
    }
}
