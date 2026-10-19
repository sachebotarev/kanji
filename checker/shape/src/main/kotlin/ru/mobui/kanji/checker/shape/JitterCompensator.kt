package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.domain.practice.JitterCompensation
import kotlin.math.*

internal data class SmoothedShape(val strokes: List<List<ShapePoint>>, val result: JitterCompensation)

/** Iterative Ramer–Douglas–Peucker simplification bounded by distance to retained segments. */
internal class JitterCompensator(private val tolerancePercent: Float) {
    init { require(tolerancePercent.isFinite() && tolerancePercent in 0f..5f) }

    fun compensate(strokes: List<List<ShapePoint>>, protectedStrokes: Set<Int> = emptySet()): SmoothedShape {
        val points = strokes.flatten()
        if (points.isEmpty() || tolerancePercent == 0f) return SmoothedShape(strokes, JitterCompensation())
        val side = max(points.maxOf { it.x } - points.minOf { it.x }, points.maxOf { it.y } - points.minOf { it.y })
        if (side <= 0f) return SmoothedShape(strokes, JitterCompensation())
        val tolerance = side * tolerancePercent / 100f
        var removed = 0
        var maxDeviation = 0f
        val simplified = strokes.mapIndexed { index, stroke ->
            if (index in protectedStrokes || stroke.size <= 2) return@mapIndexed stroke
            val keep = BooleanArray(stroke.size)
            keep[0] = true; keep[stroke.lastIndex] = true
            val stack = IntArray((stroke.size + 1) * 2)
            var stackTop = 0
            stack[stackTop++] = 0
            stack[stackTop++] = stroke.lastIndex
            while (stackTop > 0) {
                val end = stack[--stackTop]
                val start = stack[--stackTop]
                var furthest = start
                var deviation = 0f
                for (i in start + 1 until end) {
                    val d = distanceToSegment(stroke[i], stroke[start], stroke[end])
                    if (d > deviation) { deviation = d; furthest = i }
                }
                if (deviation > tolerance) {
                    keep[furthest] = true
                    stack[stackTop++] = start
                    stack[stackTop++] = furthest
                    stack[stackTop++] = furthest
                    stack[stackTop++] = end
                } else maxDeviation = max(maxDeviation, deviation)
            }
            val result = stroke.filterIndexed { i, _ -> keep[i] }
            removed += stroke.size - result.size
            result
        }
        return SmoothedShape(simplified, JitterCompensation(removed, maxDeviation / side * 100f, tolerancePercent))
    }

    private fun distanceToSegment(p: ShapePoint, a: ShapePoint, b: ShapePoint): Float {
        val dx = b.x - a.x; val dy = b.y - a.y
        val squared = dx * dx + dy * dy
        val t = if (squared == 0f) 0f else (((p.x - a.x) * dx + (p.y - a.y) * dy) / squared).coerceIn(0f, 1f)
        return hypot(p.x - a.x - t * dx, p.y - a.y - t * dy)
    }
}
