package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.domain.practice.RotationPivot
import ru.mobui.kanji.domain.practice.RotationCompensation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class RotatedShape(
    val strokes: List<List<ShapePoint>>,
    val result: RotationCompensation,
)

/** Bounded rotations around the glyph center and all four bounding-box corners. */
internal class RotationCompensator(val maxDegrees: Float) {
    init { require(maxDegrees.isFinite() && maxDegrees in 0f..20f) }

    fun candidates(strokes: List<List<ShapePoint>>): List<RotatedShape> {
        val original = RotatedShape(strokes, RotationCompensation(limitDegrees = maxDegrees))
        val points = strokes.flatten()
        if (points.isEmpty()) return listOf(original)
        val left = points.minOf { it.x }; val top = points.minOf { it.y }
        val right = points.maxOf { it.x }; val bottom = points.maxOf { it.y }
        val pivots = listOf(
            RotationPivot.Center to ShapePoint((left + right) / 2, (top + bottom) / 2),
            RotationPivot.TopLeft to ShapePoint(left, top),
            RotationPivot.TopRight to ShapePoint(right, top),
            RotationPivot.BottomLeft to ShapePoint(left, bottom),
            RotationPivot.BottomRight to ShapePoint(right, bottom),
        )
        return buildList {
            add(original)
            val angles = linkedSetOf(-maxDegrees / 2, maxDegrees / 2, -maxDegrees, maxDegrees)
            // Search outside the acceptance limit so excessive required rotation is reported.
            for (degree in 1..45) { angles.add(-degree.toFloat()); angles.add(degree.toFloat()) }
            for (angle in angles.filter { it != 0f }) {
                for ((kind, pivot) in pivots) add(RotatedShape(
                    compensate(strokes, angle, pivot), RotationCompensation(angle, kind, maxDegrees),
                ))
            }
        }
    }

    fun compensate(strokes: List<List<ShapePoint>>, degrees: Float, pivot: ShapePoint): List<List<ShapePoint>> {
        if (degrees == 0f) return strokes
        val radians = degrees * PI.toFloat() / 180f
        val c = cos(radians); val s = sin(radians)
        return strokes.map { stroke -> stroke.map { p ->
            val x = p.x - pivot.x; val y = p.y - pivot.y
            ShapePoint(pivot.x + x * c - y * s, pivot.y + x * s + y * c)
        } }
    }
}
