package ru.mobui.kanji.domain.practice

import kotlin.math.abs

/** Horizontal shear x' = x + factor * (y - top), followed by retaining the aligned left edge. */
data class ShearCompensation(
    val factor: Float = 0f,
    val degrees: Float = 0f,
    val limitDegrees: Float? = null,
    val isEstimated: Boolean = false,
) {
    val hasError: Boolean get() = limitDegrees != null && abs(degrees) > limitDegrees + .0001f
    val isApplied: Boolean get() = isEstimated && factor != 0f
}
