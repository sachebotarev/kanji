package ru.mobui.kanji.checker.shape.compensation

import ru.mobui.kanji.checker.strokes.geometry.ShapePoint

import kotlin.math.*

/** Uses one fixed reference size; costs cannot cancel and are independent of pointer sampling rate. */
internal class CompensationCostCalculator(reference: List<List<ShapePoint>>) {
    private val side: Float = reference.flatten().let { points ->
        if (points.isEmpty()) 0f else max(points.maxOf { it.x } - points.minOf { it.x }, points.maxOf { it.y } - points.minOf { it.y })
    }

    fun movement(before: List<List<ShapePoint>>, after: List<List<ShapePoint>>): Float {
        var distance = 0.0
        for ((a, b) in before.zip(after)) for ((p, q) in a.zip(b)) {
            distance = max(distance, hypot((q.x - p.x).toDouble(), (q.y - p.y).toDouble()))
        }
        return percent(distance)
    }

    /** For removed/simplified points, measure distance to the surviving part of the same stroke. */
    fun removal(before: List<List<ShapePoint>>, after: List<List<ShapePoint>>): Float {
        var distance = 0.0
        for ((original, retained) in before.zip(after)) for (p in original) {
            var nearest = Double.POSITIVE_INFINITY
            if (retained.size == 1) nearest = hypot((p.x - retained[0].x).toDouble(), (p.y - retained[0].y).toDouble())
            for ((a, b) in retained.zipWithNext()) {
                val dx = (b.x - a.x).toDouble(); val dy = (b.y - a.y).toDouble()
                val squared = dx * dx + dy * dy
                val t = if (squared == 0.0) 0.0 else (((p.x - a.x) * dx + (p.y - a.y) * dy) / squared).coerceIn(0.0, 1.0)
                nearest = min(nearest, hypot(p.x - a.x - t * dx, p.y - a.y - t * dy))
            }
            if (nearest.isFinite()) distance = max(distance, nearest)
        }
        return percent(distance)
    }

    private fun percent(distance: Double): Float = if (side > 0f) (distance / side * 100).coerceAtMost(Float.MAX_VALUE.toDouble() / 8).toFloat() else 0f
}
