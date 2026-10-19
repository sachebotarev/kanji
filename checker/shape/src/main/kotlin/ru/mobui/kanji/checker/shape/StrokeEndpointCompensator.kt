package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.domain.handwriting.ReferencePoint
import kotlin.math.*

data class EndpointCompensation(
    val strokes: List<List<ReferencePoint>>,
    val correctedEnds: Int,
    val removedLengthPercent: Float,
)

internal data class EndpointGeometryCompensation(
    val strokes: List<List<ShapePoint>>,
    val correctedEnds: Int,
    val removedLengthPercent: Float,
)

/** Conservative sharp-tail removal for comparison only. Never removes a whole stroke. */
class StrokeEndpointCompensator {
    fun compensate(strokes: List<List<ReferencePoint>>, maxLengthFraction: Float): EndpointCompensation {
        val result = compensateGeometry(strokes.map { stroke -> stroke.map { ShapePoint(it.x, it.y) } }, maxLengthFraction)
        return EndpointCompensation(
            result.strokes.map { stroke -> stroke.map { ReferencePoint(it.x, it.y) } },
            result.correctedEnds, result.removedLengthPercent,
        )
    }

    internal fun compensateGeometry(strokes: List<List<ShapePoint>>, maxLengthFraction: Float, protectedStrokes: Set<Int> = emptySet()): EndpointGeometryCompensation {
        require(maxLengthFraction.isFinite() && maxLengthFraction in 0f..0.05f)
        val points = strokes.flatten()
        if (points.isEmpty() || maxLengthFraction == 0f) return EndpointGeometryCompensation(strokes, 0, 0f)
        val side = max(points.maxOf { it.x } - points.minOf { it.x }, points.maxOf { it.y } - points.minOf { it.y })
        var corrected = 0
        var removed = 0f
        var total = 0f
        val result = strokes.mapIndexed { index, original ->
            val length = length(original)
            total += length
            if (index in protectedStrokes) return@mapIndexed original
            // This budget is shared by both ends: at least 88% of each stroke survives.
            val budget = min(side * maxLengthFraction, length * .12f)
            val first = trimEnd(original, budget)
            if (first.size != original.size) corrected++
            val used = (length - length(first)).coerceAtLeast(0f)
            val secondBudget = min(side * maxLengthFraction, (length * .12f - used).coerceAtLeast(0f))
            val secondReversed = trimEnd(first.asReversed(), secondBudget)
            if (secondReversed.size != first.size) corrected++
            val cleaned = secondReversed.asReversed().toList()
            removed += (length - length(cleaned)).coerceAtLeast(0f)
            cleaned
        }
        return EndpointGeometryCompensation(result, corrected, if (total > 0f) 100f * removed / total else 0f)
    }

    private fun trimEnd(points: List<ShapePoint>, budget: Float): List<ShapePoint> {
        if (points.size < 3 || budget <= 0.000001f) return points
        val end = points.last()
        var tailLength = 0f
        var cut = points.lastIndex
        for (i in points.lastIndex - 1 downTo 1) {
            tailLength += distance(points[i], points[i + 1])
            if (tailLength > budget) break
            if (tailLength <= 0.000001f) continue
            val joint = points[i]
            val before = pointBefore(points, i, budget * 2)
            val ax = joint.x - before.x; val ay = joint.y - before.y
            val bx = end.x - joint.x; val by = end.y - joint.y
            val denominator = hypot(ax, ay) * hypot(bx, by)
            // At least 75 degrees away from the preceding stable direction.
            if (denominator > 0.00000001f && (ax * bx + ay * by) / denominator <= .258819f) cut = i
        }
        return if (cut == points.lastIndex) points else points.take(cut + 1)
    }

    private fun pointBefore(points: List<ShapePoint>, index: Int, distance: Float): ShapePoint {
        var remaining = distance
        for (i in index downTo 1) {
            val a = points[i]; val b = points[i - 1]
            val segment = distance(a, b)
            if (segment >= remaining && segment > 0f) {
                val t = remaining / segment
                return ShapePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
            }
            remaining -= segment
        }
        return points.first()
    }
    private fun distance(a: ShapePoint, b: ShapePoint) = hypot(a.x - b.x, a.y - b.y)
    private fun length(points: List<ShapePoint>): Float = points.zipWithNext().sumOf { (a, b) -> distance(a, b).toDouble() }.toFloat()
}
