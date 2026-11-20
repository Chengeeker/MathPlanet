package com.game.maths

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val DialogInk = Color(0xFF294369)
private val DialogMuted = Color(0xFF627897)
private val StayOrange = Color(0xFFFFB64C)
private val QuitPink = Color(0xFFFFE1DF)

/**
 * The same soft, illustrated visual language as the game's question and home cards.
 * Compose Dialog supplies accessible modal focus, backdrop dimming and back handling;
 * no default Material AlertDialog surface or action layout is used.
 */
@Composable
fun PlanetExitDialog(
    isExitApp: Boolean,
    onDismiss: () -> Unit,
    onStay: () -> Unit,
    onConfirm: () -> Unit
) {
    val availableHeight = LocalConfiguration.current.screenHeightDp.dp
    val compact = availableHeight < 530.dp
    val cardShape = RoundedCornerShape(if (compact) 26.dp else 32.dp)

    Dialog(
        // Back gesture and outside tap dismiss silently; only on-screen buttons trigger a tap.
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
                    .heightIn(max = (availableHeight - 45.dp).coerceAtLeast(230.dp))
                    .shadow(13.dp, cardShape)
                    .clip(cardShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFFF6CA),
                                Color(0xFFFFEACE),
                                Color(0xFFFFE2E1)
                            )
                        )
                    )
                    .border(2.5.dp, Color.White.copy(alpha = .94f), cardShape)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = if (compact) 19.dp else 24.dp,
                        vertical = if (compact) 15.dp else 23.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PinyinText(
                    "✨ 数学星球 ✨",
                    color = Color(0xFFB47543), fontWeight = FontWeight.Black, fontSize = 14.sp
                )
                Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
                Box(
                    Modifier.size(if (compact) 66.dp else 98.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(Color.White.copy(alpha = .7f))
                        .border(
                            2.dp, Color.White.copy(alpha = .9f), RoundedCornerShape(23.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.mascot),
                        contentDescription = "数学星球的小恐龙伙伴",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(21.dp))
                    )
                }
                Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
                PinyinText(
                    if (isExitApp) "今天先玩到这里吗？" else "小恐龙还想和你闯关！",
                    color = DialogInk,
                    fontWeight = FontWeight.Black,
                    fontSize = if (compact) 21.sp else 24.sp,
                    lineHeight = if (compact) 28.sp else 32.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
                PinyinText(
                    if (isExitApp)
                        "已经收集的星星和完成的关卡都会保留。下次再来，继续和小恐龙玩数学吧！"
                    else
                        "现在回到首页，会结束这轮练习，未完成的闯关成绩不会保存哦。",
                    color = DialogMuted,
                    fontSize = if (compact) 13.sp else 15.sp,
                    lineHeight = if (compact) 19.sp else 23.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(if (compact) 13.dp else 21.dp))
                KidMainButton(
                    "🌈 继续冒险",
                    onStay,
                    StayOrange,
                    height = if (compact) 50.dp else 57.dp
                )
                Spacer(Modifier.height(9.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(if (compact) 48.dp else 53.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuitPink)
                ) {
                    PinyinText(
                        if (isExitApp) "👋 退出应用" else "🏠 返回首页",
                        color = DialogInk,
                        fontSize = if (compact) 16.sp else 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
