package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.domain.practice.StretchCompensation

internal data class StretchedShape(
    val strokes: List<List<ShapePoint>>,
    val result: StretchCompensation,
)

/** Fits width/height independently; Cascade keeps the upper-left corner, OPA keeps the box center. */
internal class StretchCompensator(
    private val horizontalLimitPercent: Float,
    private val verticalLimitPercent: Float,
    private val preserveCenter: Boolean = false,
) {
    init {
        require(horizontalLimitPercent.isFinite() && horizontalLimitPercent >= 0f)
        require(verticalLimitPercent.isFinite() && verticalLimitPercent >= 0f)
    }

    fun compensate(strokes: List<List<ShapePoint>>, reference: List<List<ShapePoint>>): StretchedShape {
        val points = strokes.flatten()
        val target = reference.flatten()
        fun undefined() = StretchedShape(strokes, StretchCompensation(
            horizontalLimitPercent = horizontalLimitPercent, verticalLimitPercent = verticalLimitPercent, isDefined = false,
        ))
        if (points.isEmpty() || target.isEmpty()) return undefined()
        val left = points.minOf { it.x }; val top = points.minOf { it.y }
        val width = points.maxOf { it.x } - left; val height = points.maxOf { it.y } - top
        val targetWidth = target.maxOf { it.x } - target.minOf { it.x }
        val targetHeight = target.maxOf { it.y } - target.minOf { it.y }
        fun factor(actual: Float, expected: Float): Float? = when {
            actual == 0f && expected == 0f -> 1f
            actual <= 0f || expected <= 0f -> null
            else -> (expected / actual).takeIf { it.isFinite() && it > 0f }
        }
        val x = factor(width, targetWidth) ?: return undefined()
        val y = factor(height, targetHeight) ?: return undefined()
        val result = StretchCompensation(x, y, horizontalLimitPercent, verticalLimitPercent)
        val pivotX = if (preserveCenter) left + width / 2 else left
        val pivotY = if (preserveCenter) top + height / 2 else top
        return StretchedShape(strokes.map { stroke -> stroke.map { p ->
            ShapePoint(pivotX + (p.x - pivotX) * x, pivotY + (p.y - pivotY) * y)
        } }, result)
    }
}
