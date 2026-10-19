package ru.mobui.kanji.domain.practice

import kotlin.math.abs

/** One corrective factor for both axes. A null limit keeps size compensation unrestricted. */
data class SizeCompensation(
    val scaleFactor: Float = 1f,
    val limitPercent: Float? = null,
    val isDefined: Boolean = true,
) {
    init {
        require(scaleFactor.isFinite() && scaleFactor > 0f)
        require(limitPercent == null || (limitPercent.isFinite() && limitPercent >= 0f))
    }

    /** Positive means stretching, negative means shrinking, relative to the drawn size. */
    val changePercent: Float get() = (scaleFactor - 1f) * 100f
    val hasError: Boolean get() = !isDefined || (limitPercent != null && abs(changePercent) > limitPercent + 0.0001f)
    val isApplied: Boolean get() = isDefined && scaleFactor != 1f
}
