package com.game.maths

/** Deterministic typographic rules, independent of Android's TextView.
 * Han advances are not widened to the longest pinyin reading.
 * The reading is instead condensed to fit the original character box.
 */
internal object PinyinGlyphLayout {
    fun annotationSizePx(baseTextPx: Float, minimumPx: Float, maximumPx: Float): Float {
        require(baseTextPx > 0f && minimumPx > 0f && maximumPx >= minimumPx)
        return (baseTextPx * .42f).coerceIn(minimumPx, maximumPx)
    }

    fun scaleFor(readingWidthPx: Float, hanAdvancePx: Float): Float {
        if (readingWidthPx <= 0f || hanAdvancePx <= 0f) return 1f
        // Leave a little visual separation between adjacent readings: long
        // syllables should not merge into a single Latin word across Han cells.
        return (hanAdvancePx * .90f / readingWidthPx).coerceAtMost(1f)
    }
}
