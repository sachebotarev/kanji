package ru.mobui.kanji.checker.shape.coverage

import ru.mobui.kanji.checker.strokes.geometry.ShapePoint

import kotlin.math.*

/** Arc-length sampling of corresponding strokes; another stroke cannot supply a missing segment. */
internal class StrokeCoverageCalculator(private val samples: Int = 48) {
    init { require(samples >= 2) }
    data class Coverage(val inputPercent: Float, val referencePercent: Float)

    fun compare(input: List<List<ShapePoint>>, reference: List<List<ShapePoint>>, assignment: List<Int>?, tolerance: Float): Coverage {
        if (assignment == null || assignment.isEmpty()) return Coverage(0f, 0f)
        var actual = 0f; var expected = 0f
        for ((i, j) in assignment.withIndex()) {
            actual += coverage(input[i], reference[j], tolerance)
            expected += coverage(reference[j], input[i], tolerance)
        }
        return Coverage(actual / assignment.size, expected / assignment.size)
    }

    private fun coverage(from: List<ShapePoint>, to: List<ShapePoint>, tolerance: Float): Float {
        if (from.isEmpty() || to.isEmpty()) return 0f
        val lengths = FloatArray(from.size)
        for (i in 1..from.lastIndex) lengths[i] = lengths[i - 1] + hypot(from[i].x - from[i - 1].x, from[i].y - from[i - 1].y)
        val length = lengths.last()
        val count = if (length <= 0f) 1 else samples + 1
        var segment = 1
        var hits = 0
        val squaredTolerance = tolerance * tolerance
        for (i in 0 until count) {
            val point = if (count == 1) from.first() else {
                val target = length * i / samples
                while (segment < from.lastIndex && lengths[segment] < target) segment++
                val a = from[segment - 1]; val b = from[segment]
                val span = lengths[segment] - lengths[segment - 1]
                val t = if (span > 0f) (target - lengths[segment - 1]) / span else 0f
                ShapePoint(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
            }
            if (distanceSquared(point, to) <= squaredTolerance + .000000001f) hits++
        }
        return hits * 100f / count
    }

    private fun distanceSquared(point: ShapePoint, stroke: List<ShapePoint>): Float {
        var best = (point.x - stroke[0].x).pow(2) + (point.y - stroke[0].y).pow(2)
        for (i in 1..stroke.lastIndex) {
            val a = stroke[i - 1]; val b = stroke[i]
            val dx = b.x - a.x; val dy = b.y - a.y
            val norm = dx * dx + dy * dy
            val t = if (norm > 0f) (((point.x - a.x) * dx + (point.y - a.y) * dy) / norm).coerceIn(0f, 1f) else 0f
            val x = point.x - a.x - t * dx; val y = point.y - a.y - t * dy
            best = min(best, x * x + y * y)
        }
        return best
    }
}
