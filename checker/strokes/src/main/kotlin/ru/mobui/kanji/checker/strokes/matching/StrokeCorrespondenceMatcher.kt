package ru.mobui.kanji.checker.strokes.matching

import ru.mobui.kanji.checker.strokes.geometry.ShapePoint
import ru.mobui.kanji.checker.strokes.geometry.distanceToStroke

import kotlin.math.*

/** One-to-one geometric assignment. Ignores writing order and direction. */
class StrokeCorrespondenceMatcher {
    fun normalize(strokes: List<List<ShapePoint>>): List<List<ShapePoint>> {
        val points = strokes.flatten()
        if (points.isEmpty()) return strokes
        val left = points.minOf { it.x }; val top = points.minOf { it.y }
        val width = points.maxOf { it.x } - left; val height = points.maxOf { it.y } - top
        val side = max(width, height)
        if (side <= 0f) return strokes
        return strokes.map { s -> s.map { ShapePoint((it.x - left + (side - width) / 2) / side, (it.y - top + (side - height) / 2) / side) } }
    }

    fun assign(input: List<List<ShapePoint>>, reference: List<List<ShapePoint>>): List<Int>? {
        if (input.size != reference.size) return null
        val n = input.size
        if (n == 0) return emptyList()
        if (n == 1) return listOf(0)
        val costs = Array(n) { i -> DoubleArray(n) { j ->
            max(input[i].maxOf { distanceToStroke(it, reference[j]).first }, reference[j].maxOf { distanceToStroke(it, input[i]).first }).toDouble()
        } }
        // Hungarian assignment, O(strokeCount^3); deterministic ties retain reference order.
        val u = DoubleArray(n + 1); val v = DoubleArray(n + 1)
        val p = IntArray(n + 1); val way = IntArray(n + 1)
        for (i in 1..n) {
            p[0] = i
            val minimum = DoubleArray(n + 1) { Double.POSITIVE_INFINITY }
            val used = BooleanArray(n + 1)
            var j0 = 0
            do {
                used[j0] = true
                val i0 = p[j0]
                var delta = Double.POSITIVE_INFINITY
                var j1 = 0
                for (j in 1..n) if (!used[j]) {
                    val current = costs[i0 - 1][j - 1] - u[i0] - v[j]
                    if (current < minimum[j]) { minimum[j] = current; way[j] = j0 }
                    if (minimum[j] < delta) { delta = minimum[j]; j1 = j }
                }
                for (j in 0..n) if (used[j]) { u[p[j]] += delta; v[j] -= delta } else minimum[j] -= delta
                j0 = j1
            } while (p[j0] != 0)
            do { val j1 = way[j0]; p[j0] = p[j1]; j0 = j1 } while (j0 != 0)
        }
        val result = IntArray(n)
        for (j in 1..n) result[p[j] - 1] = j - 1
        return result.toList()
    }
}
