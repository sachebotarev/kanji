package ru.mobui.kanji.domain.practice

import kotlin.math.abs
import kotlin.math.max

/** Signed correction in percent of the input field width/height; positive means right/down. */
data class TranslationCompensation(
    val shiftXPercent: Float = 0f,
    val shiftYPercent: Float = 0f,
    val limitPercent: Float = 100f,
) {
    init {
        require(shiftXPercent.isFinite() && shiftYPercent.isFinite())
        require(limitPercent.isFinite() && limitPercent in 0f..100f)
    }

    val maxShiftPercent: Float get() = max(abs(shiftXPercent), abs(shiftYPercent))
    // Ignore float rounding at the inclusive boundary (e.g. .3f - .2f).
    val hasError: Boolean get() = maxShiftPercent > limitPercent + 0.00001f
    val isApplied: Boolean get() = shiftXPercent != 0f || shiftYPercent != 0f
}
