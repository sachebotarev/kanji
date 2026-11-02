package ru.mobui.kanji.checker.shape.geometry

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import ru.mobui.kanji.checker.strokes.geometry.ShapePoint
import ru.mobui.kanji.checker.strokes.geometry.distanceToStroke
import ru.mobui.kanji.domain.practice.StrokeRelation

internal class StrokeRelationClassifier {
    fun relation(
        a: List<ShapePoint>,
        b: List<ShapePoint>,
        gap: Float,
        protrusion: Float,
    ): StrokeRelation {
        val aLength = length(a)
        val bLength = length(b)
        val touch = intersections(segments(a), segments(b), aLength, bLength, protrusion)
        return when {
            touch != StrokeRelation.Separate -> touch
            endpointTouches(a, b, bLength, gap, protrusion) -> StrokeRelation.TouchFirst
            endpointTouches(b, a, aLength, gap, protrusion) -> StrokeRelation.TouchSecond
            else -> StrokeRelation.Separate
        }
    }

    private fun segmentRelation(
        position: Pair<Float, Float>,
        aLength: Float,
        bLength: Float,
        protrusion: Float,
    ): StrokeRelation? {
        val interiorA = min(position.first, aLength - position.first) > protrusion
        val interiorB = min(position.second, bLength - position.second) > protrusion
        return when {
            interiorA && interiorB -> StrokeRelation.Crossing
            !interiorA && interiorB -> StrokeRelation.TouchFirst
            interiorA && !interiorB -> StrokeRelation.TouchSecond
            else -> null
        }
    }

    private fun intersections(
        a: List<StrokeSegment>,
        b: List<StrokeSegment>,
        aLength: Float,
        bLength: Float,
        protrusion: Float,
    ): StrokeRelation {
        var touch = StrokeRelation.Separate
        for (first in a) {
            for (second in b) {
                val pos = intersection(first, second)
                val relation = if (pos != null) segmentRelation(pos, aLength, bLength, protrusion) else null
                if (relation == StrokeRelation.Crossing) return relation
                if (relation != null) touch = relation
            }
        }
        return touch
    }

    private fun intersection(a: StrokeSegment, b: StrokeSegment): Pair<Float, Float>? {
        val denominator = a.dx * b.dy - a.dy * b.dx
        val nonparallel =
            a.length > 0f &&
                b.length > 0f &&
                abs(denominator) > a.length * b.length * PARALLEL_THRESHOLD_SIN
        return if (nonparallel) {
            val dx = b.start.x - a.start.x
            val dy = b.start.y - a.start.y
            val t = (dx * b.dy - dy * b.dx) / denominator
            val u = (dx * a.dy - dy * a.dx) / denominator
            if (t in 0f..1f && u in 0f..1f) {
                (a.walked + t * a.length) to (b.walked + u * b.length)
            } else null
        } else null
    }

    private fun endpointTouches(
        a: List<ShapePoint>,
        b: List<ShapePoint>,
        bLength: Float,
        gap: Float,
        protrusion: Float,
    ): Boolean =
        listOf(a.first(), a.last()).any { end ->
            val (distance, position) = distanceToStroke(end, b)
            distance <= gap && min(position, bLength - position) > protrusion
        }

    private fun segments(stroke: List<ShapePoint>): List<StrokeSegment> {
        var walked = 0f
        return stroke.zipWithNext().map { (p, q) ->
            StrokeSegment(p, q, walked).also { walked += it.length }
        }
    }

    private fun length(stroke: List<ShapePoint>): Float {
        var total = 0.0
        for (i in 0 until stroke.size - 1) total +=
            hypot(stroke[i + 1].x - stroke[i].x, stroke[i + 1].y - stroke[i].y)
        return total.toFloat()
    }

    private companion object {
        private const val PARALLEL_THRESHOLD_SIN = 0.342f // sin(20°)
    }
}

private class StrokeSegment(val start: ShapePoint, end: ShapePoint, val walked: Float) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = hypot(dx, dy)
}
