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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.sqrt

private val PlantLeafShape = Path().apply {
    moveTo(0f, 0f)
    cubicTo(.24f, .56f, .72f, .45f, 1f, 0f)
    cubicTo(.72f, -.45f, .24f, -.56f, 0f, 0f)
    close()
}

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
    drawOval(Color(0xFF5D4938), Offset(center.x - r * .47f, center.y - r * .32f), Size(r * .4f, r * .48f))
    drawOval(Color(0xFF5D4938), Offset(center.x + r * .07f, center.y - r * .32f), Size(r * .4f, r * .48f))
    drawOval(Color.White, Offset(center.x - r * .44f, center.y - r * .35f), Size(r * .34f, r * .42f))
    drawOval(Color.White, Offset(center.x + r * .1f, center.y - r * .35f), Size(r * .34f, r * .42f))
    drawCircle(color, r * .09f, Offset(center.x - r * .18f, center.y - r * .13f))
    drawCircle(color, r * .09f, Offset(center.x + r * .35f, center.y - r * .13f))
    drawCircle(Color.White, r * .035f, Offset(center.x - r * .2f, center.y - r * .17f))
    drawCircle(Color.White, r * .035f, Offset(center.x + r * .33f, center.y - r * .17f))
}

private fun DrawScope.drawPlantLeaf(base: Offset, tip: Offset, width: Float, color: Color) {
    val dx = tip.x - base.x
    val dy = tip.y - base.y
    val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val angle = atan2(dy, dx) * 180f / Math.PI.toFloat()
    withTransform({
        translate(base.x, base.y)
        rotate(angle, Offset.Zero)
        scale(length, width, Offset.Zero)
    }) {
        drawPath(PlantLeafShape, Color(0xFF24694B))
    }
    withTransform({
        translate(base.x, base.y)
        rotate(angle, Offset.Zero)
        scale(length * .96f, width * .78f, Offset.Zero)
    }) {
        drawPath(PlantLeafShape, color)
    }
    drawLine(Color(0xFF9BD48B), base, tip, width * .055f)
}

private fun DrawScope.drawPlantStem(center: Offset, r: Float, leaf: Color = Color(0xFF3D9A70)) {
    drawLine(Color(0xFF24694B), Offset(center.x, center.y + r * .16f), Offset(center.x, center.y + r * .92f), r * .24f)
    drawLine(leaf, Offset(center.x, center.y + r * .16f), Offset(center.x, center.y + r * .92f), r * .15f)
    drawLine(Color(0xFF8BCB81), Offset(center.x - r * .025f, center.y + r * .34f),
        Offset(center.x - r * .025f, center.y + r * .8f), r * .025f)
    drawPlantLeaf(Offset(center.x, center.y + r * .55f), Offset(center.x - r * 1.02f, center.y + r * .34f), r * .48f, leaf)
    drawPlantLeaf(Offset(center.x + r * .04f, center.y + r * .62f), Offset(center.x + r * .98f, center.y + r * .37f), r * .46f, leaf)
}

private fun DrawScope.drawPlantFlower(center: Offset, r: Float, petals: Int, petal: Color, core: Color) {
    repeat(petals) { index ->
        val angle = index * (Math.PI * 2 / petals).toFloat()
        val x = center.x + cos(angle) * r * .43f - r * .19f
        val y = center.y + sin(angle) * r * .43f - r * .19f
        drawOval(Color(0xFF88533E), Offset(x, y), Size(r * .4f, r * .4f))
        drawOval(petal, Offset(x + r * .025f, y + r * .025f), Size(r * .35f, r * .35f))
    }
    drawCircle(Color(0xFF70432F), r * .34f, center)
    drawCircle(core, r * .29f, center)
    drawCircle(Color(0xFFFFE19A), r * .055f, Offset(center.x - r * .12f, center.y - r * .14f))
}

private fun DrawScope.drawPeaHead(center: Offset, r: Float, barrels: Int = 1) {
    drawOval(Color(0xFF39763D), Offset(center.x - r * .72f, center.y - r * .66f), Size(r * 1.48f, r * 1.25f))
    drawOval(Color(0xFF7FCF59), Offset(center.x - r * .66f, center.y - r * .72f), Size(r * 1.35f, r * 1.2f))
    val barrelRows = when (barrels) {
        2 -> listOf(-.16f, .13f)
        4 -> listOf(-.24f, -.08f, .08f, .24f)
        else -> listOf(0f)
    }
    barrelRows.forEach { row ->
        val h = if (barrels == 1) r * .46f else r * .27f
        val top = center.y - h * .5f + row * r
        drawRoundRect(Color(0xFF3C813F), Offset(center.x + r * .22f, top - r * .025f),
            Size(r * .83f, h + r * .05f), androidx.compose.ui.geometry.CornerRadius(h * .48f))
        drawRoundRect(Color(0xFF91D967), Offset(center.x + r * .25f, top),
            Size(r * .73f, h), androidx.compose.ui.geometry.CornerRadius(h * .48f))
        drawOval(Color(0xFF29613A), Offset(center.x + r * .79f, top + h * .17f), Size(r * .22f, h * .66f))
        drawOval(Color(0xFF182D2C), Offset(center.x + r * .83f, top + h * .26f), Size(r * .15f, h * .48f))
    }
    drawPlantEyes(Offset(center.x - r * .12f, center.y - r * .34f), r * .58f)
}

internal fun DrawScope.drawPlantGlyph(
    type: DefensePlant, center: Offset, r: Float, biteProgress: Float = 0f,
    actionProgress: Float = 0f, phase: Int = 0
) {
    when (type) {
        DefensePlant.SUN, DefensePlant.PEA, DefensePlant.CHERRY, DefensePlant.WALL,
        DefensePlant.MINE, DefensePlant.SNOW, DefensePlant.CHOMPER, DefensePlant.DOUBLE,
        DefensePlant.PUFF, DefensePlant.SUNSHROOM, DefensePlant.FUME, DefensePlant.GRAVE,
        DefensePlant.HYPNO, DefensePlant.SCAREDY, DefensePlant.ICE, DefensePlant.DOOM,
        DefensePlant.LILYPAD, DefensePlant.SQUASH, DefensePlant.THREEPEATER, DefensePlant.TANGLE,
        DefensePlant.JALAPENO, DefensePlant.SPIKEWEED -> drawPlantGlyphPartOne(type, center, r, biteProgress, phase)
        else -> drawPlantGlyphPartTwo(type, center, r, biteProgress, phase)
    }
    if (actionProgress > 0f) when (type.kind) {
        PlantKind.GENERATOR, PlantKind.TWIN_GENERATOR -> {
            drawCircle(Color(0xFFFFE07A).copy(alpha = .32f + actionProgress * .32f), r * 1.25f,
                Offset(center.x, center.y - r * .35f), style = Stroke(r * .08f))
            drawCircle(Color.White.copy(alpha = .8f), r * .1f, Offset(center.x, center.y - r * .82f))
        }
        PlantKind.SHOT, PlantKind.SLOW_SHOT, PlantKind.DOUBLE_SHOT, PlantKind.TRIPLE_SHOT,
        PlantKind.PIERCE, PlantKind.SPLIT_SHOT, PlantKind.STAR_SHOT, PlantKind.CACTUS,
        PlantKind.CATTAIL, PlantKind.GATLING, PlantKind.LOB, PlantKind.LOB_SLOW -> {
            val muzzle = Offset(center.x + r * .9f, center.y - r * .28f)
            drawCircle(Color.White.copy(alpha = actionProgress * .75f), r * (.12f + actionProgress * .1f), muzzle)
            drawLine(Color(0xFFFFF0B2).copy(alpha = actionProgress * .7f), muzzle,
                Offset(muzzle.x + r * .6f, muzzle.y), r * .09f)
        }
        PlantKind.GLOOM -> repeat(8) { index ->
            val angle = index * (Math.PI / 4).toFloat() + phase * .05f
            drawLine(Color(0xFFCAAFE9).copy(alpha = actionProgress),
                Offset(center.x + cos(angle) * r * .45f, center.y - r * .12f + sin(angle) * r * .45f),
                Offset(center.x + cos(angle) * r * 1.3f, center.y - r * .12f + sin(angle) * r * 1.3f), r * .08f)
        }
        PlantKind.SUPPORT, PlantKind.MAGNET, PlantKind.COFFEE, PlantKind.GARLIC,
        PlantKind.UMBRELLA, PlantKind.UPGRADE, PlantKind.KELP -> {
            drawCircle(Color(0xFFFFE18A).copy(alpha = actionProgress * .8f), r * 1.1f,
                center, style = Stroke(r * .07f))
        }
        else -> Unit
    }
    when (type) {
        DefensePlant.TORCHWOOD -> {
            val flicker = .72f + .16f * sin(phase * .32f)
            drawOval(Color(0xFFFF922F).copy(alpha = .72f), Offset(center.x - r * .13f,
                center.y - r * 1.13f), Size(r * .36f, r * flicker))
            drawOval(Color(0xFFFFE68A), Offset(center.x - r * .04f, center.y - r * .98f),
                Size(r * .17f, r * .42f))
        }
        DefensePlant.PLANTERN -> drawCircle(Color(0xFFFFE794).copy(alpha = .2f + .1f * sin(phase * .12f)),
            r * 1.35f, Offset(center.x, center.y - r * .2f))
        DefensePlant.SUNSHROOM -> if (phase % 30 < 15) drawCircle(Color(0xFFFFE58A).copy(alpha = .38f),
            r * .16f, Offset(center.x + r * .7f, center.y - r * .6f))
        else -> Unit
    }
}
private fun DrawScope.drawPlantGlyphPartOne(type: DefensePlant, center: Offset, r: Float, biteProgress: Float, phase: Int) {
    val leaf = Color(0xFF3D9A70)
    when (type) {
        DefensePlant.SUN -> {
            drawPlantStem(center, r)
            val head = Offset(center.x, center.y - r * .2f)
            drawPlantFlower(head, r * 1.02f, 12, Color(0xFFFFA92E), Color(0xFF8C4D2E))
            drawCircle(Color(0xFFFFD36C), r * .39f, head)
            drawPlantEyes(Offset(head.x, head.y - r * .02f), r * .36f)
            drawArc(Color(0xFF713C2A), 15f, 150f, false,
                Offset(head.x - r * .18f, head.y + r * .04f), Size(r * .36f, r * .2f), style = Stroke(r * .045f))
            drawCircle(Color(0xFFFF8C72), r * .055f, Offset(head.x - r * .28f, head.y + r * .12f))
            drawCircle(Color(0xFFFF8C72), r * .055f, Offset(head.x + r * .28f, head.y + r * .12f))
        }
        DefensePlant.PEA -> {
            drawPlantStem(center, r)
            drawPeaHead(Offset(center.x - r * .12f, center.y - r * .22f), r * .64f)
        }
        DefensePlant.CHERRY -> {
            drawLine(leaf, Offset(center.x, center.y - r * .25f), Offset(center.x - r * .12f, center.y - r), r * .12f)
            drawOval(leaf, Offset(center.x - r * .1f, center.y - r * .94f), Size(r * .62f, r * .3f))
            drawCircle(Color(0xFFE4484E), r * .48f, Offset(center.x - r * .38f, center.y + r * .05f))
            drawCircle(Color(0xFFFF8053), r * .48f, Offset(center.x + r * .38f, center.y + r * .05f))
            drawCircle(Color(0xFFFFA29A), r * .11f, Offset(center.x - r * .52f, center.y - r * .1f))
            drawCircle(Color(0xFFFFB192), r * .11f, Offset(center.x + r * .25f, center.y - r * .1f))
            drawPlantEyes(Offset(center.x - r * .38f, center.y + r * .05f), r * .35f)
            drawPlantEyes(Offset(center.x + r * .38f, center.y + r * .05f), r * .35f)
        }
        DefensePlant.WALL -> {
            drawOval(Color(0xFF805639), Offset(center.x - r * .7f, center.y - r * .76f), Size(r * 1.4f, r * 1.72f))
            drawOval(Color(0xFFC99A69), Offset(center.x - r * .62f, center.y - r * .82f), Size(r * 1.24f, r * 1.62f))
            drawOval(Color(0xFFE4BD86), Offset(center.x - r * .48f, center.y - r * .73f), Size(r * .3f, r * .72f))
            drawArc(Color(0xFFE4BD86), 205f, 135f, false, Offset(center.x - r * .5f, center.y - r * .72f),
                Size(r * 1.0f, r * 1.36f), style = Stroke(r * .06f))
            drawLine(Color(0xFF8B6146), Offset(center.x - r * .45f, center.y - r * .38f), Offset(center.x - r * .5f, center.y + r * .08f), r * .055f)
            drawLine(Color(0xFF8B6146), Offset(center.x + r * .42f, center.y - r * .06f), Offset(center.x + r * .32f, center.y + r * .53f), r * .055f)
            drawPlantEyes(Offset(center.x, center.y - r * .22f), r * .39f)
            drawArc(Color(0xFF704A39), 200f, 140f, false, Offset(center.x - r * .28f, center.y + r * .02f),
                Size(r * .56f, r * .36f), style = Stroke(r * .07f))
            drawRoundRect(Color(0xFFFFF0D0), Offset(center.x - r * .2f, center.y + r * .1f), Size(r * .17f, r * .25f),
                androidx.compose.ui.geometry.CornerRadius(r * .035f))
            drawRoundRect(Color(0xFFFFF0D0), Offset(center.x + r * .03f, center.y + r * .1f), Size(r * .17f, r * .25f),
                androidx.compose.ui.geometry.CornerRadius(r * .035f))
        }
        DefensePlant.MINE -> {
            drawOval(Color(0xFF76513E), Offset(center.x - r * .94f, center.y + r * .28f), Size(r * 1.88f, r * .55f))
            drawOval(Color(0xFF9C6556), Offset(center.x - r * .72f, center.y - r * .52f), Size(r * 1.44f, r * 1.16f))
            drawOval(Color(0xFFC18368), Offset(center.x - r * .62f, center.y - r * .62f), Size(r * 1.24f, r * .98f))
            drawCircle(Color(0xFFFFE08A), r * .22f, Offset(center.x - r * .24f, center.y - r * .12f))
            drawCircle(Color(0xFFFFE08A), r * .22f, Offset(center.x + r * .27f, center.y - r * .12f))
            drawCircle(Color(0xFF4B382F), r * .09f, Offset(center.x - r * .2f, center.y - r * .09f))
            drawCircle(Color(0xFF4B382F), r * .09f, Offset(center.x + r * .31f, center.y - r * .09f))
            drawPath(Path().apply {
                moveTo(center.x - r * .43f, center.y + r * .28f); quadraticTo(center.x, center.y + r * .08f, center.x + r * .43f, center.y + r * .28f)
                lineTo(center.x + r * .32f, center.y + r * .47f); quadraticTo(center.x, center.y + r * .56f, center.x - r * .32f, center.y + r * .47f); close()
            }, Color(0xFF583B37))
            drawLine(Color(0xFF4E914B), Offset(center.x, center.y - r * .56f), Offset(center.x - r * .08f, center.y - r * .93f), r * .09f)
            drawLine(Color(0xFF6DBD62), Offset(center.x - r * .04f, center.y - r * .68f), Offset(center.x - r * .36f, center.y - r * .94f), r * .09f)
            drawLine(Color(0xFF6DBD62), Offset(center.x + r * .02f, center.y - r * .68f), Offset(center.x + r * .31f, center.y - r * .97f), r * .09f)
            drawCircle(Color(0xFFE3C77A), r * .13f, Offset(center.x, center.y - r * .76f))
        }
        DefensePlant.SNOW -> {
            drawPlantStem(center, r, Color(0xFF3D9A70))
            val head = Offset(center.x - r * .1f, center.y - r * .2f)
            drawCircle(Color(0xFF438FB5), r * .73f, head)
            drawCircle(Color(0xFF78D0EA), r * .66f, Offset(head.x - r * .04f, head.y - r * .05f))
            drawOval(Color(0xFFB9F2FF), Offset(center.x + r * .14f, center.y - r * .27f), Size(r * .69f, r * .34f))
            drawPlantEyes(Offset(head.x - r * .16f, head.y - r * .19f), r * .3f)
            drawLine(Color(0xFF4A9FC1), Offset(center.x + r * .48f, center.y - r * .58f),
                Offset(center.x + r * .56f, center.y - r * .75f), r * .045f)
            for (i in 0..2) {
                val a = i * (Math.PI / 3).toFloat()
                val b = a + Math.PI.toFloat()
                drawLine(Color.White, Offset(center.x + r * .76f + cos(a) * r * .13f, center.y - r * .72f + sin(a) * r * .13f),
                    Offset(center.x + r * .76f + cos(b) * r * .13f, center.y - r * .72f + sin(b) * r * .13f), r * .045f)
            }
        }
        DefensePlant.CHOMPER -> {
            drawPlantStem(center, r)
            val head = Path().apply {
                moveTo(center.x - r * .45f, center.y + r * .28f)
                cubicTo(center.x - r * .86f, center.y - r * .28f, center.x - r * .72f, center.y - r * .85f, center.x - r * .1f, center.y - r * .9f)
                cubicTo(center.x + r * .55f, center.y - r * 1.02f, center.x + r * .75f, center.y - r * .45f, center.x + r * .58f, center.y + r * .12f)
                lineTo(center.x + r * .42f, center.y + r * .54f); close()
            }
            drawPath(head, Color(0xFF49376A))
            drawPath(head, Color(0xFF8363B6).copy(alpha = .72f))
            drawCircle(Color(0xFFA98BD0), r * .09f, Offset(center.x - r * .42f, center.y - r * .57f))
            drawCircle(Color(0xFFA98BD0), r * .08f, Offset(center.x + r * .29f, center.y - r * .7f))
            val phase = biteProgress.coerceIn(0f, 1f)
            val mouth = if (phase < .25f) phase * 4f else if (phase < .7f) 1f else (1f - phase) / .3f
            if (mouth <= .02f) {
                drawPlantEyes(Offset(center.x - r * .03f, center.y - r * .32f), r * .36f)
                drawArc(Color(0xFF422D64), 5f, 170f, false, Offset(center.x - r * .43f, center.y + r * .02f),
                    Size(r * .86f, r * .45f), style = androidx.compose.ui.graphics.drawscope.Stroke(r * .12f))
            } else {
                val opening = r * (.15f + .48f * mouth)
                drawOval(Color(0xFF422D64), Offset(center.x - r * .52f, center.y - r * .04f), Size(r * 1.04f, opening))
                drawOval(Color(0xFFE8738D), Offset(center.x - r * .2f, center.y + opening * .42f), Size(r * .4f, opening * .28f))
                for (i in 0..4) {
                    val x = center.x - r * .42f + i * r * .21f
                    drawPath(Path().apply {
                        moveTo(x, center.y - r * .02f); lineTo(x + r * .14f, center.y - r * .02f)
                        lineTo(x + r * .07f, center.y + opening * .22f); close()
                    }, Color(0xFFFFF1D5))
                }
            }
        }
        DefensePlant.DOUBLE -> {
            drawPlantStem(center, r)
            drawPeaHead(Offset(center.x - r * .12f, center.y - r * .22f), r * .64f, barrels = 2)
        }
        DefensePlant.PUFF -> {
            drawRoundRect(Color(0xFFF2D7B9), Offset(center.x - r * .2f, center.y - r * .06f), Size(r * .4f, r * .8f),
                androidx.compose.ui.geometry.CornerRadius(r * .16f))
            drawPath(Path().apply {
                moveTo(center.x - r * .72f, center.y - r * .12f)
                cubicTo(center.x - r * .82f, center.y - r * .72f, center.x - r * .4f, center.y - r * .94f, center.x, center.y - r * .72f)
                cubicTo(center.x + r * .44f, center.y - r * .96f, center.x + r * .83f, center.y - r * .58f, center.x + r * .72f, center.y - r * .12f)
                close()
            }, Color(0xFF79519C))
            drawOval(Color(0xFFB882D5), Offset(center.x - r * .66f, center.y - r * .69f), Size(r * 1.32f, r * .62f))
            for ((x, y, size) in listOf(Triple(-.4f, -.56f, .09f), Triple(.02f, -.62f, .1f), Triple(.42f, -.49f, .08f)))
                drawCircle(Color(0xFFE7C6EB), r * size, Offset(center.x + r * x, center.y + r * y))
            drawPlantEyes(Offset(center.x, center.y - r * .12f), r * .3f)
            drawArc(Color(0xFF714B89), 10f, 150f, false, Offset(center.x - r * .16f, center.y + r * .15f),
                Size(r * .32f, r * .18f), style = Stroke(r * .04f))
        }
        DefensePlant.SUNSHROOM -> {
            drawRoundRect(Color(0xFFFFE5C2), Offset(center.x - r * .25f, center.y - r * .08f), Size(r * .5f, r * .95f),
                androidx.compose.ui.geometry.CornerRadius(r * .2f))
            drawOval(Color(0xFFB97A26), Offset(center.x - r * .82f, center.y - r * .62f), Size(r * 1.64f, r * .76f))
            drawOval(Color(0xFFFFC65A), Offset(center.x - r * .76f, center.y - r * .7f), Size(r * 1.52f, r * .7f))
            for ((x, y) in listOf(-.48f to -.5f, -.1f to -.58f, .3f to -.49f, .52f to -.36f))
                drawOval(Color(0xFFFFF4C2), Offset(center.x + r * x, center.y + r * y), Size(r * .15f, r * .09f))
            drawPlantEyes(Offset(center.x, center.y - r * .23f), r * .3f)
            drawArc(Color(0xFF9E6838), 205f, 130f, false, Offset(center.x - r * .24f, center.y + r * .04f),
                Size(r * .48f, r * .3f), style = Stroke(r * .055f))
        }
        DefensePlant.FUME -> {
            drawRoundRect(Color(0xFFEEDBCF), Offset(center.x - r * .2f, center.y - r * .08f), Size(r * .4f, r * .9f),
                androidx.compose.ui.geometry.CornerRadius(r * .15f))
            drawPath(Path().apply {
                moveTo(center.x - r * .86f, center.y - r * .15f)
                cubicTo(center.x - r * .86f, center.y - r * .7f, center.x - r * .42f, center.y - r * .82f, center.x, center.y - r * .68f)
                cubicTo(center.x + r * .45f, center.y - r * .84f, center.x + r * .84f, center.y - r * .56f, center.x + r * .84f, center.y - r * .15f)
                close()
            }, Color(0xFF66508E))
            drawOval(Color(0xFF9A7DC8), Offset(center.x - r * .76f, center.y - r * .64f), Size(r * 1.5f, r * .52f))
            drawPlantEyes(Offset(center.x - r * .18f, center.y - r * .23f), r * .22f)
            drawRoundRect(Color(0xFF60477F), Offset(center.x + r * .18f, center.y - r * .08f),
                Size(r * .76f, r * .33f), androidx.compose.ui.geometry.CornerRadius(r * .13f))
            drawOval(Color(0xFF2D263B), Offset(center.x + r * .75f, center.y - r * .03f), Size(r * .17f, r * .22f))
            drawArc(Color(0xFFD7C9F4), 195f, 150f, false, Offset(center.x - r * .68f, center.y - r * .03f),
                Size(r * 1.36f, r * .55f), style = Stroke(r * .1f))
            drawCircle(Color(0xFFC7B5E8), r * .12f, Offset(center.x + r * 1.06f, center.y - r * .47f))
        }
        DefensePlant.GRAVE -> {
            drawOval(Color(0xFF476443), Offset(center.x - r * .65f, center.y + r * .12f), Size(r * 1.3f, r * .82f))
            drawOval(Color(0xFF7AA45A), Offset(center.x - r * .69f, center.y - r * .62f), Size(r * 1.38f, r * .98f))
            drawPlantLeaf(Offset(center.x - r * .28f, center.y - r * .5f), Offset(center.x - r * .72f, center.y - r * 1.02f), r * .24f, Color(0xFF58A25C))
            drawPlantLeaf(Offset(center.x + r * .24f, center.y - r * .5f), Offset(center.x + r * .68f, center.y - r * 1.02f), r * .24f, Color(0xFF58A25C))
            drawPlantEyes(Offset(center.x, center.y - r * .18f), r * .34f, Color(0xFF263149))
            drawOval(Color(0xFF2E4036), Offset(center.x - r * .44f, center.y + r * .08f), Size(r * .88f, r * .46f))
            for (i in 0..3) drawPath(Path().apply {
                val x = center.x - r * .34f + i * r * .22f
                moveTo(x, center.y + r * .08f); lineTo(x + r * .14f, center.y + r * .08f)
                lineTo(x + r * .07f, center.y + r * .28f); close()
            }, Color(0xFFF2E8CE))
        }
        DefensePlant.HYPNO -> {
            drawRoundRect(Color(0xFFC8A7D9), Offset(center.x - r * .2f, center.y - r * .02f), Size(r * .4f, r * .82f),
                androidx.compose.ui.geometry.CornerRadius(r * .14f))
            drawOval(Color(0xFF593E79), Offset(center.x - r * .78f, center.y - r * .78f), Size(r * 1.56f, r * .86f))
            drawOval(Color(0xFF9A68B8), Offset(center.x - r * .7f, center.y - r * .82f), Size(r * 1.4f, r * .76f))
            for ((x, y, size) in listOf(Triple(-.47f, -.58f, .09f), Triple(.4f, -.6f, .08f), Triple(.1f, -.75f, .06f)))
                drawCircle(Color(0xFFDDB7E8), r * size, Offset(center.x + r * x, center.y + r * y))
            drawArc(Color(0xFFFFDBF6), 20f, 320f, false, Offset(center.x - r * .52f, center.y - r * .76f),
                Size(r * 1.04f, r * .72f), style = Stroke(r * .18f))
            drawArc(Color(0xFF512E72), 20f, 300f, false, Offset(center.x - r * .32f, center.y - r * .63f),
                Size(r * .64f, r * .48f), style = Stroke(r * .11f))
            drawCircle(Color(0xFFECD9F3), r * .16f, Offset(center.x - r * .12f, center.y - r * .12f))
            drawCircle(Color(0xFF543B69), r * .08f, Offset(center.x - r * .12f, center.y - r * .12f))
            drawCircle(Color(0xFFECD9F3), r * .16f, Offset(center.x + r * .32f, center.y - r * .12f))
            drawCircle(Color(0xFF543B69), r * .08f, Offset(center.x + r * .32f, center.y - r * .12f))
        }
        DefensePlant.SCAREDY -> {
            drawRoundRect(Color(0xFFE9D9C9), Offset(center.x - r * .22f, center.y - r * .02f), Size(r * .44f, r * .93f),
                androidx.compose.ui.geometry.CornerRadius(r * .18f))
            drawPath(Path().apply {
                moveTo(center.x - r * .75f, center.y - r * .16f)
                cubicTo(center.x - r * .72f, center.y - r * .74f, center.x - r * .4f, center.y - r * .97f, center.x, center.y - r * .72f)
                cubicTo(center.x + r * .4f, center.y - r * .97f, center.x + r * .72f, center.y - r * .74f, center.x + r * .75f, center.y - r * .16f)
                close()
            }, Color(0xFF45385C))
            drawOval(Color(0xFF8775A6), Offset(center.x - r * .69f, center.y - r * .55f), Size(r * 1.38f, r * .4f))
            for ((x, y) in listOf(-.45f to -.47f, -.18f to -.68f, .25f to -.65f, .5f to -.43f))
                drawCircle(Color(0xFFD9C9E5), r * .07f, Offset(center.x + r * x, center.y + r * y))
            drawPlantEyes(Offset(center.x, center.y - r * .14f), r * .34f, Color(0xFF49385F))
            drawArc(Color(0xFF47374C), 200f, 140f, false, Offset(center.x - r * .18f, center.y + r * .13f),
                Size(r * .36f, r * .24f), style = Stroke(r * .06f))
        }
        DefensePlant.ICE -> {
            drawRoundRect(Color(0xFFD8F5F7), Offset(center.x - r * .22f, center.y - r * .02f), Size(r * .44f, r * .85f),
                androidx.compose.ui.geometry.CornerRadius(r * .15f))
            val cap = Path().apply {
                moveTo(center.x - r * .76f, center.y - r * .1f)
                cubicTo(center.x - r * .83f, center.y - r * .72f, center.x - r * .42f, center.y - r * .96f, center.x, center.y - r * .7f)
                cubicTo(center.x + r * .4f, center.y - r * .97f, center.x + r * .8f, center.y - r * .66f, center.x + r * .76f, center.y - r * .1f)
                close()
            }
            drawPath(cap, Color(0xFF5DAEC4)); drawPath(cap, Color(0xFFB9F0F7).copy(alpha = .8f))
            for ((x, y, dot) in listOf(Triple(-.42f, -.57f, .1f), Triple(.02f, -.68f, .13f), Triple(.43f, -.52f, .09f)))
                drawCircle(Color.White, r * dot, Offset(center.x + r * x, center.y + r * y))
            drawPlantEyes(Offset(center.x, center.y - r * .12f), r * .25f)
            for (i in 0..2) {
                val x = center.x - r * .42f + i * r * .4f
                drawPath(Path().apply {
                    moveTo(x - r * .12f, center.y - r * .08f); lineTo(x + r * .12f, center.y - r * .08f)
                    lineTo(x, center.y + r * (.35f + (i % 2) * .14f)); close()
                }, Color(0xFFE6FFFF))
            }
        }
        DefensePlant.DOOM -> {
            drawRoundRect(Color(0xFFB8A8D0), Offset(center.x - r * .2f, center.y - r * .02f), Size(r * .4f, r * .82f),
                androidx.compose.ui.geometry.CornerRadius(r * .14f))
            drawOval(Color(0xFF241D36), Offset(center.x - r * .8f, center.y - r * .8f), Size(r * 1.6f, r * .9f))
            drawOval(Color(0xFF47385C), Offset(center.x - r * .7f, center.y - r * .86f), Size(r * 1.4f, r * .78f))
            for ((x, y, size) in listOf(Triple(-.45f, -.61f, .12f), Triple(-.12f, -.73f, .08f), Triple(.26f, -.64f, .1f), Triple(.5f, -.47f, .07f)))
                drawCircle(Color(0xFFD4C1F2), r * size, Offset(center.x + r * x, center.y + r * y))
            drawPlantEyes(Offset(center.x, center.y - r * .1f), r * .24f, Color(0xFF332548))
            drawArc(Color(0xFF1B1728), 200f, 140f, false, Offset(center.x - r * .16f, center.y + r * .12f),
                Size(r * .32f, r * .19f), style = Stroke(r * .06f))
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
                drawPeaHead(c, r * .36f)
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
            drawPlantEyes(Offset(center.x - r * .04f, center.y - r * .08f), r * .27f, Color(0xFF4A342B))
            drawLine(Color(0xFF8D3029), Offset(center.x - r * .48f, center.y - r * .39f),
                Offset(center.x - r * .12f, center.y - r * .49f), r * .075f)
            drawLine(Color(0xFF8D3029), Offset(center.x + r * .08f, center.y - r * .49f),
                Offset(center.x + r * .44f, center.y - r * .39f), r * .075f)
        }
        DefensePlant.SPIKEWEED -> {
            drawOval(Color(0xFF92784D), Offset(center.x - r * .92f, center.y + r * .25f), Size(r * 1.84f, r * .48f))
            for (i in 0..6) {
                val x = center.x - r * .82f + i * r * .27f
                drawLine(Color(0xFFDAAA50), Offset(x, center.y + r * .38f), Offset(x + (i % 2) * r * .1f, center.y - r * (.38f + (i % 3) * .16f)), r * .13f)
            }
        }
        else -> Unit
    }
}

private fun DrawScope.drawPlantGlyphPartTwo(type: DefensePlant, center: Offset, r: Float, biteProgress: Float, phase: Int) {
    val leaf = Color(0xFF3D9A70)
    when (type) {
        DefensePlant.TORCHWOOD -> {
            drawRoundRect(Color(0xFF88583C), Offset(center.x - r * .58f, center.y - r * .45f), Size(r * 1.16f, r * 1.38f), androidx.compose.ui.geometry.CornerRadius(r * .18f))
            drawRoundRect(Color(0xFFB47A4D), Offset(center.x - r * .47f, center.y - r * .48f), Size(r * .94f, r * .22f), androidx.compose.ui.geometry.CornerRadius(r * .1f))
            drawPlantEyes(Offset(center.x, center.y + r * .02f), r * .23f, Color(0xFF2B2829))
            drawArc(Color(0xFF57372F), 15f, 150f, false, Offset(center.x - r * .19f, center.y + r * .2f),
                Size(r * .38f, r * .2f), style = Stroke(r * .055f))
            drawLine(Color(0xFF68452F), Offset(center.x - r * .42f, center.y - r * .27f), Offset(center.x - r * .32f, center.y + r * .45f), r * .045f)
            drawLine(Color(0xFF68452F), Offset(center.x + r * .38f, center.y - r * .2f), Offset(center.x + r * .27f, center.y + r * .48f), r * .045f)
            drawCircle(Color(0xFF573A32), r * .34f, Offset(center.x, center.y + r * .55f))
            drawOval(Color(0xFFFF8A38), Offset(center.x - r * .34f, center.y - r * 1.12f), Size(r * .68f, r * .88f))
            drawOval(Color(0xFFFFD36C), Offset(center.x - r * .14f, center.y - r * .92f), Size(r * .28f, r * .56f))
        }
        DefensePlant.TALLNUT -> {
            drawRoundRect(Color(0xFF9B7554), Offset(center.x - r * .6f, center.y - r * .98f), Size(r * 1.2f, r * 2f), androidx.compose.ui.geometry.CornerRadius(r * .5f))
            drawRoundRect(Color(0xFFC49B68), Offset(center.x - r * .5f, center.y - r * .88f), Size(r * .9f, r * 1.72f), androidx.compose.ui.geometry.CornerRadius(r * .4f))
            drawArc(Color(0xFFE0BD86), 200f, 130f, false, Offset(center.x - r * .46f, center.y - r * .78f),
                Size(r * .92f, r * 1.3f), style = Stroke(r * .055f))
            drawCircle(Color(0xFFC7A778), r * .1f, Offset(center.x - r * .28f, center.y + r * .48f))
            drawCircle(Color(0xFFC7A778), r * .1f, Offset(center.x + r * .29f, center.y + r * .22f))
            drawPlantEyes(Offset(center.x, center.y - r * .5f), r * .4f)
            drawArc(Color(0xFF664C39), 15f, 150f, false, Offset(center.x - r * .22f, center.y - r * .22f),
                Size(r * .44f, r * .3f), style = Stroke(r * .07f))
            drawRoundRect(Color(0xFFFFF0D0), Offset(center.x - r * .13f, center.y - r * .12f), Size(r * .12f, r * .18f),
                androidx.compose.ui.geometry.CornerRadius(r * .03f))
            drawRoundRect(Color(0xFFFFF0D0), Offset(center.x + r * .02f, center.y - r * .12f), Size(r * .12f, r * .18f),
                androidx.compose.ui.geometry.CornerRadius(r * .03f))
        }
        DefensePlant.SEASHROOM -> {
            drawRoundRect(Color(0xFFBFEAE6), Offset(center.x - r * .2f, center.y - r * .05f), Size(r * .4f, r * .85f), androidx.compose.ui.geometry.CornerRadius(r * .15f))
            drawOval(Color(0xFF54BFC0), Offset(center.x - r * .68f, center.y - r * .68f), Size(r * 1.36f, r * .67f))
            drawCircle(Color(0xFFA5F5E8), r * .13f, Offset(center.x - r * .4f, center.y - r * .38f))
            drawCircle(Color(0xFFA5F5E8), r * .13f, Offset(center.x + r * .32f, center.y - r * .38f))
            drawArc(Color(0xFF278E9A), 15f, 150f, false, Offset(center.x - r * .34f, center.y - r * .08f), Size(r * .68f, r * .32f), style = Stroke(r * .07f))
        }
        DefensePlant.PLANTERN -> {
            drawLine(Color(0xFF815C3C), Offset(center.x, center.y - r), Offset(center.x, center.y - r * .63f), r * .12f)
            drawPlantLeaf(Offset(center.x, center.y - r * .82f), Offset(center.x - r * .42f, center.y - r * 1.12f), r * .2f, Color(0xFF4B9A5D))
            drawPlantLeaf(Offset(center.x, center.y - r * .82f), Offset(center.x + r * .42f, center.y - r * 1.12f), r * .2f, Color(0xFF4B9A5D))
            drawRoundRect(Color(0xFF594D4C), Offset(center.x - r * .64f, center.y - r * .48f), Size(r * 1.28f, r * 1.18f), androidx.compose.ui.geometry.CornerRadius(r * .32f))
            drawRoundRect(Color(0xFF9B7C5C), Offset(center.x - r * .54f, center.y - r * .45f), Size(r * 1.08f, r * 1.04f), androidx.compose.ui.geometry.CornerRadius(r * .3f))
            drawRoundRect(Color(0xFFFFE58A), Offset(center.x - r * .34f, center.y - r * .22f), Size(r * .68f, r * .62f), androidx.compose.ui.geometry.CornerRadius(r * .16f))
            drawPlantEyes(Offset(center.x, center.y - r * .1f), r * .24f, Color(0xFF675344))
            drawLine(Color(0xFFFFE58A), Offset(center.x - r, center.y - r * .55f), Offset(center.x - r * .75f, center.y - r * .55f), r * .07f)
            drawLine(Color(0xFFFFE58A), Offset(center.x + r * .75f, center.y - r * .55f), Offset(center.x + r, center.y - r * .55f), r * .07f)
        }
        DefensePlant.CACTUS -> {
            drawRoundRect(Color(0xFF2E7851), Offset(center.x - r * .36f, center.y - r * .73f), Size(r * .72f, r * 1.6f), androidx.compose.ui.geometry.CornerRadius(r * .3f))
            drawRoundRect(Color(0xFF48A777), Offset(center.x - r * .3f, center.y - r * .68f), Size(r * .6f, r * 1.5f), androidx.compose.ui.geometry.CornerRadius(r * .28f))
            drawRoundRect(Color(0xFF347F55), Offset(center.x - r * .82f, center.y - r * .22f), Size(r * .52f, r * .34f), androidx.compose.ui.geometry.CornerRadius(r * .17f))
            drawRoundRect(Color(0xFF48A777), Offset(center.x - r * .76f, center.y - r * .19f), Size(r * .46f, r * .28f), androidx.compose.ui.geometry.CornerRadius(r * .15f))
            drawRoundRect(Color(0xFF347F55), Offset(center.x + r * .3f, center.y - r * .48f), Size(r * .52f, r * .34f), androidx.compose.ui.geometry.CornerRadius(r * .17f))
            drawRoundRect(Color(0xFF48A777), Offset(center.x + r * .34f, center.y - r * .45f), Size(r * .46f, r * .28f), androidx.compose.ui.geometry.CornerRadius(r * .15f))
            for (y in listOf(-.52f, -.22f, .08f, .4f)) {
                drawLine(Color(0xFFFFE9B2), Offset(center.x - r * .1f, center.y + r * y), Offset(center.x - r * .22f, center.y + r * (y - .12f)), r * .065f)
                drawLine(Color(0xFFFFE9B2), Offset(center.x + r * .1f, center.y + r * y), Offset(center.x + r * .22f, center.y + r * (y - .12f)), r * .065f)
            }
            drawPlantEyes(Offset(center.x, center.y - r * .15f), r * .23f, Color(0xFF244C38))
            drawCircle(Color(0xFFFFDF67), r * .13f, Offset(center.x + r * .08f, center.y - r * .83f))
        }
        DefensePlant.BLOVER -> {
            drawLine(Color(0xFF3D9A70), Offset(center.x, center.y + r * .82f), Offset(center.x, center.y - r * .18f), r * .12f)
            drawOval(Color(0xFF69C67D), Offset(center.x - r * .7f, center.y - r * .9f), Size(r * .64f, r * .76f))
            drawOval(Color(0xFF8FDC89), Offset(center.x - r * .3f, center.y - r * 1.05f), Size(r * .64f, r * .76f))
            drawOval(Color(0xFF4EAA6D), Offset(center.x + r * .12f, center.y - r * .9f), Size(r * .64f, r * .76f))
            drawCircle(Color(0xFFFFE18A), r * .2f, Offset(center.x, center.y - r * .42f))
            drawPlantEyes(Offset(center.x, center.y - r * .43f), r * .2f, Color(0xFF31543D))
            drawArc(Color(0xFF31543D), 15f, 150f, false, Offset(center.x - r * .12f, center.y - r * .23f),
                Size(r * .24f, r * .15f), style = Stroke(r * .04f))
        }
        DefensePlant.SPLITPEA -> {
            drawPlantStem(center, r)
            val leftHead = Offset(center.x - r * .35f, center.y - r * .22f)
            val rightHead = Offset(center.x + r * .35f, center.y - r * .22f)
            drawOval(Color(0xFF4E9A46), Offset(leftHead.x - r * .42f, leftHead.y - r * .45f), Size(r * .76f, r * .9f))
            drawOval(Color(0xFF73C95A), Offset(rightHead.x - r * .34f, rightHead.y - r * .45f), Size(r * .76f, r * .9f))
            drawCircle(Color(0xFF8BDC69), r * .19f, Offset(leftHead.x - r * .51f, leftHead.y - r * .08f))
            drawCircle(Color(0xFF8BDC69), r * .19f, Offset(rightHead.x + r * .51f, rightHead.y - r * .08f))
            drawCircle(Color(0xFF39773D), r * .06f, Offset(leftHead.x - r * .58f, leftHead.y - r * .08f))
            drawCircle(Color(0xFF39773D), r * .06f, Offset(rightHead.x + r * .58f, rightHead.y - r * .08f))
            drawPlantEyes(Offset(leftHead.x + r * .08f, leftHead.y - r * .2f), r * .2f)
            drawPlantEyes(Offset(rightHead.x - r * .08f, rightHead.y - r * .2f), r * .2f)
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
            drawPlantEyes(Offset(center.x, center.y - r * .1f), r * .34f, Color(0xFF684525))
            drawArc(Color(0xFF9A602B), 15f, 150f, false, Offset(center.x - r * .16f, center.y + r * .14f),
                Size(r * .32f, r * .18f), style = Stroke(r * .045f))
        }
        DefensePlant.PUMPKIN -> {
            drawCircle(Color(0xFFE98235), r * .78f, center)
            drawOval(Color(0xFFFFA744), Offset(center.x - r * .72f, center.y - r * .62f), Size(r * .58f, r * 1.2f))
            drawOval(Color(0xFFDA6E2D), Offset(center.x + r * .17f, center.y - r * .62f), Size(r * .58f, r * 1.2f))
            drawLine(leaf, Offset(center.x, center.y - r * .72f), Offset(center.x + r * .1f, center.y - r), r * .16f)
            drawPlantEyes(Offset(center.x, center.y - r * .1f), r * .38f)
            drawArc(Color(0xFF7E3E2B), 15f, 150f, false, Offset(center.x - r * .3f, center.y + r * .08f),
                Size(r * .6f, r * .34f), style = Stroke(r * .08f))
            drawLine(Color(0xFFFFE6B5), Offset(center.x - r * .16f, center.y + r * .15f),
                Offset(center.x - r * .14f, center.y + r * .27f), r * .06f)
            drawLine(Color(0xFFFFE6B5), Offset(center.x + r * .14f, center.y + r * .15f),
                Offset(center.x + r * .16f, center.y + r * .27f), r * .06f)
        }
        DefensePlant.MAGNET -> {
            drawRoundRect(Color(0xFFD7B9D5), Offset(center.x - r * .22f, center.y - r * .02f), Size(r * .44f, r * .9f),
                androidx.compose.ui.geometry.CornerRadius(r * .18f))
            drawPath(Path().apply {
                moveTo(center.x - r * .78f, center.y - r * .18f)
                cubicTo(center.x - r * .84f, center.y - r * .77f, center.x - r * .42f, center.y - r * .98f, center.x, center.y - r * .72f)
                cubicTo(center.x + r * .42f, center.y - r * .98f, center.x + r * .84f, center.y - r * .77f, center.x + r * .78f, center.y - r * .18f)
                close()
            }, Color(0xFF77518D))
            drawOval(Color(0xFFC194C4), Offset(center.x - r * .72f, center.y - r * .58f), Size(r * 1.44f, r * .38f))
            drawPlantEyes(Offset(center.x, center.y - r * .18f), r * .28f)
            drawRoundRect(Color(0xFFB74755), Offset(center.x - r * .62f, center.y - r * .84f), Size(r * .24f, r * 1.22f),
                androidx.compose.ui.geometry.CornerRadius(r * .1f))
            drawRoundRect(Color(0xFFFF9E9A), Offset(center.x - r * .57f, center.y - r * .8f), Size(r * .14f, r * 1.12f),
                androidx.compose.ui.geometry.CornerRadius(r * .07f))
            drawRoundRect(Color(0xFFB74755), Offset(center.x + r * .38f, center.y - r * .84f), Size(r * .24f, r * 1.22f),
                androidx.compose.ui.geometry.CornerRadius(r * .1f))
            drawRoundRect(Color(0xFFFF9E9A), Offset(center.x + r * .43f, center.y - r * .8f), Size(r * .14f, r * 1.12f),
                androidx.compose.ui.geometry.CornerRadius(r * .07f))
            drawArc(Color(0xFFB74755), 180f, 180f, false, Offset(center.x - r * .62f, center.y - r * .93f),
                Size(r * 1.24f, r * 1.1f), style = Stroke(r * .24f))
            drawCircle(Color(0xFFF2B6C7), r * .08f, Offset(center.x - r * .58f, center.y + r * .3f))
            drawCircle(Color(0xFFF2B6C7), r * .08f, Offset(center.x + r * .56f, center.y + r * .3f))
        }
        DefensePlant.CABBAGE -> {
            drawPlantStem(center, r)
            drawOval(Color(0xFF3D873C), Offset(center.x - r * .67f, center.y - r * .63f), Size(r * 1.32f, r * 1.12f))
            drawOval(Color(0xFF86C753), Offset(center.x - r * .62f, center.y - r * .69f), Size(r * 1.23f, r * 1.04f))
            drawPath(Path().apply {
                moveTo(center.x - r * .58f, center.y - r * .17f); cubicTo(center.x - r * .74f, center.y - r * .83f, center.x - r * .25f, center.y - r * .92f, center.x + r * .02f, center.y - r * .42f)
                cubicTo(center.x + r * .32f, center.y - r * .9f, center.x + r * .7f, center.y - r * .72f, center.x + r * .58f, center.y - r * .13f)
            }, Color(0xFFB0DA6C), style = Stroke(r * .1f))
            drawPlantEyes(Offset(center.x - r * .05f, center.y - r * .18f), r * .34f)
            drawArc(Color(0xFF4B713B), 15f, 150f, false, Offset(center.x - r * .18f, center.y + r * .1f),
                Size(r * .36f, r * .23f), style = Stroke(r * .06f))
            drawLine(Color(0xFFFFF2C2), Offset(center.x + r * .55f, center.y - r * .45f), Offset(center.x + r * .97f, center.y - r * .65f), r * .08f)
        }
        DefensePlant.FLOWERPOT -> {
            drawOval(Color(0xFF5F8F54), Offset(center.x - r * .62f, center.y - r * .82f), Size(r * 1.24f, r * .55f))
            drawCircle(Color(0xFFB5D47A), r * .34f, Offset(center.x, center.y - r * .58f))
            val pot = Path().apply { moveTo(center.x - r * .72f, center.y - r * .32f); lineTo(center.x + r * .72f, center.y - r * .32f); lineTo(center.x + r * .5f, center.y + r * .82f); lineTo(center.x - r * .5f, center.y + r * .82f); close() }
            drawPath(pot, Color(0xFFB66D50))
            drawLine(Color(0xFFE5A078), Offset(center.x - r * .66f, center.y - r * .2f), Offset(center.x + r * .66f, center.y - r * .2f), r * .14f)
        }
        DefensePlant.KERNEL -> {
            drawRoundRect(Color(0xFF9A6F2E), Offset(center.x - r * .48f, center.y - r * .76f), Size(r * .96f, r * 1.48f), androidx.compose.ui.geometry.CornerRadius(r * .4f))
            drawRoundRect(Color(0xFFFFD34F), Offset(center.x - r * .43f, center.y - r * .78f), Size(r * .86f, r * 1.44f), androidx.compose.ui.geometry.CornerRadius(r * .38f))
            for (row in 0..4) for (col in 0..3) drawOval(Color(if ((row + col) % 2 == 0) 0xFFFFF08A else 0xFFD99B30),
                Offset(center.x - r * .32f + col * r * .21f, center.y - r * .57f + row * r * .26f), Size(r * .17f, r * .19f))
            drawPlantEyes(Offset(center.x - r * .03f, center.y - r * .03f), r * .25f, Color(0xFF735321))
            drawPlantLeaf(Offset(center.x - r * .3f, center.y + r * .42f), Offset(center.x - r * .88f, center.y + r * .79f), r * .32f, Color(0xFF4A9B62))
            drawPlantLeaf(Offset(center.x + r * .3f, center.y + r * .42f), Offset(center.x + r * .84f, center.y + r * .77f), r * .32f, Color(0xFF398752))
            for (i in 0..2) drawLine(Color(0xFFFFE6A0), Offset(center.x - r * .18f + i * r * .18f, center.y - r * .78f),
                Offset(center.x - r * .26f + i * r * .2f, center.y - r * 1.07f), r * .045f)
        }
        DefensePlant.COFFEE -> {
            drawOval(Color(0xFF6B3E2B), Offset(center.x - r * .77f, center.y - r * .54f), Size(r * 1.54f, r * 1.14f))
            drawOval(Color(0xFFB8794D), Offset(center.x - r * .7f, center.y - r * .62f), Size(r * 1.4f, r * 1.06f))
            drawArc(Color(0xFF70442D), 75f, 220f, false, Offset(center.x - r * .12f, center.y - r * .47f),
                Size(r * .34f, r * .76f), style = Stroke(r * .11f))
            drawPlantEyes(Offset(center.x - r * .1f, center.y - r * .08f), r * .27f, Color(0xFF49352F))
            drawArc(Color(0xFF70442D), 15f, 150f, false, Offset(center.x - r * .2f, center.y + r * .13f),
                Size(r * .4f, r * .22f), style = Stroke(r * .06f))
            drawArc(Color(0xFFFFE2A2), 200f, 140f, false, Offset(center.x - r * .28f, center.y - r * .9f),
                Size(r * .56f, r * .6f), style = Stroke(r * .09f))
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
            val leaf = Path().apply {
                moveTo(center.x, center.y + r * .83f)
                cubicTo(center.x - r * .18f, center.y + r * .24f, center.x - r * 1.05f, center.y + r * .02f, center.x - r, center.y - r * .42f)
                cubicTo(center.x - r * .72f, center.y - r * 1.03f, center.x - r * .18f, center.y - r * .95f, center.x, center.y - r * .75f)
                cubicTo(center.x + r * .4f, center.y - r * 1.1f, center.x + r * 1.02f, center.y - r * .83f, center.x + r, center.y - r * .34f)
                cubicTo(center.x + r * .96f, center.y + r * .16f, center.x + r * .2f, center.y + r * .24f, center.x, center.y + r * .83f)
                close()
            }
            drawPath(leaf, Color(0xFF287E50)); drawPath(leaf, Color(0xFF62B868))
            drawLine(Color(0xFFB0E28A), Offset(center.x, center.y + r * .68f), Offset(center.x, center.y - r * .72f), r * .08f)
            drawLine(Color(0xFF3F9858), Offset(center.x, center.y - r * .02f), Offset(center.x - r * .75f, center.y - r * .49f), r * .055f)
            drawLine(Color(0xFF3F9858), Offset(center.x, center.y - r * .02f), Offset(center.x + r * .76f, center.y - r * .46f), r * .055f)
            drawCircle(Color(0xFF3F9858), r * .35f, Offset(center.x, center.y + r * .47f))
            drawPlantEyes(Offset(center.x, center.y + r * .4f), r * .22f, Color(0xFF244C38))
        }
        DefensePlant.MARIGOLD -> {
            drawPlantStem(center, r)
            val head = Offset(center.x, center.y - r * .12f)
            drawPlantFlower(head, r * .88f, 16, Color(0xFFFF9A31), Color(0xFFFFD55A))
            drawCircle(Color(0xFF8B532B), r * .34f, head)
            drawPlantEyes(Offset(head.x, head.y - r * .02f), r * .3f, Color(0xFF513620))
            drawArc(Color(0xFF784A2D), 15f, 150f, false, Offset(head.x - r * .16f, head.y + r * .12f),
                Size(r * .32f, r * .17f), style = Stroke(r * .045f))
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
            drawPeaHead(Offset(center.x - r * .12f, center.y - r * .22f), r * .64f, barrels = 4)
        }
        DefensePlant.TWINSUN -> {
            drawPlantStem(center, r)
            for ((head, petal, core) in listOf(
                Triple(Offset(center.x - r * .42f, center.y - r * .18f), Color(0xFFFFA834), Color(0xFFFFE77B)),
                Triple(Offset(center.x + r * .43f, center.y - r * .18f), Color(0xFFFFCE45), Color(0xFFFFF4B0)))) {
                drawPlantFlower(head, r * .6f, 10, petal, Color(0xFF8C4D2E))
                drawCircle(core, r * .25f, head)
                drawPlantEyes(Offset(head.x, head.y - r * .01f), r * .23f)
            }
        }
        DefensePlant.GLOOM -> {
            drawRoundRect(Color(0xFF4D3B73), Offset(center.x - r * .22f, center.y + r * .04f), Size(r * .44f, r * .76f),
                androidx.compose.ui.geometry.CornerRadius(r * .15f))
            val cap = Path().apply {
                moveTo(center.x - r * .78f, center.y + r * .08f)
                cubicTo(center.x - r * .82f, center.y - r * .2f, center.x - r * .68f, center.y - r * .4f, center.x - r * .44f, center.y - r * .4f)
                cubicTo(center.x - r * .3f, center.y - r * .82f, center.x + r * .22f, center.y - r * .82f, center.x + r * .36f, center.y - r * .45f)
                cubicTo(center.x + r * .7f, center.y - r * .48f, center.x + r * .86f, center.y - r * .14f, center.x + r * .78f, center.y + r * .08f); close()
            }
            drawPath(cap, Color(0xFF493B72)); drawPath(cap, Color(0xFF8062B2))
            drawOval(Color(0xFF292640), Offset(center.x - r * .43f, center.y - r * .16f), Size(r * .86f, r * .43f))
            drawPlantEyes(Offset(center.x, center.y - r * .27f), r * .28f, Color(0xFF35284F))
            for (i in 0..3) drawCircle(Color(0xFFC1A5E5), r * .085f,
                Offset(center.x - r * .42f + i * r * .28f, center.y - r * .48f))
        }
        DefensePlant.CATTAIL -> {
            drawLine(Color(0xFF24694B), Offset(center.x, center.y + r * .82f), Offset(center.x, center.y - r * .27f), r * .16f)
            drawLine(Color(0xFF65B872), Offset(center.x, center.y + r * .76f), Offset(center.x, center.y - r * .26f), r * .09f)
            drawPlantLeaf(Offset(center.x, center.y + r * .48f), Offset(center.x - r * .88f, center.y + r * .22f), r * .32f, Color(0xFF4E9E62))
            drawPlantLeaf(Offset(center.x, center.y + r * .56f), Offset(center.x + r * .84f, center.y + r * .29f), r * .3f, Color(0xFF4E9E62))
            val head = Offset(center.x, center.y - r * .57f)
            drawPath(Path().apply {
                moveTo(head.x - r * .5f, head.y + r * .23f)
                lineTo(head.x - r * .63f, head.y - r * .38f); lineTo(head.x - r * .25f, head.y - r * .22f)
                quadraticTo(head.x, head.y - r * .78f, head.x + r * .25f, head.y - r * .22f)
                lineTo(head.x + r * .63f, head.y - r * .38f); lineTo(head.x + r * .5f, head.y + r * .23f)
                quadraticTo(head.x, head.y + r * .52f, head.x - r * .5f, head.y + r * .23f); close()
            }, Color(0xFF7B4E42))
            drawPath(Path().apply {
                moveTo(head.x - r * .4f, head.y + r * .14f)
                quadraticTo(head.x - r * .3f, head.y - r * .4f, head.x, head.y - r * .27f)
                quadraticTo(head.x + r * .35f, head.y - r * .42f, head.x + r * .4f, head.y + r * .14f)
                quadraticTo(head.x, head.y + r * .42f, head.x - r * .4f, head.y + r * .14f); close()
            }, Color(0xFFD49382))
            drawPlantEyes(Offset(head.x, head.y - r * .02f), r * .24f, Color(0xFF553B36))
            drawCircle(Color(0xFF5B413B), r * .065f, Offset(head.x + r * .1f, head.y + r * .2f))
            drawLine(Color(0xFF6F4742), Offset(head.x - r * .1f, head.y + r * .21f), Offset(head.x - r * .42f, head.y + r * .1f), r * .035f)
            drawLine(Color(0xFF6F4742), Offset(head.x + r * .17f, head.y + r * .21f), Offset(head.x + r * .46f, head.y + r * .1f), r * .035f)
        }
        DefensePlant.WINTERMELON -> {
            drawPlantStem(center, r)
            drawCircle(Color(0xFF518DB5), r * .73f, Offset(center.x, center.y - r * .18f))
            drawArc(Color(0xFFB7E7F4), 80f, 205f, false, Offset(center.x - r * .52f, center.y - r * .82f), Size(r * .4f, r * 1.2f), style = Stroke(r * .1f))
            drawArc(Color(0xFFB7E7F4), 80f, 205f, false, Offset(center.x + r * .12f, center.y - r * .82f), Size(r * .4f, r * 1.2f), style = Stroke(r * .1f))
            drawPlantEyes(Offset(center.x, center.y - r * .24f), r * .32f, Color(0xFF3B5666))
            drawArc(Color(0xFF3B5666), 15f, 150f, false, Offset(center.x - r * .17f, center.y + r * .02f),
                Size(r * .34f, r * .2f), style = Stroke(r * .05f))
            for (i in 0..3) drawCircle(Color.White.copy(alpha = .78f), r * .055f,
                Offset(center.x - r * .42f + i * r * .27f, center.y - r * .53f))
        }
        DefensePlant.GOLDMAGNET -> {
            drawRoundRect(Color(0xFFFFD45C), Offset(center.x - r * .72f, center.y - r * .76f), Size(r * .3f, r * 1.38f), androidx.compose.ui.geometry.CornerRadius(r * .1f))
            drawRoundRect(Color(0xFFFFD45C), Offset(center.x + r * .42f, center.y - r * .76f), Size(r * .3f, r * 1.38f), androidx.compose.ui.geometry.CornerRadius(r * .1f))
            drawArc(Color(0xFFFFD45C), 180f, 180f, false, Offset(center.x - r * .72f, center.y - r * .86f), Size(r * 1.44f, r * 1.24f), style = Stroke(r * .28f))
            drawCircle(Color.White, r * .12f, Offset(center.x - r * .5f, center.y - r * .5f))
            drawCircle(Color.White, r * .12f, Offset(center.x + r * .57f, center.y - r * .5f))
            drawCircle(Color(0xFFFFA640), r * .11f, Offset(center.x, center.y + r * .2f))
            drawCircle(Color(0xFFFFF0A1), r * .2f, Offset(center.x, center.y + r * .68f))
            drawCircle(Color(0xFFE3A82A), r * .15f, Offset(center.x, center.y + r * .68f), style = Stroke(r * .04f))
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
            drawRoundRect(Color(0xFF5C7045), Offset(center.x - r * .86f, center.y - r * .48f), Size(r * 1.72f, r * .94f), androidx.compose.ui.geometry.CornerRadius(r * .22f))
            drawRoundRect(Color(0xFF8E6B47), Offset(center.x - r * .7f, center.y + r * .25f), Size(r * 1.4f, r * .2f), androidx.compose.ui.geometry.CornerRadius(r * .06f))
            for (x in listOf(-.48f, .35f)) {
                drawRoundRect(Color(0xFFAA7A3F), Offset(center.x + r * x, center.y - r * .55f), Size(r * .55f, r * .87f), androidx.compose.ui.geometry.CornerRadius(r * .2f))
                drawRoundRect(Color(0xFFFFCF58), Offset(center.x + r * (x + .06f), center.y - r * .54f), Size(r * .42f, r * .83f), androidx.compose.ui.geometry.CornerRadius(r * .18f))
                for (row in 0..2) for (col in 0..1) drawOval(Color(0xFFE5A938),
                    Offset(center.x + r * (x + .14f + col * .18f), center.y - r * .32f + row * r * .2f), Size(r * .12f, r * .14f))
                drawRoundRect(Color(0xFF3A302D), Offset(center.x + r * (x + .35f), center.y - r * .23f), Size(r * .3f, r * .3f),
                    androidx.compose.ui.geometry.CornerRadius(r * .1f))
            }
            drawCircle(Color(0xFF4F443D), r * .18f, Offset(center.x - r * .6f, center.y + r * .58f))
            drawCircle(Color(0xFF4F443D), r * .18f, Offset(center.x + r * .6f, center.y + r * .58f))
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
        else -> Unit
    }
}
internal fun DrawScope.drawZombieGlyph(
    type: ZombieKind, center: Offset, r: Float, poleConsumed: Boolean = false,
    armorRemaining: Int = type.armor, gearRemoved: Boolean = false, angry: Boolean = false,
    jumped: Boolean = false, actionMs: Long = 0L, jumpingMs: Long = 0L,
    retreating: Boolean = false, friendly: Boolean = false, submerged: Boolean = false,
    targetX: Float = -1f, targetRow: Int = -1, phase: Int = 0,
    gearLostMs: Long = 0L
) {
    if (type.behavior == ZombieBehavior.DIVE && submerged) {
        drawOval(Color(0xFF77CEDB), Offset(center.x - r * .5f, center.y + r * .35f),
            Size(r, r * .22f))
        drawLine(Color(0xFF5BC6D4), Offset(center.x + r * .1f, center.y + r * .4f),
            Offset(center.x + r * .38f, center.y - r * .62f), r * .12f)
        drawCircle(Color(0xFF5BC6D4), r * .16f, Offset(center.x + r * .38f, center.y - r * .64f))
        return
    }
    val skin = when (type) {
        ZombieKind.YETI -> Color(0xFF90C5CC)
        ZombieKind.IMP -> Color(0xFFA9C67B)
        else -> Color(0xFF9CB36B)
    }
    val coat = if (friendly) Color(0xFF4C9C75) else when (type) {
        ZombieKind.DANCER -> Color(0xFF694991)
        ZombieKind.BACKUP -> Color(0xFF4F6E9D)
        ZombieKind.DIGGER -> Color(0xFF58604E)
        ZombieKind.FOOTBALL -> Color(0xFF754740)
        ZombieKind.YETI -> Color(0xFF719FA8)
        ZombieKind.IMP -> Color(0xFF89735C)
        else -> Color(0xFF75604C)
    }
    val walk = sin(phase * .28f) * r * .2f
    val trousers = Path().apply {
        moveTo(center.x - r * .46f, center.y + r * .64f); lineTo(center.x + r * .43f, center.y + r * .64f)
        lineTo(center.x + r * .4f + walk, center.y + r * 1.08f); lineTo(center.x + r * .09f + walk, center.y + r * 1.08f)
        lineTo(center.x - r * .04f, center.y + r * .84f); lineTo(center.x - r * .24f - walk, center.y + r * 1.08f)
        lineTo(center.x - r * .54f - walk, center.y + r * 1.08f); close()
    }
    drawPath(trousers, Color(0xFF344A60))
    drawLine(Color(0xFF62798A), Offset(center.x - r * .05f, center.y + r * .73f),
        Offset(center.x - r * .17f - walk, center.y + r * 1.01f), r * .045f)
    drawPath(Path().apply {
        moveTo(center.x - r * .56f - walk, center.y + r * 1.02f); lineTo(center.x - r * .22f - walk, center.y + r * 1.02f)
        lineTo(center.x - r * .18f - walk, center.y + r * 1.16f); lineTo(center.x - r * .65f - walk, center.y + r * 1.16f); close()
    }, Color(0xFF353A3C))
    drawPath(Path().apply {
        moveTo(center.x + r * .1f + walk, center.y + r * 1.02f); lineTo(center.x + r * .44f + walk, center.y + r * 1.02f)
        lineTo(center.x + r * .52f + walk, center.y + r * 1.16f); lineTo(center.x + r * .05f + walk, center.y + r * 1.16f); close()
    }, Color(0xFF353A3C))
    drawRoundRect(Color(0xFF4C3C33), Offset(center.x - r * .62f, center.y + r * .01f), Size(r * 1.24f, r * .88f),
        androidx.compose.ui.geometry.CornerRadius(r * .22f))
    drawRoundRect(coat, Offset(center.x - r * .56f, center.y + r * .04f), Size(r * 1.12f, r * .82f),
        androidx.compose.ui.geometry.CornerRadius(r * .2f))
    drawLine(Color(0xFFB9A58B), Offset(center.x - r * .22f, center.y + r * .08f),
        Offset(center.x, center.y + r * .31f), r * .08f)
    drawLine(Color(0xFFB9A58B), Offset(center.x + r * .22f, center.y + r * .08f),
        Offset(center.x, center.y + r * .31f), r * .08f)
    drawPath(Path().apply {
        moveTo(center.x - r * .15f, center.y + r * .2f); lineTo(center.x + r * .15f, center.y + r * .2f)
        lineTo(center.x + r * .11f, center.y + r * .76f); lineTo(center.x, center.y + r * .9f)
        lineTo(center.x - r * .11f, center.y + r * .76f); close()
    }, Color(0xFFBC4944))
    drawLine(Color(0xFFFFAAA0), Offset(center.x - r * .09f, center.y + r * .4f),
        Offset(center.x + r * .08f, center.y + r * .4f), r * .035f)
    drawLine(Color(0xFFFFAAA0), Offset(center.x - r * .08f, center.y + r * .58f),
        Offset(center.x + r * .07f, center.y + r * .58f), r * .03f)
    drawLine(Color(0xFF4D5260), Offset(center.x - r * .44f, center.y + r * .21f),
        Offset(center.x - r * .88f, center.y + r * .03f + walk), r * .19f)
    drawLine(Color(0xFF4D5260), Offset(center.x - r * .88f, center.y + r * .03f + walk),
        Offset(center.x - r * 1.02f, center.y + r * .14f + walk), r * .13f)
    drawOval(skin, Offset(center.x - r * 1.13f, center.y + r * .02f + walk), Size(r * .28f, r * .23f))
    drawLine(Color(0xFF4D5260), Offset(center.x + r * .42f, center.y + r * .22f),
        Offset(center.x + r * .76f, center.y + r * .4f - walk), r * .16f)
    drawOval(skin, Offset(center.x + r * .67f, center.y + r * .32f - walk), Size(r * .3f, r * .24f))
    drawPath(Path().apply {
        moveTo(center.x - r * .43f, center.y + r * .34f); lineTo(center.x - r * .28f, center.y + r * .47f)
        lineTo(center.x - r * .38f, center.y + r * .78f); lineTo(center.x - r * .49f, center.y + r * .67f); close()
    }, Color(0xFF927657))
    drawPath(Path().apply {
        moveTo(center.x + r * .43f, center.y + r * .34f); lineTo(center.x + r * .28f, center.y + r * .47f)
        lineTo(center.x + r * .38f, center.y + r * .78f); lineTo(center.x + r * .49f, center.y + r * .67f); close()
    }, Color(0xFF927657))
    drawOval(Color(0xFF738A52), Offset(center.x - r * .55f, center.y - r * .7f), Size(r * 1.1f, r * 1.42f))
    drawOval(skin, Offset(center.x - r * .51f, center.y - r * .76f), Size(r * 1.02f, r * 1.38f))
    drawCircle(Color(0xFFB3C77F), r * .12f, Offset(center.x - r * .5f, center.y - r * .13f))
    drawCircle(Color(0xFFB3C77F), r * .12f, Offset(center.x + r * .5f, center.y - r * .13f))
    drawPath(Path().apply {
        moveTo(center.x - r * .51f, center.y - r * .67f); lineTo(center.x - r * .28f, center.y - r * .78f)
        lineTo(center.x - r * .03f, center.y - r * .65f); lineTo(center.x + r * .25f, center.y - r * .77f)
        lineTo(center.x + r * .48f, center.y - r * .6f); lineTo(center.x + r * .39f, center.y - r * .48f)
        lineTo(center.x - r * .43f, center.y - r * .49f); close()
    }, Color(0xFF554C43))
    drawOval(Color.White, Offset(center.x - r * .38f, center.y - r * .45f), Size(r * .4f, r * .38f))
    drawOval(Color.White, Offset(center.x + r * .02f, center.y - r * .45f), Size(r * .4f, r * .38f))
    drawOval(Color(0xFF273149), Offset(center.x - r * .17f, center.y - r * .29f), Size(r * .16f, r * .2f))
    drawOval(Color(0xFF273149), Offset(center.x + r * .2f, center.y - r * .29f), Size(r * .16f, r * .2f))
    drawCircle(Color.White, r * .045f, Offset(center.x - r * .19f, center.y - r * .35f))
    drawCircle(Color.White, r * .045f, Offset(center.x + r * .18f, center.y - r * .35f))
    drawOval(Color(0xFF82945D), Offset(center.x - r * .62f, center.y - r * .16f), Size(r * .2f, r * .2f))
    drawOval(Color(0xFF82945D), Offset(center.x + r * .42f, center.y - r * .16f), Size(r * .2f, r * .2f))
    drawOval(Color(0xFF809359), Offset(center.x - r * .57f, center.y - r * .12f), Size(r * .4f, r * .23f))
    drawArc(Color(0xFF4D4039), 195f, 150f, false, Offset(center.x - r * .25f, center.y + r * .02f),
        Size(r * .5f, r * .28f), style = Stroke(r * .075f))
    drawRoundRect(Color(0xFFF0E8D2), Offset(center.x - r * .19f, center.y + r * .04f),
        Size(r * .15f, r * .14f), androidx.compose.ui.geometry.CornerRadius(r * .02f))
    drawRoundRect(Color(0xFFF0E8D2), Offset(center.x + r * .02f, center.y + r * .04f),
        Size(r * .15f, r * .14f), androidx.compose.ui.geometry.CornerRadius(r * .02f))
    val brow = if (angry) Color(0xFFC24742) else Color(0xFF48523D)
    drawLine(brow, Offset(center.x - r * .37f, center.y - r * .51f),
        Offset(center.x - r * .08f, center.y - r * if (angry) .38f else .49f), r * .06f)
    drawLine(brow, Offset(center.x + r * .08f, center.y - r * if (angry) .38f else .49f),
        Offset(center.x + r * .37f, center.y - r * .51f), r * .06f)
    drawArc(Color(0xFF51513D), 10f, 160f, false, Offset(center.x - r * .24f, center.y - r * .03f),
        Size(r * .48f, r * .25f), style = Stroke(r * .07f))
    drawRoundRect(Color(0xFFF5E8C8), Offset(center.x - r * .1f, center.y + r * .03f),
        Size(r * .09f, r * .13f), androidx.compose.ui.geometry.CornerRadius(r * .02f))
    drawRoundRect(Color(0xFFF5E8C8), Offset(center.x + r * .02f, center.y + r * .03f),
        Size(r * .09f, r * .13f), androidx.compose.ui.geometry.CornerRadius(r * .02f))
    if (actionMs > 0L && type.behavior !in setOf(ZombieBehavior.GIANT, ZombieBehavior.BOSS,
            ZombieBehavior.CATAPULT, ZombieBehavior.SUMMON)) {
        val bite = sin(phase * .45f) * r * .28f
        drawLine(Color(0xFF9CB36B), Offset(center.x + r * .36f, center.y + r * .28f),
            Offset(center.x + r * .82f, center.y + r * .12f + bite), r * .11f)
    }
    when (type.behavior) {
        ZombieBehavior.ARMOR, ZombieBehavior.SHIELD, ZombieBehavior.SLED, ZombieBehavior.LADDER -> Unit
        ZombieBehavior.VAULT -> if (type == ZombieKind.VAULT && !poleConsumed) {
            drawLine(Color(0xFFB78FE8), Offset(center.x - r * .8f, center.y + r * .7f),
                Offset(center.x + r * .72f, center.y - r * 1.35f), r * .12f)
        }
        ZombieBehavior.POGO -> if (!gearRemoved) {
            drawLine(Color(0xFF66727D), Offset(center.x - r * .58f, center.y - r * .22f),
                Offset(center.x - r * .58f, center.y + r * 1.03f), r * .12f)
            val spring = Path().apply {
                moveTo(center.x - r * .72f, center.y + r * .08f)
                lineTo(center.x - r * .43f, center.y + r * .2f); lineTo(center.x - r * .72f, center.y + r * .32f)
                lineTo(center.x - r * .43f, center.y + r * .44f); lineTo(center.x - r * .72f, center.y + r * .56f)
                lineTo(center.x - r * .43f, center.y + r * .68f); lineTo(center.x - r * .72f, center.y + r * .8f)
            }
            drawPath(spring, Color(0xFFCBD5DC), style = Stroke(r * .09f))
            drawRoundRect(Color(0xFFE5A84A), Offset(center.x - r * .88f, center.y + r * .94f),
                Size(r * .61f, r * .16f), androidx.compose.ui.geometry.CornerRadius(r * .06f))
        }
        ZombieBehavior.AIRBORNE -> {
            if (type == ZombieKind.BALLOON) {
                drawOval(Color(0xFFB64853), Offset(center.x - r * .5f, center.y - r * 2.08f), Size(r, r * 1.18f))
                drawOval(Color(0xFFE8737C), Offset(center.x - r * .42f, center.y - r * 2.02f), Size(r * .82f, r * 1.02f))
                drawLine(Color(0xFF9B5960), Offset(center.x, center.y - r * .92f), Offset(center.x, center.y - r * .55f), r * .035f)
                drawLine(Color.White, Offset(center.x - r * .18f, center.y - r * 1.78f),
                    Offset(center.x - r * .08f, center.y - r * 1.4f), r * .045f)
            } else {
                drawLine(Color(0xFFC2D5E8), Offset(center.x + r * .16f, center.y - r * 2f),
                    Offset(center.x + r * .16f, center.y - r * .58f), r * .08f)
                drawLine(Color(0xFFC2D5E8), Offset(center.x - r * .24f, center.y - r * 1.96f),
                    Offset(center.x + r * .16f, center.y - r * 1.75f), r * .08f)
                drawRoundRect(Color(0xFFE1BA64), Offset(center.x - r * .02f, center.y - r * .66f),
                    Size(r * .36f, r * .22f), androidx.compose.ui.geometry.CornerRadius(r * .05f))
            }
        }
        ZombieBehavior.BUNGEE -> {
            if (targetX >= 0f && targetRow in -4..4)
                drawLine(Color(0xFFBBC7D8), Offset(center.x, center.y + r * .2f),
                    Offset(center.x + targetX * r * 4f, center.y + targetRow * r * 4f), r * .06f)
            drawLine(Color(0xFFCBD7E6), Offset(center.x - r * .2f, center.y - r * .6f),
                Offset(center.x + r * .2f, center.y - r * 1.95f), r * .1f)
            drawLine(Color(0xFFCBD7E6), Offset(center.x - r * .65f, center.y - r * 1.9f),
                Offset(center.x + r * .55f, center.y - r * 1.9f), r * .1f)
        }
        ZombieBehavior.DIGGER -> {
            if (!gearRemoved) {
                val drill = Path().apply {
                    moveTo(center.x + r * .38f, center.y - r * .28f); lineTo(center.x + r * 1.35f, center.y)
                    lineTo(center.x + r * .38f, center.y + r * .28f); close()
                }
                drawPath(drill, Color(0xFFBCC5CF))
            }
            drawArc(Color(0xFFE2B74F), 180f, 180f, false,
                Offset(center.x - r * .36f, center.y - r * .9f), Size(r * .72f, r * .5f), style = Stroke(r * .13f))
            drawRect(Color(0xFFD59A38), Offset(center.x - r * .45f, center.y - r * .69f), Size(r * .9f, r * .09f))
        }
        ZombieBehavior.CATAPULT -> if (actionMs > 0L) {
            val release = sin(phase * .4f) * r * .45f
            drawCircle(Color(0xFF3D3440), r * .22f, Offset(center.x + r * .65f, center.y - r * 1.15f - release))
            drawCircle(Color(0xFFCF7440), r * .18f, Offset(center.x + r * .65f, center.y - r * 1.15f - release))
        }
        ZombieBehavior.CRUSH -> {
            drawRoundRect(Color(0xFF59616A), Offset(center.x - r * .92f, center.y - r * .86f),
                Size(r * 1.84f, r * .56f), androidx.compose.ui.geometry.CornerRadius(r * .2f))
            drawCircle(Color(0xFFFFB35B), r * .18f, Offset(center.x + r * .35f, center.y + r * .22f))
        }
        ZombieBehavior.GIANT -> if (type == ZombieKind.GIANT) {
            val swing = if (actionMs > 0) .65f * sin(phase * .6f) else 0f
            val handle = Offset(center.x + r * (.42f - swing), center.y + r * .26f)
            val club = Offset(center.x + r * (1.05f - swing), center.y + r * .95f)
            drawLine(Color(0xFF493A31), handle, club, r * .3f)
            drawLine(Color(0xFF8B633F), handle, club, r * .21f)
            drawLine(Color(0xFFB38352), Offset(handle.x + r * .03f, handle.y + r * .08f),
                Offset(club.x - r * .02f, club.y - r * .08f), r * .035f)
            drawCircle(Color(0xFF594436), r * .08f, Offset(club.x - r * .04f, club.y - r * .08f))
            drawLine(Color(0xFF5B4232), Offset(club.x - r * .18f, club.y - r * .24f),
                Offset(club.x + r * .1f, club.y - r * .32f), r * .045f)
            drawRoundRect(Color(0xFF78634C), Offset(center.x + r * .72f, center.y + r * .68f),
                Size(r * .48f, r * .38f), androidx.compose.ui.geometry.CornerRadius(r * .08f))
        } else {
            drawRoundRect(Color(0xFF59616A), Offset(center.x - r * .85f, center.y + r * .34f),
                Size(r * 1.7f, r * .66f), androidx.compose.ui.geometry.CornerRadius(r * .18f))
            drawCircle(Color(0xFFE35B4F), r * .2f, Offset(center.x + r * .53f, center.y + r * .04f))
            drawCircle(Color(0xFFFFC75C), r * .12f, Offset(center.x - r * .5f, center.y + r * .49f))
        }
        ZombieBehavior.BOSS -> {
            drawRoundRect(Color(0xFF59616A), Offset(center.x - r * 1.2f, center.y + r * .42f),
                Size(r * 2.4f, r * .65f), androidx.compose.ui.geometry.CornerRadius(r * .18f))
            drawCircle(Color(0xFF303A46), r * .28f, Offset(center.x - r * .68f, center.y + r * .99f))
            drawCircle(Color(0xFF303A46), r * .28f, Offset(center.x + r * .68f, center.y + r * .99f))
            drawCircle(Color(0xFFFF624F), r * .12f, Offset(center.x + r * .44f, center.y - r * .15f))
            drawLine(Color(0xFFB8C7D0), Offset(center.x + r * .68f, center.y - r * .62f),
                Offset(center.x + r * .84f, center.y - r * 1.25f), r * .09f)
            drawArc(Color(0xFF718694), 180f, 180f, false,
                Offset(center.x - r * .55f, center.y - r * 1.15f), Size(r * 1.1f, r * .8f),
                style = Stroke(r * .18f))
        }
        ZombieBehavior.EXPLODE -> if (!gearRemoved) {
            val toy = Offset(center.x - r * .48f, center.y - r * .62f)
            drawPath(Path().apply {
                moveTo(toy.x - r * .12f, toy.y + r * .15f); lineTo(toy.x - r * .04f, toy.y - r * .32f)
                lineTo(toy.x + r * .08f, toy.y + r * .13f); lineTo(toy.x + r * .18f, toy.y - r * .28f)
                lineTo(toy.x + r * .28f, toy.y + r * .18f)
            }, Color(0xFFBFC8C8), style = Stroke(r * .08f))
            drawCircle(Color(0xFFEE784B), r * .28f, toy)
            drawPath(Path().apply {
                moveTo(toy.x - r * .2f, toy.y - r * .12f); lineTo(toy.x - r * .38f, toy.y - r * .38f)
                lineTo(toy.x - r * .06f, toy.y - r * .28f); close()
            }, Color(0xFFB74948))
            drawPath(Path().apply {
                moveTo(toy.x + r * .1f, toy.y - r * .26f); lineTo(toy.x + r * .36f, toy.y - r * .45f)
                lineTo(toy.x + r * .26f, toy.y - r * .1f); close()
            }, Color(0xFFB74948))
            drawCircle(Color(0xFFFFE88D), r * .055f, Offset(toy.x - r * .1f, toy.y - r * .04f))
            drawCircle(Color(0xFFFFE88D), r * .055f, Offset(toy.x + r * .1f, toy.y - r * .04f))
            drawRoundRect(Color(0xFFE5B64B), Offset(center.x - r * .83f, center.y + r * .25f),
                Size(r * .58f, r * .45f), androidx.compose.ui.geometry.CornerRadius(r * .08f))
            drawLine(Color(0xFFC24842), Offset(center.x - r * .8f, center.y + r * .48f),
                Offset(center.x - r * .28f, center.y + r * .48f), r * .09f)
        }
        ZombieBehavior.RAGE -> drawLine(Color(0xFFD86D67), Offset(center.x - r * .3f, center.y + r * .02f),
            Offset(center.x + r * .3f, center.y + r * .02f), r * .11f)
        else -> Unit
    }
    when (type) {
        ZombieKind.SIGNAL -> {
            drawLine(Color(0xFF6D5338), Offset(center.x - r * .54f, center.y - r * .52f),
                Offset(center.x - r * .72f, center.y - r * 1.16f), r * .07f)
            val wave = sin(phase * .22f) * r * .1f
            val flag = Path().apply { moveTo(center.x - r * .7f, center.y - r * 1.12f); lineTo(center.x + r * .06f + wave, center.y - r * .96f); lineTo(center.x - r * .65f, center.y - r * .78f); close() }
            drawPath(flag, Color(0xFFE55D54))
            drawCircle(Color(0xFFFFD66D), r * .08f, Offset(center.x - r * .39f, center.y - r * .96f))
        }
        ZombieKind.SHELL -> {
            if (!gearRemoved) {
                val cone = Path().apply {
                    moveTo(center.x - r * .48f, center.y - r * .65f); lineTo(center.x, center.y - r * 1.18f)
                    lineTo(center.x + r * .46f, center.y - r * .65f); close()
                }
                drawPath(cone, Color(0xFFDE7441))
                drawLine(Color(0xFFFFB568), Offset(center.x - r * .25f, center.y - r * .94f),
                    Offset(center.x, center.y - r * 1.12f), r * .07f)
                drawRoundRect(Color(0xFF71563A), Offset(center.x - r * .55f, center.y - r * .7f), Size(r * 1.1f, r * .15f),
                    androidx.compose.ui.geometry.CornerRadius(r * .06f))
            }
        }
        ZombieKind.HEAVY -> {
            if (!gearRemoved) {
                drawRoundRect(Color(0xFF858D94), Offset(center.x - r * .5f, center.y - r * 1.1f), Size(r, r * .45f),
                    androidx.compose.ui.geometry.CornerRadius(r * .12f))
                drawRoundRect(Color(0xFFB5BEC1), Offset(center.x - r * .55f, center.y - r * 1.18f), Size(r * 1.1f, r * .18f),
                    androidx.compose.ui.geometry.CornerRadius(r * .06f))
                drawLine(Color(0xFFDDE2DF), Offset(center.x - r * .27f, center.y - r * 1.06f),
                    Offset(center.x - r * .27f, center.y - r * .75f), r * .045f)
            }
        }
        ZombieKind.SCREEN -> {
            if (!gearRemoved) {
                drawRoundRect(Color(0xFF8A9694), Offset(center.x - r * .67f, center.y - r * .71f), Size(r * 1.34f, r * .83f),
                    androidx.compose.ui.geometry.CornerRadius(r * .08f))
                repeat(4) { i -> drawLine(Color(0xFF526260), Offset(center.x - r * .5f + i * r * .32f, center.y - r * .65f),
                    Offset(center.x - r * .5f + i * r * .32f, center.y + r * .05f), r * .025f) }
                repeat(3) { i -> drawLine(Color(0xFF526260), Offset(center.x - r * .6f, center.y - r * .5f + i * r * .25f),
                    Offset(center.x + r * .6f, center.y - r * .5f + i * r * .25f), r * .025f) }
            }
        }
        ZombieKind.FOOTBALL -> {
            if (!gearRemoved) {
                drawRoundRect(Color(0xFF823B39), Offset(center.x - r * .74f, center.y - r * .16f), Size(r * 1.48f, r * .49f),
                    androidx.compose.ui.geometry.CornerRadius(r * .2f))
                drawLine(Color(0xFFECA17D), Offset(center.x - r * .68f, center.y + r * .02f),
                    Offset(center.x + r * .68f, center.y + r * .02f), r * .055f)
                drawOval(Color(0xFF9B403C), Offset(center.x - r * .67f, center.y - r * .83f), Size(r * 1.34f, r * .62f))
                drawOval(Color(0xFFDD6557), Offset(center.x - r * .56f, center.y - r * .94f), Size(r * 1.12f, r * .49f))
                drawLine(Color(0xFFFFE9D4), Offset(center.x - r * .22f, center.y - r * .78f),
                    Offset(center.x + r * .22f, center.y - r * .78f), r * .055f)
                drawLine(Color(0xFFFFE9D4), Offset(center.x, center.y - r * .9f),
                    Offset(center.x, center.y - r * .66f), r * .045f)
            }
        }
        ZombieKind.NEWSPAPER -> {
            if (!gearRemoved) {
                drawRoundRect(Color(0xFFE9E2CC), Offset(center.x - r * .62f, center.y - r * .04f), Size(r * 1.24f, r * .58f),
                    androidx.compose.ui.geometry.CornerRadius(r * .04f))
                drawLine(Color(0xFF726E61), Offset(center.x - r * .45f, center.y + r * .09f),
                    Offset(center.x + r * .45f, center.y + r * .09f), r * .045f)
                drawLine(Color(0xFF726E61), Offset(center.x - r * .45f, center.y + r * .22f),
                    Offset(center.x + r * .2f, center.y + r * .22f), r * .035f)
                drawCircle(skin, r * .14f, Offset(center.x - r * .67f, center.y + r * .25f))
                drawCircle(skin, r * .14f, Offset(center.x + r * .67f, center.y + r * .25f))
                drawOval(Color(0xFF514A3D), Offset(center.x - r * .43f, center.y - r * .49f), Size(r * .4f, r * .34f),
                    style = Stroke(r * .045f))
                drawOval(Color(0xFF514A3D), Offset(center.x + r * .03f, center.y - r * .49f), Size(r * .4f, r * .34f),
                    style = Stroke(r * .045f))
                drawLine(Color(0xFF514A3D), Offset(center.x - r * .03f, center.y - r * .34f),
                    Offset(center.x + r * .07f, center.y - r * .34f), r * .04f)
            } else {
                drawPath(Path().apply {
                    moveTo(center.x + r * .45f, center.y + r * .13f); lineTo(center.x + r * .82f, center.y - r * .03f)
                    lineTo(center.x + r * .7f, center.y + r * .37f); close()
                }, Color(0xFFE9E2CC))
            }
        }
        ZombieKind.ZAMBONI -> {
            drawRoundRect(Color(0xFF973E3D), Offset(center.x - r * 1.03f, center.y + r * .36f), Size(r * 2.06f, r * .64f),
                androidx.compose.ui.geometry.CornerRadius(r * .12f))
            drawRoundRect(Color(0xFFBD4F46), Offset(center.x - r * .54f, center.y + r * .08f), Size(r * 1.08f, r * .5f),
                androidx.compose.ui.geometry.CornerRadius(r * .08f))
            drawRoundRect(Color(0xFF77858B), Offset(center.x - r * .92f, center.y + r * .42f), Size(r * .9f, r * .29f),
                androidx.compose.ui.geometry.CornerRadius(r * .08f))
            drawPath(Path().apply {
                moveTo(center.x - r * .92f, center.y + r * .7f); lineTo(center.x - r * 1.13f, center.y + r * .98f)
                lineTo(center.x + r * .08f, center.y + r * .98f); lineTo(center.x - r * .06f, center.y + r * .7f); close()
            }, Color(0xFFD3AA57))
            drawCircle(Color(0xFF343E49), r * .2f, Offset(center.x - r * .65f, center.y + r * .96f))
            drawCircle(Color(0xFF343E49), r * .2f, Offset(center.x + r * .64f, center.y + r * .96f))
            drawCircle(Color(0xFF87959A), r * .1f, Offset(center.x - r * .65f, center.y + r * .96f))
            drawCircle(Color(0xFF87959A), r * .1f, Offset(center.x + r * .64f, center.y + r * .96f))
            drawRect(Color(0xFFF0B44B), Offset(center.x - r * .25f, center.y + r * .17f), Size(r * .16f, r * .12f))
            drawRect(Color(0xFFF0B44B), Offset(center.x + r * .09f, center.y + r * .17f), Size(r * .16f, r * .12f))
        }
        ZombieKind.DRIFTER -> Unit
        ZombieKind.DUCK -> drawOval(Color(0xFFFFD15E), Offset(center.x - r * .92f, center.y + r * .2f), Size(r * 1.84f, r * .66f),
            style = Stroke(r * .17f))
        ZombieKind.SNORKEL -> {
            drawLine(Color(0xFF5BC6D4), Offset(center.x + r * .25f, center.y - r * .55f),
                Offset(center.x + r * .8f, center.y - r * 1.18f), r * .15f)
            drawCircle(Color(0xFF5BC6D4), r * .2f, Offset(center.x + r * .8f, center.y - r * 1.18f))
        }
        ZombieKind.BOBSLED -> {
            val sled = Path().apply {
                moveTo(center.x - r * 1.12f, center.y + r * .7f); lineTo(center.x - r * .9f, center.y + r * .46f)
                lineTo(center.x + r * .78f, center.y + r * .46f); lineTo(center.x + r * 1.08f, center.y + r * .7f)
                lineTo(center.x + r * .88f, center.y + r * .9f); lineTo(center.x - r * .88f, center.y + r * .9f); close()
            }
            drawPath(sled, Color(0xFF62B5C5))
            drawLine(Color(0xFFD6F2F1), Offset(center.x - r * .76f, center.y + r * .83f),
                Offset(center.x + r * .75f, center.y + r * .83f), r * .055f)
            repeat(4) { i ->
                val x = center.x - r * .6f + i * r * .4f
                drawOval(Color(0xFF667B4E), Offset(x - r * .2f, center.y + r * .09f), Size(r * .4f, r * .5f))
                drawOval(Color(0xFF9CB36B), Offset(x - r * .17f, center.y + r * .06f), Size(r * .34f, r * .46f))
                drawOval(Color.White, Offset(x - r * .12f, center.y + r * .2f), Size(r * .1f, r * .12f))
                drawOval(Color.White, Offset(x + r * .02f, center.y + r * .2f), Size(r * .1f, r * .12f))
                drawCircle(Color(0xFF273149), r * .035f, Offset(x - r * .06f, center.y + r * .25f))
                drawCircle(Color(0xFF273149), r * .035f, Offset(x + r * .06f, center.y + r * .25f))
            }
        }
        ZombieKind.DOLPHIN -> if (!jumped) {
            val dolphin = Path().apply {
                moveTo(center.x + r * .58f, center.y + r * .22f)
                quadraticTo(center.x + r * .05f, center.y + r * .68f, center.x - r * .58f, center.y + r * .48f)
                quadraticTo(center.x - r * .88f, center.y + r * .38f, center.x - r * 1.25f, center.y + r * .05f)
                quadraticTo(center.x - r * .83f, center.y + r * .08f, center.x - r * .45f, center.y - r * .08f)
                lineTo(center.x - r * .24f, center.y - r * .45f)
                lineTo(center.x + r * .02f, center.y - r * .16f)
                quadraticTo(center.x + r * .4f, center.y - r * .22f, center.x + r * .58f, center.y + r * .22f)
                close()
            }
            drawPath(dolphin, Color(0xFF64B8CB))
            drawLine(Color(0xFFBFEAF0), Offset(center.x - r * .88f, center.y + r * .13f),
                Offset(center.x - r * 1.18f, center.y + r * .08f), r * .045f)
            drawCircle(Color(0xFF214554), r * .055f, Offset(center.x - r * .68f, center.y + r * .13f))
        }
        ZombieKind.DIGGER -> {
            drawPath(Path().apply {
                moveTo(center.x - r * .56f, center.y - r * .56f)
                quadraticTo(center.x - r * .48f, center.y - r * 1.08f, center.x, center.y - r * 1.13f)
                quadraticTo(center.x + r * .5f, center.y - r * 1.07f, center.x + r * .56f, center.y - r * .56f); close()
            }, Color(0xFFB9782E))
            drawPath(Path().apply {
                moveTo(center.x - r * .48f, center.y - r * .59f)
                quadraticTo(center.x - r * .38f, center.y - r * .98f, center.x, center.y - r * 1.02f)
                quadraticTo(center.x + r * .38f, center.y - r * .98f, center.x + r * .48f, center.y - r * .59f); close()
            }, Color(0xFFE8B64A))
            drawLine(Color(0xFFB9782E), Offset(center.x - r * .7f, center.y - r * .57f),
                Offset(center.x + r * .7f, center.y - r * .57f), r * .12f)
            drawCircle(Color(0xFFFFF0A3), r * .1f, Offset(center.x + r * .28f, center.y - r * .73f))
        }
        ZombieKind.POGO -> {
            drawOval(Color(0xFF513D3B), Offset(center.x - r * .55f, center.y - r * 1.12f), Size(r * 1.1f, r * .45f))
            drawOval(Color(0xFFCE4C45), Offset(center.x - r * .48f, center.y - r * 1.08f), Size(r * .96f, r * .38f))
            drawLine(Color(0xFFFFB66B), Offset(center.x - r * .15f, center.y - r * .98f),
                Offset(center.x + r * .32f, center.y - r * .98f), r * .06f)
        }
        ZombieKind.YETI -> {
            for (i in 0..4) {
                val x = center.x - r * .52f + i * r * .26f
                drawPath(Path().apply {
                    moveTo(x, center.y - r * .54f); lineTo(x + r * .13f, center.y - r * .78f)
                    lineTo(x + r * .22f, center.y - r * .52f); close()
                }, Color(0xFFDDF2E9))
            }
            drawOval(Color(0xFFB5D9DD), Offset(center.x - r * .58f, center.y + r * .22f), Size(r * 1.16f, r * .58f))
            drawOval(Color(0xFFDDF2E9), Offset(center.x - r * .52f, center.y + r * .15f), Size(r * 1.04f, r * .48f))
        }
        ZombieKind.BUNGEE -> {
            drawOval(Color(0xFF343943), Offset(center.x - r * .42f, center.y - r * .54f), Size(r * .4f, r * .26f))
            drawOval(Color(0xFF343943), Offset(center.x + r * .02f, center.y - r * .54f), Size(r * .4f, r * .26f))
            drawOval(Color(0xFFBFD0D4), Offset(center.x - r * .34f, center.y - r * .49f), Size(r * .21f, r * .15f))
            drawOval(Color(0xFFBFD0D4), Offset(center.x + r * .09f, center.y - r * .49f), Size(r * .21f, r * .15f))
            drawLine(Color(0xFF292E38), Offset(center.x - r * .66f, center.y + r * .16f),
                Offset(center.x + r * .66f, center.y + r * .16f), r * .14f)
        }
        ZombieKind.LADDER -> if (!gearRemoved) {
            drawLine(Color(0xFFB78E5C), Offset(center.x + r * .65f, center.y - r), Offset(center.x + r * .9f, center.y + r), r * .1f)
            repeat(4) { i -> drawLine(Color(0xFFB78E5C), Offset(center.x + r * .58f, center.y - r * .65f + i * r * .4f),
                Offset(center.x + r * .87f, center.y - r * .65f + i * r * .4f), r * .08f) }
        }
        ZombieKind.IMP -> {
            drawPath(Path().apply {
                moveTo(center.x - r * .43f, center.y - r * .43f); lineTo(center.x - r * .96f, center.y - r * .86f)
                lineTo(center.x - r * .79f, center.y - r * .16f); close()
            }, Color(0xFF8A4B3E))
            drawPath(Path().apply {
                moveTo(center.x + r * .35f, center.y - r * .44f); lineTo(center.x + r * .85f, center.y - r * .94f)
                lineTo(center.x + r * .73f, center.y - r * .14f); close()
            }, Color(0xFF8A4B3E))
            drawCircle(Color(0xFFFFD28B), r * .11f, Offset(center.x - r * .5f, center.y + r * .18f))
        }
        ZombieKind.DASHER -> {
            drawLine(Color(0xFFE4D274), Offset(center.x - r * .88f, center.y - r * .5f),
                Offset(center.x - r * 1.24f, center.y - r * .5f), r * .06f)
            drawLine(Color(0xFFE4D274), Offset(center.x - r * .82f, center.y - r * .26f),
                Offset(center.x - r * 1.12f, center.y - r * .26f), r * .05f)
        }
        ZombieKind.DANCER -> {
            drawOval(Color(0xFF382A46), Offset(center.x - r * .39f, center.y - r * .85f), Size(r * .78f, r * .2f))
            drawArc(Color(0xFFB7A0D8), 195f, 150f, false,
                Offset(center.x - r * .42f, center.y - r * 1.02f), Size(r * .84f, r * .4f), style = Stroke(r * .12f))
            drawCircle(Color(0xFFFFD66D), r * .1f, Offset(center.x + r * .45f, center.y + r * .18f))
            if (actionMs > 0) {
                val sway = sin(phase * .4f) * r * .34f
                drawLine(Color(0xFF8FAE76), Offset(center.x - r * .47f, center.y + r * .22f),
                    Offset(center.x - r * .8f, center.y - r * .1f + sway), r * .09f)
                drawLine(Color(0xFF8FAE76), Offset(center.x + r * .47f, center.y + r * .22f),
                    Offset(center.x + r * .8f, center.y - r * .1f - sway), r * .09f)
            }
        }
        ZombieKind.BACKUP -> drawPath(Path().apply {
            moveTo(center.x - r * .28f, center.y + r * .18f); lineTo(center.x, center.y + r * .38f)
            lineTo(center.x + r * .28f, center.y + r * .18f); lineTo(center.x, center.y + r * .56f); close()
        }, Color(0xFFFFD66D))
        ZombieKind.JACK -> if (!gearRemoved) {
            drawRoundRect(Color(0xFFE5B64B), Offset(center.x - r * .78f, center.y + r * .25f),
                Size(r * .5f, r * .49f), androidx.compose.ui.geometry.CornerRadius(r * .08f))
            drawLine(Color(0xFFC24842), Offset(center.x - r * .77f, center.y + r * .49f),
                Offset(center.x - r * .29f, center.y + r * .49f), r * .1f)
            drawLine(Color(0xFF9FA6AA), Offset(center.x - r * .52f, center.y + r * .25f),
                Offset(center.x - r * .72f, center.y + r * .04f), r * .06f)
        }
        ZombieKind.CATAPULT -> {
            drawCircle(Color(0xFF303A46), r * .23f, Offset(center.x - r * .58f, center.y + r * .88f))
            drawCircle(Color(0xFF303A46), r * .23f, Offset(center.x + r * .62f, center.y + r * .88f))
            drawCircle(Color(0xFF8C999A), r * .1f, Offset(center.x - r * .58f, center.y + r * .88f))
            drawCircle(Color(0xFF8C999A), r * .1f, Offset(center.x + r * .62f, center.y + r * .88f))
            drawRoundRect(Color(0xFF62462E), Offset(center.x - r * .72f, center.y + r * .42f),
                Size(r * 1.55f, r * .38f), androidx.compose.ui.geometry.CornerRadius(r * .07f))
            drawLine(Color(0xFFC79A5E), Offset(center.x - r * .7f, center.y + r * .46f),
                Offset(center.x - r * .76f, center.y + r * .1f), r * .09f)
            drawLine(Color(0xFFC79A5E), Offset(center.x + r * .67f, center.y + r * .44f),
                Offset(center.x + r * .7f, center.y + r * .07f), r * .09f)
            drawRoundRect(Color(0xFF956A43), Offset(center.x + r * .24f, center.y - r * .25f),
                Size(r * .58f, r * .36f), androidx.compose.ui.geometry.CornerRadius(r * .08f))
            drawLine(Color(0xFFD3A45D), Offset(center.x + r * .46f, center.y - r * .18f),
                Offset(center.x + r * .8f, center.y - r * 1.15f), r * .13f)
            drawLine(Color(0xFF6A4D38), Offset(center.x + r * .48f, center.y - r * .18f),
                Offset(center.x + r * .81f, center.y - r * 1.15f), r * .035f)
        }
        ZombieKind.GIANT -> drawCircle(Color(0xFF875D43), r * .13f, Offset(center.x - r * .55f, center.y - r * .67f))
        ZombieKind.BOSS -> {
            drawArc(Color(0xFF798D98), 185f, 170f, false, Offset(center.x - r * .67f, center.y - r * 1.04f),
                Size(r * 1.34f, r * .72f), style = Stroke(r * .2f))
            drawRoundRect(Color(0xFF586874), Offset(center.x - r * .72f, center.y - r * .82f),
                Size(r * 1.44f, r * .31f), androidx.compose.ui.geometry.CornerRadius(r * .08f))
            drawOval(Color(0xFFFF4E45), Offset(center.x - r * .43f, center.y - r * .4f), Size(r * .3f, r * .24f))
            drawOval(Color(0xFFFF4E45), Offset(center.x + r * .12f, center.y - r * .4f), Size(r * .3f, r * .24f))
            drawLine(Color(0xFF6B7781), Offset(center.x - r * .38f, center.y + r * .2f),
                Offset(center.x + r * .38f, center.y + r * .2f), r * .12f)
            drawLine(Color(0xFFFFB84D), Offset(center.x - r * .55f, center.y + r * .6f),
                Offset(center.x - r * .72f, center.y + r * 1.15f), r * .07f)
            drawLine(Color(0xFFFFB84D), Offset(center.x + r * .55f, center.y + r * .6f),
                Offset(center.x + r * .72f, center.y + r * 1.15f), r * .07f)
        }
        ZombieKind.VAULT, ZombieKind.BALLOON -> Unit
    }
    if (gearRemoved && gearLostMs > 0L && type != ZombieKind.NEWSPAPER) {
        val progress = (1f - gearLostMs / 650f).coerceIn(0f, 1f)
        val gearCenter = Offset(center.x + r * (.62f + progress * .7f),
            center.y - r * (1.15f + sin(progress * Math.PI.toFloat()) * .75f))
        when (type) {
            ZombieKind.SHELL -> drawPath(Path().apply {
                moveTo(gearCenter.x, gearCenter.y - r * .48f); lineTo(gearCenter.x - r * .32f, gearCenter.y + r * .28f)
                lineTo(gearCenter.x + r * .32f, gearCenter.y + r * .28f); close()
            }, Color(0xFFFF9A35))
            ZombieKind.HEAVY -> {
                drawRoundRect(Color(0xFFB5BEC1), Offset(gearCenter.x - r * .35f, gearCenter.y - r * .2f),
                    Size(r * .7f, r * .48f), androidx.compose.ui.geometry.CornerRadius(r * .1f))
                drawLine(Color(0xFFDDE2DF), Offset(gearCenter.x - r * .4f, gearCenter.y - r * .23f),
                    Offset(gearCenter.x + r * .4f, gearCenter.y - r * .23f), r * .08f)
            }
            ZombieKind.FOOTBALL -> drawOval(Color(0xFFDD6557),
                Offset(gearCenter.x - r * .42f, gearCenter.y - r * .25f), Size(r * .84f, r * .5f))
            ZombieKind.SCREEN -> drawRoundRect(Color(0xFF8A9694),
                Offset(gearCenter.x - r * .4f, gearCenter.y - r * .42f), Size(r * .8f, r * .84f),
                androidx.compose.ui.geometry.CornerRadius(r * .06f))
            ZombieKind.LADDER -> {
                drawLine(Color(0xFFB78E5C), Offset(gearCenter.x - r * .25f, gearCenter.y - r * .45f),
                    Offset(gearCenter.x - r * .2f, gearCenter.y + r * .45f), r * .07f)
                drawLine(Color(0xFFB78E5C), Offset(gearCenter.x + r * .2f, gearCenter.y - r * .45f),
                    Offset(gearCenter.x + r * .25f, gearCenter.y + r * .45f), r * .07f)
                repeat(3) { i -> drawLine(Color(0xFFE0BD83),
                    Offset(gearCenter.x - r * .2f, gearCenter.y - r * .25f + i * r * .25f),
                    Offset(gearCenter.x + r * .23f, gearCenter.y - r * .23f + i * r * .25f), r * .05f) }
            }
            ZombieKind.DIGGER -> drawLine(Color(0xFFBCC5CF), Offset(gearCenter.x - r * .35f, gearCenter.y + r * .3f),
                Offset(gearCenter.x + r * .35f, gearCenter.y - r * .3f), r * .1f)
            ZombieKind.JACK -> drawRoundRect(Color(0xFFE5B64B),
                Offset(gearCenter.x - r * .3f, gearCenter.y - r * .3f), Size(r * .6f, r * .6f),
                androidx.compose.ui.geometry.CornerRadius(r * .08f))
            ZombieKind.POGO -> drawLine(Color(0xFFCBD5DC), Offset(gearCenter.x, gearCenter.y - r * .4f),
                Offset(gearCenter.x, gearCenter.y + r * .4f), r * .12f)
            else -> Unit
        }
    }
    if (retreating) {
        val drift = kotlin.math.abs(sin(phase * .25f))
        drawLine(Color(0xFFB9DDE4).copy(alpha = .55f), Offset(center.x - r * .75f, center.y + r * .65f),
            Offset(center.x - r * (1.35f + drift * .35f), center.y + r * .65f), r * .07f)
        drawLine(Color(0xFFB9DDE4).copy(alpha = .35f), Offset(center.x - r * .55f, center.y + r * .86f),
            Offset(center.x - r * 1.05f, center.y + r * .86f), r * .05f)
    }
    if (armorRemaining > 0 && type !in setOf(ZombieKind.SHELL, ZombieKind.HEAVY,
        ZombieKind.SCREEN, ZombieKind.FOOTBALL, ZombieKind.LADDER))
        drawArc(Color(0xFFF3C95F), 195f, 150f, false,
            Offset(center.x - r * .9f, center.y - r * .9f), Size(r * 1.8f, r * 1.8f), style = Stroke(r * .12f))
}
