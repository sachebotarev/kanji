package ru.mobui.kanji.checker.strokes.writing

import kotlin.math.hypot
import kotlin.math.pow
import ru.mobui.kanji.checker.strokes.geometry.ShapePoint
import ru.mobui.kanji.domain.practice.StrokeCheckResult
import ru.mobui.kanji.domain.practice.StrokeOrderMismatch

/** Checks original normalized strokes using an existing geometric assignment. */
class StrokeWritingEvaluator {
    fun check(
        actual: List<List<ShapePoint>>,
        expected: List<List<ShapePoint>>,
        assignment: List<Int>,
        checkOrder: Boolean,
        checkDirection: Boolean,
    ): StrokeCheckResult {
        val order =
            if (!checkOrder) emptyList()
            else
                assignment.mapIndexedNotNull { i, j ->
                    if (i != j) StrokeOrderMismatch(i + 1, j + 1) else null
                }
        val reversed =
            if (!checkDirection) emptyList()
            else
                assignment.mapIndexedNotNull { i, j ->
                    val fractions = listOf(0f, FIRST_QUARTER, MIDPOINT, LAST_QUARTER, 1f)
                    val forward = fractions.sumOf {
                        distanceSquared(at(actual[i], it), at(expected[j], it)).toDouble()
                    }
                    val backward = fractions.sumOf {
                        distanceSquared(at(actual[i], it), at(expected[j], 1 - it)).toDouble()
                    }
                    if (backward + DIRECTION_EPSILON < forward) j + 1 else null
                }
        return StrokeCheckResult(orderMismatches = order, reversedStrokes = reversed)
    }

    private fun length(stroke: List<ShapePoint>): Float =
        stroke.zipWithNext().sumOf { (a, b) -> hypot(b.x - a.x, b.y - a.y).toDouble() }.toFloat()

    private fun distanceSquared(a: ShapePoint, b: ShapePoint): Float =
        (a.x - b.x).pow(2) + (a.y - b.y).pow(2)

    private fun at(stroke: List<ShapePoint>, fraction: Float): ShapePoint {
        val target = length(stroke) * fraction
        var walked = 0f
        for ((a, b) in stroke.zipWithNext()) {
            val segment = hypot(b.x - a.x, b.y - a.y)
            if (segment > 0f && walked + segment >= target) {
                val t = (target - walked) / segment
                return ShapePoint(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
            }
            walked += segment
        }
        return stroke.last()
    }
}

private const val FIRST_QUARTER = .25f
private const val MIDPOINT = .5f
private const val LAST_QUARTER = .75f
private const val DIRECTION_EPSILON = .000001
