package com.game.maths

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ReplacementSpan
import android.text.style.LineHeightSpan
import android.widget.TextView
import android.icu.text.Transliterator
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView

/** When off, the original Material3 composable is used with identical parameters. */
internal val LocalPinyinEnabled=compositionLocalOf {false}

internal object PinyinReadings {
    fun containsHan(value:String)=value.any {it in '\u3400'..'\u9fff'}
    /** ICU Han-Latin is supported on Android 10+. Never download dictionaries at runtime. */
    private val engine: Transliterator? by lazy {
        if(Build.VERSION.SDK_INT<29) null
        else runCatching {
            val ids=Transliterator.getAvailableIDs().toList()
            val id=when {
                "Han-Latin" in ids -> "Han-Latin"
                "Any-Latin" in ids -> "Any-Latin"
                else -> return@runCatching null
            }
            Transliterator.getInstance(id)
        }.getOrNull()
    }

    // Context-specific fixes for common polyphonic characters in mathematics/game screens.
    private val phrases=mapOf(
        "音乐" to listOf("yīn","yuè"), "快乐" to listOf("kuài","lè"),
        "重试" to listOf("chóng","shì"),"重复" to listOf("chóng","fù"),
        "长方形" to listOf("cháng","fāng","xíng"),
        "长短" to listOf("cháng","duǎn"),"长度" to listOf("cháng","dù"),
        "银行" to listOf("yín","háng"),"行走" to listOf("xíng","zǒu"),
        "行程" to listOf("xíng","chéng"),"几个" to listOf("jǐ","gè"),
        "得分" to listOf("dé","fēn"),"得到" to listOf("dé","dào"),
        "正方形" to listOf("zhèng","fāng","xíng"),
        "计算" to listOf("jì","suàn"),"数一数" to listOf("shǔ","yì","shǔ"),
        "数学" to listOf("shù","xué"),"数字" to listOf("shù","zì"),
        "数量" to listOf("shù","liàng"),"数学魔法" to listOf("shù","xué","mó","fǎ"),
        "数值" to listOf("shù","zhí"),"数字范围" to listOf("shù","zì","fàn","wéi")
    )

    fun forText(text:String): List<Pair<Int,String>> {
        val translit=engine ?: return emptyList()
        val result=mutableListOf<Pair<Int,String>>()
        var i=0
        while(i<text.length) {
            if(text[i] !in '\u3400'..'\u9fff') {i++;continue}
            val start=i
            while(i<text.length && text[i] in '\u3400'..'\u9fff') i++
            val sequence=text.substring(start,i)
            val words=synchronized(translit) {
                translit.transliterate(sequence).trim().split(Regex("\\s+"))
            }
            val syllables=if(words.size==sequence.length) words.toMutableList()
                else sequence.map { letter ->
                    synchronized(translit) {translit.transliterate(letter.toString()).trim()}
                }.toMutableList()
            // Apply longest matching phrase overrides on contiguous runs.
            phrases.entries.sortedByDescending {it.key.length}.forEach { (phrase, reading) ->
                var pos=sequence.indexOf(phrase)
                while(pos>=0) {
                    for (j in reading.indices) syllables[pos+j]=reading[j]
                    pos=sequence.indexOf(phrase,pos+phrase.length)
                }
            }
            syllables.forEachIndexed {offset,pronunciation ->
                if(pronunciation.isNotBlank() && pronunciation.none {it in '\u3400'..'\u9fff'})
                    result += start+offset to pronunciation
            }
        }
        return result
    }
}

/** Reserve ruby space for the ENTIRE line, including numerals and punctuation.
 * Inflating FontMetrics only inside the Han ReplacementSpan made digits appear
 * lower: Android measured the annotated and plain runs with different metrics.
 * Every run now uses the same line box and shares an identical baseline.
 */
private class PinyinLineHeightSpan(private val reservePx:Int):LineHeightSpan {
    override fun chooseHeight(text:CharSequence,start:Int,end:Int,
        spanstartv:Int,v:Int,fm:Paint.FontMetricsInt) {
        fm.ascent -= reservePx
        fm.top -= reservePx
    }
}

private class PinyinSpan(
    private val ruby: String,
    private val rubySizePx: Float
): ReplacementSpan() {
    override fun getSize(paint: Paint, text: CharSequence, start: Int,
        end: Int, fm: Paint.FontMetricsInt?): Int {
        // Crucial: DO NOT widen Han glyphs to the full syllable width.
        // Widening every Han is what caused all UI text to become spaced apart.
        return kotlin.math.ceil(paint.measureText(text, start, end).toDouble())
            .toInt().coerceAtLeast(1)
    }

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int,
        x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val oldSize = paint.textSize
        val oldScale = paint.textScaleX
        val oldColor = paint.color
        val oldAscent = paint.fontMetrics.ascent
        val han = text.subSequence(start, end).toString()
        val hanWidth = paint.measureText(han)
        val advance = getSize(paint,text,start,end,null).toFloat()
        val centerX = x + advance / 2f
        canvas.drawText(han, centerX-hanWidth/2f, y.toFloat(), paint)

        paint.textSize=rubySizePx
        paint.textScaleX=1f
        val readingWidth=paint.measureText(ruby)
        // One glyph keeps one glyph's layout width; a long syllable may
        // be horizontally condensed but never moves the neighboring Han.
        paint.textScaleX = PinyinGlyphLayout.scaleFor(readingWidth,advance)
        val renderedWidth=paint.measureText(ruby)
        canvas.drawText(ruby,centerX-renderedWidth/2f,
            y+oldAscent-oldSize*.04f,paint)
        paint.textSize=oldSize
        paint.textScaleX=oldScale
        paint.color=oldColor
    }
}

/** One glyph + one tone-marked pinyin annotation. Android TextView manages natural line wraps. */
private fun rubyText(source:String,reservePx:Int,rubySizePx:Float):CharSequence {
    val output=SpannableStringBuilder(source)
    if(source.isNotEmpty()) {
        output.setSpan(PinyinLineHeightSpan(reservePx),0,source.length,
            Spanned.SPAN_INCLUSIVE_INCLUSIVE)
    }
    PinyinReadings.forText(source).forEach { (index,ruby) ->
        output.setSpan(PinyinSpan(ruby,rubySizePx),index,index+1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return output
}

@Composable
internal fun PinyinText(
    text:String,
    modifier:Modifier=Modifier,
    color:Color=Color.Unspecified,
    fontSize:TextUnit=TextUnit.Unspecified,
    fontStyle:FontStyle?=null,
    fontWeight:FontWeight?=null,
    fontFamily:FontFamily?=null,
    letterSpacing:TextUnit=TextUnit.Unspecified,
    textDecoration:TextDecoration?=null,
    textAlign:TextAlign?=null,
    lineHeight:TextUnit=TextUnit.Unspecified,
    overflow:TextOverflow=TextOverflow.Clip,
    softWrap:Boolean=true,
    maxLines:Int=Int.MAX_VALUE,
    minLines:Int=1,
    onTextLayout:((TextLayoutResult)->Unit)?=null,
    style:TextStyle=androidx.compose.material3.LocalTextStyle.current
) {
    val enabled=LocalPinyinEnabled.current
    val context=LocalContext.current
    val density=LocalDensity.current
    val resolved=if(color!=Color.Unspecified) color
        else if(style.color!=Color.Unspecified)style.color else LocalContentColor.current
    val resolvedSize=if(fontSize.isSpecified) fontSize
        else if(style.fontSize.isSpecified) style.fontSize else 16.sp
    val textPx=with(density) {resolvedSize.toPx()}
    // Opt-in does not affect any existing layout when disabled.
    if(!enabled || !PinyinReadings.containsHan(text) || Build.VERSION.SDK_INT<29 ||
        PinyinReadings.forText(text).isEmpty()) {
        MaterialText(text=text,modifier=modifier,color=color,fontSize=fontSize,
            fontStyle=fontStyle,fontWeight=fontWeight,fontFamily=fontFamily,
            letterSpacing=letterSpacing,textDecoration=textDecoration,textAlign=textAlign,
            lineHeight=lineHeight,overflow=overflow,softWrap=softWrap,
            maxLines=maxLines,minLines=minLines,onTextLayout=onTextLayout,style=style)
        return
    }
    // A single shared line metric positions digits and Han on exactly the same
    // baseline; only the annotation is drawn above the normal glyph baseline.
    val rubySizePx=with(density) {
        PinyinGlyphLayout.annotationSizePx(textPx, 7.sp.toPx(), 10.sp.toPx())
    }
    val rubyReserve=kotlin.math.ceil(rubySizePx*1.3f).toInt()
    val ruby=remember(text,rubyReserve,rubySizePx) {
        rubyText(text,rubyReserve,rubySizePx)
    }
    AndroidView(
        factory={ ctx -> TextView(ctx).apply {
            includeFontPadding=false
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }},
        modifier=modifier,
        update={view ->
            view.text=ruby
            view.setTextColor(resolved.toArgb())
            view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,textPx)
            val bold=(fontWeight?.weight ?: style.fontWeight?.weight ?: 400)>=600
            val italic=(fontStyle ?: style.fontStyle)==FontStyle.Italic
            view.setTypeface(null,(if(bold)android.graphics.Typeface.BOLD else 0) or
                (if(italic)android.graphics.Typeface.ITALIC else 0))
            view.setSingleLine(!softWrap)
            view.maxLines=maxLines.coerceAtLeast(1)
            view.minLines=minLines.coerceAtLeast(1)
            view.ellipsize=if(overflow==TextOverflow.Ellipsis)TextUtils.TruncateAt.END else null
            // Both the TextView line box and its glyphs need centering.
            // textAlignment alone only centers horizontally; gravity also
            // centers ruby + Han vertically inside fixed-height buttons.
            view.gravity=android.view.Gravity.CENTER_VERTICAL or when(textAlign) {
                TextAlign.Center -> android.view.Gravity.CENTER_HORIZONTAL
                TextAlign.End, TextAlign.Right -> android.view.Gravity.END
                else -> android.view.Gravity.START
            }
            view.textAlignment=when(textAlign) {
                TextAlign.Center -> android.view.View.TEXT_ALIGNMENT_CENTER
                TextAlign.End, TextAlign.Right -> android.view.View.TEXT_ALIGNMENT_VIEW_END
                else -> android.view.View.TEXT_ALIGNMENT_VIEW_START
            }
            if(lineHeight.isSpecified) {
                val linePx=with(density) {lineHeight.toPx()}
                view.setLineSpacing((linePx-textPx*1.3f).coerceAtLeast(0f),1f)
            } else view.setLineSpacing(0f,1f)
        }
    )
}
