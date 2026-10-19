package ru.mobui.kanji.domain.practice

import kotlin.math.abs

/** Independent corrective factors measured after uniform size compensation. */
data class StretchCompensation(
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val horizontalLimitPercent: Float = 15f,
    val verticalLimitPercent: Float = 15f,
    val isDefined: Boolean = true,
) {
    init {
        require(scaleX.isFinite() && scaleX > 0f && scaleY.isFinite() && scaleY > 0f)
        require(horizontalLimitPercent.isFinite() && horizontalLimitPercent >= 0f)
        require(verticalLimitPercent.isFinite() && verticalLimitPercent >= 0f)
    }
    val changeXPercent: Float get() = (scaleX - 1f) * 100f
    val changeYPercent: Float get() = (scaleY - 1f) * 100f
    val hasError: Boolean get() = !isDefined || abs(changeXPercent) > horizontalLimitPercent + .0001f ||
        abs(changeYPercent) > verticalLimitPercent + .0001f
    val isApplied: Boolean get() = isDefined && (scaleX != 1f || scaleY != 1f)
}
