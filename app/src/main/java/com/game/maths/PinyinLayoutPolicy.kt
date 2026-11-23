package com.game.maths

/**
 * Shared screen-size rules for ruby-annotated controls.
 * AndroidView/TextView ruby has a larger line box than Compose Text:
 * this minimum applies equally to the portrait phone and tablet's compact panel.
 */
internal object PinyinLayoutPolicy {
    const val MIN_ANNOTATED_BUTTON_HEIGHT_DP = 82f

    fun buttonHeightDp(originalHeightDp: Float, annotated: Boolean): Float =
        if (annotated) maxOf(originalHeightDp, MIN_ANNOTATED_BUTTON_HEIGHT_DP)
        else originalHeightDp

    /** Labels with ruby should wrap inside their own taller button, never truncate. */
    fun mainButtonHeightDp(originalHeightDp:Float,annotated:Boolean,labelLength:Int):Float =
        if(annotated && labelLength>8) maxOf(originalHeightDp,104f)
        else buttonHeightDp(originalHeightDp,annotated)

    fun mainButtonLines(annotated:Boolean,labelLength:Int):Int =
        if(annotated && labelLength>8) 2 else 1

    /** Split page headings away from a return/settings pill on small screens. */
    fun useStackedPageHeading(pinyinEnabled:Boolean,availableWidthDp:Float):Boolean =
        pinyinEnabled && availableWidthDp < 460f

    /** Keep mobile title legible but avoid ruby glyphs fighting the settings pill. */
    fun pageTitleSp(originalSp:Float,pinyinEnabled:Boolean):Float =
        if(pinyinEnabled) minOf(originalSp,21f) else originalSp

    fun useStackedProgressHeader(pinyinEnabled: Boolean, wideLayout: Boolean): Boolean =
        pinyinEnabled && !wideLayout

    fun useStackedAnswerHeading(pinyinEnabled: Boolean, spacious: Boolean): Boolean =
        pinyinEnabled && !spacious

    /** Audio output labels need full-width buttons in narrow pinyin layouts.
     * Without pinyin, retain the original side-by-side settings choices.
     */
    fun stackVoiceModeOptions(pinyinEnabled: Boolean, availableWidthDp: Float): Boolean =
        pinyinEnabled && availableWidthDp < 440f
}
