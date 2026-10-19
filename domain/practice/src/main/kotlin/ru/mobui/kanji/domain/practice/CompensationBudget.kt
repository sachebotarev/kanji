package ru.mobui.kanji.domain.practice

/** Additive geometric effort: maximum movement at each stage / reference longest side * 100. */
data class CompensationBudget(
    val translationPercent: Float = 0f,
    val sizePercent: Float = 0f,
    val stretchPercent: Float = 0f,
    val rotationPercent: Float = 0f,
    val jitterPercent: Float = 0f,
    val limitPercent: Float? = null,
    val shearPercent: Float = 0f,
) {
    val totalPercent: Float get() = translationPercent + sizePercent + stretchPercent + rotationPercent + jitterPercent + shearPercent
    val hasError: Boolean get() = limitPercent != null && totalPercent > limitPercent + .0001f
}
