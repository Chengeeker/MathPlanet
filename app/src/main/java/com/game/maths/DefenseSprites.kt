package com.game.maths

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun PlantPortrait(type: DefensePlant, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = type.title }) {
        drawPlantGlyph(type, Offset(size.width / 2, size.height / 2), min(size.width, size.height) * .42f)
    }
}

@Composable
internal fun ZombiePortrait(type: ZombieKind, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = type.title }) {
        drawZombieGlyph(type, Offset(size.width / 2, size.height / 2), min(size.width, size.height) * .42f)
    }
}

private fun DrawScope.drawPlantEyes(center: Offset, r: Float, color: Color = Color(0xFF263149)) {
    drawCircle(Color.White, r * .12f, Offset(center.x - r * .2f, center.y - r * .08f))
    drawCircle(Color.White, r * .12f, Offset(center.x + r * .2f, center.y - r * .08f))
    drawCircle(color, r * .06f, Offset(center.x - r * .18f, center.y - r * .06f))
    drawCircle(color, r * .06f, Offset(center.x + r * .22f, center.y - r * .06f))
}

private fun DrawScope.drawPlantStem(center: Offset, r: Float, leaf: Color = Color(0xFF3D9A70)) {
    drawLine(leaf, Offset(center.x, center.y + r * .2f), Offset(center.x, center.y + r * .92f), r * .16f)
    drawOval(leaf, Offset(center.x - r * .95f, center.y + r * .3f), Size(r * .78f, r * .35f))
    drawOval(leaf, Offset(center.x + r * .16f, center.y + r * .36f), Size(r * .78f, r * .34f))
}

private fun DrawScope.drawPlantFlower(center: Offset, r: Float, petals: Int, petal: Color, core: Color) {
    repeat(petals) { index ->
        val angle = index * (Math.PI * 2 / petals).toFloat()
        drawOval(petal, Offset(center.x + cos(angle) * r * .43f - r * .19f,
            center.y + sin(angle) * r * .43f - r * .19f), Size(r * .38f, r * .38f))
    }
    drawCircle(core, r * .32f, center)
}

internal fun DrawScope.drawPlantGlyph(type: DefensePlant, center: Offset, r: Float, biteProgress: Float = 0f) {
    val leaf = Color(0xFF3D9A70)
    when (type) {
        DefensePlant.SUN -> {
            drawPlantStem(center, r)
            drawPlantFlower(Offset(center.x, center.y - r * .12f), r * .95f, 12, Color(0xFFFFA92E), Color(0xFF7D4B2B))
            drawPlantEyes(Offset(center.x, center.y - r * .12f), r * .44f)
        }
        DefensePlant.PEA -> {
            drawPlantStem(center, r)
            drawOval(Color(0xFF83D35C), Offset(center.x - r * .65f, center.y - r * .62f), Size(r * .98f, r * 1.08f))
            drawCircle(Color(0xFF9AE873), r * .34f, Offset(center.x + r * .34f, center.y - r * .32f))
            drawOval(Color(0xFF4F9E46), Offset(center.x + r * .25f, center.y - r * .18f), Size(r * .8f, r * .36f))
            drawPlantEyes(Offset(center.x - r * .08f, center.y - r * .4f), r * .4f)
        }
        DefensePlant.CHERRY -> {
            drawLine(leaf, Offset(center.x, center.y - r * .25f), Offset(center.x - r * .12f, center.y - r), r * .12f)
            drawOval(leaf, Offset(center.x - r * .1f, center.y - r * .94f), Size(r * .62f, r * .3f))
            drawCircle(Color(0xFFE4484E), r * .48f, Offset(center.x - r * .38f, center.y + r * .05f))
            drawCircle(Color(0xFFFF8053), r * .48f, Offset(center.x + r * .38f, center.y + r * .05f))
            drawPlantEyes(Offset(center.x - r * .38f, center.y + r * .05f), r * .35f)
            drawPlantEyes(Offset(center.x + r * .38f, center.y + r * .05f), r * .35f)
        }
        DefensePlant.WALL -> {
            drawRoundRect(Color(0xFFB98254), Offset(center.x - r * .68f, center.y - r * .72f), Size(r * 1.36f, r * 1.55f),
                androidx.compose.ui.geometry.CornerRadius(r * .46f))
            drawLine(Color(0xFF75513F), Offset(center.x - r * .3f, center.y - r * .45f), Offset(center.x - r * .42f, center.y + r * .12f), r * .07f)
            drawLine(Color(0xFF75513F), Offset(center.x + r * .2f, center.y - r * .15f), Offset(center.x + r * .39f, center.y + r * .58f), r * .07f)
            drawPlantEyes(Offset(center.x, center.y - r * .2f), r * .48f)
        }
        DefensePlant.MINE -> {
            val mound = Path().apply {
                moveTo(center.x - r, center.y + r * .55f); lineTo(center.x - r * .68f, center.y - r * .08f)
                lineTo(center.x - r * .28f, center.y + r * .18f); lineTo(center.x, center.y - r * .62f)
                lineTo(center.x + r * .3f, center.y + r * .08f); lineTo(center.x + r * .75f, center.y - r * .12f)
                lineTo(center.x + r, center.y + r * .55f); close()
            }
            drawPath(mound, Color(0xFF9C6556))
            drawCircle(Color(0xFFFFE08A), r * .18f, Offset(center.x, center.y - r * .2f))
            drawArc(Color(0xFFFFC65A), -90f, 250f, false, Offset(center.x - r * .72f, center.y - r * .72f),
                Size(r * 1.44f, r * 1.44f), style = Stroke(r * .12f))
        }
        DefensePlant.SNOW -> {
            drawPlantStem(center, r, Color(0xFF3D9A70))
            drawCircle(Color(0xFF72CBE8), r * .7f, Offset(center.x - r * .12f, center.y - r * .18f))
            drawOval(Color(0xFFB9F2FF), Offset(center.x + r * .2f, center.y - r * .22f), Size(r * .66f, r * .32f))
            drawPlantEyes(Offset(center.x - r * .22f, center.y - r * .22f), r * .36f)
            for (i in 0..2) {
                val a = i * (Math.PI / 3).toFloat()
                val b = a + Math.PI.toFloat()
                drawLine(Color.White, Offset(center.x + cos(a) * r * .82f, center.y - r * .38f + sin(a) * r * .18f),
                    Offset(center.x + cos(b) * r * .82f, center.y - r * .38f + sin(b) * r * .18f), r * .06f)
            }
        }
        DefensePlant.CHOMPER -> {
            drawPlantStem(center, r)
            drawOval(Color(0xFF8363B6), Offset(center.x - r * .68f, center.y - r * .75f), Size(r * 1.4f, r * 1.2f))
            val phase = biteProgress.coerceIn(0f, 1f)
            val mouth = if (phase < .25f) phase * 4f else if (phase < .7f) 1f else (1f - phase) / .3f
            if (mouth <= .02f) {
                drawArc(Color(0xFF422D64), 5f, 170f, false, Offset(center.x - r * .38f, center.y - r * .02f),
                    Size(r * .76f, r * .38f), style = androidx.compose.ui.graphics.drawscope.Stroke(r * .1f))
            } else {
                val opening = r * (.15f + .48f * mouth)
                drawOval(Color(0xFF422D64), Offset(center.x - r * .52f, center.y - r * .04f), Size(r * 1.04f, opening))
                drawOval(Color(0xFFE8738D), Offset(center.x - r * .2f, center.y + opening * .42f), Size(r * .4f, opening * .28f))
                for (i in 0..3) {
                    val x = center.x - r * .39f + i * r * .26f
                    drawLine(Color.White, Offset(x, center.y + opening * .04f), Offset(x + r * .05f, center.y + opening * .2f), r * .07f)
                }
            }
        }
        DefensePlant.DOUBLE -> {
            drawPlantStem(center, r)
            for (x in listOf(-.36f, .38f)) {
                val c = Offset(center.x + r * x, center.y - r * .2f)
                drawCircle(Color(0xFF76C957), r * .42f, c)
                drawOval(Color(0xFF4F9E46), Offset(c.x + r * .2f, c.y - r * .1f), Size(r * .56f, r * .24f))
                drawPlantEyes(c, r * .29f)
            }
        }
        DefensePlant.PUFF -> {
            drawRoundRect(Color(0xFFF2D7B9), Offset(center.x - r * .2f, center.y - r * .06f), Size(r * .4f, r * .8f),
                androidx.compose.ui.geometry.CornerRadius(r * .16f))
            drawOval(Color(0xFFB882D5), Offset(center.x - r * .72f, center.y - r * .68f), Size(r * 1.44f, r * .76f))
            drawCircle(Color(0xFFFFE7A8), r * .11f, Offset(center.x - r * .35f, center.y - r * .34f))
            drawCircle(Color(0xFFFFE7A8), r * .11f, Offset(center.x + r * .32f, center.y - r * .34f))
        }
        DefensePlant.SUNSHROOM -> {
            drawRoundRect(Color(0xFFFFE5C2), Offset(center.x - r * .25f, center.y - r * .08f), Size(r * .5f, r * .95f),
                androidx.compose.ui.geometry.CornerRadius(r * .2f))
            drawOval(Color(0xFFFFC65A), Offset(center.x - r * .78f, center.y - r * .74f), Size(r * 1.56f, r * .78f))
            drawCircle(Color(0xFFFFF4C2), r * .19f, Offset(center.x - r * .32f, center.y - r * .35f))
            drawCircle(Color(0xFFFFF4C2), r * .19f, Offset(center.x + r * .32f, center.y - r * .35f))
            drawArc(Color(0xFFFFF2B0), 205f, 130f, false, Offset(center.x - r * .4f, center.y - r * .12f), Size(r * .8f, r * .48f), style = Stroke(r * .08f))
        }
        DefensePlant.FUME -> {
            drawRoundRect(Color(0xFFEEDBCF), Offset(center.x - r * .2f, center.y - r * .08f), Size(r * .4f, r * .9f),
                androidx.compose.ui.geometry.CornerRadius(r * .15f))
            drawOval(Color(0xFF9477C8), Offset(center.x - r * .75f, center.y - r * .62f), Size(r * 1.5f, r * .7f))
            drawArc(Color(0xFFD7C9F4), 195f, 150f, false, Offset(center.x - r * .9f, center.y - r * .15f), Size(r * 1.8f, r * .7f), style = Stroke(r * .13f))
            drawCircle(Color(0xFFC7B5E8), r * .16f, Offset(center.x + r * .78f, center.y - r * .56f))
            drawCircle(Color(0xFFC7B5E8), r * .12f, Offset(center.x + r * .98f, center.y - r * .8f))
        }
        DefensePlant.GRAVE -> {
            drawRoundRect(Color(0xFF687486), Offset(center.x - r * .65f, center.y - r * .88f), Size(r * 1.3f, r * 1.8f),
                androidx.compose.ui.geometry.CornerRadius(r * .6f))
            drawRoundRect(Color(0xFF8993A1), Offset(center.x - r * .43f, center.y - r * .63f), Size(r * .86f, r * 1.4f),
                androidx.compose.ui.geometry.CornerRadius(r * .42f))
            drawLine(Color(0xFF586477), Offset(center.x - r * .3f, center.y - r * .1f), Offset(center.x + r * .3f, center.y + r * .45f), r * .1f)
        }
        DefensePlant.HYPNO -> {
            drawCircle(Color(0xFF9A68B8), r * .72f, center)
            drawArc(Color(0xFFFFDBF6), 15f, 300f, false, Offset(center.x - r * .56f, center.y - r * .56f), Size(r * 1.12f, r * 1.12f), style = Stroke(r * .18f))
            drawArc(Color(0xFF512E72), 190f, 285f, false, Offset(center.x - r * .34f, center.y - r * .34f), Size(r * .68f, r * .68f), style = Stroke(r * .13f))
            drawCircle(Color.White, r * .1f, center)
        }
        DefensePlant.SCAREDY -> {
            drawPlantStem(center, r, Color(0xFF4C9A63))
            val head = Path().apply {
                moveTo(center.x, center.y - r * .86f); cubicTo(center.x + r * .68f, center.y - r * .55f, center.x + r * .6f, center.y + r * .1f, center.x, center.y + r * .3f)
                cubicTo(center.x - r * .62f, center.y + r * .08f, center.x - r * .6f, center.y - r * .55f, center.x, center.y - r * .86f); close()
            }
            drawPath(head, Color(0xFF9EDB69))
            drawPlantEyes(Offset(center.x, center.y - r * .35f), r * .35f, Color(0xFF713F64))
        }
        DefensePlant.ICE -> {
            val crystal = Path().apply {
                moveTo(center.x, center.y - r); lineTo(center.x + r * .7f, center.y - r * .22f); lineTo(center.x + r * .42f, center.y + r * .85f)
                lineTo(center.x - r * .4f, center.y + r * .85f); lineTo(center.x - r * .7f, center.y - r * .22f); close()
            }
            drawPath(crystal, Color(0xFF9EEBFA))
            drawLine(Color.White, Offset(center.x, center.y - r * .7f), Offset(center.x, center.y + r * .65f), r * .08f)
            drawLine(Color(0xFF4DACC8), Offset(center.x - r * .55f, center.y - r * .18f), Offset(center.x + r * .55f, center.y + r * .45f), r * .07f)
        }
        DefensePlant.DOOM -> {
            drawCircle(Color(0xFF372B50), r * .78f, center)
            drawCircle(Color(0xFF171327), r * .44f, center)
            drawArc(Color(0xFFB99BE8), 205f, 300f, false, Offset(center.x - r * .9f, center.y - r * .9f), Size(r * 1.8f, r * 1.8f), style = Stroke(r * .12f))
            drawCircle(Color(0xFFFFD98C), r * .11f, Offset(center.x - r * .18f, center.y - r * .04f))
        }
        DefensePlant.LILYPAD -> {
            drawOval(Color(0xFF3D9A70), Offset(center.x - r, center.y - r * .18f), Size(r * 2f, r * 1.1f))
            val notch = Path().apply { moveTo(center.x, center.y + r * .3f); lineTo(center.x + r * .38f, center.y - r * .16f); lineTo(center.x + r * .45f, center.y + r * .55f); close() }
            drawPath(notch, Color(0xFF17223F))
            drawLine(Color(0xFFB0E3A5), Offset(center.x - r * .75f, center.y + r * .2f), Offset(center.x, center.y + r * .4f), r * .08f)
        }
        DefensePlant.SQUASH -> {
            drawOval(Color(0xFF8CB952), Offset(center.x - r * .76f, center.y - r * .68f), Size(r * 1.52f, r * 1.35f))
            drawOval(Color(0xFFB7D16A), Offset(center.x - r * .5f, center.y - r * .55f), Size(r * .3f, r * .8f))
            drawPlantEyes(Offset(center.x, center.y - r * .25f), r * .4f)
            drawArc(Color(0xFF30402E), 10f, 160f, false, Offset(center.x - r * .42f, center.y + r * .05f), Size(r * .84f, r * .4f), style = Stroke(r * .1f))
        }
        DefensePlant.THREEPEATER -> {
            drawPlantStem(center, r)
            for ((x, y) in listOf(-.42f to -.1f, 0f to -.42f, .42f to -.1f)) {
                val c = Offset(center.x + r * x, center.y + r * y)
                drawCircle(Color(0xFF70C756), r * .34f, c)
                drawRoundRect(Color(0xFF4B9145), Offset(c.x + r * .18f, c.y - r * .1f), Size(r * .42f, r * .2f), androidx.compose.ui.geometry.CornerRadius(r * .08f))
            }
        }
        DefensePlant.TANGLE -> {
            drawOval(Color(0xFF43A98C), Offset(center.x - r * .43f, center.y + r * .1f), Size(r * .86f, r * .8f))
            for (tendril in listOf(floatArrayOf(-.25f, 0f, -.58f, -.72f), floatArrayOf(0f, .18f, .58f, -.68f), floatArrayOf(.2f, .45f, -.45f, .78f))) {
                drawLine(Color(0xFF287F74), Offset(center.x + r * tendril[0], center.y + r * tendril[1]),
                    Offset(center.x + r * tendril[2], center.y + r * tendril[3]), r * .18f)
            }
            drawCircle(Color(0xFFB5E6CF), r * .16f, Offset(center.x, center.y + r * .35f))
        }
        DefensePlant.JALAPENO -> {
            val pepper = Path().apply {
                moveTo(center.x - r * .2f, center.y - r * .72f); cubicTo(center.x + r * .58f, center.y - r * .64f, center.x + r * .58f, center.y + r * .55f, center.x, center.y + r * .86f)
                cubicTo(center.x - r * .55f, center.y + r * .42f, center.x - r * .52f, center.y - r * .35f, center.x - r * .2f, center.y - r * .72f); close()
            }
            drawPath(pepper, Color(0xFFE85A46))
            drawLine(leaf, Offset(center.x - r * .18f, center.y - r * .66f), Offset(center.x + r * .22f, center.y - r), r * .13f)
            drawLine(Color(0xFFFFC85A), Offset(center.x + r * .35f, center.y - r * .28f), Offset(center.x + r * .68f, center.y - r * .48f), r * .08f)
        }
        DefensePlant.SPIKEWEED -> {
            drawOval(Color(0xFF92784D), Offset(center.x - r * .92f, center.y + r * .25f), Size(r * 1.84f, r * .48f))
            for (i in 0..6) {
                val x = center.x - r * .82f + i * r * .27f
                drawLine(Color(0xFFDAAA50), Offset(x, center.y + r * .38f), Offset(x + (i % 2) * r * .1f, center.y - r * (.38f + (i % 3) * .16f)), r * .13f)
            }
        }
        DefensePlant.TORCHWOOD -> {
            drawRoundRect(Color(0xFF88583C), Offset(center.x - r * .58f, center.y - r * .45f), Size(r * 1.16f, r * 1.38f), androidx.compose.ui.geometry.CornerRadius(r * .18f))
            drawCircle(Color(0xFF573A32), r * .34f, Offset(center.x, center.y + r * .55f))
            drawOval(Color(0xFFFF8A38), Offset(center.x - r * .34f, center.y - r * 1.12f), Size(r * .68f, r * .88f))
            drawOval(Color(0xFFFFD36C), Offset(center.x - r * .14f, center.y - r * .92f), Size(r * .28f, r * .56f))
        }
        DefensePlant.TALLNUT -> {
            drawRoundRect(Color(0xFF9B7554), Offset(center.x - r * .6f, center.y - r * .98f), Size(r * 1.2f, r * 2f), androidx.compose.ui.geometry.CornerRadius(r * .5f))
            drawCircle(Color(0xFFC7A778), r * .1f, Offset(center.x - r * .28f, center.y + r * .48f))
            drawCircle(Color(0xFFC7A778), r * .1f, Offset(center.x + r * .29f, center.y + r * .22f))
            drawPlantEyes(Offset(center.x, center.y - r * .5f), r * .4f)
        }
        DefensePlant.SEASHROOM -> {
            drawRoundRect(Color(0xFFBFEAE6), Offset(center.x - r * .2f, center.y - r * .05f), Size(r * .4f, r * .85f), androidx.compose.ui.geometry.CornerRadius(r * .15f))
            drawOval(Color(0xFF54BFC0), Offset(center.x - r * .68f, center.y - r * .68f), Size(r * 1.36f, r * .67f))
            drawCircle(Color(0xFFA5F5E8), r * .13f, Offset(center.x - r * .4f, center.y - r * .38f))
            drawCircle(Color(0xFFA5F5E8), r * .13f, Offset(center.x + r * .32f, center.y - r * .38f))
            drawArc(Color(0xFF278E9A), 15f, 150f, false, Offset(center.x - r * .34f, center.y - r * .08f), Size(r * .68f, r * .32f), style = Stroke(r * .07f))
        }
        DefensePlant.PLANTERN -> {
            drawLine(Color(0xFF815C3C), Offset(center.x, center.y - r), Offset(center.x, center.y - r * .65f), r * .12f)
            drawRoundRect(Color(0xFF8C704C), Offset(center.x - r * .62f, center.y - r * .48f), Size(r * 1.24f, r * 1.18f), androidx.compose.ui.geometry.CornerRadius(r * .14f))
            drawRoundRect(Color(0xFFFFE58A), Offset(center.x - r * .34f, center.y - r * .22f), Size(r * .68f, r * .62f), androidx.compose.ui.geometry.CornerRadius(r * .08f))
            drawLine(Color(0xFFFFE58A), Offset(center.x - r, center.y - r * .55f), Offset(center.x - r * .75f, center.y - r * .55f), r * .07f)
            drawLine(Color(0xFFFFE58A), Offset(center.x + r * .75f, center.y - r * .55f), Offset(center.x + r, center.y - r * .55f), r * .07f)
        }
        DefensePlant.CACTUS -> {
            drawRoundRect(Color(0xFF48A777), Offset(center.x - r * .32f, center.y - r * .72f), Size(r * .64f, r * 1.55f), androidx.compose.ui.geometry.CornerRadius(r * .26f))
            drawRoundRect(Color(0xFF48A777), Offset(center.x - r * .78f, center.y - r * .2f), Size(r * .5f, r * .32f), androidx.compose.ui.geometry.CornerRadius(r * .16f))
            drawRoundRect(Color(0xFF48A777), Offset(center.x + r * .28f, center.y - r * .47f), Size(r * .5f, r * .32f), androidx.compose.ui.geometry.CornerRadius(r * .16f))
            for (y in listOf(-.48f, -.1f, .28f)) drawLine(Color(0xFFFFE9B2), Offset(center.x - r * .1f, center.y + r * y), Offset(center.x + r * .1f, center.y + r * y), r * .07f)
        }
        DefensePlant.BLOVER -> {
            drawLine(Color(0xFF3D9A70), Offset(center.x, center.y + r * .82f), Offset(center.x, center.y - r * .18f), r * .12f)
            drawOval(Color(0xFF69C67D), Offset(center.x - r * .7f, center.y - r * .9f), Size(r * .64f, r * .76f))
            drawOval(Color(0xFF8FDC89), Offset(center.x - r * .3f, center.y - r * 1.05f), Size(r * .64f, r * .76f))
            drawOval(Color(0xFF4EAA6D), Offset(center.x + r * .12f, center.y - r * .9f), Size(r * .64f, r * .76f))
            drawCircle(Color(0xFFFFE18A), r * .14f, Offset(center.x, center.y - r * .42f))
        }
        DefensePlant.SPLITPEA -> {
            drawPlantStem(center, r)
            drawCircle(Color(0xFF6FBF58), r * .62f, Offset(center.x, center.y - r * .18f))
            drawRoundRect(Color(0xFF488A47), Offset(center.x - r * 1.05f, center.y - r * .42f), Size(r * .68f, r * .23f), androidx.compose.ui.geometry.CornerRadius(r * .1f))
            drawRoundRect(Color(0xFF488A47), Offset(center.x + r * .37f, center.y - r * .12f), Size(r * .68f, r * .23f), androidx.compose.ui.geometry.CornerRadius(r * .1f))
            drawPlantEyes(Offset(center.x, center.y - r * .35f), r * .36f)
        }
        DefensePlant.STARFRUIT -> {
            val p = Path()
            for (i in 0 until 10) {
                val angle = -Math.PI / 2 + i * Math.PI / 5
                val radius = if (i % 2 == 0) r * .92f else r * .43f
                val x = center.x + cos(angle).toFloat() * radius
                val y = center.y + sin(angle).toFloat() * radius
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.close(); drawPath(p, Color(0xFFFFC846))
            drawCircle(Color(0xFFFFF2B0), r * .2f, center)
        }
        DefensePlant.PUMPKIN -> {
            drawCircle(Color(0xFFE98235), r * .78f, center)
            drawOval(Color(0xFFFFA744), Offset(center.x - r * .72f, center.y - r * .62f), Size(r * .58f, r * 1.2f))
            drawOval(Color(0xFFDA6E2D), Offset(center.x + r * .17f, center.y - r * .62f), Size(r * .58f, r * 1.2f))
            drawLine(leaf, Offset(center.x, center.y - r * .72f), Offset(center.x + r * .1f, center.y - r), r * .16f)
            drawPlantEyes(Offset(center.x, center.y - r * .1f), r * .38f)
        }
        DefensePlant.MAGNET -> {
            drawRoundRect(Color(0xFFE17B7E), Offset(center.x - r * .7f, center.y - r * .78f), Size(r * .28f, r * 1.35f), androidx.compose.ui.geometry.CornerRadius(r * .1f))
            drawRoundRect(Color(0xFFE17B7E), Offset(center.x + r * .42f, center.y - r * .78f), Size(r * .28f, r * 1.35f), androidx.compose.ui.geometry.CornerRadius(r * .1f))
            drawArc(Color(0xFFE17B7E), 180f, 180f, false, Offset(center.x - r * .7f, center.y - r * .84f), Size(r * 1.4f, r * 1.2f), style = Stroke(r * .28f))
            drawCircle(Color(0xFFB4D9E5), r * .14f, Offset(center.x - r * .48f, center.y - r * .55f))
            drawCircle(Color(0xFFB4D9E5), r * .14f, Offset(center.x + r * .55f, center.y - r * .55f))
        }
        DefensePlant.CABBAGE -> {
            drawPlantStem(center, r)
            drawCircle(Color(0xFF71B84C), r * .64f, center)
            drawOval(Color(0xFF9DD968), Offset(center.x - r * .62f, center.y - r * .62f), Size(r * .78f, r * .85f))
            drawOval(Color(0xFF5E9D45), Offset(center.x + r * .1f, center.y - r * .58f), Size(r * .66f, r * .82f))
            drawPlantEyes(Offset(center.x, center.y - r * .18f), r * .36f)
            drawLine(Color(0xFFFFF2C2), Offset(center.x + r * .58f, center.y - r * .42f), Offset(center.x + r, center.y - r * .64f), r * .09f)
        }
        DefensePlant.FLOWERPOT -> {
            drawOval(Color(0xFF5F8F54), Offset(center.x - r * .62f, center.y - r * .82f), Size(r * 1.24f, r * .55f))
            drawCircle(Color(0xFFB5D47A), r * .34f, Offset(center.x, center.y - r * .58f))
            val pot = Path().apply { moveTo(center.x - r * .72f, center.y - r * .32f); lineTo(center.x + r * .72f, center.y - r * .32f); lineTo(center.x + r * .5f, center.y + r * .82f); lineTo(center.x - r * .5f, center.y + r * .82f); close() }
            drawPath(pot, Color(0xFFB66D50))
            drawLine(Color(0xFFE5A078), Offset(center.x - r * .66f, center.y - r * .2f), Offset(center.x + r * .66f, center.y - r * .2f), r * .14f)
        }
        DefensePlant.KERNEL -> {
            drawRoundRect(Color(0xFFE5B83E), Offset(center.x - r * .48f, center.y - r * .8f), Size(r * .96f, r * 1.55f), androidx.compose.ui.geometry.CornerRadius(r * .4f))
            for (row in 0..3) for (col in 0..2) drawCircle(Color(if ((row + col) % 2 == 0) 0xFFFFE17A else 0xFFC8912D), r * .1f,
                Offset(center.x - r * .25f + col * r * .25f, center.y - r * .48f + row * r * .3f))
            drawOval(Color(0xFF4A9B62), Offset(center.x - r * .83f, center.y + r * .25f), Size(r * .52f, r * .68f))
            drawOval(Color(0xFF398752), Offset(center.x + r * .3f, center.y + r * .24f), Size(r * .52f, r * .68f))
        }
        DefensePlant.COFFEE -> {
            drawRoundRect(Color(0xFFC28B5B), Offset(center.x - r * .52f, center.y - r * .42f), Size(r * 1.04f, r * 1.18f), androidx.compose.ui.geometry.CornerRadius(r * .18f))
            drawOval(Color(0xFF805239), Offset(center.x - r * .65f, center.y - r * .55f), Size(r * 1.3f, r * .28f))
            drawArc(Color(0xFF805239), 270f, 180f, false, Offset(center.x + r * .35f, center.y - r * .1f), Size(r * .62f, r * .62f), style = Stroke(r * .12f))
            drawCircle(Color(0xFF49352F), r * .16f, Offset(center.x - r * .18f, center.y + r * .06f))
            drawCircle(Color(0xFF49352F), r * .13f, Offset(center.x + r * .18f, center.y + r * .34f))
            drawArc(Color(0xFFFFE2A2), 200f, 140f, false, Offset(center.x - r * .3f, center.y - r * 1.08f), Size(r * .6f, r * .8f), style = Stroke(r * .09f))
        }
        DefensePlant.GARLIC -> {
            val bulb = Path().apply {
                moveTo(center.x, center.y - r * .78f); cubicTo(center.x + r * .3f, center.y - r * .45f, center.x + r * .78f, center.y - r * .45f, center.x + r * .64f, center.y + r * .22f)
                cubicTo(center.x + r * .54f, center.y + r * .82f, center.x - r * .54f, center.y + r * .82f, center.x - r * .64f, center.y + r * .22f)
                cubicTo(center.x - r * .78f, center.y - r * .45f, center.x - r * .3f, center.y - r * .45f, center.x, center.y - r * .78f); close()
            }
            drawPath(bulb, Color(0xFFF0E5CE))
            drawLine(Color(0xFFD4C4A7), Offset(center.x, center.y - r * .38f), Offset(center.x - r * .12f, center.y + r * .56f), r * .07f)
            drawLine(Color(0xFFD4C4A7), Offset(center.x - r * .08f, center.y - r * .3f), Offset(center.x - r * .4f, center.y + r * .42f), r * .06f)
            drawPlantEyes(Offset(center.x, center.y - r * .15f), r * .32f)
        }
        DefensePlant.UMBRELLA -> {
            drawArc(Color(0xFFE87592), 180f, 180f, true, Offset(center.x - r, center.y - r * .82f), Size(r * 2f, r * 1.38f))
            drawLine(Color(0xFF5E6176), Offset(center.x, center.y - r * .14f), Offset(center.x, center.y + r * .85f), r * .1f)
            drawLine(Color.White, Offset(center.x - r * .65f, center.y - r * .18f), Offset(center.x - r * .32f, center.y - r * .18f), r * .06f)
            drawLine(Color.White, Offset(center.x + r * .32f, center.y - r * .18f), Offset(center.x + r * .65f, center.y - r * .18f), r * .06f)
        }
        DefensePlant.MARIGOLD -> {
            drawPlantStem(center, r)
            drawPlantFlower(Offset(center.x, center.y - r * .12f), r * .88f, 16, Color(0xFFFF9A31), Color(0xFFFFD55A))
            drawCircle(Color(0xFF8B532B), r * .13f, Offset(center.x, center.y - r * .12f))
        }
        DefensePlant.MELON -> {
            drawPlantStem(center, r)
            drawCircle(Color(0xFF4E9E4F), r * .72f, Offset(center.x, center.y - r * .2f))
            for (x in listOf(-.4f, 0f, .4f)) drawArc(Color(0xFFB7D77A), 80f, 200f, false,
                Offset(center.x + r * x - r * .16f, center.y - r * .72f), Size(r * .32f, r * 1.05f), style = Stroke(r * .08f))
            drawPlantEyes(Offset(center.x - r * .12f, center.y - r * .32f), r * .36f)
            drawRoundRect(Color(0xFF426F3D), Offset(center.x + r * .25f, center.y - r * .15f), Size(r * .8f, r * .22f), androidx.compose.ui.geometry.CornerRadius(r * .09f))
        }
        DefensePlant.GATLING -> {
            drawPlantStem(center, r)
            drawCircle(Color(0xFF3F9F59), r * .7f, Offset(center.x - r * .05f, center.y - r * .28f))
            for (i in 0..3) drawRoundRect(Color(0xFF347947), Offset(center.x + r * .25f, center.y - r * .62f + i * r * .25f), Size(r * .82f, r * .15f), androidx.compose.ui.geometry.CornerRadius(r * .07f))
            drawPlantEyes(Offset(center.x - r * .22f, center.y - r * .42f), r * .3f)
        }
        DefensePlant.TWINSUN -> {
            drawPlantStem(center, r)
            drawPlantFlower(Offset(center.x - r * .42f, center.y - r * .18f), r * .6f, 8, Color(0xFFFFA834), Color(0xFFFFE77B))
            drawPlantFlower(Offset(center.x + r * .43f, center.y - r * .18f), r * .6f, 8, Color(0xFFFFCE45), Color(0xFFFFF4B0))
        }
        DefensePlant.GLOOM -> {
            drawPlantStem(center, r)
            drawCircle(Color(0xFF665092), r * .78f, Offset(center.x, center.y - r * .12f))
            drawCircle(Color(0xFF332A58), r * .46f, Offset(center.x, center.y - r * .12f))
            for (i in 0..7) {
                val a = i * (Math.PI / 4).toFloat()
                drawCircle(Color(0xFFB99BE8), r * .14f, Offset(center.x + cos(a) * r * .62f, center.y - r * .12f + sin(a) * r * .62f))
            }
        }
        DefensePlant.CATTAIL -> {
            drawPlantStem(center, r)
            drawLine(Color(0xFF559D68), Offset(center.x, center.y + r * .6f), Offset(center.x + r * .12f, center.y - r * .9f), r * .14f)
            drawOval(Color(0xFF805E45), Offset(center.x - r * .12f, center.y - r * 1.05f), Size(r * .42f, r * .78f))
            drawCircle(Color(0xFFFFF0C1), r * .07f, Offset(center.x - r * .04f, center.y - r * .72f))
        }
        DefensePlant.WINTERMELON -> {
            drawPlantStem(center, r)
            drawCircle(Color(0xFF518DB5), r * .73f, Offset(center.x, center.y - r * .18f))
            drawArc(Color(0xFFB7E7F4), 80f, 205f, false, Offset(center.x - r * .52f, center.y - r * .82f), Size(r * .4f, r * 1.2f), style = Stroke(r * .1f))
            drawArc(Color(0xFFB7E7F4), 80f, 205f, false, Offset(center.x + r * .12f, center.y - r * .82f), Size(r * .4f, r * 1.2f), style = Stroke(r * .1f))
            drawCircle(Color.White, r * .11f, Offset(center.x - r * .25f, center.y - r * .36f))
            drawCircle(Color.White, r * .11f, Offset(center.x + r * .2f, center.y - r * .36f))
        }
        DefensePlant.GOLDMAGNET -> {
            drawRoundRect(Color(0xFFFFD45C), Offset(center.x - r * .72f, center.y - r * .76f), Size(r * .3f, r * 1.38f), androidx.compose.ui.geometry.CornerRadius(r * .1f))
            drawRoundRect(Color(0xFFFFD45C), Offset(center.x + r * .42f, center.y - r * .76f), Size(r * .3f, r * 1.38f), androidx.compose.ui.geometry.CornerRadius(r * .1f))
            drawArc(Color(0xFFFFD45C), 180f, 180f, false, Offset(center.x - r * .72f, center.y - r * .86f), Size(r * 1.44f, r * 1.24f), style = Stroke(r * .28f))
            drawCircle(Color.White, r * .12f, Offset(center.x - r * .5f, center.y - r * .5f))
            drawCircle(Color.White, r * .12f, Offset(center.x + r * .57f, center.y - r * .5f))
            drawCircle(Color(0xFFFFA640), r * .11f, Offset(center.x, center.y + r * .2f))
        }
        DefensePlant.SPIKEROCK -> {
            val rock = Path().apply {
                moveTo(center.x - r, center.y + r * .5f); lineTo(center.x - r * .8f, center.y - r * .2f); lineTo(center.x - r * .35f, center.y - r * .75f)
                lineTo(center.x + r * .18f, center.y - r * .82f); lineTo(center.x + r * .72f, center.y - r * .3f); lineTo(center.x + r, center.y + r * .48f); close()
            }
            drawPath(rock, Color(0xFF777C78))
            for (x in listOf(-.62f, -.2f, .25f, .65f)) drawLine(Color(0xFFD1B36C), Offset(center.x + r * x, center.y),
                Offset(center.x + r * (x + .05f), center.y - r * .65f), r * .12f)
            drawCircle(Color(0xFFFFD978), r * .1f, Offset(center.x + r * .3f, center.y + r * .15f))
        }
        DefensePlant.COB -> {
            drawRoundRect(Color(0xFF798C4B), Offset(center.x - r * .7f, center.y - r * .45f), Size(r * 1.05f, r * .9f), androidx.compose.ui.geometry.CornerRadius(r * .22f))
            drawRoundRect(Color(0xFFB78244), Offset(center.x + r * .1f, center.y - r * .32f), Size(r * .85f, r * .64f), androidx.compose.ui.geometry.CornerRadius(r * .16f))
            drawRoundRect(Color(0xFF493C37), Offset(center.x + r * .7f, center.y - r * .2f), Size(r * .52f, r * .4f), androidx.compose.ui.geometry.CornerRadius(r * .12f))
            drawCircle(Color(0xFF4F443D), r * .18f, Offset(center.x - r * .3f, center.y + r * .58f))
            drawCircle(Color(0xFF4F443D), r * .18f, Offset(center.x + r * .48f, center.y + r * .58f))
            drawCircle(Color(0xFFFFE27A), r * .1f, Offset(center.x - r * .36f, center.y - r * .2f))
        }
        DefensePlant.IMITATER -> {
            drawCircle(Color(0xFFE9DCC1), r * .73f, center)
            val mask = Path().apply {
                moveTo(center.x - r * .6f, center.y - r * .2f); quadraticTo(center.x, center.y - r * .75f, center.x + r * .6f, center.y - r * .2f)
                lineTo(center.x + r * .4f, center.y + r * .42f); quadraticTo(center.x, center.y + r * .76f, center.x - r * .4f, center.y + r * .42f); close()
            }
            drawPath(mask, Color(0xFFAAA08F))
            drawPlantEyes(Offset(center.x, center.y - r * .1f), r * .3f)
            drawArc(Color.White, -70f, 220f, false, Offset(center.x - r * .55f, center.y - r * .55f), Size(r * 1.1f, r * 1.1f), style = Stroke(r * .1f))
        }
    }
}
internal fun DrawScope.drawZombieGlyph(type: ZombieKind, center: Offset, r: Float, poleConsumed: Boolean = false) {
    val body = if (type.behavior == ZombieBehavior.FAST) Color(0xFF6F7F9C) else Color(0xFF56677F)
    val face = Color(0xFFE9E5D7)
    drawOval(body, Offset(center.x - r * .72f, center.y - r * .65f), Size(r * 1.44f, r * 1.72f))
    drawOval(face, Offset(center.x - r * .48f, center.y - r * .48f), Size(r * .96f, r * .88f))
    drawCircle(Color(0xFF273149), r * .09f, Offset(center.x - r * .17f, center.y - r * .12f))
    drawCircle(Color(0xFF273149), r * .09f, Offset(center.x + r * .17f, center.y - r * .12f))
    when (type.behavior) {
        ZombieBehavior.ARMOR, ZombieBehavior.SHIELD, ZombieBehavior.SLED, ZombieBehavior.LADDER -> {
            drawRoundRect(Color(0xFFD2B76D), Offset(center.x - r * .83f, center.y - r * .72f),
                Size(r * 1.66f, r * .38f), androidx.compose.ui.geometry.CornerRadius(r * .14f))
            drawLine(Color(0xFF8D7849), Offset(center.x - r * .55f, center.y - r * .53f),
                Offset(center.x + r * .55f, center.y - r * .53f), r * .1f)
        }
        ZombieBehavior.VAULT -> if (!poleConsumed) {
            drawLine(Color(0xFFB78FE8), Offset(center.x - r * .8f, center.y + r * .7f),
                Offset(center.x + r * .72f, center.y - r * 1.35f), r * .12f)
        }
        ZombieBehavior.POGO -> {
            drawLine(Color(0xFFB78FE8), Offset(center.x - r * .8f, center.y + r * .7f),
                Offset(center.x + r * .72f, center.y - r * 1.35f), r * .12f)
        }
        ZombieBehavior.AIRBORNE -> {
            drawCircle(Color(0xFFE88FA0), r * .55f, Offset(center.x, center.y - r * 1.15f))
            drawLine(Color.White, Offset(center.x, center.y - r * .65f), Offset(center.x, center.y - r * .25f), r * .04f)
        }
        ZombieBehavior.DIGGER -> {
            val drill = Path().apply {
                moveTo(center.x + r * .38f, center.y - r * .28f); lineTo(center.x + r * 1.35f, center.y)
                lineTo(center.x + r * .38f, center.y + r * .28f); close()
            }
            drawPath(drill, Color(0xFFBCC5CF))
        }
        ZombieBehavior.CATAPULT -> {
            drawCircle(Color(0xFF394356), r * .2f, Offset(center.x - r * .45f, center.y + r * .84f))
            drawCircle(Color(0xFF394356), r * .2f, Offset(center.x + r * .45f, center.y + r * .84f))
            drawLine(Color(0xFFD3A45D), Offset(center.x, center.y - r * .3f), Offset(center.x + r * .85f, center.y - r * 1.15f), r * .13f)
        }
        ZombieBehavior.CRUSH, ZombieBehavior.GIANT -> {
            drawRoundRect(Color(0xFF59616A), Offset(center.x - r * .92f, center.y - r * .86f),
                Size(r * 1.84f, r * .56f), androidx.compose.ui.geometry.CornerRadius(r * .2f))
            drawCircle(Color(0xFFFFB35B), r * .18f, Offset(center.x + r * .35f, center.y + r * .22f))
        }
        ZombieBehavior.EXPLODE -> drawCircle(Color(0xFFE8735E), r * .23f, Offset(center.x, center.y - r * .9f))
        ZombieBehavior.RAGE -> drawLine(Color(0xFFD86D67), Offset(center.x - r * .3f, center.y + r * .02f),
            Offset(center.x + r * .3f, center.y + r * .02f), r * .11f)
        else -> Unit
    }
    when (type) {
        ZombieKind.SIGNAL -> drawLine(Color(0xFFFFD66D), Offset(center.x - r * .55f, center.y - r * .75f),
            Offset(center.x - r * .8f, center.y - r * 1.35f), r * .1f)
        ZombieKind.DUCK -> drawOval(Color(0xFFFFD15E), Offset(center.x - r * .92f, center.y + r * .2f), Size(r * 1.84f, r * .66f),
            style = Stroke(r * .17f))
        ZombieKind.SNORKEL -> {
            drawLine(Color(0xFF5BC6D4), Offset(center.x + r * .25f, center.y - r * .55f),
                Offset(center.x + r * .8f, center.y - r * 1.18f), r * .15f)
            drawCircle(Color(0xFF5BC6D4), r * .2f, Offset(center.x + r * .8f, center.y - r * 1.18f))
        }
        ZombieKind.BOBSLED -> drawRoundRect(Color(0xFF83C8D2), Offset(center.x - r * .95f, center.y + r * .65f),
            Size(r * 1.9f, r * .34f), androidx.compose.ui.geometry.CornerRadius(r * .16f))
        ZombieKind.DOLPHIN -> {
            val fin = Path().apply { moveTo(center.x - r * .25f, center.y - r * .55f); lineTo(center.x + r * .25f, center.y - r * 1.2f); lineTo(center.x + r * .55f, center.y - r * .45f); close() }
            drawPath(fin, Color(0xFF64B8CB))
        }
        ZombieKind.BUNGEE -> {
            drawLine(Color(0xFFBECDE3), Offset(center.x, center.y - r * .55f), Offset(center.x, center.y - r * 1.35f), r * .08f)
            drawCircle(Color(0xFFE1BA64), r * .2f, Offset(center.x, center.y - r * 1.4f))
        }
        ZombieKind.LADDER -> {
            drawLine(Color(0xFFB78E5C), Offset(center.x + r * .65f, center.y - r), Offset(center.x + r * .9f, center.y + r), r * .1f)
            repeat(4) { i -> drawLine(Color(0xFFB78E5C), Offset(center.x + r * .58f, center.y - r * .65f + i * r * .4f),
                Offset(center.x + r * .87f, center.y - r * .65f + i * r * .4f), r * .08f) }
        }
        ZombieKind.IMP -> drawCircle(Color(0xFFFFD28B), r * .16f, Offset(center.x - r * .5f, center.y + r * .18f))
        ZombieKind.YETI -> drawCircle(Color(0xFF9AD6DF), r * .15f, Offset(center.x - r * .45f, center.y - r * .68f))
        ZombieKind.DRIFTER, ZombieKind.SHELL, ZombieKind.VAULT, ZombieKind.HEAVY, ZombieKind.DASHER,
        ZombieKind.NEWSPAPER, ZombieKind.SCREEN, ZombieKind.FOOTBALL, ZombieKind.DANCER, ZombieKind.BACKUP,
        ZombieKind.ZAMBONI, ZombieKind.JACK, ZombieKind.BALLOON, ZombieKind.DIGGER, ZombieKind.POGO,
        ZombieKind.CATAPULT, ZombieKind.GIANT, ZombieKind.BOSS -> Unit
    }
    if (type.armor > 0) drawArc(Color(0xFFF3C95F), 195f, 150f, false,
        Offset(center.x - r * .9f, center.y - r * .9f), Size(r * 1.8f, r * 1.8f), style = Stroke(r * .16f))
    if (type == ZombieKind.DANCER || type == ZombieKind.BOSS) {
        drawRoundRect(Color(0xFF8370AE), Offset(center.x - r * .46f, center.y + r * .24f),
            Size(r * .92f, r * .42f), androidx.compose.ui.geometry.CornerRadius(r * .1f))
    }
}
