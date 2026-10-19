package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.domain.practice.StrokeRelation
import kotlin.math.*

internal class StrokeRelations {
    fun relation(a: List<ShapePoint>, b: List<ShapePoint>, gap: Float, protrusion: Float): StrokeRelation {
        val aLength = length(a); val bLength = length(b)
        var walkedA = 0f
        var touch = StrokeRelation.Separate
        for (i in 0 until a.size - 1) {
            val p = a[i]; val q = a[i + 1]
            val ax = q.x - p.x; val ay = q.y - p.y; val aSegment = hypot(ax, ay)
            var walkedB = 0f
            for (j in 0 until b.size - 1) {
                val r = b[j]; val s = b[j + 1]
                val bx = s.x - r.x; val by = s.y - r.y; val bSegment = hypot(bx, by)
                val denominator = ax * by - ay * bx
                if (aSegment > 0f && bSegment > 0f && abs(denominator) > aSegment * bSegment * PARALLEL_THRESHOLD_SIN) {
                    val dx = r.x - p.x; val dy = r.y - p.y
                    val t = (dx * by - dy * bx) / denominator
                    val u = (dx * ay - dy * ax) / denominator
                    if (t in 0f..1f && u in 0f..1f) {
                        val alongA = walkedA + t * aSegment; val alongB = walkedB + u * bSegment
                        val interiorA = min(alongA, aLength - alongA) > protrusion
                        val interiorB = min(alongB, bLength - alongB) > protrusion
                        if (interiorA && interiorB) return StrokeRelation.Crossing
                        if (!interiorA && interiorB) touch = StrokeRelation.TouchFirst
                        if (interiorA && !interiorB) touch = StrokeRelation.TouchSecond
                    }
                }
                walkedB += bSegment
            }
            walkedA += aSegment
        }
        if (touch != StrokeRelation.Separate) return touch
        for (end in listOf(a.first(), a.last())) {
            val (distance, position) = distanceToStroke(end, b)
            if (distance <= gap && min(position, bLength - position) > protrusion) return StrokeRelation.TouchFirst
        }
        for (end in listOf(b.first(), b.last())) {
            val (distance, position) = distanceToStroke(end, a)
            if (distance <= gap && min(position, aLength - position) > protrusion) return StrokeRelation.TouchSecond
        }
        return StrokeRelation.Separate
    }

    private fun length(stroke: List<ShapePoint>): Float {
        if (stroke.size < 2) return 0f
        var total = 0.0
        for (i in 0 until stroke.size - 1) {
            total += hypot(stroke[i + 1].x - stroke[i].x, stroke[i + 1].y - stroke[i].y)
        }
        return total.toFloat()
    }

    private companion object {
        private const val PARALLEL_THRESHOLD_SIN = 0.342f // sin(20°)
    }
}
